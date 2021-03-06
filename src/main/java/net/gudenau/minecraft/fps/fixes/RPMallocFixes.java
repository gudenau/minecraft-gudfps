package net.gudenau.minecraft.fps.fixes;

import net.gudenau.minecraft.fps.util.annotation.AssemblyTarget;
import net.gudenau.minecraft.asm.api.v1.annotation.ForceBootloader;
import net.gudenau.minecraft.asm.api.v1.annotation.ForceInline;
import org.lwjgl.system.rpmalloc.RPmalloc;

@ForceBootloader
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
    
    @ForceInline
    @AssemblyTarget
    public static void threadStart(){
        RPmalloc.rpmalloc_thread_initialize();
    }
    
    @ForceInline
    @AssemblyTarget
    public static void threadEnd(){
        RPmalloc.rpmalloc_thread_finalize();
    }
}
