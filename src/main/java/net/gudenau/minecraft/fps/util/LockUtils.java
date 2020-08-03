package net.gudenau.minecraft.fps.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;

public class LockUtils{
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
