package net.gudenau.minecraft.fps.mixin.client;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.client.texture.Sprite$Interpolation")
public interface Sprite$InterpolationAccessor{
    @Accessor("field_21757") Sprite getOwner();
    @Accessor NativeImage[] getImages();

    @Invoker int invokeGetPixelColor(int frame, int image, int x, int y);
}
