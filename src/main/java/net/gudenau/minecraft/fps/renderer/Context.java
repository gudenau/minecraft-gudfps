package net.gudenau.minecraft.fps.renderer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.gudenau.minecraft.asm.util.Locker;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.system.MemoryUtil.NULL;

public class Context{
    private static final Locker CAPS_LOCKER = new Locker();
    private static final Long2ObjectMap<GLCapabilities> CAPS = new Long2ObjectOpenHashMap<>();
    
    private final long handle;
    private volatile Thread claim = null;
    
    public Context(long handle){
        this.handle = handle;
    }
    
    public void claim(){
        if(claim == null){
            claim = Thread.currentThread();
            GLFW.glfwMakeContextCurrent(handle);
            GL.setCapabilities(CAPS_LOCKER.computeIfAbsent(CAPS, handle, (handle)->GL.createCapabilities(false)));
        }else{
            throw new IllegalStateException(String.format(
                "%s tried to claim a Context owned by %s",
                Thread.currentThread().getName(),
                claim.getName()
            ));
        }
    }
    
    public void release(){
        if(claim != null){
            claim = null;
            GLFW.glfwMakeContextCurrent(NULL);
            GL.setCapabilities(null);
        }else{
            throw new IllegalStateException(String.format(
                "%s tried to release a claim to a Context that was not claimed",
                Thread.currentThread().getName()
            ));
        }
    }
    
    public void free(){
        GLFW.glfwDestroyWindow(handle);
    }
}
