package net.gudenau.minecraft.fps.cpu;

public class CpuNatives{
    native static void cpuid(int function, long registers);
    native static void cpuid(int function, int subFunction, long registers);
}
