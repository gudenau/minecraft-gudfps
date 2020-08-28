package net.gudenau.minecraft.fps.transformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandle;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.gudenau.minecraft.fps.GudFPS;
import net.gudenau.minecraft.fps.util.AsmUtils;
import net.gudenau.minecraft.fps.util.LockUtils;
import net.gudenau.minecraft.fps.util.ReflectionHelper;
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
    private static ClassLoader classLoader;
    
    private static final String ANNOTATION_FORCE_BOOTLOADER = "Lnet/gudenau/minecraft/fps/util/annotation/ForceBootloader;";
    
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
        if(doesClassExist("jdk.internal.vm.annotation.ForceInline")){
            config.enableInline.doIf(true, ()->register(new ForceInlineTransformer()));
        }
        if(devel){
            config.removeBlockPos.doIf(true, ()->register(new BlockPosRemover()));
            config.rpmalloc.doIf(true, ()->register(new RPmallocTransformer()));
        }
    }
    private static boolean doesClassExist(String name){
        try{
            ClassLoader.getSystemClassLoader().loadClass(name);
            return true;
        }catch(ClassNotFoundException e){
            return false;
        }
    }
    
    private static void register(Transformer transformer){
        TRANSFORMERS.add(transformer);
    }
    
    public static byte[] transform(String name, String transformedName, byte[] basicClass){
        try{
            return doTransform(name, transformedName, basicClass);
        }catch(Throwable t){
            System.err.printf("Failed to transform %s (%s)\n", name, transformedName);
            t.printStackTrace();
            System.exit(0);
            throw new RuntimeException();
        }
    }
    
    private static byte[] doTransform(String name, String transformedName, byte[] basicClass){
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
    
            if(shouldForceBootstrap(classNode)){
                forceBootstrap(classNode.name, basicClass);
                return null;
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
        
        ClassWriter classWriter = new ClassWriter(flags.getFlags()){
            @Override
            protected ClassLoader getClassLoader(){
                return classLoader;
            }
        };
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
        
        if(shouldForceBootstrap(classNode)){
            forceBootstrap(classNode.name, newClass);
            return null;
        }
        
        return newClass;
    }
    
    private static final MethodHandle ClassLoader$defineClass1;
    static{
        try{
            ClassLoader$defineClass1 = ReflectionHelper.findStatic(
                ClassLoader.class,
                "defineClass1",
                Class.class,
                ClassLoader.class, String.class, byte[].class, int.class, int.class, ProtectionDomain.class, String.class
            );
        }catch(ReflectiveOperationException e){
            e.printStackTrace();
            System.exit(0);
            throw new RuntimeException(e);
        }
    }
    
    private static final Set<Class<?>> BOOTSTRAP_CLASSES = new HashSet<>();
    private static final ReadWriteLock BOOTSTRAP_CLASSES_LOCK = new ReentrantReadWriteLock();
    public static void forceBootstrap(String name, byte[] bytecode){
        //Class<?> klass = Jni.DefineClass(name, null, bytecode);
        try{
            Class<?> klass = (Class<?>)ClassLoader$defineClass1.invokeExact(
                (ClassLoader)null,
                name,
                bytecode, 0, bytecode.length,
                (ProtectionDomain)null,
                (String)null
            );
            LockUtils.withWriteLock(BOOTSTRAP_CLASSES_LOCK, ()->BOOTSTRAP_CLASSES.add(klass));
        }catch(Throwable throwable){
            new RuntimeException("Failed to load " + name, throwable).printStackTrace();
            System.exit(0);
            throw new RuntimeException("Failed to load " + name, throwable);
        }
    }
    
    public static boolean shouldForceBootstrap(ClassNode classNode){
        return AsmUtils.hasAnnotation(classNode, ANNOTATION_FORCE_BOOTLOADER);
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
    
    public static void setClassLoader(ClassLoader classLoader){
        Transformers.classLoader = classLoader;
    }
}
