package net.gudenau.minecraft.fps.util;

import org.apache.logging.log4j.util.TriConsumer;

public class Quad<A, B, C, D> extends Tuple<A, B, C>{
    private final D d;
    
    public Quad(A a, B b, C c, D d){
        super(a, b, c);
        this.d = d;
    }
    
    public D getD(){
        return d;
    }
    
    public static <A, B, C, D> void forEach(Iterable<Quad<A, B, C, D>> collection, QuadConsumer<A, B, C, D> consumer){
        collection.forEach((q)->consumer.accept(q.getA(), q.getB(), q.getC(), q.getD()));
    }
}
