package net.gudenau.minecraft.fps.util;

import org.apache.logging.log4j.util.TriConsumer;

public class Tuple<A, B, C> extends Pair<A, B>{
    private final C c;
    
    public Tuple(A a, B b, C c){
        super(a, b);
        this.c = c;
    }
    
    public final C getC(){ return c; }
    
    public static <A, B, C> void forEach(Iterable<Tuple<A, B, C>> collection, TriConsumer<A, B, C> consumer){
        collection.forEach((t)->consumer.accept(t.getA(), t.getB(), t.getC()));
    }
}
