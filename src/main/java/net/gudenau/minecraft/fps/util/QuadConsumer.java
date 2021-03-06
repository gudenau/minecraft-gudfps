package net.gudenau.minecraft.fps.util;

@FunctionalInterface
public interface QuadConsumer<A, B, C, D>{
    void accept(A a, B b, C c, D d);
}
