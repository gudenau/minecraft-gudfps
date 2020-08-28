package net.gudenau.minecraft.fps.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionHelper{
    private static final MethodHandles.Lookup IMPL_LOOKUP;
    
    static{
        IMPL_LOOKUP = forceGetField(
            MethodHandles.Lookup.class,
            null,
            Modifier.STATIC | Modifier.FINAL,
            MethodHandles.Lookup.class
        );
    }
    
    @SuppressWarnings("unchecked")
    private static <O, T> T forceGetField(Class<O> owner, O instance, int mods, Class<T> type){
        for(Field field : owner.getDeclaredFields()){
            if(
                field.getModifiers() == mods &&
                field.getType() == type
            ){
                try{
                    field.setAccessible(true);
                    return (T)field.get(instance);
                }catch(ReflectiveOperationException ignored){}
            }
        }
        throw new RuntimeException(String.format(
            "Failed to get field from %s of type %s",
            owner.getName(),
            type.getName()
        ));
    }
    
    public static MethodHandle findGetter(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findGetter(owner, name, type);
    }
    
    public static <O, T extends O> MethodHandle findGetter(Class<T> owner, O instance, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findGetter(owner, name, type).bindTo(instance);
    }
    
    public static MethodHandle findStaticGetter(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findStaticGetter(owner, name, type);
    }
    
    public static MethodHandle findSetter(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findSetter(owner, name, type);
    }
    
    public static <O, T extends O> MethodHandle findSetter(Class<T> owner, O instance, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findSetter(owner, name, type).bindTo(instance);
    }
    
    public static MethodHandle findStaticSetter(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findStaticSetter(owner, name, type);
    }
    
    public static <T> Class<T> loadClass(String name) throws ReflectiveOperationException{
        return loadClass(ReflectionHelper.class.getClassLoader(), name);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadClass(ClassLoader loader, String name) throws ReflectiveOperationException{
        return (Class<T>)loader.loadClass(name);
    }
    
    public static <O, T extends O> MethodHandle findVirtual(Class<T> owner, O instance, String name, MethodType type) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findVirtual(owner, name, type).bindTo(instance);
    }
    
    public static MethodHandle findStatic(Class<?> owner, String name, Class<?> returnType, Class<?>... params) throws ReflectiveOperationException{
        return IMPL_LOOKUP.findStatic(
            owner,
            name,
            MethodType.methodType(returnType, params)
        );
    }
}
