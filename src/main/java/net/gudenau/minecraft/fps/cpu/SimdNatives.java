package net.gudenau.minecraft.fps.cpu;

public class SimdNatives{
    public static native void lerpBytes(long a, long b, long result, int count, int progress, int total);
}
