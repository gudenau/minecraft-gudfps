package net.gudenau.minecraft.fps.fixes;

import net.gudenau.minecraft.fps.util.AssemblyTarget;
import org.lwjgl.system.rpmalloc.RPmalloc;

@AssemblyTarget
public class RPMallocFixes{
    @AssemblyTarget
    public static Runnable runnable(Runnable task){
        return ()->{
            RPmalloc.rpmalloc_thread_initialize();
            try{
                task.run();
            }finally{
                RPmalloc.rpmalloc_thread_finalize();
            }
        };
    }
    
    @AssemblyTarget
    public static void threadStart(){
        RPmalloc.rpmalloc_thread_initialize();
    }
    
    @AssemblyTarget
    public static void threadEnd(){
        RPmalloc.rpmalloc_thread_finalize();
    }
}
