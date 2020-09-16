package net.gudenau.minecraft.fps.util.threading;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class Synchronizer{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public final <T> T readLock(Supplier<T> task){
        readLock.lock();
        try{
            return task.get();
        }finally{
            readLock.unlock();
        }
    }

    public final <T> T writeLock(Supplier<T> task){
        writeLock.lock();
        try{
            return task.get();
        }finally{
            writeLock.unlock();
        }
    }

    public final void readLock(Runnable task){
        readLock.lock();
        try{
            task.run();
        }finally{
            readLock.unlock();
        }
    }

    public final void writeLock(Runnable task){
        writeLock.lock();
        try{
            task.run();
        }finally{
            writeLock.unlock();
        }
    }
}
