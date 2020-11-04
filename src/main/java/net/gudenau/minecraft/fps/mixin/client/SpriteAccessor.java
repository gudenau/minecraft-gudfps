package net.gudenau.minecraft.fps.mixin.client;

import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Sprite.class)
public interface SpriteAccessor{
    @Accessor AnimationResourceMetadata getAnimationMetadata();
    @Accessor int getFrameTicks();
    @Accessor int getFrameIndex();
    @Accessor int[] getFrameXs();
    @Accessor int[] getFrameYs();
    @Accessor Sprite.Info getInfo();
    @Invoker void invokeUpload(int x, int y, NativeImage[] images);
}
