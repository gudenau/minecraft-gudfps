package net.gudenau.minecraft.fps;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.gudenau.minecraft.asm.api.v0.AsmInitializer;
import net.gudenau.minecraft.asm.api.v0.AsmRegistry;
import net.gudenau.minecraft.fps.transformer.*;
import net.gudenau.minecraft.fps.util.LibraryLoader;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.rpmalloc.RPmalloc;

public class GudFPSAsm implements AsmInitializer{
    @Override
    public void onInitializeAsm(){
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
    
        AsmRegistry registry = AsmRegistry.getInstance();
        registry.registerClassCache(new TransformerCache());
    
        GudFPS.Config config = GudFPS.CONFIG;
        boolean devel = false;
        try{
            devel = Files.isDirectory(Paths.get(GudFPSAsm.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }catch(URISyntaxException ignored){}
        //TODO https://discordapp.com/channels/507304429255393322/556200510592647168/683490818077884493
        //TODO funroll
        config.removeForEach.doIf(true, ()->registry.registerTransformer(new ForEachRemover()));
        config.precomputeConstants.doIf(true, ()->registry.registerTransformer(new ConstantPrecomputer()));
        config.optimizeMath.doIf(true, ()->registry.registerTransformer(new MathOptimizer()));
        if(devel){
            config.removeBlockPos.doIf(true, ()->registry.registerTransformer(new BlockPosRemover()));
            config.rpmalloc.doIf(true, ()->registry.registerTransformer(new RPmallocTransformer()));
        }
    }
}
