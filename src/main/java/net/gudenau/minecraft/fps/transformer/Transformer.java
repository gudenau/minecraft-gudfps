package net.gudenau.minecraft.fps.transformer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public interface Transformer{
    boolean transform(ClassNode classNode, Flags flags);
    
    final class Flags{
        private boolean computeMaxes = false;
        private boolean computeFrames = false;
        
        public void requestMaxes(){
            computeMaxes = true;
        }
    
        public void requestFrames(){
            computeFrames = true;
        }
        
        public int getFlags(){
            return (computeFrames ? ClassWriter.COMPUTE_FRAMES : 0) |
                   (computeMaxes ? ClassWriter.COMPUTE_MAXS : 0);
        }
    }
}
