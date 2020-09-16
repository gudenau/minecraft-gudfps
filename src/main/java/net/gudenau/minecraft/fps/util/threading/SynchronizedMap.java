package net.gudenau.minecraft.fps.util.threading;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SynchronizedMap<K, V> implements Map<K, V>{
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();
    private final Map<K, V> map;

    SynchronizedMap(Map<K, V> map){
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
    public boolean isEmpty(){
        readLock.lock();
        try{
            return map.isEmpty();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key){
        readLock.lock();
        try{
            return map.containsKey(key);
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
    public V get(Object key){
        readLock.lock();
        try{
            return map.get(key);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value){
        writeLock.lock();
        try{
            return map.put(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key){
        writeLock.lock();
        try{
            return map.remove(key);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> other){
        writeLock.lock();
        try{
            map.putAll(other);
        }finally{
            writeLock.unlock();
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
    public Set<K> keySet(){
        return new SynchronizedSet<>(map.keySet(), lock);
    }

    @Override
    public Collection<V> values(){
        return new SynchronizedCollection<>(map.values(), lock);
    }

    @Override
    public Set<Entry<K, V>> entrySet(){
        return new SynchronizedSet<>(map.entrySet(), lock);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue){
        V value = get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action){
        readLock.lock();
        try{
            map.clear();
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function){
        writeLock.lock();
        try{
            map.replaceAll(function);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V putIfAbsent(K key, V value){
        V oldValue = get(key);
        if(oldValue == null){
            writeLock.lock();
            try{
                return map.putIfAbsent(key, value);
            }finally{
                writeLock.unlock();
            }
        }
        return oldValue;
    }

    @Override
    public boolean remove(Object key, Object value){
        writeLock.lock();
        try{
            return map.remove(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue){
        writeLock.lock();
        try{
            return map.replace(key, oldValue, newValue);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V replace(K key, V value){
        writeLock.lock();
        try{
            return map.replace(key, value);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction){
        V value = get(key);
        if(value == null){
            writeLock.lock();
            try{
                return map.computeIfAbsent(key, mappingFunction);
            }finally{
                writeLock.unlock();
            }
        }
        return value;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.computeIfPresent(key, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.compute(key, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction){
        writeLock.lock();
        try{
            return map.merge(key, value, remappingFunction);
        }finally{
            writeLock.unlock();
        }
    }
}
