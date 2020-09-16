package net.gudenau.minecraft.fps.util.threading;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.*;

public final class Long2ObjectSynchronizedMap<V> implements Long2ObjectMap<V>{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final Long2ObjectMap<V> map;

    public Long2ObjectSynchronizedMap(Long2ObjectMap<V> map){
        this.map = map;
    }

    @Override
    public int size(){
        readLock.lock();
        try{
            return map.size();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void clear(){
        writeLock.lock();
        try{
            map.clear();
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean isEmpty(){
        readLock.lock();
        try{
            return map.isEmpty();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value){
        readLock.lock();
        try{
            return map.containsValue(value);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends Long, ? extends V> m){
        writeLock.lock();
        try{
            map.putAll(m);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public void defaultReturnValue(V rv){
        writeLock.lock();
        try{
            map.defaultReturnValue(rv);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V defaultReturnValue(){
        readLock.lock();
        try{
            return map.defaultReturnValue();
        }finally{
            readLock.unlock();
        }
    }

    //TODO
    @Override
    public ObjectSet<Entry<V>> long2ObjectEntrySet(){
        return map.long2ObjectEntrySet();
    }

    //TODO
    @Override
    public LongSet keySet(){
        return map.keySet();
    }

    //TODO
    @Override
    public ObjectCollection<V> values(){
        return map.values();
    }

    @Override
    public void forEach(BiConsumer<? super Long, ? super V> action){
        readLock.lock();
        try{
            map.forEach(action);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super Long, ? super V, ? extends V> function){
        writeLock.lock();
        try{
            map.replaceAll(function);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V put(long key, V value){
        writeLock.lock();
        try{
            return map.put(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V get(long key){
        readLock.lock();
        try{
            return map.get(key);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public V remove(long key){
        writeLock.lock();
        try{
            return map.remove(key);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean containsKey(long key){
        readLock.lock();
        try{
            return map.containsKey(key);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public V getOrDefault(long key, V defaultValue){
        readLock.lock();
        try{
            return map.getOrDefault(key, defaultValue);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public V putIfAbsent(long key, V value){
        V defaultValue;
        V existing;
        readLock.lock();
        try{
            defaultValue = map.defaultReturnValue();
            existing = map.get(key);
        }finally{
            readLock.unlock();
        }
        if(defaultValue == existing){
            writeLock.lock();
            try{
                return map.putIfAbsent(key, value);
            }finally{
                writeLock.unlock();
            }
        }else{
            return defaultValue;
        }
    }

    @Override
    public boolean remove(long key, Object value){
        writeLock.lock();
        try{
            return map.remove(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean replace(long key, V oldValue, V newValue){
        writeLock.lock();
        try{
            return map.replace(key, oldValue, newValue);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V replace(long key, V value){
        writeLock.lock();
        try{
            return map.replace(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V computeIfAbsent(long key, LongFunction<? extends V> mappingFunction){
        V defaultValue;
        V existing;
        readLock.lock();
        try{
            defaultValue = defaultReturnValue();
            existing = get(key);
        }finally{
            readLock.unlock();
        }

        if(defaultValue != existing){
            writeLock.lock();
            try{
                return map.computeIfAbsent(key, mappingFunction);
            }finally{
                writeLock.unlock();
            }
        }else{
            return existing;
        }
    }

    @Override
    public V computeIfAbsentPartial(long key, Long2ObjectFunction<? extends V> mappingFunction){
        V defaultValue;
        V existing;
        readLock.lock();
        try{
            defaultValue = defaultReturnValue();
            existing = get(key);
        }finally{
            readLock.unlock();
        }

        if(defaultValue != existing){
            writeLock.lock();
            try{
                return map.computeIfAbsentPartial(key, mappingFunction);
            }finally{
                writeLock.unlock();
            }
        }else{
            return existing;
        }
    }

    @Override
    public V computeIfPresent(long key, BiFunction<? super Long, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.computeIfPresent(key, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V compute(long key, BiFunction<? super Long, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.compute(key, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V merge(long key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.merge(key, value, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }
}
