package net.gudenau.minecraft.fps.transformer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.fps.util.ArrayUtils;
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
                instructions.add(AsmUtils.createExceptionList(
                    Type.getType(RuntimeException.class),
                    String.format("BlockPos.%s%s was called", method.name, method.desc)
                ));
            });
            classNode.superName = Type.getInternalName(Object.class);
            flags.requestMaxes();
            return true;
        }
        
        AtomicBoolean modified = new AtomicBoolean(false);
        processMethods(classNode, modified);
        return modified.get();
    }
    
    private void processMethods(ClassNode owner, AtomicBoolean modified){
        List<MethodNode> methods = owner.methods;
        if(methods == null || methods.isEmpty()){
            return;
        }
        
        for(MethodNode method : methods){
            AbstractInsnNode currentInstruction = method.instructions.getFirst();
            
            Int2ObjectMap<Type> localMap = new Int2ObjectOpenHashMap<>();
            List<AbstractInsnNode> currentInstructions = new LinkedList<>();
            List<MethodChunk> methodChunks = new ArrayList<>();
            
            if(currentInstruction instanceof LabelNode){
                for(LocalVariableNode localVariable : method.localVariables){
                    if(localVariable.start == currentInstruction){
                        localMap.put(localVariable.index, Type.getType(localVariable.desc));
                    }else if(localVariable.end == currentInstruction){
                        localMap.remove(localVariable.index);
                    }
                }
            }
            
            String methodName = owner.name + "." + method.name + method.desc;
            
            while(currentInstruction != null){
                AbstractInsnNode nextInstruction = currentInstruction.getNext();
                if(nextInstruction instanceof LabelNode){
                    if(!currentInstructions.isEmpty()){
                        methodChunks.add(new MethodChunk(currentInstructions, localMap, methodName));
                        currentInstructions.clear();
                    }
    
                    for(LocalVariableNode localVariable : method.localVariables){
                        if(localVariable.start == currentInstruction){
                            localMap.put(localVariable.index, Type.getType(localVariable.desc));
                        }else if(localVariable.end == currentInstruction){
                            localMap.remove(localVariable.index);
                        }
                    }
                    currentInstructions.add(nextInstruction);
                    nextInstruction = nextInstruction.getNext();
                }
                if(nextInstruction instanceof FrameNode){
                    FrameNode frame = (FrameNode)nextInstruction;
                    switch(frame.type){
                        // Replace all the locals
                        case Opcodes.F_NEW:
                        case Opcodes.F_FULL: {
                            localMap.clear();
                        }
                        
                        // Append 1-3 locals
                        case Opcodes.F_APPEND:{
                            int local = 0;
                            for(Int2ObjectMap.Entry<Type> typeEntry : localMap.int2ObjectEntrySet()){
                                local = Math.max(
                                    local,
                                    typeEntry.getIntKey() + typeEntry.getValue().getSize()
                                );
                            }
                            
                            for(Object o : frame.local){
                                Type type = null;
                                if(o instanceof Integer){
                                    if(o == Opcodes.TOP){
                                        //FIXME?
                                    }else if(o == Opcodes.INTEGER){
                                        type = Type.INT_TYPE;
                                    }else if(o == Opcodes.FLOAT){
                                        type = Type.FLOAT_TYPE;
                                    }else if(o == Opcodes.DOUBLE){
                                        type = Type.DOUBLE_TYPE;
                                    }else if(o == Opcodes.LONG){
                                        type = Type.LONG_TYPE;
                                    }else if(o == Opcodes.UNINITIALIZED_THIS){
                                        type = Type.getObjectType(owner.name);
                                    }else{
                                        throw new RuntimeException("Unknown stack local: " + o);
                                    }
                                }else if(o instanceof String){
                                    type = Type.getObjectType((String)o);
                                }else if(o instanceof LabelNode){
                                    type = Type.getType(Object.class);
                                }else{
                                    throw new RuntimeException("Unknown stack local: " + o);
                                }
        
                                if(type != null){
                                    localMap.put(local, type);
                                    local += type.getSize();
                                }
                            }
                        } break;
                        
                        // Remove 1-3 elements
                        case Opcodes.F_CHOP:{
                            int chop = frame.local.size();
                            while(chop > 0){
                                int maxLocal = 0;
                                for(int key : localMap.keySet()){
                                    maxLocal = Math.max(maxLocal, key);
                                }
                                localMap.remove(maxLocal);
                                chop--;
                            }
                        } break;
                        
                        // NOPs
                        case Opcodes.F_SAME:
                        case Opcodes.F_SAME1: break;
                    }
                }
                currentInstructions.add(currentInstruction);
                currentInstruction = nextInstruction;
            }
    
            for(MethodChunk methodChunk : methodChunks){
                methodChunk.remap();
            }
        }
    }
    
    private static class MethodChunk{
        private final List<AbstractInsnNode> instructions;
        private final Int2ObjectMap<Type> locals;
        private final String methodName;
        
        private MethodChunk(List<AbstractInsnNode> instructions, Int2ObjectMap<Type> locals, String methodName){
            this.instructions = new ArrayList<>(instructions);
            this.locals = new Int2ObjectOpenHashMap<>(locals);
            this.methodName = methodName;
        }
    
        public void remap(){
            Int2ObjectMap<Type> newLocals = new Int2ObjectOpenHashMap<>(locals.size());
            Int2IntMap localMap = new Int2IntOpenHashMap(locals.size());
            
            int offset = 0;
            for(Int2ObjectMap.Entry<Type> entry : locals.int2ObjectEntrySet()){
                int entryIndex = entry.getIntKey();
                Type entryType = entry.getValue();
                localMap.put(entryIndex, entryIndex + offset);
                if(BLOCK_POS_TYPES.contains(entryType)){
                    newLocals.put(entryIndex + offset, Type.LONG_TYPE);
                    offset++;
                }else{
                    newLocals.put(entryIndex + offset, entryType);
                }
            }
            
            for(AbstractInsnNode instruction : instructions){
                switch(instruction.getType()){
                    case AbstractInsnNode.VAR_INSN:{
                        VarInsnNode varNode = (VarInsnNode)instruction;
                        varNode.var = localMap.computeIfAbsent(varNode.var, (local)->{
                            throw new RuntimeException("Failed to find local " + local + " for method " + methodName);
                        });
                    } break;
                }
            }
        }
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
}
