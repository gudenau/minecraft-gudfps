package net.gudenau.minecraft.fps.transformer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.gudenau.minecraft.asm.api.v0.Identifier;
import net.gudenau.minecraft.asm.api.v0.Transformer;
import net.gudenau.minecraft.fps.util.ArrayUtils;
import net.gudenau.minecraft.fps.util.AsmUtils;
import net.gudenau.minecraft.fps.util.Stats;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class BlockPosRemover implements Transformer{
    private static final String NAME_BlockPos;
    
    private static final Set<Type> BLOCK_POS_TYPES;
    
    static{
        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
        NAME_BlockPos = resolver
            .mapClassName("intermediary", "net.minecraft.class_2338")
            .replaceAll("\\.", "/");
        String BlockPos$Mutable = resolver
            .mapClassName("intermediary", "net.minecraft.class_2338$class_2339")
            .replaceAll("\\.", "/");
        BLOCK_POS_TYPES = Arrays.stream(new Type[]{
            Type.getObjectType(NAME_BlockPos),
            Type.getObjectType(BlockPos$Mutable)
        }).collect(Collectors.toSet());
    }
    
    private static final Type BLOCK_POS_FIXES = Type.getObjectType("net/gudenau/minecraft/fps/fixes/BlockPosFixes");
    
    private static final Stats STATS = Stats.getStats("BlockPos Remover");
    
    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "blockpos_remover");
    }
    
    @Override
    public boolean handlesClass(String name, String transformedName){
        return true;
    }
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        if(classNode.name.startsWith(NAME_BlockPos)){
            STATS.incrementStat("gutted classes");
            classNode.methods.forEach((method)->{
                STATS.incrementStat("gutted methods");
                InsnList instructions = method.instructions;
                instructions.clear();
                instructions.add(AsmUtils.throwException(
                    RuntimeException.class,
                    String.format("BlockPos.%s%s was called", method.name, method.desc)
                ));
            });
            classNode.superName = Type.getInternalName(Object.class);
            flags.requestMaxes();
            return true;
        }
        
        AtomicBoolean modified = new AtomicBoolean(false);
        processMethods(classNode, modified);
        if(modified.get()){
            flags.requestFrames();
            flags.requestMaxes();
            return true;
        }
        return false;
    }
    
    private void processMethods(ClassNode owner, AtomicBoolean modified){
        List<MethodNode> methods = owner.methods;
        if(methods == null || methods.isEmpty()){
            return;
        }
        
        for(MethodNode method : methods){
            Type methodType = Type.getMethodType(method.desc);
            Type newMethodType = remapMethod(methodType);
            if(!newMethodType.equals(methodType)){
                modified.set(true);
                System.err.printf(
                    "Changed %s.%s from %s to %s\n",
                    owner.name,
                    method.name,
                    method.desc,
                    newMethodType.getDescriptor()
                );
                method.desc = newMethodType.getDescriptor();
            }
    
            boolean changed = false;
            List<LocalVariableNode> localVariables = method.localVariables;
            Map<LabelNode, Set<LocalVariableNode>> localNodeMap = new Object2ObjectOpenHashMap<>();
            Map<LocalVariableNode, Type> originalLocalTypes = new Object2ObjectOpenHashMap<>();
            if(localVariables != null && !localVariables.isEmpty()){
                for(LocalVariableNode local : localVariables){
                    Type desc = Type.getType(local.desc);
                    originalLocalTypes.put(local, desc);
                    if(BLOCK_POS_TYPES.contains(desc)){
                        desc = Type.LONG_TYPE;
                        local.desc = desc.getDescriptor();
                        changed = true;
                    }
                    localNodeMap.computeIfAbsent(local.start, (l)->new HashSet<>()).add(local);
                }
            }
    
            // Scan so we skip more expensive stuff on non-BlockPos methods
            if(!changed){
                outer:
                for(AbstractInsnNode instruction : method.instructions){
                    switch(instruction.getType()){
                        case AbstractInsnNode.TYPE_INSN:{
                            TypeInsnNode node = (TypeInsnNode)instruction;
                            if(BLOCK_POS_TYPES.contains(Type.getObjectType(node.desc))){
                                changed = true;
                                break outer;
                            }
                        } break;
                        
                        case AbstractInsnNode.FIELD_INSN:{
                            FieldInsnNode node = (FieldInsnNode)instruction;
                            if(BLOCK_POS_TYPES.contains(Type.getObjectType(node.desc))){
                                changed = true;
                                break outer;
                            }
                        } break;
    
                        case AbstractInsnNode.METHOD_INSN:{
                            MethodInsnNode node = (MethodInsnNode)instruction;
                            if(BLOCK_POS_TYPES.contains(Type.getObjectType(node.owner))){
                                changed = true;
                                break outer;
                            }
                            if(methodNeedsFixing(node.desc)){
                                changed = true;
                                break outer;
                            }
                        } break;
    
                        case AbstractInsnNode.INVOKE_DYNAMIC_INSN:{
                            InvokeDynamicInsnNode node = (InvokeDynamicInsnNode)instruction;
                            if(methodNeedsFixing(node.desc)){
                                changed = true;
                                break outer;
                            }
                            
                            for(Object bsmArg : node.bsmArgs){
                                if(bsmArg instanceof Type){
                                    Type type = (Type)bsmArg;
                                    switch(type.getSort()){
                                        case Type.METHOD:{
                                            if(methodNeedsFixing(type)){
                                                changed = true;
                                                break outer;
                                            }
                                        } break;
                                        
                                        case Type.ARRAY:{
                                            if(BLOCK_POS_TYPES.contains(type.getElementType())){
                                                changed = true;
                                                break outer;
                                            }
                                        } break;
                                        
                                        case Type.OBJECT:{
                                            if(BLOCK_POS_TYPES.contains(bsmArg)){
                                                changed = true;
                                                break outer;
                                            }
                                        } break;
                                    }
                                }else if(bsmArg instanceof Handle){
                                    Handle handle = (Handle)bsmArg;
                                    if(BLOCK_POS_TYPES.contains(Type.getObjectType(handle.getOwner()))){
                                        changed = true;
                                        break outer;
                                    }
                                    int tag = handle.getTag();
                                    if(
                                        Opcodes.H_INVOKEVIRTUAL == tag ||
                                        Opcodes.H_INVOKESTATIC == tag ||
                                        Opcodes.H_INVOKESPECIAL == tag ||
                                        Opcodes.H_NEWINVOKESPECIAL == tag ||
                                        Opcodes.H_INVOKEINTERFACE == tag
                                    ){
                                        if(methodNeedsFixing(handle.getDesc())){
                                            changed = true;
                                            break outer;
                                        }
                                    }
                                }
                            }
                        } break;
    
                        case AbstractInsnNode.LDC_INSN:{
                            LdcInsnNode node = (LdcInsnNode)instruction;
                            Object constant = node.cst;
                            if(constant instanceof Type && BLOCK_POS_TYPES.contains(constant)){
                                changed = true;
                                break outer;
                            }
                        } break;
    
                        case AbstractInsnNode.MULTIANEWARRAY_INSN:{
                            MultiANewArrayInsnNode node = (MultiANewArrayInsnNode)instruction;
                            Type desc = Type.getType(node.desc);
                            if(BLOCK_POS_TYPES.contains(desc)){
                                changed = true;
                                break outer;
                            }
                        } break;
                    }
                }
            }
            
            if(changed){
                if(method.signature != null){
                    //TODO
                    method.signature = null;
                }
                
                LocalInfo locals = new LocalInfo(owner, method, methodType, newMethodType);
                
                AbstractInsnNode instruction = method.instructions.getFirst();
                while(instruction != null){
                    AbstractInsnNode next = instruction.getNext();
                    if(next != null){
                        switch(next.getType()){
                            case AbstractInsnNode.FRAME:{
                                FrameNode node = (FrameNode)next;
                                switch(node.type){
                                    /* An expanded frame. See {@link ClassReader#EXPAND_FRAMES}. */
                                    case Opcodes.F_NEW:
                                        /* A compressed frame with complete frame data. */
                                    case Opcodes.F_FULL:{
                                        locals.clear();
                                    }
            
                                    /*
                                     * A compressed frame where locals are the same as the locals in the previous frame, except that
                                     * additional 1-3 locals are defined, and with an empty stack.
                                     */
                                    case Opcodes.F_APPEND:{
                                        locals.append(node.local);
                                    } break;
            
                                    /*
                                     * A compressed frame where locals are the same as the locals in the previous frame, except that
                                     * the last 1-3 locals are absent and with an empty stack.
                                     */
                                    case Opcodes.F_CHOP:{
                                        locals.chop(node.local.size());
                                    } break;
            
                                    /*
                                     * A compressed frame with exactly the same locals as the previous frame and with an empty stack.
                                     */
                                    case Opcodes.F_SAME: break;
            
                                    /*
                                     * A compressed frame with exactly the same locals as the previous frame and with a single value
                                     * on the stack.
                                     */
                                    case Opcodes.F_SAME1: break;
                                }
                            } break;
    
                            case AbstractInsnNode.LABEL:{
                                @SuppressWarnings("SuspiciousMethodCalls")
                                Set<LocalVariableNode> localNodes = localNodeMap.get(next);
                                if(localNodes != null){
                                    for(LocalVariableNode localNode : localNodes){
                                        localNode.index = locals.remapLocal(
                                            localNode.index,
                                            originalLocalTypes.get(localNode),
                                            Type.getType(localNode.desc)
                                        );
                                    }
                                }
                            } break;
                        }
                    }
                    
                    switch(instruction.getType()){
                        case AbstractInsnNode.FIELD_INSN:{
                            FieldInsnNode node = (FieldInsnNode)instruction;
                            Type desc = Type.getType(node.desc);
                            if(BLOCK_POS_TYPES.contains(desc)){
                                node.desc = Type.LONG_TYPE.getDescriptor();
                            }
                        } break;
                        
                        case AbstractInsnNode.METHOD_INSN:{
                            MethodInsnNode node = (MethodInsnNode)instruction;
                            Type methodOwner = Type.getObjectType(node.owner);
                            Type methodDesc = Type.getMethodType(node.desc);
                            if(BLOCK_POS_TYPES.contains(methodOwner)){
                                node.owner = BLOCK_POS_FIXES.getInternalName();
                                if(node.getOpcode() != Opcodes.INVOKESTATIC){
                                    Type returnType = methodDesc.getReturnType();
                                    methodDesc = Type.getMethodType(
                                        BLOCK_POS_TYPES.contains(returnType) ? Type.LONG_TYPE : returnType,
                                        ArrayUtils.prefix(Type.LONG_TYPE, remapArguments(methodDesc.getArgumentTypes()))
                                    );
                                }
                                node.setOpcode(Opcodes.INVOKESTATIC);
                            }else{
                                methodDesc = remapMethod(methodDesc);
                            }
                            node.desc = methodDesc.getDescriptor();
                        } break;
                        
                        case AbstractInsnNode.TYPE_INSN:{
                            TypeInsnNode node = (TypeInsnNode)instruction;
                            Type desc = Type.getObjectType(node.desc);
                            if(BLOCK_POS_TYPES.contains(desc)){
                                InsnList instructions = method.instructions;
                                instruction = node.getNext();
                                
                                switch(node.getOpcode()){
                                    case Opcodes.NEW:{
                                        instruction = transformNew(instructions, node);
                                    } break;
                                    
                                    case Opcodes.ANEWARRAY:{
                                        node.desc = Type.LONG_TYPE.getDescriptor();
                                    } break;
                                    
                                    case Opcodes.CHECKCAST:{
                                        instructions.remove(node);
                                    } break;
                                    
                                    case Opcodes.INSTANCEOF:{
                                        instructions.insertBefore(
                                            node,
                                            new InsnNode(Opcodes.ICONST_1)
                                        );
                                        instructions.remove(node);
                                    } break;
                                }
                                
                                continue;
                            }
                        } break;
                        
                        case AbstractInsnNode.VAR_INSN:{
                            VarInsnNode node = (VarInsnNode)instruction;
                            node.setOpcode(locals.remapOpcode(node.var, node.getOpcode()));
                            node.var = locals.remap(node.var);
                        } break;
                    }
    
                    instruction = instruction.getNext();
                }
                
                method.maxLocals = locals.getMaxLocals();
                modified.set(true);
            }
        }
    }
    
    private boolean methodNeedsFixing(String desc){
        return methodNeedsFixing(Type.getMethodType(desc));
    }
    
    private boolean methodNeedsFixing(Type methodType){
        if(BLOCK_POS_TYPES.contains(methodType.getReturnType())){
            return true;
        }
        for(Type arg : methodType.getArgumentTypes()){
            if(BLOCK_POS_TYPES.contains(arg)){
                return true;
            }
        }
        return false;
    }
    
    private static void test(){
        /*
        NEW net/minecraft/util/math/BlockPos
        DUP
        ICONST_1
        ICONST_1
        ICONST_1
        INVOKESPECIAL net/minecraft/util/math/BlockPos.<init> (III)V
        POP
        
        new BlockPos(1, 1, 1);
         */
        
        /*
        ICONST_1
        ICONST_1
        ICONST_1
        INVOKESTATIC net/gudenau/minecraft/fps/fixes/BlockPosFixes.init (III)J
        POP2
        
        BlockPosFixes.init(1, 1, 1);
         */
        
        /*
        ALOAD 0
        CHECKCAST net/minecraft/util/math/BlockPos$Mutable
        ASTORE 1
        
        BlockPos.Mutable mutable = (BlockPos.Mutable)pos;
        */
        
        /*
        ALOAD 0
        INSTANCEOF net/minecraft/util/math/BlockPos$Mutable
        IFEQ L5
        
        if(pos instanceof BlockPos.Mutable){
        
        }
        */
    }
    
    private AbstractInsnNode transformNew(InsnList instructions, TypeInsnNode node){
        MethodInsnNode init = AsmUtils.findInstruction(node, (isn)->
            isn.getType() == AbstractInsnNode.METHOD_INSN && ((MethodInsnNode)isn).name.equals("<init>")
        );
        
        String initDesc = init.desc;
        initDesc = initDesc.substring(0, initDesc.length() - 1) + 'J';
        instructions.insertBefore(init, new MethodInsnNode(
            Opcodes.INVOKESTATIC,
            BLOCK_POS_FIXES.getInternalName(),
            "init",
            initDesc,
            false
        ));
        instructions.remove(node.getNext());
        instructions.remove(node);
        instructions.remove(init);
        return init.getNext();
    }
    
    private Type remapMethod(Type original){
        Type returnType = original.getReturnType();
        if(BLOCK_POS_TYPES.contains(returnType)){
            returnType = Type.LONG_TYPE;
        }
        Type[] arguments = original.getArgumentTypes();
        return Type.getMethodType(
            returnType,
            remapArguments(arguments)
        );
    }
    
    private Type[] remapArguments(Type[] arguments){
        for(int i = 0; i < arguments.length; i++){
            Type argument = arguments[i];
            if(BLOCK_POS_TYPES.contains(argument)){
                arguments[i] = Type.LONG_TYPE;
            }
        }
        return arguments;
    }
    
    private final static class LocalInfo{
        private final ClassNode owner;
        private final MethodNode method;
        
        private final Stack<Type> originalLocals = new Stack<>();
        private final Stack<Type> newLocals = new Stack<>();
        private final Int2IntMap localMap = new Int2IntOpenHashMap();
        private final BitSet remapOpcode = new BitSet();
        private int lastOriginalLocal = 0;
        private int lastNewLocal = 0;
        private int maxLocals;
    
        private final Stack<Type> tempOriginalLocals = new Stack<>();
        private final Stack<Type> tempNewLocals = new Stack<>();
        private final Int2IntMap tempLocalMap = new Int2IntOpenHashMap();
        private final BitSet tempRemapOpcodes = new BitSet();
        private int tempLastOriginalLocal = 0;
        private int tempLastNewLocal = 0;
        
        public LocalInfo(ClassNode owner, MethodNode method, Type originalMethod, Type newMethod){
            this.owner = owner;
            this.method = method;
            
            if((method.access & Opcodes.ACC_STATIC) == 0){
                Type type = Type.getObjectType(owner.name);
                originalLocals.push(type);
                newLocals.push(type);
                lastOriginalLocal++;
                lastNewLocal++;
                localMap.put(0, 0);
            }
            
            Type[] originalParams = originalMethod.getArgumentTypes();
            Type[] newParams = newMethod.getArgumentTypes();
            
            for(int i = 0; i < originalParams.length; i++){
                Type originalParam = originalParams[i];
                Type newParam = newParams[i];
                
                if(!originalParam.equals(newParam)){
                    remapOpcode.set(lastOriginalLocal);
                }
                
                originalLocals.push(originalParam);
                newLocals.push(newParam);

                localMap.put(lastOriginalLocal, lastNewLocal);
                
                lastOriginalLocal += originalParam.getSize();
                lastNewLocal += newParam.getSize();
            }
            maxLocals = lastNewLocal;
        }
    
        private void clearTemp(){
            tempOriginalLocals.clear();
            tempNewLocals.clear();
            tempLocalMap.clear();
            tempRemapOpcodes.clear();
            tempLastOriginalLocal = 0;
            tempLastNewLocal = 0;
        }
    
        public void clear(){
            originalLocals.clear();
            newLocals.clear();
            localMap.clear();
            remapOpcode.clear();
            lastOriginalLocal = 0;
            lastNewLocal = 0;
            clearTemp();
        }
    
        public void append(List<Object> locals){
            clearTemp();
            for(Object local : locals){
                localMap.put(lastOriginalLocal, lastNewLocal);
                
                if(local instanceof String){
                    Type type = Type.getObjectType((String)local);
                    originalLocals.push(type);
                    lastOriginalLocal++;
                    lastNewLocal++;
                    if(BLOCK_POS_TYPES.contains(type)){
                        newLocals.push(Type.LONG_TYPE);
                        remapOpcode.set(lastOriginalLocal - 1);
                        lastNewLocal++;
                    }else{
                        newLocals.push(type);
                    }
                }else if(local instanceof Integer){
                    Type type = null;
                    switch((Integer)local){
                        case /*ITEM_TOP*/ 0:
                        case /*ITEM_NULL*/ 5:
                        case /*ITEM_UNINITIALIZED_THIS*/ 6:{
                            //FIXME?
                            type = Type.getType(Object.class);
                        } break;
    
                        case /*ITEM_INTEGER*/ 1:{
                            type = Type.INT_TYPE;
                        } break;
    
                        case /*ITEM_FLOAT*/ 2:{
                            type = Type.FLOAT_TYPE;
                        } break;
                        
                        case /*ITEM_DOUBLE*/ 3:{
                            type = Type.DOUBLE_TYPE;
                        } break;
                        
                        case /*ITEM_LONG*/ 4:{
                            type = Type.LONG_TYPE;
                        } break;
                    }
                    
                    originalLocals.push(type);
                    newLocals.push(type);
                    int size = type.getSize();
                    lastOriginalLocal += size;
                    lastNewLocal += size;
                }
            }
            
            maxLocals = Math.max(maxLocals, lastNewLocal);
        }
        
        public void chop(int count){
            clearTemp();
            while(count > 0){
                Type originalType = originalLocals.pop();
                Type newType = newLocals.pop();
                
                lastOriginalLocal -= originalType.getSize();
                lastNewLocal -= newType.getSize();
                
                localMap.remove(lastOriginalLocal);
                remapOpcode.clear(lastOriginalLocal);
                
                count--;
            }
        }
    
        public int remap(int local){
            int newLocal = localMap.getOrDefault(local, -1);
            if(newLocal == -1){
                newLocal = tempLocalMap.getOrDefault(local, -1);
            }
            if(newLocal == -1){
                System.err.printf("Error in %s.%s%s\n", owner.name, method.name, method.desc);
                System.err.printf("Local %s is not known!\n", local);
                
                System.err.println("\nOriginal locals:");
                dumpLocals(originalLocals);
    
                System.err.println("\nNew locals:");
                dumpLocals(newLocals);
                
                if(!tempOriginalLocals.isEmpty()){
                    System.err.println("\nTemp Original locals:");
                    dumpLocals(tempOriginalLocals);
    
                    System.err.println("\nTemp New locals:");
                    dumpLocals(tempNewLocals);
                    
                    tempRemapOpcodes.set(local);
                }
                
                System.err.println();
                System.err.flush();
                
                local = local + lastNewLocal - lastOriginalLocal + tempLastNewLocal - tempLastOriginalLocal;
                maxLocals = Math.max(maxLocals, local);
                return local;
            }else{
                return newLocal;
            }
        }
    
        private void dumpLocals(Stack<Type> originalLocals){
            int index = 0;
            for(Type type : originalLocals){
                System.err.printf("\t%d:\t%s\n", index, type.getDescriptor());
                int size = type.getSize();
                if(size == 2){
                    System.err.printf("\t%d:\tUNUSABLE\n", index + 1);
                }
                index += size;
            }
        }
    
        public int remapLocal(int local, Type originalType, Type newType){
            int newLocal = localMap.getOrDefault(local, -1);
            if(newLocal == -1){
                newLocal = tempLocalMap.getOrDefault(local, -1);
            }
            if(newLocal == -1){
                newLocal = tempLastNewLocal;
                
                tempOriginalLocals.push(originalType);
                tempNewLocals.push(newType);
                
                tempLastOriginalLocal += originalType.getSize();
                tempLastNewLocal += newType.getSize();
                
                if(!originalType.equals(newType)){
                    tempRemapOpcodes.set(newLocal);
                }
                
                tempLocalMap.put(local, newLocal);
            }
            if(newLocal == -1){
                newLocal = local + lastNewLocal - lastOriginalLocal + tempLastNewLocal - tempLastOriginalLocal;
                maxLocals = Math.max(newLocal, maxLocals);
            }
            return newLocal;
        }
        
        public int remapOpcode(int var, int opcode){
            if(remapOpcode.get(var) || tempRemapOpcodes.get(var)){
                if(opcode == Opcodes.ALOAD){
                    return Opcodes.LLOAD;
                }else if(opcode == Opcodes.ASTORE){
                    return Opcodes.LSTORE;
                }{
                    new RuntimeException("FIXME").printStackTrace();
                    System.exit(0);
                    return -1;
                }
            }
            return opcode;
        }
    
        public int getMaxLocals(){
            System.out.printf(
                "Max locals for %s.%s%s: %d\n",
                owner.name,
                method.name,
                method.desc,
                maxLocals
            );
            System.out.flush();
            return maxLocals;
        }
    }
}
