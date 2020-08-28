package net.gudenau.minecraft.fps.transformer;

import net.gudenau.minecraft.fps.util.AsmUtils;
import net.gudenau.minecraft.fps.util.Stats;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class ForceInlineTransformer implements Transformer{
    private static final String ANNOTATION_FORCE_INLINE_BAD = "Lnet/gudenau/minecraft/fps/util/annotation/ForceInline;";
    private static final String ANNOTATION_FORCE_INLINE_GOOD = "Ljdk/internal/vm/annotation/ForceInline;";
    
    private static final Stats STATS = Stats.getStats("ForceInline");
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = false;
        for(MethodNode method : classNode.methods){
            AnnotationNode node = AsmUtils.getAnnotation(method, ANNOTATION_FORCE_INLINE_BAD);
            if(node != null){
                STATS.incrementStat("methods");
                node.desc = ANNOTATION_FORCE_INLINE_GOOD;
                changed = true;
            }
        }
        if(changed){
            STATS.incrementStat("classes");
        }
        return changed;
    }
}
