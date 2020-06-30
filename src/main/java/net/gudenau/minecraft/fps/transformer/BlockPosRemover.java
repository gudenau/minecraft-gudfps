package net.gudenau.minecraft.fps.transformer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.gudenau.minecraft.fps.util.ArrayUtils;
import net.gudenau.minecraft.fps.util.AsmUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.FRAME;

public class BlockPosRemover implements Transformer{
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        if(classNode.name.startsWith("net/minecraft/util/math/BlockPos")){
            classNode.methods.forEach((method)->{
                InsnList instructions = method.instructions;
                AsmUtils.findReturns(instructions).forEach((instruction)->{
                    method.instructions.insertBefore(
                        instruction,
                        AsmUtils.throwException(
                            "java/lang/RuntimeException",
                            String.format("BlockPos.%s%s was called!", method.name, method.desc)
                        )
                    );
                    method.instructions.remove(instruction);
                });
                System.out.println(method.name + method.desc);
            });
            System.exit(0);
            flags.requestMaxes();
            return true;
        }
    
        classNode.fields.forEach((field)->
            field.desc = fixType(field.desc)
        );
        
        classNode.methods.forEach((method)->{
            if(classNode.name.equals("net/minecraft/util/Util") && method.name.equals("createServerWorkerExecutor")){
                System.out.print("");
            }
            
            InsnList instructions = method.instructions;
    
            Type oldMethodDesc = Type.getType(method.desc);
            Type newMethodDesc = fixType(oldMethodDesc);
            method.desc = newMethodDesc.getDescriptor();
            //FIXME
            method.signature = null;
            
            List<LocalVariableNode> localVariables = method.localVariables;
            if(localVariables != null && !localVariables.isEmpty()){
                List<LabelNode> labels = AsmUtils.findNodesOrdered(instructions, LabelNode.class);
                Object2IntMap<LabelNode> labelToPositions = new Object2IntOpenHashMap<>();
                Int2ObjectMap<LabelNode> positionToLabels = new Int2ObjectOpenHashMap<>();
                for(int i = 0; i < labels.size(); i++){
                    LabelNode label = labels.get(i);
                    labelToPositions.put(label, i);
                    positionToLabels.put(i, label);
                }
                
                List<LocalVariableNode> sortedLocals = new ArrayList<>(localVariables);
                sortedLocals.sort((a, b)->{
                    int startA = labelToPositions.getInt(a.start);
                    int startB = labelToPositions.getInt(b.start);
                    if(startA != startB){
                        return Integer.compare(startA, startB);
                    }else{
                        return Integer.compare(a.index, b.index);
                    }
                });
                
                Int2ObjectMap<ArrayList<LocalVariableNode>> starts = new Int2ObjectOpenHashMap<>();
                Int2ObjectMap<ArrayList<LocalVariableNode>> ends = new Int2ObjectOpenHashMap<>();
    
                for(LocalVariableNode local : sortedLocals){
                    starts.computeIfAbsent(labelToPositions.getInt(local.start), (k)->new ArrayList<>()).add(local);
                    ends.computeIfAbsent(labelToPositions.getInt(local.end), (k)->new ArrayList<>()).add(local);
                }
                
                Int2ObjectMap<LocalVariableNode> usedLocals = new Int2ObjectOpenHashMap<>();
    
                Int2IntMap positionMap = new Int2IntOpenHashMap();
                
                boolean unityLocals = false;
                
                for(AbstractInsnNode instruction : instructions){
                    if(instruction instanceof LabelNode){
                        int location = labelToPositions.getInt(instruction);
    
                        ArrayList<LocalVariableNode> locals = ends.get(location);
                        if(locals != null){
                            for(LocalVariableNode local : locals){
                                int size = Type.getType(local.desc).getSize();
                                if(size == 0){
                                    continue;
                                }
                                
                                positionMap.remove(local.index);
                                for(int i = 0; i < size; i++){
                                    usedLocals.remove(local.index + i);
                                }
                            }
                            
                            if(!unityLocals){
                                unityLocals = true;
                                for(Int2IntMap.Entry entry : positionMap.int2IntEntrySet()){
                                    if(entry.getIntKey() != entry.getIntValue()){
                                        unityLocals = false;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        locals = starts.get(location);
                        if(locals != null){
                            for(LocalVariableNode local : locals){
                                int size = Type.getType(local.desc).getSize();
                                if(size == 0){
                                    continue;
                                }
                                int localOffset;
                                outer:
                                for(localOffset = 0; true; localOffset++){
                                    for(int i = 0; i < size; i++){
                                        if(usedLocals.containsKey(localOffset)){
                                            continue outer;
                                        }
                                    }
                                    break;
                                }
                                positionMap.put(local.index, localOffset);
                                local.index = localOffset;
                                for(int i = 0; i < size; i++){
                                    usedLocals.put(localOffset + i, local);
                                }
                            }
                            
                            unityLocals = true;
                            for(Int2IntMap.Entry entry : positionMap.int2IntEntrySet()){
                                if(entry.getIntKey() != entry.getIntValue()){
                                    unityLocals = false;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if(!unityLocals && instruction instanceof VarInsnNode){
                        VarInsnNode var = (VarInsnNode)instruction;
                        var.var = positionMap.computeIfAbsent(var.var, (k)->{
                            System.err.printf(
                                "Unknown var %d in %s.%s%s for opcode %s\n",
                                k,
                                classNode.name.replaceAll("/", "."),
                                method.name,
                                method.desc,
                                AsmUtils.opcodeName(var.getOpcode())
                            );
                            return k;
                        });
                    }
                }
            }
            
            AsmUtils.findNodes(instructions, (node)->
                node.getOpcode() == NEW && ((TypeInsnNode)node).desc.equals("net/minecraft/util/math/BlockPos")
            ).forEach(instructions::remove);
            
            AsmUtils.<FrameNode>findNodes(instructions, (node)->
                node.getType() == FRAME
            ).forEach((node)->{
                fixFrameTypes(node.local);
                fixFrameTypes(node.stack);
            });

            AsmUtils.<MethodInsnNode>findNodes(
                instructions,
                (node)->node instanceof MethodInsnNode
            ).forEach((node)->
                fixMethodReference(method, node)
            );
            
            AsmUtils.<FieldInsnNode>findNodes(
                instructions,
                (node)->node instanceof FieldInsnNode
            ).forEach((node)->
                node.desc = fixType(node.desc)
            );
        });
        
        //TODO
        return true;
    }
    
    private static final int[] LOAD_OPS = {
        BALOAD, CALOAD, SALOAD, IALOAD, FALOAD, LALOAD, DALOAD, AALOAD
    };
    
    // The ASM one is very limited
    private int getVarOpcode(Type type, int opcode){
        boolean isLoad = ArrayUtils.contains(LOAD_OPS, opcode);
        return type.getOpcode(isLoad ? IALOAD : IASTORE);
    }
    
    private void fixFrameTypes(List<Object> locals){
        if(locals == null){
            return;
        }
        
        for(int i = 0; i < locals.size(); i++){
            Object local = locals.get(i);
            if(local instanceof String){
                locals.set(i, fixDesc((String)local));
            }
        }
    }
    
    private void fixMethodReference(MethodNode method, MethodInsnNode node){
        if(node.owner.equals("net/minecraft/util/math/BlockPos")){
            node.owner = "net/gudenau/minecraft/fps/fixes/BlockPosFixes";
            
            if(node.name.equals("<init>")){
                node.name = "init";
            }
            
            InsnList patch = new InsnList();
            if(node.getOpcode() == INVOKEVIRTUAL){
                Type methodType = Type.getType(node.desc);
                patch.add(new MethodInsnNode(
                    INVOKESTATIC,
                    node.owner,
                    node.name,
                    Type.getMethodDescriptor(Type.LONG_TYPE, ArrayUtils.prefix(Type.LONG_TYPE, fixTypes(methodType.getArgumentTypes()))),
                    false
                ));
            }else{
                patch.add(new MethodInsnNode(
                    INVOKESTATIC,
                    node.owner,
                    node.name,
                    fixType(node.desc),
                    false
                ));
            }
            InsnList instructions = method.instructions;
            instructions.insertBefore(node, patch);
            instructions.remove(node);
        }else{
            node.desc = fixType(node.desc);
        }
    }
    
    private String fixDesc(String desc){
        if(desc.startsWith("net/minecraft/util/math/BlockPos")){
            return "J";
        }else{
            return desc;
        }
    }
    
    private String fixType(String type){
        return fixType(Type.getType(type)).getDescriptor();
    }
    
    private Type fixType(Type type){
        if(type.getSort() == Type.OBJECT){
            if(type.getInternalName().startsWith("net/minecraft/util/math/BlockPos")){
                return Type.LONG_TYPE;
            }
        }else if(type.getSort() == Type.METHOD){
            return Type.getMethodType(
                fixType(type.getReturnType()),
                fixTypes(type.getArgumentTypes())
            );
        }else if(type.getSort() == Type.ARRAY){
            if(type.getElementType().getInternalName().startsWith("net/minecraft/util/math/BlockPos")){
                // *crys in outdated java*
                //return Type.getType("[".repeat(type.getDimensions()) + "J");
                final char[] single = new char[type.getDimensions()];
                Arrays.fill(single, 'J');
                return Type.getType(new String(single) + "J");
            }
        }
        return type;
    }
    
    private Type[] fixTypes(Type[] types){
        for(int i = 0; i < types.length; i++){
            types[i] = fixType(types[i]);
        }
        return types;
    }
}
