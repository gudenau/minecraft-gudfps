package net.gudenau.minecraft.fps.transformer;

import java.util.Set;
import net.gudenau.minecraft.fps.util.ArrayUtils;
import net.gudenau.minecraft.fps.util.AsmUtils;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class ConstantPrecomputer implements Transformer{
    private static final int[] INSTRUCTIONS_INT = {
        IADD, ISUB, IMUL, IDIV, IREM, IXOR, IOR, IAND, ISHL, ISHR, IUSHR
    };
    private static final int[] INSTRUCTIONS_FLOAT = {
        FADD, FSUB, FMUL, FDIV, FREM
    };
    private static final int[] INSTRUCTIONS_LONG = {
        LADD, LSUB, LMUL, LDIV, LREM, LXOR, LOR, LAND, LSHL, LSHR, LUSHR
    };
    private static final int[] INSTRUCTIONS_DOUBLE = {
        DADD, DSUB, DMUL, DDIV, DREM
    };
    private static final int[] INSTRUCTIONS_MATH = ArrayUtils.combine(INSTRUCTIONS_INT, INSTRUCTIONS_FLOAT, INSTRUCTIONS_LONG, INSTRUCTIONS_DOUBLE);
    
    private static final int[] CONSTANT_INT = {
        SIPUSH, BIPUSH, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5
    };
    private static final int[] CONSTANT_FLOAT = {
        FCONST_0, FCONST_1, FCONST_2
    };
    private static final int[] CONSTANT_LONG = {
        LCONST_0, LCONST_1
    };
    private static final int[] CONSTANT_DOUBLE = {
        DCONST_0, DCONST_1
    };
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = false;
        for(MethodNode method : classNode.methods){
            InsnList instructions = method.instructions;
            Set<InsnNode> nodes = AsmUtils.getMatchingNodes(instructions, (node)->
                ArrayUtils.contains(INSTRUCTIONS_MATH, node.getOpcode())
            );
            if(nodes.isEmpty()){
                continue;
            }
            for(InsnNode node : nodes){
                changed |= transform(instructions, node);
            }
        }
        
        return changed;
    }
    
    private boolean transform(InsnList instructions, InsnNode operationNode){
        AbstractInsnNode arg2 = operationNode.getPrevious();
        if(arg2 == null){
            return false;
        }
        
        AbstractInsnNode arg1 = arg2.getPrevious();
        if(arg1 == null){
            return false;
        }
        
        int opcode = operationNode.getOpcode();
        AbstractInsnNode replacement;
        if(ArrayUtils.contains(INSTRUCTIONS_INT, opcode)){
            replacement = transformInt(operationNode, arg1, arg2);
        }else if(ArrayUtils.contains(INSTRUCTIONS_FLOAT, opcode)){
            replacement = transformFloat(operationNode, arg1, arg2);
        }else if(ArrayUtils.contains(INSTRUCTIONS_LONG, opcode)){
            replacement = transformLong(operationNode, arg1, arg2);
        }else if(ArrayUtils.contains(INSTRUCTIONS_DOUBLE, opcode)){
            replacement = transformDouble(operationNode, arg1, arg2);
        }else{
            return false;
        }
    
        if(replacement != null){
            instructions.insert(operationNode, replacement);
            instructions.remove(operationNode);
            instructions.remove(arg1);
            instructions.remove(arg2);
            
            return true;
        }else{
            return false;
        }
    }
    
    private Number getConstant(AbstractInsnNode insn){
        switch(insn.getOpcode()){
            // Ints
            case BIPUSH:
            case SIPUSH: return ((IntInsnNode)insn).operand;
            case ICONST_M1: return -1;
            case ICONST_0: return 0;
            case ICONST_1: return 1;
            case ICONST_2: return 2;
            case ICONST_3: return 3;
            case ICONST_4: return 4;
            case ICONST_5: return 5;
            
            // Floats
            case FCONST_0: return 0F;
            case FCONST_1: return 1F;
            case FCONST_2: return 2F;
            
            // Longs
            case LCONST_0: return 0L;
            case LCONST_1: return 1L;
            
            // Doubles
            case DCONST_0: return 0.0;
            case DCONST_1: return 1.0;
            
            case LDC: return (Number)((LdcInsnNode)insn).cst;
        }
        throw new RuntimeException("Unknown instruction " + insn);
    }
    
    private AbstractInsnNode transformInt(InsnNode operationNode, AbstractInsnNode arg1, AbstractInsnNode arg2){
        if(
            !(ArrayUtils.contains(CONSTANT_INT, arg1.getOpcode()) || arg1.getOpcode() == LDC && ((LdcInsnNode)arg1).cst instanceof Integer) ||
            !(ArrayUtils.contains(CONSTANT_INT, arg2.getOpcode()) || arg2.getOpcode() == LDC && ((LdcInsnNode)arg2).cst instanceof Integer)
        ){
            return null;
        }
        
        int constant1 = getConstant(arg1).intValue();
        int constant2 = getConstant(arg2).intValue();
        int result;
        
        switch(operationNode.getOpcode()){
            case IADD:{
                result = constant1 + constant2;
            } break;
            case ISUB:{
                result = constant1 - constant2;
            } break;
            case IMUL:{
                result = constant1 * constant2;
            } break;
            case IDIV:{
                result = constant1 / constant2;
            } break;
            case IREM:{
                result = constant1 % constant2;
            } break;
            case IXOR:{
                result = constant1 ^ constant2;
            } break;
            case IOR:{
                result = constant1 | constant2;
            } break;
            case IAND:{
                result = constant1 & constant2;
            } break;
            case ISHL:{
                result = constant1 << constant2;
            } break;
            case ISHR:{
                result = constant1 >> constant2;
            } break;
            case IUSHR:{
                result = constant1 >>> constant2;
            } break;
            default: return null;
        }
        
        AbstractInsnNode replacement;
        if(result == -1){
            replacement = new InsnNode(ICONST_M1);
        }else if(result == 0){
            replacement = new InsnNode(ICONST_0);
        }else if(result == 1){
            replacement = new InsnNode(ICONST_1);
        }else if(result == 2){
            replacement = new InsnNode(ICONST_2);
        }else if(result == 3){
            replacement = new InsnNode(ICONST_3);
        }else if(result == 4){
            replacement = new InsnNode(ICONST_4);
        }else if(result == 5){
            replacement = new InsnNode(ICONST_5);
        }else if(result <= Byte.MAX_VALUE && result >= Byte.MIN_VALUE){
            replacement = new IntInsnNode(BIPUSH, result);
        }else if(result <= Short.MAX_VALUE && result >= Short.MIN_VALUE){
            replacement = new IntInsnNode(SIPUSH, result);
        }else{
            replacement = new LdcInsnNode(result);
        }
        
        return replacement;
    }
    
    private AbstractInsnNode transformFloat(InsnNode operationNode, AbstractInsnNode arg1, AbstractInsnNode arg2){
        if(
            !(ArrayUtils.contains(CONSTANT_FLOAT, arg1.getOpcode()) || arg1.getOpcode() == LDC && ((LdcInsnNode)arg1).cst instanceof Float) ||
            !(ArrayUtils.contains(CONSTANT_FLOAT, arg2.getOpcode()) || arg2.getOpcode() == LDC && ((LdcInsnNode)arg2).cst instanceof Float)
        ){
            return null;
        }
    
        float constant1 = getConstant(arg1).floatValue();
        float constant2 = getConstant(arg2).floatValue();
        float result;
    
        switch(operationNode.getOpcode()){
            case FADD:{
                result = constant1 + constant2;
            } break;
            case FSUB:{
                result = constant1 - constant2;
            } break;
            case FMUL:{
                result = constant1 * constant2;
            } break;
            case FDIV:{
                result = constant1 / constant2;
            } break;
            case FREM:{
                result = constant1 % constant2;
            } break;
            default: return null;
        }
    
        AbstractInsnNode replacement;
        if(result == 0){
            replacement = new InsnNode(FCONST_0);
        }else if(result == 1){
            replacement = new InsnNode(FCONST_1);
        }else if(result == 2){
            replacement = new InsnNode(FCONST_2);
        }else{
            replacement = new LdcInsnNode(result);
        }
    
        return replacement;
    }
    
    private AbstractInsnNode transformLong(InsnNode operationNode, AbstractInsnNode arg1, AbstractInsnNode arg2){
        if(
            !(ArrayUtils.contains(CONSTANT_LONG, arg1.getOpcode()) || arg1.getOpcode() == LDC && ((LdcInsnNode)arg1).cst instanceof Long) ||
            !(ArrayUtils.contains(CONSTANT_LONG, arg2.getOpcode()) || arg2.getOpcode() == LDC && ((LdcInsnNode)arg2).cst instanceof Long)
        ){
            return null;
        }
    
        long constant1 = getConstant(arg1).longValue();
        long constant2 = getConstant(arg2).longValue();
        long result;
    
        switch(operationNode.getOpcode()){
            case LADD:{
                result = constant1 + constant2;
            } break;
            case LSUB:{
                result = constant1 - constant2;
            } break;
            case LMUL:{
                result = constant1 * constant2;
            } break;
            case LDIV:{
                result = constant1 / constant2;
            } break;
            case LREM:{
                result = constant1 % constant2;
            } break;
            case LXOR:{
                result = constant1 ^ constant2;
            } break;
            case LOR:{
                result = constant1 | constant2;
            } break;
            case LAND:{
                result = constant1 & constant2;
            } break;
            case LSHL:{
                result = constant1 << constant2;
            } break;
            case LSHR:{
                result = constant1 >> constant2;
            } break;
            case LUSHR:{
                result = constant1 >>> constant2;
            } break;
            default: return null;
        }
    
        AbstractInsnNode replacement;
        if(result == 0){
            replacement = new InsnNode(LCONST_0);
        }else if(result == 1){
            replacement = new InsnNode(LCONST_1);
        }else{
            replacement = new LdcInsnNode(result);
        }
    
        return replacement;
    }
    
    private AbstractInsnNode transformDouble(InsnNode operationNode, AbstractInsnNode arg1, AbstractInsnNode arg2){
        if(
            !(ArrayUtils.contains(CONSTANT_DOUBLE, arg1.getOpcode()) || arg1.getOpcode() == LDC && ((LdcInsnNode)arg1).cst instanceof Double) ||
            !(ArrayUtils.contains(CONSTANT_DOUBLE, arg2.getOpcode()) || arg2.getOpcode() == LDC && ((LdcInsnNode)arg2).cst instanceof Double)
        ){
            return null;
        }
    
        double constant1 = getConstant(arg1).doubleValue();
        double constant2 = getConstant(arg2).doubleValue();
        double result;
    
        switch(operationNode.getOpcode()){
            case DADD:{
                result = constant1 + constant2;
            } break;
            case DSUB:{
                result = constant1 - constant2;
            } break;
            case DMUL:{
                result = constant1 * constant2;
            } break;
            case DDIV:{
                result = constant1 / constant2;
            } break;
            case DREM:{
                result = constant1 % constant2;
            } break;
            default: return null;
        }
    
        AbstractInsnNode replacement;
        if(result == 0){
            replacement = new InsnNode(DCONST_0);
        }else if(result == 1){
            replacement = new InsnNode(DCONST_1);
        }else{
            replacement = new LdcInsnNode(result);
        }
    
        return replacement;
    }
}
