package net.gudenau.minecraft.fps.util.threading;

import java.util.Iterator;
import java.util.concurrent.locks.Lock;

public final class SynchronizedIterator<T> implements Iterator<T>{
    private final Iterator<T> iterator;
    private final Lock lock;

    public SynchronizedIterator(Iterator<T> iterator, Lock lock){
        this.iterator = iterator;
        this.lock = lock;
        lock.lock();
    }

    @Override
    public boolean hasNext(){
        if(iterator.hasNext()){
            return true;
        }else{
            lock.unlock();
            return false;
        }
    }

    @Override
    public T next(){
        return iterator.next();
    }
}
