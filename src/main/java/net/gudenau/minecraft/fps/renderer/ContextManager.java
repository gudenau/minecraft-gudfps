package net.gudenau.minecraft.fps.renderer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import net.gudenau.minecraft.asm.util.Locker;
import net.gudenau.minecraft.fps.util.annotation.AssemblyTarget;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.system.MemoryUtil.NULL;

public class ContextManager{
    private static final int CONTEXT_COUNT = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(CONTEXT_COUNT, new ThreadFactory(){
        private final AtomicInteger threadNumber = new AtomicInteger(0);
        private final ThreadGroup threadGroup = new ThreadGroup("gud_fps render threads");
        
        @Override
        public Thread newThread(Runnable task){
            return new Thread(
                threadGroup,
                task,
                "gud_fps renderer #" + threadNumber.incrementAndGet()
            );
        }
    });
    private static final ThreadLocal<Context> LOCAL_CONTEXT = new ThreadLocal<>();
    
    private final static Locker FREE_CONTEXTS_LOCKER = new Locker();
    private final static List<Context> FREE_CONTEXTS = new LinkedList<>();
    private final static List<Context> CONTEXTS = new ArrayList<>();
    
    public static int getContextCount(){
        return CONTEXT_COUNT;
    }
    
    @AssemblyTarget
    public static void close(){
        for(Context context : CONTEXTS){
            context.free();
        }
    }
    
    @AssemblyTarget
    public static void init(){
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        
        for(int i = 0; i < CONTEXT_COUNT; i++){
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            
            long handle = GLFW.glfwCreateWindow(100, 100, "Window #" + i, NULL, windowHandle);
            FREE_CONTEXTS_LOCKER.writeLock(()->FREE_CONTEXTS.add(new Context(handle)));
        }
        CONTEXTS.addAll(FREE_CONTEXTS);
    }
    
    /*
    private static Context claimContext(){
        Context context = FREE_CONTEXTS_LOCKER.writeLock(()->{
            if(FREE_CONTEXTS.isEmpty()){
                throw new IllegalStateException(String.format(
                    "Thread %s tried to claim a context when none where available",
                    Thread.currentThread().getName()
                ));
            }else{
                return FREE_CONTEXTS.remove(0);
            }
        });
        context.claim();
        return context;
    }
    
    private static void releaseContext(Context context){
        context.release();
        FREE_CONTEXTS_LOCKER.writeLock(()->FREE_CONTEXTS.add(context));
    }
     */
    
    @AssemblyTarget
    public static void schedule(Runnable task){
        THREAD_POOL.submit(()->{
            Context context = LOCAL_CONTEXT.get();
            if(context == null){
                context = FREE_CONTEXTS_LOCKER.writeLock(()->FREE_CONTEXTS.remove(0));
                LOCAL_CONTEXT.set(context);
            }
    
            context.claim();
            try{
                task.run();
            }finally{
                context.release();
            }
        });
    }
    
    public static ExecutorService getPool(){
        return THREAD_POOL;
    }
}
