package net.gudenau.minecraft.fps.util.threading;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

public final class SynchronizedSet<T> implements Set<T>{
    private final Set<T> set;
    private final Lock readLock;
    private final Lock writeLock;

    public SynchronizedSet(Set<T> set, ReadWriteLock lock){
        this.set = set;
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public int size(){
        readLock.lock();
        try{
            return set.size();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty(){
        readLock.lock();
        try{
            return set.isEmpty();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o){
        readLock.lock();
        try{
            return set.contains(o);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator(){
        return new SynchronizedIterator<>(set.iterator(), readLock);
    }

    @Override
    public Object[] toArray(){
        readLock.lock();
        try{
            return set.toArray();
        }finally{
            readLock.unlock();
        }
    }

    @SuppressWarnings("SuspiciousToArrayCall" )
    @Override
    public <T1> T1[] toArray(T1[] a){
        readLock.lock();
        try{
            return set.toArray(a);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean add(T t){
        writeLock.lock();
        try{
            return set.add(t);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o){
        writeLock.lock();
        try{
            return set.remove(o);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c){
        readLock.lock();
        try{
            return set.containsAll(c);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c){
        writeLock.lock();
        try{
            return set.addAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c){
        writeLock.lock();
        try{
            return set.retainAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c){
        writeLock.lock();
        try{
            return set.removeAll(c);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public void clear(){
        writeLock.lock();
        try{
            set.clear();
        }finally{
            writeLock.unlock();
        }
    }
}
