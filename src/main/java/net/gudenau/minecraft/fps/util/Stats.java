package net.gudenau.minecraft.fps.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Stats{
    private static final Map<String, Stats> STAT_MAP = new HashMap<>();
    private static final ReadWriteLock STAT_MAP_LOCK = new ReentrantReadWriteLock();
    
    static{
        Runtime.getRuntime().addShutdownHook(new Thread(()->
            LockUtils.withReadLock(STAT_MAP_LOCK, ()->{
                StringBuilder builder = new StringBuilder();
                STAT_MAP.values().forEach((stat)->{
                    builder.append(stat.name).append(":\n");
                    LockUtils.withReadLock(stat.statMapLock, ()->stat.statMap.forEach((name, value)->
                        builder.append("    ").append(name).append(": ").append(value.get()).append("\n")
                    ));
                });
                // :^)
                System.out.printf("%s\n", builder);
            })
        , "Stat Printer"));
    }
    
    public static Stats getStats(String name){
        Stats stats = LockUtils.withReadLock(STAT_MAP_LOCK, ()->STAT_MAP.get(name));
        if(stats != null){
            return stats;
        }
        return LockUtils.withWriteLock(STAT_MAP_LOCK, ()->STAT_MAP.computeIfAbsent(name, Stats::new));
    }
    
    private final String name;
    
    private final Map<String, AtomicLong> statMap = new HashMap<>();
    private final ReadWriteLock statMapLock = new ReentrantReadWriteLock();
    
    public Stats(String name){
        this.name = name;
    }
    
    public void incrementStat(String stat){
        AtomicLong value = LockUtils.withReadLock(statMapLock, ()->statMap.get(stat));
        if(value == null){
            value = LockUtils.withWriteLock(statMapLock, ()->
                statMap.computeIfAbsent(stat, (n)->new AtomicLong(0))
            );
        }
        value.incrementAndGet();
    }
    
    public void addStat(String stat, int amount){
        AtomicLong value = LockUtils.withReadLock(statMapLock, ()->statMap.get(stat));
        if(value == null){
            value = LockUtils.withWriteLock(statMapLock, ()->
                statMap.computeIfAbsent(stat, (n)->new AtomicLong(amount))
            );
        }else{
            value.addAndGet(amount);
        }
    }
    
    public long getStat(String stat){
        AtomicLong value = LockUtils.withReadLock(statMapLock, ()->statMap.get(stat));
        if(value != null){
            return value.get();
        }else{
            return LockUtils.withWriteLock(statMapLock, ()->
                statMap.computeIfAbsent(stat, (n)->new AtomicLong(0)).get()
            );
        }
    }
}
