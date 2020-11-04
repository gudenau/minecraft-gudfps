package net.gudenau.minecraft.fps.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class UnsafeHelper{
    private static final MethodHandle allocateMemory;
    private static final MethodHandle putFloat;
    private static final MethodHandle getFloat;
    private static final MethodHandle ensureClassInitialized;
    
    static {
        try{
            Class<?> Unsafe = ReflectionHelper.loadClass("sun.misc.Unsafe");
            Field Unsafe$theUnsafe = Unsafe.getDeclaredField("theUnsafe");
            Unsafe$theUnsafe.setAccessible(true);
            Object theUnsafe = Unsafe$theUnsafe.get(null);
    
            allocateMemory = ReflectionHelper.findVirtual(
                Unsafe,
                theUnsafe,
                "allocateMemory",
                long.class, long.class
            );
    
            putFloat = ReflectionHelper.findVirtual(
                Unsafe,
                theUnsafe,
                "putFloat",
                void.class, long.class, float.class
            );
    
            getFloat = ReflectionHelper.findVirtual(
                Unsafe,
                theUnsafe,
                "getFloat",
                float.class, long.class
            );
    
            ensureClassInitialized = ReflectionHelper.findVirtual(
                Unsafe,
                theUnsafe,
                "ensureClassInitialized",
                void.class, Class.class
            );
        }catch(ReflectiveOperationException e){
            throw new RuntimeException(e);
        }
    }
    
    public static long allocateMemory(long size){
        try{
            return (long)allocateMemory.invokeExact(size);
        }catch(Throwable e){
            throw new RuntimeException(e);
        }
    }
    
    public static void putFloat(long pointer, float value){
        try{
            putFloat.invokeExact(pointer, value);
        }catch(Throwable e){
            throw new RuntimeException(e);
        }
    }
    
    public static float getFloat(long pointer){
        try{
            return (float)getFloat.invokeExact(pointer);
        }catch(Throwable e){
            throw new RuntimeException(e);
        }
    }
    
    public static void ensureClassInitialized(Class<?> klass){
        try{
            ensureClassInitialized.invokeExact(klass);
        }catch(Throwable e){
            throw new RuntimeException(e);
        }
    }
}
