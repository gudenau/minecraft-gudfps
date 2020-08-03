package net.gudenau.minecraft.fps.transformer;

import java.util.Set;
import net.gudenau.minecraft.fps.util.ArrayUtils;
import net.gudenau.minecraft.fps.util.AsmUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class MathOptimizer implements Transformer{
    private static final int[] DIV = {
        IDIV, LDIV,
    };
    private static final int[] MUL = {
        IMUL, LMUL,
    };
    private static final int[] BOTH = ArrayUtils.combine(DIV, MUL);
    
    private static final int[] LONG = {
        LDIV, LMUL
    };
    private static final int[] INT = {
        IDIV, IMUL
    };
    
    private static final int[] CONSTANTS = {
        ICONST_2, ICONST_4, LDC
    };
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = true;
    
        for(MethodNode method : classNode.methods){
            InsnList instructions = method.instructions;
            Set<InsnNode> isnNodes = AsmUtils.findNodes(instructions, (node)->{
                if(node instanceof InsnNode){
                    InsnNode insnNode = (InsnNode)node;
                    return ArrayUtils.contains(BOTH, insnNode.getOpcode());
                }
                return false;
            });
    
            for(InsnNode isnNode : isnNodes){
                AbstractInsnNode lastNode = isnNode.getPrevious();
                if(ArrayUtils.contains(CONSTANTS, lastNode.getOpcode())){
                    boolean isLong = ArrayUtils.contains(LONG, isnNode.getOpcode());
                    boolean isMul = ArrayUtils.contains(MUL, isnNode.getOpcode());
    
                    if(lastNode instanceof LdcInsnNode){
                        LdcInsnNode ldc = (LdcInsnNode)lastNode;
                        long value = ((Number)ldc.cst).longValue();
                        if(Long.bitCount(value) != 1){
                            continue;
                        }
                        int shiftAmount = 0;
                        while((value = value >>> 1) != 0){
                            shiftAmount++;
                        }
                        
                        if(shiftAmount < 6){
                            instructions.insertBefore(lastNode, new InsnNode(ICONST_0 + shiftAmount));
                            instructions.remove(lastNode);
                        }else{
                            ldc.cst = shiftAmount;
                        }
                    }else{
                        int opcode;
                        int isnNodeOpcode = isnNode.getOpcode();
                        if(isnNodeOpcode == ICONST_2){
                            opcode = ICONST_1;
                        }else if(isnNodeOpcode == ICONST_4){
                            opcode = ICONST_2;
                        }else{
                            continue;
                        }
                        instructions.insertBefore(isnNode, new InsnNode(opcode));
                        instructions.remove(isnNode);
                    }
    
                    InsnNode shiftNode;
                    if(isLong){
                        shiftNode = new InsnNode(isMul ? LSHL : LSHR);
                    }else{
                        shiftNode = new InsnNode(isMul ? ISHL : ISHR);
                    }
                    instructions.insertBefore(isnNode, shiftNode);
                    instructions.remove(isnNode);
                    
                    changed = true;
                }
                /*
                    ILOAD 0
                    ICONST_2
                    IDIV
                 */
            }
        }
        
        return changed;
    }
    
    private static void asdf(){
        int a = 100;
        int b = a / 2;
    }
}
