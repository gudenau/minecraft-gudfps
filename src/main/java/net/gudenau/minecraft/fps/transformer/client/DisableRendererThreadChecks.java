package net.gudenau.minecraft.fps.transformer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.asm.api.v1.type.MethodType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class DisableRendererThreadChecks implements Transformer{
    private static final String RenderSystem;
    private static final String RenderSystem$assertThread;
    
    static{
        //TODO
        RenderSystem = "com/mojang/blaze3d/systems/RenderSystem";
        RenderSystem$assertThread = "assertThread";
    }
    
    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "disable_renderer_thread_checks");
    }
    
    @Override
    public boolean handlesClass(String name, String transformedName){
        return true;
    }
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = false;
        
        for(MethodNode method : classNode.methods){
            /*
            INVOKEDYNAMIC get()Ljava/util/function/Supplier; [
                // handle kind 0x6 : INVOKESTATIC
                java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
                // arguments:
                ()Ljava/lang/Object;,
                // handle kind 0x6 : INVOKESTATIC
                com/mojang/blaze3d/systems/RenderSystem.isOnRenderThread()Z,
                ()Ljava/lang/Boolean;
            ]
            INVOKESTATIC com/mojang/blaze3d/systems/RenderSystem.assertThread (Ljava/util/function/Supplier;)V
             */
            MethodType assertThread = new MethodType(Type.getObjectType(RenderSystem), RenderSystem$assertThread, Type.VOID_TYPE, Type.getType(Supplier.class));
            List<AbstractInsnNode> badInstructions = new ArrayList<>();
            for(MethodInsnNode methodCall : AsmUtils.findMethodCalls(method, 0, Opcodes.INVOKESTATIC, assertThread)){
                AbstractInsnNode previousNode = methodCall.getPrevious();
                if(previousNode.getType() != AbstractInsnNode.INVOKE_DYNAMIC_INSN){
                    continue;
                }
                badInstructions.add(methodCall);
                badInstructions.add(previousNode);
            }
            if(!badInstructions.isEmpty()){
                changed = true;
                for(AbstractInsnNode instruction : badInstructions){
                    method.instructions.remove(instruction);
                }
            }
        }
        
        return changed;
    }
}
