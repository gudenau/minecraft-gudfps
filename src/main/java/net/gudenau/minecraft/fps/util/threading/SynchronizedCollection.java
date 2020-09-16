package net.gudenau.minecraft.fps.util.threading;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public final class SynchronizedCollection<T> implements Collection<T>{
    private final Collection<T> collection;
    private final Lock readLock;
    private final Lock writeLock;

    SynchronizedCollection(Collection<T> collection, ReadWriteLock lock){
        this.collection = collection;
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public int size(){
        readLock.lock();
        try{
            return collection.size();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty(){
        readLock.lock();
        try{
            return collection.isEmpty();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o){
        readLock.lock();
        try{
            return collection.contains(o);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator(){
        return new SynchronizedIterator<>(collection.iterator(), readLock);
    }

    @Override
    public Object[] toArray(){
        readLock.lock();
        try{
            return collection.toArray();
        }finally{
            readLock.unlock();
        }
    }

    @SuppressWarnings("SuspiciousToArrayCall" )
    @Override
    public <T1> T1[] toArray(T1[] a){
        readLock.lock();
        try{
            return collection.toArray(a);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean add(T t){
        writeLock.lock();
        try{
            return collection.add(t);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o){
        writeLock.lock();
        try{
            return collection.remove(o);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c){
        readLock.lock();
        try{
            return collection.containsAll(c);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c){
        writeLock.lock();
        try{
            return collection.addAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c){
        writeLock.lock();
        try{
            return collection.removeAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c){
        writeLock.lock();
        try{
            return collection.retainAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public void clear(){
        writeLock.lock();
        try{
            collection.clear();
        }finally{
            writeLock.unlock();
        }
    }
}
