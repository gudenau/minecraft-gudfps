package net.gudenau.minecraft.fps.util;

import net.gudenau.minecraft.asm.api.v0.AsmUtils;
import net.gudenau.minecraft.asm.api.v0.TypeCache;
import net.gudenau.minecraft.asm.api.v0.type.MethodType;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class StagingAsmUtils{
    private static String remapSignature(String signature, Map<Type, Type> typeMap){
        //TODO
        return signature;
    }

    private static boolean remapStringTypes(List<String> types, Map<Type, Type> typeMap){
        if(types == null || types.isEmpty()){
            return false;
        }

        TypeCache Type = TypeCache.getTypeCache();
        boolean changed = false;

        for(int i = 0; i < types.size(); i++){
            Type oldType = Type.getObjectType(types.get(i));
            Type newType = typeMap.get(oldType);
            if(newType != null){
                types.set(i, newType.getInternalName());
                changed = true;
            }
        }

        return changed;
    }

    public static boolean remapTypes(ClassNode classNode, Map<Type, Type> typeMap){
        boolean changed = false;

        classNode.signature = remapSignature(classNode.signature, typeMap);

        Type oldType = Type.getObjectType(classNode.superName);
        Type newType = typeMap.get(oldType);
        if(newType != null){
            classNode.superName = newType.getInternalName();
            changed = true;
        }

        changed |= remapStringTypes(classNode.interfaces, typeMap);

        if(classNode.outerClass != null){
            oldType = Type.getObjectType(classNode.outerClass);
            newType = typeMap.get(oldType);
            if(newType != null){
                classNode.outerClass = newType.getInternalName();
                changed = true;
            }
        }

        changed |= remapAnnotations(classNode, typeMap);

        List<InnerClassNode> innerClasses = classNode.innerClasses;
        if(innerClasses != null && !innerClasses.isEmpty()){
            for(InnerClassNode innerClass : innerClasses){
                oldType = Type.getObjectType(innerClass.name);
                newType = typeMap.get(oldType);
                if(newType != null){
                    innerClass.name = newType.getInternalName();
                    changed = true;

                    if(innerClass.innerName != null){
                        innerClass.innerName = innerClass.name.substring(innerClass.name.lastIndexOf('$') + 1);
                    }
                }

                if(innerClass.outerName != null){
                    oldType = Type.getObjectType(innerClass.outerName);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        innerClass.outerName = newType.getInternalName();
                        changed = true;
                    }
                }
            }
        }

        List<FieldNode> fields = classNode.fields;
        if(fields != null && !fields.isEmpty()){
            for(FieldNode field : fields){
                oldType = Type.getType(field.desc);
                newType = typeMap.get(oldType);
                if(newType != null){
                    field.desc = newType.getDescriptor();
                    field.signature = remapSignature(field.signature, typeMap);
                    changed = true;
                }
            }
        }

        List<MethodNode> methods = classNode.methods;
        if(methods != null && !methods.isEmpty()){
            for(MethodNode method : methods){
                changed |= remapTypes(method, typeMap);
            }
        }

        return changed;
    }

    public static boolean remapTypes(MethodNode method, Map<Type, Type> typeMap){
        boolean changed = false;

        Type oldType = Type.getMethodType(method.desc);
        Type newType = remapMethodType(oldType, typeMap);
        if(newType != null){
            method.desc = newType.getDescriptor();
            changed = true;
        }

        String oldSignature = method.signature;
        method.signature = remapSignature(method.signature, typeMap);
        changed |= !Objects.equals(oldSignature, method.signature);

        changed |= remapStringTypes(method.exceptions, typeMap);
        changed |= remapAnnotations(method, typeMap);

        List<LocalVariableNode> localVariables = method.localVariables;
        if(localVariables != null && !localVariables.isEmpty()){
            for(LocalVariableNode localVariable : localVariables){
                oldType = Type.getType(localVariable.desc);
                newType = typeMap.get(oldType);
                if(newType != null){
                    changed = true;
                    localVariable.desc = newType.getDescriptor();
                    localVariable.signature = remapSignature(localVariable.signature, typeMap);
                }
            }
        }

        for(AbstractInsnNode instruction : method.instructions){
            switch(instruction.getType()){
                case AbstractInsnNode.TYPE_INSN:{
                    TypeInsnNode node = (TypeInsnNode)instruction;
                    oldType = Type.getObjectType(node.desc);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        node.desc = newType.getInternalName();
                        changed = true;
                    }
                } break;

                case AbstractInsnNode.FIELD_INSN:{
                    FieldInsnNode node = (FieldInsnNode)instruction;

                    oldType = Type.getObjectType(node.owner);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        node.owner = newType.getInternalName();
                        changed = true;
                    }

                    oldType = Type.getType(node.desc);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        node.desc = newType.getDescriptor();
                        changed = true;
                    }
                } break;

                case AbstractInsnNode.METHOD_INSN:{
                    MethodInsnNode node = (MethodInsnNode)instruction;

                    oldType = Type.getObjectType(node.owner);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        node.owner = newType.getInternalName();
                        changed = true;
                    }

                    oldType = Type.getType(node.desc);
                    newType = remapMethodType(oldType, typeMap);
                    if(newType != null){
                        node.desc = newType.getDescriptor();
                        changed = true;
                    }
                } break;

                case AbstractInsnNode.INVOKE_DYNAMIC_INSN:{
                    InvokeDynamicInsnNode node = (InvokeDynamicInsnNode)instruction;

                    oldType = Type.getMethodType(node.desc);
                    newType = remapMethodType(oldType, typeMap);
                    if(newType != null){
                        node.desc = newType.getDescriptor();
                        changed = true;
                    }

                    Handle newHandle = remapHandle(node.bsm, typeMap);
                    if(newHandle != null){
                        node.bsm = newHandle;
                        changed = true;
                    }

                    Object[] bsmArgs = node.bsmArgs;
                    for(int i = 0; i < bsmArgs.length; i++){
                        Object arg = bsmArgs[i];
                        if(arg instanceof Type){
                            newType = typeMap.get(arg);
                            if(newType != null){
                                bsmArgs[i] = newType;
                                changed = true;
                            }
                        }else if(arg instanceof Handle){
                            newHandle = remapHandle(node.bsm, typeMap);
                            if(newHandle != null){
                                bsmArgs[i] = newHandle;
                                changed = true;
                            }
                        }
                    }
                } break;

                case AbstractInsnNode.LDC_INSN:{
                    LdcInsnNode node = (LdcInsnNode)instruction;
                    if(node.cst instanceof Type){
                        newType = typeMap.get(node.cst);
                        if(newType != null){
                            node.cst = newType;
                            changed = true;
                        }
                    }
                } break;

                case AbstractInsnNode.MULTIANEWARRAY_INSN:{
                    MultiANewArrayInsnNode node = (MultiANewArrayInsnNode)instruction;
                    oldType = Type.getType(node.desc);
                    newType = typeMap.get(oldType);
                    if(newType != null){
                        node.desc = newType.getDescriptor();
                        changed = true;
                    }
                }break;

                case AbstractInsnNode.FRAME:{
                    FrameNode node = (FrameNode)instruction;

                    List<Object> locals = node.local;
                    if(locals != null && !locals.isEmpty()){
                        for(int i = 0; i < locals.size(); i++){
                            Object local = locals.get(i);
                            if(local instanceof String){
                                oldType = Type.getObjectType((String)local);
                                newType = typeMap.get(oldType);
                                if(newType != null){
                                    locals.set(i, newType.getInternalName());
                                    changed = true;
                                }
                            }
                        }
                    }

                    List<Object> stacks = node.stack;
                    if(stacks != null && !stacks.isEmpty()){
                        for(int i = 0; i < stacks.size(); i++){
                            Object stack = stacks.get(i);
                            if(stack instanceof String){
                                oldType = Type.getObjectType((String)stack);
                                newType = typeMap.get(oldType);
                                if(newType != null){
                                    stacks.set(i, newType.getInternalName());
                                    changed = true;
                                }
                            }
                        }
                    }
                }break;
            }
        }

        return changed;
    }

    private static Handle remapHandle(Handle handle, Map<Type, Type> typeMap){
        boolean changed = false;

        Type oldType = Type.getObjectType(handle.getOwner());
        Type newType = typeMap.get(oldType);
        String owner;
        if(newType == null){
            owner = handle.getOwner();
        }else{
            owner = newType.getInternalName();
            changed = true;
        }

        oldType = Type.getMethodType(handle.getDesc());
        newType = remapMethodType(oldType, typeMap);
        String desc;
        if(newType == null){
            desc = oldType.getDescriptor();
        }else{
            desc = newType.getDescriptor();
            changed = true;
        }

        return changed ? new Handle(
            handle.getTag(),
            owner,
            handle.getName(),
            desc,
            handle.isInterface()
        ) : null;
    }

    public static Type remapMethodType(Type methodType, Map<Type, Type> typeMap){
        boolean changed = false;

        Type oldType;
        Type newType;

        Type[] params = methodType.getArgumentTypes();
        params = Arrays.copyOf(params, params.length);
        for(int i = 0; i < params.length; i++){
            oldType = params[i];
            newType = typeMap.getOrDefault(oldType, oldType);
            if(!newType.equals(oldType)){
                changed = true;
                params[i] = newType;
            }
        }

        oldType = methodType.getReturnType();
        newType = typeMap.getOrDefault(oldType, oldType);
        if(!newType.equals(oldType)){
            changed = true;
        }

        return changed ? Type.getMethodType(newType, params) : null;
    }

    public static boolean remapAnnotations(ClassNode owner, Map<Type, Type> typeMap){
        return remapAnnotations(owner.visibleAnnotations, owner.invisibleAnnotations, typeMap);
    }

    public static boolean remapAnnotations(MethodNode method, Map<Type, Type> typeMap){
        return remapAnnotations(method.visibleAnnotations, method.invisibleAnnotations, typeMap);
    }

    public static boolean remapAnnotations(List<AnnotationNode> visible, List<AnnotationNode> invisible, Map<Type, Type> typeMap){
        boolean changed = false;

        if(visible != null && !visible.isEmpty()){
            for(AnnotationNode annotation : visible){
                Type oldType = Type.getObjectType(annotation.desc);
                Type newType = typeMap.get(oldType);
                if(newType != null){
                    annotation.desc = newType.getInternalName();
                    changed = true;
                }
            }
        }

        if(invisible != null && !invisible.isEmpty()){
            for(AnnotationNode annotation : invisible){
                Type oldType = Type.getObjectType(annotation.desc);
                Type newType = typeMap.get(oldType);
                if(newType != null){
                    annotation.desc = newType.getInternalName();
                    changed = true;
                }
            }
        }

        return changed;
    }

    public static MethodNode findOrCreateMethod(ClassNode classNode, int access, String owner, String name, String description, String... exceptions){
        Optional<MethodNode> optionalMethod = AsmUtils.getInstance().findMethod(classNode, name, description);
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
            return AsmUtils.getInstance().findMethod(classNode, name, description).get();
        }
    }
    
    public static InsnList createMethodCall(int opcode, MethodType method){
        InsnList patch = new InsnList();
        //       final int opcode, final String owner, final String name, final String descriptor
        switch(opcode){
            case INVOKESTATIC:{
                patch.add(new MethodInsnNode(
                    opcode,
                    method.getOwner().getInternalName(),
                    method.getName(),
                    method.getDescriptor().getDescriptor(),
                    false
                ));
            } break;
            
            case INVOKEINTERFACE:
            case INVOKESPECIAL:
            case INVOKEVIRTUAL:{
                new RuntimeException(String.format(
                    "Implement createMethodCall with opcode %s",
                    AsmUtils.getInstance().getOpcodeName(opcode)
                )).printStackTrace();
                System.exit(0);
            } break;
            
            default:{
                throw new RuntimeException(String.format(
                    "Opcode %s is not a legal method opcode",
                    opcode
                ));
            } /* break; */
        }
        return patch;
    }

    public static boolean replaceConstructors(ClassNode classNode, Type target, Type replacement){
        AsmUtils utils = AsmUtils.getInstance();
        boolean changed = false;

        String owner = target.getInternalName();
        MethodType constructor = new MethodType(target, "<init>", Type.VOID_TYPE);

        for(MethodNode method : classNode.methods){
            InsnList instructions = method.instructions;
            for(TypeInsnNode newNode : utils.<TypeInsnNode>findMatchingNodes(instructions, (node)->{
                if(node.getOpcode() == NEW){
                    TypeInsnNode typeNode = (TypeInsnNode)node;
                    return owner.equals(typeNode.desc);
                }
                return false;
            })){
                MethodInsnNode initNode = utils.findNextMethodCall(newNode, AsmUtils.METHOD_FLAG_IGNORE_DESCRIPTION, INVOKESPECIAL, constructor).get();
                initNode.setOpcode(INVOKESTATIC);
                initNode.owner = replacement.getInternalName();
                initNode.name = "init";
                String desc = initNode.desc;
                initNode.desc = desc.substring(0, desc.length() - 1) + target.getDescriptor();
                instructions.remove(newNode.getNext());
                instructions.remove(newNode);
                changed = true;
            }
        }
        return changed;
    }
}
