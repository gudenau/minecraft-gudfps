package net.gudenau.minecraft.fps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.gudenau.minecraft.fps.util.Stats;
import net.minecraft.block.Block;

public class GudFPS implements ModInitializer{
    public static final Config CONFIG = loadConfig();
    
    private final Stats stats = Stats.getStats("Force Loader");
    
    @Override
    public void onInitialize(){
        System.err.println("Things will break, GudFPS is not meant to be 100% stable. :3");
        
        if(CONFIG.forceLoadClasses.get()){
            System.err.println("Force loading Minecraft classes, this might take a moment...");
            try{
                Path path = Paths.get(Block.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if(Files.isDirectory(path)){
                    forceLoadDir(path);
                }else{
                    forceLoadJar(path);
                }
            }catch(IOException | URISyntaxException ignored){
                System.err.println("Failed to force load classes");
            }
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
            classLoader.loadClass(className.substring(0, className.length() - 6).replaceAll("/", "."));
            stats.incrementStat("loaded");
        }catch(ClassNotFoundException ignored){
            stats.incrementStat("failed");
        }
    }
    
    private static Config loadConfig(){
        Path parentPath = FabricLoader.getInstance().getConfigDirectory().toPath().resolve("gud");
        Path path = parentPath.resolve("fps.conf");
        
        try{
            if(!Files.exists(parentPath)){
                Files.createDirectories(parentPath);
            }
            if(!Files.exists(path)){
                Files.createFile(path);
            }
            
            Map<String, String> optionMap = new HashMap<>();
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))){
                for(String line = reader.readLine(); line != null; line = reader.readLine()){
                    String[] split = line.split("=", 2);
                    if(split.length == 2){
                        optionMap.put(split[0], split[1]);
                    }
                }
            }
            
            int hash = optionMap.hashCode();
            Config config = new Config(optionMap);
            if(hash != optionMap.hashCode()){
                List<Map.Entry<String, String>> entryList = new ArrayList<>(optionMap.entrySet());
                entryList.sort((e1, e2)->e1.getKey().compareToIgnoreCase(e2.getKey()));
                try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path)))){
                    for(Map.Entry<String, String> entry : entryList){
                        writer.write(entry.getKey());
                        writer.write("=");
                        writer.write(entry.getValue());
                        writer.newLine();
                    }
                }
            }
            return config;
        }catch(IOException e){
            new RuntimeException("Failed to process configs, using defaults").printStackTrace();
            return new Config(Collections.emptyMap());
        }
    }
    
    public static final class Config{
        public final Option<Boolean> removeForEach = new BooleanOption("removeForEach", true);
        public final Option<Boolean> removeBlockPos = new BooleanOption("removeBlockPos", true);
        public final Option<Boolean> precomputeConstants = new BooleanOption("precomputeConstants", true);
        public final Option<Boolean> optimizeMath = new BooleanOption("optimizeMath", true);
        public final Option<Boolean> rpmalloc = new BooleanOption("rpmalloc", true);
        public final Option<Boolean> verify = new BooleanOption("verify", false);
        public final Option<Boolean> forceLoadClasses = new BooleanOption("forceLoadClasses", true);
    
        private Config(Map<String, String> options){
            try{
                Map<String, String> realOptions = new HashMap<>();
                for(Field field : getClass().getDeclaredFields()){
                    Option<?> option = (Option<?>)field.get(this);
                    String value = options.get(option.getName());
                    if(value != null){
                        try{
                            option.set(value);
                        }catch(Throwable ignored){}
                    }
                    realOptions.put(option.getName(), String.valueOf(option.get()));
                }
                options.clear();
                realOptions.forEach(options::put);
            }catch(ReflectiveOperationException e){
                throw new RuntimeException(":-(", e);
            }
        }
    
        @Override
        public boolean equals(Object o){
            if(this == o){
                return true;
            }
            if(o == null || getClass() != o.getClass()){
                return false;
            }
            Config config = (Config)o;
            return Objects.equals(removeForEach, config.removeForEach) &&
                   Objects.equals(removeBlockPos, config.removeBlockPos) &&
                   Objects.equals(precomputeConstants, config.precomputeConstants) &&
                   Objects.equals(rpmalloc, config.rpmalloc) &&
                   Objects.equals(verify, config.verify) &&
                   Objects.equals(optimizeMath, config.optimizeMath);
        }
    
        @Override
        public int hashCode(){
            return
                (removeForEach.get() ? (1 << 0) : 0) |
                (removeBlockPos.get() ? (1 << 1) : 0) |
                (precomputeConstants.get() ? (1 << 2) : 0) |
                (rpmalloc.get() ? (1 << 3) : 0) |
                (optimizeMath.get() ? (1 << 4) : 0);
                // Bit 5 is reserved
        }
    }
    
    public static class Option<T>{
        private final String name;
        private final Function<String, T> parser;
        private T value;
    
        private Option(String name, T value, Function<String, T> parser){
            this.name = name;
            this.value = value;
            this.parser = parser;
        }
        
        public T get(){
            return value;
        }
        
        private String getName(){
            return name;
        }
        
        private void set(String value){
            this.value = parser.apply(value);
        }
        
        public void doIf(T value, Runnable task){
            if(this.value.equals(value)){
                task.run();
            }
        }
    
        @Override
        public boolean equals(Object o){
            if(this == o){
                return true;
            }
            if(o == null || getClass() != o.getClass()){
                return false;
            }
            Option<?> option = (Option<?>)o;
            return Objects.equals(name, option.name) &&
                   Objects.equals(value, option.value);
        }
    
        @Override
        public int hashCode(){
            return Objects.hash(name, value);
        }
    }
    
    private static final class BooleanOption extends Option<Boolean>{
        private BooleanOption(String name, boolean value){
            super(name, value, Boolean::parseBoolean);
        }
    }
}
