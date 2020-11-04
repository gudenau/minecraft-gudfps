package net.gudenau.minecraft.fps;

import net.fabricmc.api.ModInitializer;
import net.gudenau.minecraft.fps.util.Config;
import net.gudenau.minecraft.fps.util.Stats;
import net.gudenau.minecraft.fps.util.UnsafeHelper;
import net.minecraft.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class GudFPS implements ModInitializer{
    public static final Config CONFIG = Config.loadConfig();
    
    private static final Logger LOGGER = LogManager.getLogger("gud_fps");
    public static MethodHandle init = null;

    private final Stats stats = Stats.getStats("Force Loader");
    
    @Override
    public void onInitialize(){
        LOGGER.fatal("Things may break, GudFPS is not meant to be 100% stable. :3");
        
//        CpuFeatures.Vendor vendor = CpuFeatures.getVendor();
//        LOGGER.info("The CPU appears to be from {}", vendor == null ? "UNKNOWN" : vendor.name());
//
//        Set<CpuFeatures.Feature> features = CpuFeatures.getFeatures();
//        LOGGER.info("Cpu features:");
//        for(CpuFeatures.Feature feature : features){
//            LOGGER.info("    {}", feature.name());
//        }
        
        if(CONFIG.forceLoadClasses.get()){
            LOGGER.warn("Force loading Minecraft classes, this might take a moment...");
            try{
                Path path = Paths.get(Block.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if(Files.isDirectory(path)){
                    forceLoadDir(path);
                }else{
                    forceLoadJar(path);
                }
            }catch(IOException | URISyntaxException ignored){
                LOGGER.fatal("Failed to force load classes");
            }
        }

        if(init != null){
            try{
                init.invokeExact();
            }catch(Throwable ignored){}
        }
    }
    
    private void forceLoadDir(Path path) throws IOException{
        forceLoadDir(path, path);
    }
    
    private void forceLoadDir(Path root, Path current) throws IOException{
        if(Files.isDirectory(current)){
            Iterator<Path> iterator = Files.list(root).iterator();
            while(iterator.hasNext()){
                forceLoadDir(root, iterator.next());
            }
        }else{
            if(current.getFileName().endsWith(".class")){
                current = root.relativize(current);
                tryLoad(current.toString());
            }
        }
    }
    
    private void forceLoadJar(Path path) throws IOException{
        try(JarFile jar = new JarFile(path.toFile())){
            Enumeration<JarEntry> iterator = jar.entries();
            while(iterator.hasMoreElements()){
                JarEntry entry = iterator.nextElement();
                String name = entry.getName();
                if(!entry.isDirectory() && name.endsWith(".class")){
                    tryLoad(name);
                }
            }
        }
    }

    private static final ClassLoader classLoader = GudFPS.class.getClassLoader();
    private void tryLoad(String className){
        try{
            Class<?> klass = classLoader.loadClass(className.substring(0, className.length() - 6).replaceAll("/", "."));
            ensureClassInitialized(klass);
            stats.incrementStat("loaded");
        }catch(ClassNotFoundException ignored){
            stats.incrementStat("failed");
        }
    }
    
    private void ensureClassInitialized(Class<?> klass){
        Class<?> parent = klass.getSuperclass();
        if(parent != null){
            ensureClassInitialized(parent);
        }
        for(Class<?> iface : klass.getInterfaces()){
            ensureClassInitialized(iface);
        }
        UnsafeHelper.ensureClassInitialized(klass);
    }
}
