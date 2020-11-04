package net.gudenau.minecraft.fps.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.gudenau.minecraft.fps.cpu.CpuFeatures;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class Config{
    public static Config loadConfig(){
        Path parentPath = FabricLoader.getInstance().getConfigDir().resolve("gud");
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

    public final Option<Boolean> removeForEach = new BooleanOption("removeForEach", true);
    public final Option<Boolean> removeBlockPos = new BooleanOption("removeBlockPos", true);
    public final Option<Boolean> precomputeConstants = new BooleanOption("precomputeConstants", true);
    public final Option<Boolean> optimizeMath = new BooleanOption("optimizeMath", true);
    public final Option<Boolean> rpmalloc = new BooleanOption("rpmalloc", true);
    public final Option<Boolean> verify = new BooleanOption("verify", false);
    public final Option<Boolean> forceLoadClasses = new BooleanOption("forceLoadClasses", true);
    public final Option<Boolean> disableRendererThreadChecks = new ClientBooleanOption("disableRendererThreadChecks", true);
    public final Option<Boolean> identifierPool = new BooleanOption("identifierPool", true);
    //public final Option<Boolean> avx = new PredicatedBooleanOption("avx", false, ()-> CpuFeatures.hasFeature(CpuFeatures.Feature.AVX));
    public final Option<Boolean> avx = new BooleanOption("avx", false);

    private Config(Map<String, String> options){
        try{
            boolean client = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
            Map<String, String> realOptions = new HashMap<>();
            for(Field field : getClass().getDeclaredFields()){
                Option<?> option = (Option<?>)field.get(this);
                if(!client && option instanceof OptionalOption){
                    ((OptionalOption)option).check();
                    continue;
                }
                String value = options.get(option.getName());
                if(value != null){
                    try{
                        option.set(value);
                    }catch(Throwable ignored){}
                }
                if(option instanceof DependentOption){
                    realOptions.put(option.getName(), String.valueOf(((DependentOption<?>)option).getTrue()));
                }else{
                    realOptions.put(option.getName(), String.valueOf(option.get()));
                }
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
            Objects.equals(optimizeMath, config.optimizeMath) &&
            Objects.equals(disableRendererThreadChecks, config.disableRendererThreadChecks) &&
            Objects.equals(identifierPool, config.identifierPool) &&
            Objects.equals(avx, config.avx);
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    @Override
    public int hashCode(){
        return
            (removeForEach.get() ? (1 << 0) : 0) |
                (removeBlockPos.get() ? (1 << 1) : 0) |
                (precomputeConstants.get() ? (1 << 2) : 0) |
                (rpmalloc.get() ? (1 << 3) : 0) |
                (optimizeMath.get() ? (1 << 4) : 0) |
                (disableRendererThreadChecks.get() ? (1 << 5) : 0) |
                (identifierPool.get() ? (1 << 6) : 0) |
                (avx.get() ? (1 << 7) : 0);
    }

    private interface OptionalOption{
        void check();
    }

    private interface DependentOption<T>{
        T getTrue();
    }

    public static class Option<T>{
        private final String name;
        private final Function<String, T> parser;
        T value;

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
            if(get().equals(value)){
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

    private static class BooleanOption extends Option<Boolean>{
        private BooleanOption(String name, boolean value){
            super(name, value, Boolean::parseBoolean);
        }
    }

    private static class ClientBooleanOption extends BooleanOption implements OptionalOption{
        private ClientBooleanOption(String name, boolean value){
            super(name, value);
        }

        @Override
        public void check(){
            value = false;
        }
    }

    private static class PredicatedBooleanOption extends BooleanOption implements OptionalOption{
        private final BooleanSupplier predicate;

        private PredicatedBooleanOption(String name, boolean value, BooleanSupplier predicate){
            super(name, value);
            this.predicate = predicate;
        }

        @Override
        public void check(){
            if(!predicate.getAsBoolean()){
                value = false;
            }
        }
    }

    private static class DependentBooleanOption extends BooleanOption implements DependentOption<Boolean>{
        private final Option<Boolean>[] dependencies;

        @SafeVarargs
        private DependentBooleanOption(String name, boolean defaultValue, Option<Boolean>... dependencies){
            super(name, defaultValue);
            this.dependencies = dependencies;
        }

        @Override
        public Boolean getTrue(){
            return super.get();
        }

        @Override
        public Boolean get(){
            for(Option<Boolean> dependency : dependencies){
                if(!dependency.get()){
                    return false;
                }
            }
            return super.get();
        }
    }
}
