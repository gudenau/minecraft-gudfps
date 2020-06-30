package net.gudenau.minecraft.fps.transformer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.gudenau.minecraft.fps.util.AsmUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class ForEachRemover implements Transformer{
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = false;
        
        for(MethodNode method : classNode.methods){
            Set<MethodInsnNode> nodes = AsmUtils.findMethodCalls(
                method.instructions,
                INVOKEINTERFACE,
                "java/util/List",
                "forEach",
                "(Ljava/util/function/Consumer;)V"
            );
            if(nodes.isEmpty()){
                continue;
            }
            System.out.printf("%s.%s%s\n", classNode.name, method.name, method.desc);
            for(MethodInsnNode node : nodes){
                removeForEach(method, node);
            }
            changed = true;
        }
        
        if(changed){
            flags.requestMaxes();
        }
        
        return changed;
    }
    
    private void removeForEach(MethodNode method, MethodInsnNode methodNode){
        InvokeDynamicInsnNode dynamicNode = AsmUtils.getLastNode(methodNode, INVOKEDYNAMIC);
        if(dynamicNode == null){
            return;
        }
        
        /*
        Stack layout before INVOKEDYNAMIC:
         - List to iterate
         - Lambda captures
         
        Stack layout after INVOKEDYNAMIC:
         - List to iterate
         - Lambda handle
         */
        
        Handle lambdaHandle = (Handle)dynamicNode.bsmArgs[1];
        Type[] lambdaHandleArguments = Type.getType(lambdaHandle.getDesc()).getArgumentTypes();
        Type lambdaArgument = (Type)dynamicNode.bsmArgs[2];
        String lambdaArgumentClass = lambdaArgument.getArgumentTypes()[0].getInternalName();
        
        Type[] lambdaParams = Type.getType(lambdaHandle.getDesc()).getArgumentTypes();
        
        List<VarInsnNode> lambdaCaptures = AsmUtils.getLastNodes(dynamicNode, lambdaParams.length - 1);
        
        int iteratorLocation = method.maxLocals;
        int argumentLocation = iteratorLocation + 1;
    
        InsnList patch = new InsnList();
        // Iterator<T> iterator = List<T>.iterator();
        patch.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/List", "iterator", "()Ljava/util/Iterator;", true));
        patch.add(new VarInsnNode(ASTORE, iteratorLocation));
        
        // while(iterator.hasNext()){
        LabelNode loopNode = new LabelNode();
        patch.add(loopNode);
        
        Object[] locals = new Object[lambdaCaptures.size() + 2];
        locals[0] = "java/util/List";
        for(int i = 0; i < locals.length - 2; i++){
            locals[i + 1] = lambdaHandleArguments[i].getInternalName();
        }
        locals[locals.length - 1] = "java/util/Iterator";
        
        patch.add(new FrameNode(
            F_FULL,
            locals.length,
            locals,
            0,
            null
        ));
        patch.add(new VarInsnNode(ALOAD, iteratorLocation));
        patch.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true));
        LabelNode jumpNode = new LabelNode();
        patch.add(new JumpInsnNode(IFEQ, jumpNode));
        
        //     T element = iterator.next();
        patch.add(new VarInsnNode(ALOAD, iteratorLocation));
        patch.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true));
        patch.add(new TypeInsnNode(CHECKCAST, lambdaArgumentClass));
        patch.add(new VarInsnNode(ASTORE, argumentLocation));
        
        //    lambda(captures, element);
        Collections.reverse(lambdaCaptures);
        lambdaCaptures.forEach((node)->
            patch.add(new VarInsnNode(node.getOpcode(), node.var))
        );
        patch.add(new VarInsnNode(ALOAD, argumentLocation));
        patch.add(new MethodInsnNode(
            AsmUtils.handleTagToOpcode(lambdaHandle.getTag()),
            lambdaHandle.getOwner(),
            lambdaHandle.getName(),
            lambdaHandle.getDesc(),
            lambdaHandle.isInterface()
        ));
        
        // }
        patch.add(new JumpInsnNode(GOTO, loopNode));
        patch.add(new FrameNode(F_CHOP, 1, null, 0, null));
        
        InsnList insnList = method.instructions;
        
        insnList.insert(methodNode, patch);
        lambdaCaptures.forEach(insnList::remove);
        insnList.remove(dynamicNode);
        insnList.remove(methodNode);
    }
}
