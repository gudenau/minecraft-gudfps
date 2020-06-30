package net.gudenau.minecraft.fps.mixin;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({"UnusedMixin", "unused", "OverwriteAuthorRequired"})
@Mixin(Util.class)
public class Playground{
    @Shadow private static void method_18841(List a, CompletableFuture[] b, CompletableFuture c, CompletableFuture d){}
    
    @Overwrite
    public static <V> CompletableFuture<List<V>> combine(List<? extends CompletableFuture<? extends V>> futures) {
        List<V> list = Lists.newArrayListWithCapacity(futures.size());
        CompletableFuture<?>[] completableFutures = new CompletableFuture[futures.size()];
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        for(CompletableFuture<? extends V> completableFuture2 : futures){
            method_18841(list, completableFutures, completableFuture, completableFuture2);
        }
        return CompletableFuture.allOf(completableFutures).applyToEither(completableFuture, (var1)->list);
    }
}
