package net.gudenau.minecraft.fps.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import static org.objectweb.asm.Opcodes.*;

public class AsmUtils{
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> Set<T> getMatchingNodes(InsnList instructions, Function<AbstractInsnNode, Boolean> checker){
        Set<T> matching = new HashSet<>();
        instructions.forEach((node)->{
            if(checker.apply(node)){
                matching.add((T)node);
            }
        });
        return matching;
    }
    
    private static final int[] INVOKES = {
        INVOKEVIRTUAL,
        INVOKESPECIAL,
        INVOKESTATIC,
        INVOKEINTERFACE
    };
    public static Set<MethodInsnNode> findMethodCalls(InsnList instructions, int opcode, String owner, String name, String description){
        Set<MethodInsnNode> matching = new HashSet<>();
        instructions.forEach((node)->{
            if(opcode == -1 ? ArrayUtils.contains(INVOKES, node.getOpcode()) : node.getOpcode() == opcode){
                MethodInsnNode method = (MethodInsnNode)node;
                if(
                    (name == null || method.name.equals(name)) &&
                    (description == null || method.desc.equals(description)) &&
                    (owner == null || method.owner.equals(owner))
                ){
                    matching.add(method);
                }
            }
        });
        return matching;
    }
    
    
    
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> T getLastNode(AbstractInsnNode node, int opcode){
        AbstractInsnNode lastNode = node.getPrevious();
        if(lastNode == null || lastNode.getOpcode() != opcode){
            return null;
        }
        return (T)lastNode;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> List<T> getLastNodes(AbstractInsnNode node, int count){
        List<T> nodes = new ArrayList<>(count);
        for(int i = 0; i < count; i++){
            node = node.getPrevious();
            nodes.add((T)node);
        }
        return nodes;
    }
    
    public static int handleTagToOpcode(int tag){
        switch(tag){
            case H_GETFIELD: return GETFIELD;
            case H_GETSTATIC: return GETSTATIC;
            case H_PUTFIELD: return PUTFIELD;
            case H_PUTSTATIC: return PUTSTATIC;
            case H_INVOKEVIRTUAL: return INVOKEVIRTUAL;
            case H_INVOKESTATIC: return INVOKESTATIC;
            case H_INVOKESPECIAL: return INVOKESPECIAL;
            case H_NEWINVOKESPECIAL: return NEW;
            case H_INVOKEINTERFACE: return INVOKEINTERFACE;
            default: return tag;
        }
    }
    
    private static final int[] RETURNS = {
        IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN
    };
    public static Set<InsnNode> findReturns(InsnList instructions){
        Set<InsnNode> nodes = new HashSet<>();
        instructions.forEach((node)->{
            if(ArrayUtils.contains(RETURNS, node.getOpcode())){
                nodes.add((InsnNode)node);
            }
        });
        return nodes;
    }
    
    /*
    NEW java/lang/RuntimeException
    DUP
    ALOAD 1
    INVOKESPECIAL java/lang/RuntimeException.<init> (Ljava/lang/String;)V
    ATHROW
     */
    public static InsnList throwException(String name, String method){
        InsnList instructions = new InsnList();
        instructions.add(new TypeInsnNode(NEW, name));
        instructions.add(new InsnNode(DUP));
        instructions.add(new LdcInsnNode(method));
        instructions.add(new MethodInsnNode(INVOKESPECIAL, name, "<init>", "(Ljava/lang/String;)V", false));
        instructions.add(new InsnNode(ATHROW));
        return instructions;
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> List<T> findNodesOrdered(InsnList instructions, Class<T> nodeType){
        List<T> nodes = new ArrayList<>();
        instructions.forEach((node)->{
            if(nodeType.isAssignableFrom(node.getClass())){
               nodes.add((T)node);
            }
        });
        return nodes;
    }
    
    public static <T extends AbstractInsnNode> Set<T> findNodes(InsnList instructions, Class<T> type){
        return findNodes(instructions, type::isInstance);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends AbstractInsnNode> Set<T> findNodes(InsnList instructions, Function<AbstractInsnNode, Boolean> check){
        Set<T> nodes = new HashSet<>();
        instructions.forEach((node)->{
            if(check.apply(node)){
                nodes.add((T)node);
            }
        });
        return nodes;
    }
    
    public static void forEachInRange(Pair<? extends AbstractInsnNode, ? extends AbstractInsnNode> range, Consumer<AbstractInsnNode> action){
        forEachInRange(range.getA(), range.getB(), action);
    }
    
    public static void forEachInRange(AbstractInsnNode start, AbstractInsnNode end, Consumer<AbstractInsnNode> action){
        AbstractInsnNode node = start;
        while(node != null && !node.equals(end)){
            action.accept(node);
            node = node.getNext();
        }
    }
    
    public static int getArgumentSize(Type method){
        int size = 0;
        for(Type argumentType : method.getArgumentTypes()){
            size += argumentType.getSize();
        }
        return size;
    }
    
    public static String opcodeName(int opcode){
        switch(opcode){
            case NOP: return "NOP";
            case ACONST_NULL: return "ACONST_NULL";
            case ICONST_M1: return "ICONST_M1";
            case ICONST_0: return "ICONST_0";
            case ICONST_1: return "ICONST_1";
            case ICONST_2: return "ICONST_2";
            case ICONST_3: return "ICONST_3";
            case ICONST_4: return "ICONST_4";
            case ICONST_5: return "ICONST_5";
            case LCONST_0: return "LCONST_0";
            case LCONST_1: return "LCONST_1";
            case FCONST_0: return "FCONST_0";
            case FCONST_1: return "FCONST_1";
            case FCONST_2: return "FCONST_2:";
            case DCONST_0: return "DCONST_0";
            case DCONST_1: return "DCONST_1";
            case BIPUSH: return "BIPUSH";
            case SIPUSH: return "SIPUSH";
            case LDC: return "LDC";
            case ILOAD: return "ILOAD";
            case LLOAD: return "LLOAD";
            case FLOAD: return "FLOAD";
            case DLOAD: return "DLOAD";
            case ALOAD: return "ALOAD";
            case IALOAD: return "IALOAD";
            case LALOAD: return "LALOAD";
            case FALOAD: return "FALOAD";
            case DALOAD: return "DALOAD";
            case AALOAD: return "AALOAD";
            case BALOAD: return "BALOAD";
            case CALOAD: return "CALOAD";
            case SALOAD: return "SALOAD";
            case ISTORE: return "ISTORE";
            case LSTORE: return "LSTORE";
            case FSTORE: return "FSTORE";
            case DSTORE: return "DSTORE";
            case ASTORE: return "ASTORE";
            case IASTORE: return "IASTORE";
            case LASTORE: return "LASTORE";
            case FASTORE: return "FASTORE";
            case DASTORE: return "DASTORE";
            case AASTORE: return "AASTORE";
            case BASTORE: return "BASTORE";
            case CASTORE: return "CASTORE";
            case SASTORE: return "SASTORE";
            case POP: return "POP";
            case POP2: return "POP2";
            case DUP: return "DUP";
            case DUP_X1: return "DUP_X1";
            case DUP_X2: return "DUP_X2";
            case DUP2: return "DUP2:";
            case DUP2_X1: return "DUP2_X1";
            case DUP2_X2: return "DUP2_X2";
            case SWAP: return "SWAP";
            case IADD: return "IADD";
            case LADD: return "LADD:";
            case FADD: return "FADD";
            case DADD: return "DADD";
            case ISUB: return "ISUB";
            case LSUB: return "LSUB";
            case FSUB: return "FSUB";
            case DSUB: return "DSUB";
            case IMUL: return "IMUL";
            case LMUL: return "LMUL";
            case FMUL: return "FMUL";
            case DMUL: return "DMUL";
            case IDIV: return "IDIV";
            case LDIV: return "LDIV";
            case FDIV: return "FDIV";
            case DDIV: return "DDIV";
            case IREM: return "IREM";
            case LREM: return "LREM";
            case FREM: return "FREM";
            case DREM: return "DREM";
            case INEG: return "INEG";
            case LNEG: return "LNEG";
            case FNEG: return "FNEG";
            case DNEG: return "DNEG";
            case ISHL: return "ISHL";
            case LSHL: return "LSHL";
            case ISHR: return "ISHR";
            case LSHR: return "LSHR";
            case IUSHR: return "IUSHR";
            case LUSHR: return "LUSHR";
            case IAND: return "IAND";
            case LAND: return "LAND";
            case IOR: return "IOR";
            case LOR: return "LOR";
            case IXOR: return "IXOR";
            case LXOR: return "LXOR";
            case IINC: return "IINC:";
            case I2L: return "I2L";
            case I2F: return "I2F:";
            case I2D: return "I2D";
            case L2I: return "L2I";
            case L2F: return "L2F";
            case L2D: return "L2D";
            case F2I: return "F2I";
            case F2L: return "F2L";
            case F2D: return "F2D";
            case D2I: return "D2I";
            case D2L: return "D2L";
            case D2F: return "D2F";
            case I2B: return "I2B";
            case I2C: return "I2C";
            case I2S: return "I2S";
            case LCMP: return "LCMP:";
            case FCMPL: return "FCMPL";
            case FCMPG: return "FCMPG";
            case DCMPL: return "DCMPL";
            case DCMPG: return "DCMPG";
            case IFEQ: return "IFEQ:";
            case IFNE: return "IFNE";
            case IFLT: return "IFLT";
            case IFGE: return "IFGE";
            case IFGT: return "IFGT";
            case IFLE: return "IFLE";
            case IF_ICMPEQ: return "IF_ICMPEQ";
            case IF_ICMPNE: return "IF_ICMPNE";
            case IF_ICMPLT: return "IF_ICMPLT";
            case IF_ICMPGE: return "IF_ICMPGE";
            case IF_ICMPGT: return "IF_ICMPGT";
            case IF_ICMPLE: return "IF_ICMPLE";
            case IF_ACMPEQ: return "IF_ACMPEQ";
            case IF_ACMPNE: return "IF_ACMPNE";
            case GOTO: return "GOTO";
            case JSR: return "JSR";
            case RET: return "RET";
            case TABLESWITCH: return "TABLESWITCH";
            case LOOKUPSWITCH: return "LOOKUPSWITCH";
            case IRETURN: return "IRETURN";
            case LRETURN: return "LRETURN";
            case FRETURN: return "FRETURN";
            case DRETURN: return "DRETURN";
            case ARETURN: return "ARETURN";
            case RETURN: return "RETURN";
            case GETSTATIC: return "GETSTATIC";
            case PUTSTATIC: return "PUTSTATIC";
            case GETFIELD: return "GETFIELD";
            case PUTFIELD: return "PUTFIELD";
            case INVOKEVIRTUAL: return "INVOKEVIRTUAL";
            case INVOKESPECIAL: return "INVOKESPECIAL";
            case INVOKESTATIC: return "INVOKESTATIC";
            case INVOKEINTERFACE: return "INVOKEINTERFACE";
            case INVOKEDYNAMIC: return "INVOKEDYNAMIC";
            case NEW: return "NEW:";
            case NEWARRAY: return "NEWARRAY";
            case ANEWARRAY: return "ANEWARRAY";
            case ARRAYLENGTH: return "ARRAYLENGTH";
            case ATHROW: return "ATHROW";
            case CHECKCAST: return "CHECKCAST";
            case INSTANCEOF: return "INSTANCEOF";
            case MONITORENTER: return "MONITORENTER";
            case MONITOREXIT: return "MONITOREXIT";
            case MULTIANEWARRAY: return "MULTIANEWARRAY";
            case IFNULL: return "IFNULL";
            case IFNONNULL: return "IFNONNULL";
            default: return "UNKNOWN";
        }
    }
    
    public static Optional<MethodNode> findMethod(ClassNode owner, String name, String description){
        for(MethodNode method : owner.methods){
            if(
                method.name.equals(name) &&
                method.desc.equals(description)
            ){
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }
    
    public static MethodNode findOrCreateMethod(ClassNode classNode, int access, String owner, String name, String description, String... exceptions){
        Optional<MethodNode> optionalMethod = findMethod(classNode, name, description);
        if(optionalMethod.isPresent()){
            return optionalMethod.get();
        }else{
            // LOCALVARIABLE this Lnet/minecraft/util/Util$1; L0 L5 0
            MethodVisitor visitor = classNode.visitMethod(access, name, description, null, exceptions);
            visitor.visitCode();
            Label start = new Label();
            visitor.visitLabel(start);
            visitor.visitVarInsn(ALOAD, 0);
            visitor.visitMethodInsn(
                INVOKESPECIAL,
                owner,
                name,
                description,
                false
            );
            visitor.visitInsn(RETURN);
            Label end = new Label();
            visitor.visitLabel(end);
            
            visitor.visitLocalVariable("this", "L" + owner + ";", null, start, end, 0);
            visitor.visitMaxs(1, 1);
            
            visitor.visitEnd();
            return findMethod(classNode, name, description).get();
        }
    }
    
    public static int opcodeFromHandle(Handle handle){
        switch(handle.getTag()){
            case H_GETFIELD: return GETFIELD;
            case H_GETSTATIC: return GETSTATIC;
            case H_PUTFIELD: return PUTFIELD;
            case H_PUTSTATIC: return PUTSTATIC;
            case H_INVOKEVIRTUAL: return INVOKEVIRTUAL;
            case H_INVOKESTATIC: return INVOKESTATIC;
            case H_INVOKESPECIAL: return INVOKESPECIAL;
            case H_NEWINVOKESPECIAL: return NEW;
            case H_INVOKEINTERFACE: return INVOKEINTERFACE;
            default: throw new IllegalArgumentException(String.format(
                "Unknown handle type: %d",
                handle.getTag()
            ));
        }
    }
}
