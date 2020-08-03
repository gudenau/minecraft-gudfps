package net.gudenau.minecraft.fps.transformer;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.gudenau.minecraft.fps.util.ArrayUtils;
import net.gudenau.minecraft.fps.util.AsmUtils;
import net.gudenau.minecraft.fps.util.Pair;
import org.apache.commons.lang3.BitField;
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
            InsnList instructions = method.instructions;
    
            Type oldMethodDesc = Type.getType(method.desc);
            Type newMethodDesc = fixType(oldMethodDesc);
            method.desc = newMethodDesc.getDescriptor();
            //FIXME
            method.signature = null;
    
            AsmUtils.<FrameNode>findNodes(instructions, (node)->
                node.getType() == FRAME
            ).forEach((node)->{
                fixFrameTypes(node.local);
                fixFrameTypes(node.stack);
            });
            
            List<LocalVariableNode> localVariables = method.localVariables;
            if(localVariables != null && !localVariables.isEmpty()){
                fixupLocals(oldMethodDesc, newMethodDesc, method);
            }
            
            AsmUtils.findNodes(instructions, (node)->
                node.getOpcode() == NEW && ((TypeInsnNode)node).desc.equals("net/minecraft/util/math/BlockPos")
            ).forEach(instructions::remove);

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
    
    private void fixupLocals(Type oldDescriptor, Type newDescriptor, MethodNode method){
        Int2IntMap localMap = new Int2IntOpenHashMap();
    
        { // Compute initial locals
            int oldLocals = 0;
            int newLocals = 0;
            if((method.access & ACC_STATIC) == 0){
                localMap.put(0, 0);
                oldLocals++;
                newLocals++;
            }
            
            Type[] oldArguments = oldDescriptor.getArgumentTypes();
            Type[] newArguments = newDescriptor.getArgumentTypes();
            for(int i = 0; i < oldArguments.length; i++){
                Type oldArgument = oldArguments[i];
                Type newArgument = newArguments[i];
                int oldSize = oldArgument.getSize();
                int newSize = newArgument.getSize();
                for(int o = 0; o < newSize; o++){
                    localMap.put(oldLocals + o, newLocals);
                }
                oldLocals += oldSize;
                newLocals += newSize;
            }
        }
        
        AbstractInsnNode instruction = method.instructions.getFirst();
        while(instruction != null){
            if(instruction instanceof FrameNode){
                FrameNode frame = (FrameNode)instruction;
                switch(frame.type){
                    default:{
                        System.out.println("Implement frame of type" + frame.type);
                    } break;
                }
            }else if(instruction instanceof VarInsnNode){
                VarInsnNode varNode = (VarInsnNode)instruction;
                varNode.var = localMap.getOrDefault(varNode.var, Byte.MAX_VALUE);
            }
            
            instruction = instruction.getNext();
        }
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
