package net.gudenau.minecraft.fps.util;

public class Pair<A, B>{
    private final A a;
    private final B b;
    
    public Pair(A a, B b){
        this.a = a;
        this.b = b;
    }
    
    public final A getA(){
        return a;
    }
    
    public final B getB(){
        return b;
    }
}
