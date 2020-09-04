package net.gudenau.minecraft.fps.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import org.lwjgl.system.Platform;

public class LibraryLoader{
    private static final String MAVEN_BASE = "https://repo1.maven.org/maven2/org/lwjgl/";
    
    private static final String LWJGL_VERSION = "3.2.2";
    private static final String[] LIBS = {
        "rpmalloc",
        "lz4",
        "xxhash"
    };
    
    public static void setup() throws IOException{
        Path libsPath = Paths.get("gud_fps", "libs");
        if(!Files.exists(libsPath)){
            Files.createDirectories(libsPath);
        }
    
        String platform = Platform.get().getName().toLowerCase();
        Set<Path> paths = new HashSet<>();
        for(String lib : LIBS){
            //"org/lwjgl/lwjgl-rpmalloc/3.2.2/lwjgl-rpmalloc-3.2.2.jar",
            String libJar = "lwjgl-" + lib + "-" + LWJGL_VERSION + ".jar";
            Path libPath = libsPath.resolve(libJar);
            paths.add(libPath);
            if(!Files.exists(libPath)){
                download(
                    libPath,
                    MAVEN_BASE + "lwjgl-" + lib + "/" + LWJGL_VERSION + "/" + libJar
                );
            }
            String nativeJar = "lwjgl-" + lib + "-" + LWJGL_VERSION + "-natives-" + platform + ".jar";
            Path nativePath = libsPath.resolve(nativeJar);
            paths.add(nativePath);
            if(!Files.exists(nativePath)){
                download(
                    nativePath,
                    MAVEN_BASE + "lwjgl-" + lib + "/" + LWJGL_VERSION + "/" + nativeJar
                );
            }
        }
        
        ClassLoader classLoader = LibraryLoader.class.getClassLoader();
        Class<?> KnotClassLoader = classLoader.getClass();
        if(!KnotClassLoader.getName().equals("net.fabricmc.loader.launch.knot.KnotClassLoader")){
            System.err.println("Wrong class loader");
            System.exit(0);
        }
    
        try{
            // Get non-accessible types
            Class<?> KnotClassDelegate = ReflectionHelper.loadClass("net.fabricmc.loader.launch.knot.KnotClassDelegate");
            Class<?> DynamicURLClassLoader = ReflectionHelper.loadClass("net.fabricmc.loader.launch.knot.KnotClassLoader$DynamicURLClassLoader");
            
            // Get fields
            MethodHandle delegate$getter = ReflectionHelper.findGetter(KnotClassLoader, classLoader, "delegate", KnotClassDelegate);
            MethodHandle urlLoader$getter = ReflectionHelper.findGetter(KnotClassLoader, classLoader, "urlLoader", DynamicURLClassLoader);
            MethodHandle metadataCacheField$getter = ReflectionHelper.findGetter(KnotClassDelegate, "metadataCache", Map.class);
            
            // Add the URLs
            Object dynamicClassLoader = urlLoader$getter.invoke();
            MethodHandle addURL = ReflectionHelper.unreflect(DynamicURLClassLoader.getDeclaredMethod("addURL", URL.class)).bindTo(dynamicClassLoader);
            //MethodHandle addURL = ReflectionHelper.findVirtual(DynamicURLClassLoader, dynamicClassLoader, "addUrl", MethodType.methodType(void.class, URL.class));
            
            Set<URL> urls = new HashSet<>(); // Save for later
            for(Path path : paths){
                URL url = path.toUri().toURL();
                urls.add(url);
                addURL.invoke(url);
            }
            
            // The following hacks fix a CME
            // Get the delegate
            Object knotDelegate = delegate$getter.invoke();
            metadataCacheField$getter = metadataCacheField$getter.bindTo(knotDelegate);
            
            // Cache the metadata
            @SuppressWarnings("unchecked")
            Map<String, Object> metadataCache = (Map<String, Object>)metadataCacheField$getter.invoke();
    
            // Find the lambda.
            //   private static synthetic lambda$getMetadata$0(Ljava/lang/String;)Lnet/fabricmc/loader/launch/knot/KnotClassDelegate$Metadata;
            int modifiers = Modifier.PRIVATE | Modifier.STATIC | /* synthetic */ 0x00001000;
            Class<?>[] params = new Class[]{
                String.class
            };
            
            Method applyMethod = null;
            for(Method method : KnotClassDelegate.getDeclaredMethods()){
                //noinspection MagicConstant
                if(
                    method.getModifiers() == modifiers &&
                    Arrays.equals(method.getParameterTypes(), params)
                ){
                    applyMethod = method;
                    method.setAccessible(true);
                    break;
                }
            }
            
            if(applyMethod == null){
                RuntimeException exception = new RuntimeException("Failed to find lambda");
                exception.printStackTrace();
                System.exit(0);
                throw exception;
            }
            
            MethodHandle apply = ReflectionHelper.unreflect(applyMethod);
            
            for(URL url : urls){
                metadataCache.put(url.toString(), apply.invoke(url.toString()));
            }
            
            // Fixes a really weird issue in the cache
            classLoader.loadClass("org.lwjgl.util.xxhash.LibXXHash");
        }catch(Throwable e){
            System.err.println("Failed to modify path");
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    private static void download(Path destination, String source) throws IOException{
        System.out.println("Downloading " + source);
        URL url = new URL(source);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        try(
            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(destination.toFile())
        ){
            output.getChannel().transferFrom(Channels.newChannel(input), 0, Long.MAX_VALUE);
        }
    }
}
