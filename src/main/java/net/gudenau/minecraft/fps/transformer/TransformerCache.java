package net.gudenau.minecraft.fps.transformer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.gudenau.minecraft.asm.api.v0.ClassCache;
import net.gudenau.minecraft.asm.api.v0.Identifier;
import net.gudenau.minecraft.fps.GudFPS;
import net.gudenau.minecraft.fps.fixes.RPMallocFixes;
import net.gudenau.minecraft.fps.util.threading.SynchronizedUtils;
import net.gudenau.minecraft.fps.util.Stats;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.rpmalloc.RPmalloc;
import org.lwjgl.util.lz4.LZ4;
import org.lwjgl.util.xxhash.XXHash;

public class TransformerCache implements ClassCache{
    private static volatile boolean loaded = false;
    
    private static final Path CACHE_PATH = Paths.get("./gud_fps/cache");
    private static volatile Long2ObjectMap<ByteBuffer> cache;
    private static final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    private static Stats stats;
    
    private long seed;
    
    private static long calculateSeed(){
        long seed = 0xCAFEBABEDEADBEEFL;
        List<ModContainer> mods = new LinkedList<>(FabricLoader.getInstance().getAllMods());
        mods.sort(Comparator.comparing(m->m.getMetadata().getId()));
    
        MessageDigest digest;
        try{
            digest = MessageDigest.getInstance("SHA-1");
        }catch(NoSuchAlgorithmException e){
            // Spec says this can't happen.....
            System.err.println("JVM violates the MessageDigest spec.");
            e.printStackTrace();
            System.exit(0);
            return -1; // Make javac happy.
        }
    
        for(int i = 0; i < mods.size(); i++){
            ModMetadata meta = mods.get(i).getMetadata();
            digest.update((byte)i);
            digest.update(meta.getId().getBytes(StandardCharsets.UTF_8));
            digest.update((byte)~i);
            digest.update(meta.getVersion().getFriendlyString().getBytes(StandardCharsets.UTF_8));
        }
    
        byte[] result = digest.digest();
        for(int i = 0; i < result.length; i++){
            seed ^= (((long)result[i]) & 0xFF) << ((i & 7) << 3);
        }
        return seed;
    }
    
    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "cache");
    }
    
    @Override
    public void load(){
        seed = calculateSeed();
        
        stats = Stats.getStats("Cache");
        
        cache = new Long2ObjectOpenHashMap<>();
        
        if(GudFPS.CONFIG.rpmalloc.get()){
            new Thread(RPMallocFixes.runnable(this::loadCache), "CacheLoader").start();
        }else{
            new Thread(this::loadCache, "CacheLoader").start();
        }
    }
    
    private void loadCache(){
        SynchronizedUtils.withWriteLock(cacheLock, ()->{
            try{
                if(Files.exists(CACHE_PATH)){
                    try(RandomAccessFile file = new RandomAccessFile(CACHE_PATH.toFile(), "r")){
                        MappedByteBuffer fileBuffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                        fileBuffer.order(ByteOrder.LITTLE_ENDIAN);
                
                        long savedSeed = fileBuffer.getLong();
                        if(savedSeed != seed){
                            return;
                        }
                
                        int config = fileBuffer.getInt();
                        if(config != GudFPS.CONFIG.hashCode()){
                            return;
                        }
                
                        int cacheSize = fileBuffer.getInt();
                        stats.addStat("loaded", cacheSize);
                        for(int i = 0; i < cacheSize; i++){
                            long hash = fileBuffer.getLong();
                            int rawSize = fileBuffer.getInt();
                            int compressedSize = fileBuffer.getInt();
                            fileBuffer.limit(compressedSize + fileBuffer.position());
                            try{
                                ByteBuffer buffer = MemoryUtil.memAlloc(rawSize);
                                int result = LZ4.LZ4_decompress_safe(fileBuffer, buffer);
                                if(result < 0){
                                    throw new RuntimeException("Failed to decompress cache (" + result + ")");
                                }else if(result != rawSize){
                                    throw new RuntimeException("Failed to decompress cache (bad size)");
                                }
                                cache.put(hash, buffer);
                            }finally{
                                fileBuffer.position(fileBuffer.limit());
                                fileBuffer.limit(fileBuffer.capacity());
                            }
                        }
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }finally{
                loaded = true;
            }
        });
    }
    
    @Override
    public void save(){
        if(GudFPS.CONFIG.rpmalloc.get()){
            RPmalloc.rpmalloc_thread_initialize();
        }
        try{
            Path parent = CACHE_PATH.getParent();
            if(!Files.exists(parent)){
                Files.createDirectories(parent);
            }
            if(!Files.exists(CACHE_PATH)){
                Files.createFile(CACHE_PATH);
            }
            final Lock lock = cacheLock.writeLock();
            lock.lock();
            
            byte[] array = new byte[Long.BYTES * 2];
            ByteBuffer arrayBuffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN);
            
            try(OutputStream outputStream = Files.newOutputStream(CACHE_PATH)){
                WritableByteChannel channel = Channels.newChannel(outputStream);
                
                arrayBuffer.putLong(seed);
                arrayBuffer.putInt(GudFPS.CONFIG.hashCode());
                arrayBuffer.putInt(cache.size());
                stats.addStat("saved", cache.size());
                outputStream.write(array, 0, arrayBuffer.position());
                arrayBuffer.clear();
    
                ByteBuffer compressedBuffer = MemoryUtil.memAlloc(1024);
                try{
                    for(Long2ObjectMap.Entry<ByteBuffer> entry : cache.long2ObjectEntrySet()){
                        ByteBuffer buffer = entry.getValue();
                        try{
                            compressedBuffer = MemoryUtil.memRealloc(
                                compressedBuffer,
                                Math.max(LZ4.LZ4_compressBound(buffer.capacity()), compressedBuffer.capacity())
                            );
                            int compressedSize = LZ4.LZ4_compress_default(buffer, compressedBuffer);
    
                            arrayBuffer.putLong(entry.getLongKey());
                            arrayBuffer.putInt(buffer.capacity());
                            arrayBuffer.putInt(compressedSize);
                            outputStream.write(array, 0, arrayBuffer.position());
                            arrayBuffer.clear();
                            
                            compressedBuffer.limit(compressedSize);
                            channel.write(compressedBuffer);
                            compressedBuffer.clear();
                        }finally{
                            MemoryUtil.memFree(buffer);
                        }
                    }
                }finally{
                    MemoryUtil.memFree(compressedBuffer);
                }
                
                cache.clear();
            }finally{
                lock.unlock();
            }
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            if(GudFPS.CONFIG.rpmalloc.get()){
                RPmalloc.rpmalloc_thread_finalize();
            }
        }
    }
    
    @Override
    public Optional<byte[]> getEntry(byte[] original){
        if(!loaded || original == null){
            return Optional.empty();
        }
        
        ByteBuffer originalBuffer = MemoryUtil.memAlloc(original.length);
        try{
            originalBuffer.put(original).position(0);
            long hash = XXHash.XXH64(originalBuffer, seed);
            return SynchronizedUtils.withReadLock(cacheLock, ()->{
                ByteBuffer modifiedBuffer = cache.get(hash);
                if(modifiedBuffer == null){
                    stats.incrementStat("misses");
                    return Optional.empty();
                }
                stats.incrementStat("hits");
                byte[] modified = new byte[modifiedBuffer.capacity()];
                modifiedBuffer.get(modified).position(0);
                return Optional.of(modified);
            });
        }finally{
            MemoryUtil.memFree(originalBuffer);
        }
    }
    
    @Override
    public void putEntry(byte[] original, byte[] modified){
        if(original == null || modified == null){
            return;
        }
    
        stats.incrementStat("additions");
        
        ByteBuffer originalBuffer = MemoryUtil.memAlloc(original.length);
        try{
            ByteBuffer modifiedBuffer = MemoryUtil.memAlloc(modified.length);
            modifiedBuffer.put(modified).position(0);
            
            originalBuffer.put(original).position(0);
            long hash = XXHash.XXH64(originalBuffer, seed);
    
            SynchronizedUtils.withWriteLock(cacheLock, ()->
                cache.put(hash, modifiedBuffer)
            );
        }finally{
            MemoryUtil.memFree(originalBuffer);
        }
    }
}
