package net.gudenau.minecraft.fps.mixin;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.gudenau.minecraft.fps.GudFPS;
import net.gudenau.minecraft.fps.transformer.TransformerCache;
import net.gudenau.minecraft.fps.transformer.Transformers;
import net.gudenau.minecraft.fps.util.LibraryLoader;
import net.gudenau.minecraft.fps.util.ReflectionHelper;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.rpmalloc.RPmalloc;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public class Plugin implements IMixinConfigPlugin{
    static{
        if(!System.getProperty("os.arch").contains("64")){
            System.err.printf(
                "\n\n\n\n\n\n\n\n\n\n" +
                "WARNING:\n" +
                "It appears that you are running a 32 bit version of Java, things\n" +
                "are very likely to break!\n" +
                "\n\n\n\n\n\n\n\n\n\n"
            );
        }
    
        try{
            LibraryLoader.setup();
        }catch(IOException e){
            throw new RuntimeException("Failed to setup required libs", e);
        }
    
        if(GudFPS.CONFIG.rpmalloc.get()){
            Configuration.MEMORY_ALLOCATOR.set("rpmalloc");
            RPmalloc.rpmalloc_initialize();
        }
        
        TransformerCache.load();
        
        if(System.getProperty("gud_fps.dump") != null){
            try{
                delete(Paths.get(".", "gud_fps", "dump"));
            }catch(IOException ignored){}
        }
        
        ClassLoader classLoader = Plugin.class.getClassLoader();
        Transformers.setClassLoader(classLoader);
    
        try{
            // Get all of the non-public classes
            Class<? extends ClassLoader> KnotClassLoader = ReflectionHelper.loadClass(classLoader, "net.fabricmc.loader.launch.knot.KnotClassLoader");
            Class<?> KnotClassDelegate = ReflectionHelper.loadClass(classLoader, "net.fabricmc.loader.launch.knot.KnotClassDelegate");
            
            // Get the delegate
            MethodHandle KnotClassLoader$delegate$getter = ReflectionHelper.findGetter(KnotClassLoader, classLoader, "delegate", KnotClassDelegate);
            Object KnotClassLoader$delegate = KnotClassLoader$delegate$getter.invoke();

            // Get the proxy
            MethodHandle KnotClassDelegate$mixinTransformer$getter = ReflectionHelper.findGetter(KnotClassDelegate, KnotClassLoader$delegate, "mixinTransformer", FabricMixinTransformerProxy.class);
            FabricMixinTransformerProxy KnotClassDelegate$mixinTransformer = (FabricMixinTransformerProxy)KnotClassDelegate$mixinTransformer$getter.invoke();
    
            // Get the original env transformer
            MethodHandle MixinEnvironment$transformer$getter = ReflectionHelper.findStaticGetter(MixinEnvironment.class, "transformer", IMixinTransformer.class);
            IMixinTransformer originalTransformer = (IMixinTransformer)MixinEnvironment$transformer$getter.invoke();
            
            // Clear it out
            MethodHandle MixinEnvironment$transformer$setter = ReflectionHelper.findStaticSetter(MixinEnvironment.class, "transformer", IMixinTransformer.class);
            MixinEnvironment$transformer$setter.invoke((Object)null);
            
            // Create the transformer
            OurMixinTransformer ourTransformer = new OurMixinTransformer(KnotClassDelegate$mixinTransformer);
            
            // And restore the env transformer
            MixinEnvironment$transformer$setter.invoke(originalTransformer);
            
            // Set our transformer
            MethodHandle KnotClassDelegate$mixinTransformer$setter = ReflectionHelper.findSetter(KnotClassDelegate, KnotClassLoader$delegate, "mixinTransformer", FabricMixinTransformerProxy.class);
            KnotClassDelegate$mixinTransformer$setter.invoke(ourTransformer);
        }catch(Throwable e){
            throw new RuntimeException("Failed to hook into class loading", e);
        }
    }
    
    private static void delete(Path path) throws IOException{
        if(!Files.exists(path)){
            return;
        }
        if(Files.isDirectory(path)){
            for(Path child : Files.list(path).collect(Collectors.toSet())){
                delete(child);
            }
        }else{
            Files.delete(path);
        }
    }
    
    private static final class OurMixinTransformer extends FabricMixinTransformerProxy{
        private final FabricMixinTransformerProxy parent;
    
        private static final Set<String> BLACKLIST = new HashSet<>(Arrays.asList(
            "net.gudenau.minecraft.fps.",
            "org.objectweb.asm.",
            //"net.fabricmc.api.",
            "com.google.gson.",
            "org.lwjgl.",
            "it.unimi.dsi.fastutil."
        ));
        private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "net.gudenau.minecraft.fps.fixes."
        ));
        
        public OurMixinTransformer(FabricMixinTransformerProxy parent){
            this.parent = parent;
        }
    
        @Override
        public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass){
            for(String blacklisted : BLACKLIST){
                if(name.startsWith(blacklisted)){
                    for(String whitelistd : WHITELIST){
                        if(!name.startsWith(whitelistd)){
                            return parent.transformClassBytes(name, transformedName, basicClass);
                        }
                    }
                }
            }
            
            byte[] cachedClass = TransformerCache.getEntry(basicClass);
            if(cachedClass != null){
                ClassNode node = new ClassNode();
                new ClassReader(cachedClass).accept(node, 0);
                if(Transformers.shouldForceBootstrap(node)){
                    Transformers.forceBootstrap(node.name, cachedClass);
                    return null;
                }
                return cachedClass;
            }
            byte[] mixinClass = parent.transformClassBytes(name, transformedName, basicClass);
            byte[] transformedClass = Transformers.transform(name, transformedName, mixinClass == null ? basicClass : mixinClass);
            TransformerCache.putEntry(basicClass, transformedClass);
            return transformedClass;
        }
    }
    
    @Override
    public void onLoad(String mixinPackage){}
    
    @Override
    public String getRefMapperConfig(){
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){}
    
    @Override
    public List<String> getMixins(){
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
}
