package net.gudenau.minecraft.fps.mixin;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.gudenau.minecraft.fps.GudFPS;
import net.gudenau.minecraft.fps.util.ReflectionHelper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public class Plugin implements IMixinConfigPlugin{
    private static final MethodHandle EasterEgger$getMixins;
    
    private static IMixinTransformer transformer = null;

    static{
        MethodHandle getMixins;
        try{
            MethodHandle MixinEnvironment$transformer$getter = net.gudenau.minecraft.asm.impl.ReflectionHelper.findStaticGetter(MixinEnvironment.class, "transformer", IMixinTransformer.class);
            transformer = (IMixinTransformer)MixinEnvironment$transformer$getter.invokeExact();
            
            Class<?> EasterEgger = Plugin.class.getClassLoader().loadClass("net.gudenau.minecraft.fps.util.EasterEgger");
            ReflectionHelper.findStatic(
                EasterEgger,
                "init",
                void.class
            ).invokeExact();
            getMixins = ReflectionHelper.findStatic(
                EasterEgger,
                "getMixins",
                List.class,
                IMixinTransformer.class
            );
            GudFPS.init = ReflectionHelper.findStatic(
                EasterEgger,
                "gameInit",
                void.class
            );
        }catch(Throwable ignored){
            // Not a gud build, just ignore it.
            getMixins = null;
        }
        EasterEgger$getMixins = getMixins;
    }
    
    @Override
    public void onLoad(String mixinPackage){}
    
    @Override
    public String getRefMapperConfig(){
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName){
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets){}
    
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getMixins(){
        try{
            return EasterEgger$getMixins == null ? Collections.emptyList() : (List<String>)EasterEgger$getMixins.invokeExact(transformer);
        }catch(Throwable throwable){
            return Collections.emptyList();
        }
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo){}
}
