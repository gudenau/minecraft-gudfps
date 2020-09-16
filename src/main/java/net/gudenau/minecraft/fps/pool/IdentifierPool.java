package net.gudenau.minecraft.fps.pool;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.gudenau.minecraft.fps.util.Stats;
import net.gudenau.minecraft.fps.util.annotation.AssemblyTarget;
import net.gudenau.minecraft.fps.util.threading.SynchronizedUtils;
import net.minecraft.util.Identifier;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;

@AssemblyTarget
public final class IdentifierPool{
    private static final Map<String, Map<String, Identifier>> POOL = SynchronizedUtils.synchronizedMap(new Object2ObjectOpenHashMap<>());
    private static final MethodHandle Identifier$Constructor;
    private static final Stats STATS = Stats.getStats("Identifier Pool");

    static{
        try{
            Identifier$Constructor = MethodHandles.lookup()
                .findConstructor(Identifier.class, MethodType.methodType(void.class, String[].class));
        }catch(NoSuchMethodException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

    @AssemblyTarget
    public static Identifier init(String id){
        STATS.incrementStat("init(String)");
        String[] split = id.split(":", 2);
        if(split.length == 1){
            return doInit("minecraft", id);
        }else{
            return doInit(split[0], split[1]);
        }
    }

    @AssemblyTarget
    public static Identifier init(String namespace, String path){
        STATS.incrementStat("init(String, String)");
        return doInit(namespace, path);
    }

    public static Identifier doInit(String namespace, String path){
        return POOL.computeIfAbsent(namespace, (ns)->SynchronizedUtils.synchronizedMap(new Object2ObjectOpenHashMap<>()))
            .computeIfAbsent(path, (p)->{
                try{
                    STATS.incrementStat("misses");
                    return (Identifier)Identifier$Constructor.invoke((Object)new String[]{namespace, path});
                }catch(Throwable throwable){
                    throw new RuntimeException(throwable);
                }
            });
    }
}
