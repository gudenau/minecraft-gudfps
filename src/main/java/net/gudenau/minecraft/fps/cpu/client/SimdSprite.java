package net.gudenau.minecraft.fps.cpu.client;

import net.gudenau.minecraft.fps.cpu.SimdNatives;
import net.gudenau.minecraft.fps.mixin.client.NativeImageAccessor;
import net.gudenau.minecraft.fps.mixin.client.Sprite$InterpolationAccessor;
import net.gudenau.minecraft.fps.mixin.client.SpriteAccessor;
import net.gudenau.minecraft.fps.util.annotation.AssemblyTarget;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

public class SimdSprite{
    @AssemblyTarget
    public static void interpolateSprite(Sprite$InterpolationAccessor sprite$interpolation){
        NativeImage[] images = sprite$interpolation.getImages();

        Sprite sprite = sprite$interpolation.getOwner();
        SpriteAccessor spriteAccessor = (SpriteAccessor)sprite;
        Sprite.Info info = spriteAccessor.getInfo();

        AnimationResourceMetadata animationMetadata = spriteAccessor.getAnimationMetadata();

        int frameIndex = spriteAccessor.getFrameIndex();

        int progress = spriteAccessor.getFrameTicks();
        int total = animationMetadata.getFrameTime(frameIndex);
        int invertedProgress = total - progress;
        float delta = (float)progress/total;
        float invertedDelta = 1 - delta;

        int animationFrameIndex = animationMetadata.getFrameIndex(frameIndex);
        int animationFrameCount = animationMetadata.getFrameCount() == 0 ? sprite.getFrameCount() : animationMetadata.getFrameCount();
        int animationNextFrameIndex = animationMetadata.getFrameIndex((frameIndex + 1) % animationFrameCount);

        if(
            animationFrameIndex != animationNextFrameIndex &&
            animationFrameIndex >= 0 &&
            animationFrameIndex < sprite.getFrameCount()
        ){
            for(int mipLevel = 0; mipLevel < images.length; mipLevel++){
                int scaledWidth = info.getWidth() >> mipLevel;
                int scaledHeight = info.getHeight() >> mipLevel;

                int pixels = scaledWidth * scaledHeight;
                if(pixels < 4){
                    for(int y = 0; y < scaledHeight; y++){
                        for(int x = 0; x < scaledWidth; x++){
                            int end = sprite$interpolation.invokeGetPixelColor(
                                animationFrameIndex, mipLevel,
                                x, y
                            );
                            int start = sprite$interpolation.invokeGetPixelColor(
                                animationNextFrameIndex, mipLevel,
                                x, y
                            );

                            int endB = (end       ) & 0xFF;
                            int endG = (end >>>  8) & 0xFF;
                            int endR = (end >>> 16) & 0xFF;
                            int endA = (end >>> 24) & 0xFF;

                            int startB = (start       ) & 0xFF;
                            int startG = (start >>>  8) & 0xFF;
                            int startR = (start >>> 16) & 0xFF;
                            int startA = (start >>> 24) & 0xFF;

                            int b = (invertedProgress * endB + progress * startB) / total;
                            int g = (invertedProgress * endG + progress * startG) / total;
                            //int r = (invertedProgress * endR + progress * startR) / total;
                            //int a = (invertedProgress * endA + progress * startA) / total;
                            int r = (int)(delta * endR + invertedDelta * startR);
                            int a = (int)(delta * endA + invertedDelta * startA);

//                        b = progress;
//                        g = total;
//                        r = 0xFF;
//                        a = 0xFF;

                            int result =
                                ((b      ) & 0x000000FF) |
                                    ((g <<  8) & 0x0000FF00) |
                                    ((r << 16) & 0x00FF0000) |
                                    ((a << 24) & 0xFF000000);

                            images[mipLevel].setPixelColor(x, y, result);
                        }
                    }
                }else{
                    int[] frameXs = spriteAccessor.getFrameXs();
                    int[] frameYs = spriteAccessor.getFrameYs();

                    //return Sprite.this.images[layer].getPixelColor(x + (Sprite.this.frameXs[frameIndex] * Sprite.this.info.width >> layer), y + (Sprite.this.frameYs[frameIndex] * Sprite.this.info.height >> layer));
                    long from = ((NativeImageAccessor)(Object)images[mipLevel]).getPointer() + ((frameXs[animationFrameIndex] * info.getWidth()) >>> mipLevel);
                    long to = ((NativeImageAccessor)(Object)images[mipLevel]).getPointer() + ((frameXs[animationNextFrameIndex] * info.getHeight()) >>> mipLevel);
                    long result = ((NativeImageAccessor)(Object)images[mipLevel]).getPointer();
                    SimdNatives.lerpBytes(from, to, result, pixels << 2, progress, total);
                }
            }
        }

        spriteAccessor.invokeUpload(0, 0, images);
    /*
    double d = 1.0D - (double)Sprite.this.frameTicks / (double)Sprite.this.animationMetadata.getFrameTime(Sprite.this.frameIndex);
            int i = Sprite.this.animationMetadata.getFrameIndex(Sprite.this.frameIndex);
            int j = Sprite.this.animationMetadata.getFrameCount() == 0 ? Sprite.this.getFrameCount() : Sprite.this.animationMetadata.getFrameCount();
            int k = Sprite.this.animationMetadata.getFrameIndex((Sprite.this.frameIndex + 1) % j);
            if (i != k && k >= 0 && k < Sprite.this.getFrameCount()) {
                for(int l = 0; l < this.images.length; ++l) {
                    int m = Sprite.this.info.width >> l;
                    int n = Sprite.this.info.height >> l;

                    for(int o = 0; o < n; ++o) {
                        for(int p = 0; p < m; ++p) {
                            int q = this.getPixelColor(i, l, p, o);
                            int r = this.getPixelColor(k, l, p, o);
                            int s = this.lerp(d, q >> 16 & 255, r >> 16 & 255);
                            int t = this.lerp(d, q >> 8 & 255, r >> 8 & 255);
                            int u = this.lerp(d, q & 255, r & 255);
                            this.images[l].setPixelColor(p, o, q & -16777216 | s << 16 | t << 8 | u);
                        }
                    }
                }

                Sprite.this.upload(0, 0, this.images);
            }
     */
        /*
        Sprite sprite = sprite$interpolation.getOwner();
        SpriteAccessor spriteAccessor = (SpriteAccessor)sprite;
        Sprite.Info info = spriteAccessor.getInfo();
    
        NativeImage[] images = sprite$interpolation.getImages();
        
        AnimationResourceMetadata animation = spriteAccessor.getAnimationMetadata();
        
        int spriteFrameIndex = spriteAccessor.getFrameIndex();
        int frameTicks = spriteAccessor.getFrameTicks();
        int frameTime = animation.getFrameTime(spriteFrameIndex);
        //float delta = 1 - (float)frameTicks / frameTime;
        
        int frameIndex = animation.getFrameIndex(spriteFrameIndex);
        int frameCount = animation.getFrameCount() == 0 ? sprite.getFrameCount() : animation.getFrameCount();
        int nextFrame = animation.getFrameIndex((spriteFrameIndex + 1) % frameCount);

        int[] frameXs = spriteAccessor.getFrameXs();
        int[] frameYs = spriteAccessor.getFrameYs();

        if(frameIndex != nextFrame && nextFrame >= 0 && nextFrame < sprite.getFrameCount()){
            for(int layer = 0; layer < images.length; layer++){
                int width = sprite.getWidth() >> layer;
                int height = sprite.getHeight() >> layer;
                //int pixels = width * height;

                // private int getPixelColor(int frameIndex, int layer, int x, int y) {
                //     return Sprite.this.images[layer].getPixelColor(x + (Sprite.this.frameXs[frameIndex] * Sprite.this.info.width >> layer), y + (Sprite.this.frameYs[frameIndex] * Sprite.this.info.height >> layer));
                // }

                NativeImage layerImage = images[layer];

                for(int y = 0; y < height; y++){
                    for(int x = 0; x < width; x++){
                        int start = layerImage.getPixelColor(
                            x + (frameXs[frameIndex] * info.getWidth() >> layer),
                            y + (frameYs[frameIndex] * info.getHeight() >> layer)
                        );
                        int end = layerImage.getPixelColor(
                            x + (frameXs[nextFrame] * info.getWidth() >> layer),
                            y + (frameYs[nextFrame] * info.getHeight() >> layer)
                        );

                        // (ticks*to + (1 - ticks)*to) / frameTime
                        int startRed = start >>> 16 & 0xFF;
                        int startGreen = start >>> 8 & 0xFF;
                        int startBlue = start & 0xFF;
                        int startAlpha = start >>> 24 & 0xFF;

                        int endRed = end >>> 16 & 0xFF;
                        int endGreen = end >>> 8 & 0xFF;
                        int endBlue = end & 0xFF;
                        int endAlpha = end >>> 24 & 0xFF;

                        int red = (frameTicks * endRed + (frameTime - frameTicks) * startRed) / frameTime;
                        int green = (frameTicks * endGreen + (frameTime - frameTicks) * startGreen) / frameTime;
                        int blue = (frameTicks * endBlue + (frameTime - frameTicks) * startBlue) / frameTime;
                        int alpha = (frameTicks * endAlpha + (frameTime - frameTicks) * startAlpha) / frameTime;

                        int result =
                            ((red << 16) & 0x00FF0000) |
                            ((green << 8) & 0x0000FF00) |
                            ((blue) & 0x000000FF) |
                            ((alpha << 24) & 0xFF000000);

                        layerImage.setPixelColor(x, y, result);
                    }
                }

                /*
                for(int y = 0; y < height; ++y) {
                    for(int x = 0; x < width; ++x) {
                        int source = getPixelColor(frameIndex, l, x, y);
                        int target = getPixelColor(nextFrame, l, x, y);
                        int red = lerp(delta, source >> 16 & 255, target >> 16 & 255);
                        int grn = lerp(delta, source >> 8 & 255, target >> 8 & 255);
                        int blu = lerp(delta, source & 255, target & 255);
                        images[l].setPixelColor(x, y, source & 0xFF000000 | red << 16 | grn << 8 | blu);
                    }
                }
                * /
            }
        }
        
        spriteAccessor.invokeUpload(0, 0, images);
         */
    }
}
