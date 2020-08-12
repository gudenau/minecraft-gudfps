package net.gudenau.minecraft.fps.transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.gudenau.minecraft.fps.GudFPS;
import net.gudenau.minecraft.fps.util.Stats;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class Transformers{
    private static final List<Transformer> TRANSFORMERS = new ArrayList<>();
    
    private static final boolean dumpClasses = GudFPS.CONFIG.dump.get();
    private static final boolean forceDumpClasses = GudFPS.CONFIG.forceDump.get();
    private static final boolean verifyClasses = GudFPS.CONFIG.verify.get();
    private static final Stats transformerStats = Stats.getStats("Transformer");
    private static final Stats dumperStats = Stats.getStats("Dump");
    
    static {
        GudFPS.Config config = GudFPS.CONFIG;
        boolean devel = false;
        try{
            devel = Files.isDirectory(Paths.get(Transformers.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }catch(URISyntaxException ignored){}
        //TODO https://discordapp.com/channels/507304429255393322/556200510592647168/683490818077884493
        //TODO funroll
        config.removeForEach.doIf(true, ()->register(new ForEachRemover()));
        config.precomputeConstants.doIf(true, ()->register(new ConstantPrecomputer()));
        config.optimizeMath.doIf(true, ()->register(new MathOptimizer()));
        if(devel){
            config.removeBlockPos.doIf(true, ()->register(new BlockPosRemover()));
            config.rpmalloc.doIf(true, ()->register(new RPmallocTransformer()));
        }
    }
    
    private static void register(Transformer transformer){
        TRANSFORMERS.add(transformer);
    }
    
    public static byte[] transform(String name, String transformedName, byte[] basicClass){
        if(basicClass == null){
            return null;
        }
        
        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        
        Set<Transformer> usedTransformers = new HashSet<>();
        Transformer.Flags flags = new Transformer.Flags();
        for(Transformer transformer : TRANSFORMERS){
            if(transformer.transform(classNode, flags)){
                usedTransformers.add(transformer);
            }
        }
        
        if(usedTransformers.isEmpty()){
            if(dumpClasses && forceDumpClasses){
                dumperStats.incrementStat("forced");
                dumpClass(classNode, basicClass);
            }
            return basicClass;
        }
        
        if(name.equals(transformedName)){
            System.out.printf("Class %s was transformed by:\n", name);
        }else{
            System.out.printf("Class %s (%s) was transformed by:\n", name, transformedName);
        }
        for(Transformer transformer : usedTransformers){
            System.out.printf("    %s\n", transformer.getClass().getSimpleName());
        }
        for(Transformer transformer : usedTransformers){
            transformerStats.incrementStat(transformer.getClass().getSimpleName());
        }
        
        ClassWriter classWriter = new ClassWriter(flags.getFlags());
        classNode.accept(classWriter);
        byte[] newClass = classWriter.toByteArray();
        if(dumpClasses){
            dumperStats.incrementStat("modified");
            dumpClass(classNode, newClass);
        }
        if(verifyClasses){
            System.err.printf("Verifying %s\n", classNode.name);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            boolean exception = false;
            try{
                CheckClassAdapter.verify(new ClassReader(newClass), false, new PrintWriter(stream));
            }catch(Throwable ignored){
                exception = true;
            }
            if(exception || stream.size() > 0){
                CheckClassAdapter.verify(new ClassReader(newClass), true, new PrintWriter(System.err));
                System.exit(0);
            }
        }
        
        return newClass;
    }
    
    private static final ExecutorService DUMP_SERVICE;
    static {
        if(dumpClasses | forceDumpClasses){
            DUMP_SERVICE = Executors.newFixedThreadPool(1);
            Runtime.getRuntime().addShutdownHook(new Thread(DUMP_SERVICE::shutdown, "Dump Service Closer"));
        }else{
            DUMP_SERVICE = null;
        }
    }
    
    private static void dumpClass(ClassNode node, byte[] bytecode){
        DUMP_SERVICE.submit(()->{
            try{
                Path dumpPath = Paths.get(".", "gud_fps", "dump", node.name + ".class");
                Path parent = dumpPath.getParent();
                if(!Files.exists(parent)){
                    try{
                        Files.createDirectories(parent);
                    }catch(IOException ignored){
                    }
                }
                if(!Files.exists(dumpPath)){
                    try{
                        Files.createFile(dumpPath);
                    }catch(IOException ignored){
                    }
                }
                try(OutputStream stream = Files.newOutputStream(dumpPath)){
                    stream.write(bytecode);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    }
}
