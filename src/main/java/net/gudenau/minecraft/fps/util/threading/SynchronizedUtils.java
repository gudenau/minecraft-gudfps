package net.gudenau.minecraft.fps.util.threading;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

public final class SynchronizedUtils{
    public static <K, V> Map<K, V> synchronizedMap(Map<K, V> map){
        return new SynchronizedMap<>(map);
    }

    public static <V> Long2ObjectMap<V> synchronizedMap(Long2ObjectMap<V> map){
        return new Long2ObjectSynchronizedMap<>(map);
    }

    public static void withLock(Lock lock, Runnable task){
        try{
            lock.lock();
            task.run();
        }finally{
            lock.unlock();
        }
    }
    
    public static <T> T withLock(Lock lock, Supplier<T> task){
        try{
            lock.lock();
            return task.get();
        }finally{
            lock.unlock();
        }
    }
    
    public static void withWriteLock(ReadWriteLock lock, Runnable task){
        withLock(lock.writeLock(), task);
    }
    
    public static <T> T withWriteLock(ReadWriteLock lock, Supplier<T> task){
        return withLock(lock.writeLock(), task);
    }
    
    public static void withReadLock(ReadWriteLock lock, Runnable task){
        withLock(lock.readLock(), task);
    }
    
    public static <T> T withReadLock(ReadWriteLock lock, Supplier<T> task){
        return withLock(lock.readLock(), task);
    }
}
