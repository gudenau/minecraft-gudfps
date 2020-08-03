package net.gudenau.minecraft.fps.transformer;

import java.util.HashSet;
import java.util.Set;
import net.gudenau.minecraft.fps.util.AsmUtils;
import net.gudenau.minecraft.fps.util.Stats;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class RPmallocTransformer implements Transformer{
    private final Stats stats = Stats.getStats("RPmalloc");
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        boolean changed = false;
    
        if(classNode.superName.equals("java/lang/Thread")){
            AsmUtils.findMethod(classNode, "run", "()V").ifPresent((method)->{
                method.instructions.insert(new MethodInsnNode(
                    INVOKESTATIC,
                    "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                    "threadStart",
                    "()V",
                    false
                ));
    
                for(AbstractInsnNode node : AsmUtils.findReturns(method.instructions)){
                    method.instructions.insertBefore(node, new MethodInsnNode(
                        INVOKESTATIC,
                        "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                        "threadEnd",
                        "()V",
                        false
                    ));
                }
            });
    
            stats.incrementStat("Thread");
            changed = true;
        }
        
        if(classNode.superName.equals("java/util/concurrent/ForkJoinWorkerThread")){
            MethodNode method = AsmUtils.findOrCreateMethod(classNode, ACC_PUBLIC, "java/util/concurrent/ForkJoinWorkerThread", "onStart", "()V");
            method.instructions.insert(new MethodInsnNode(
                INVOKESTATIC,
                "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                "threadStart",
                "()V",
                false
            ));
    
            method = AsmUtils.findOrCreateMethod(classNode, ACC_PUBLIC, "java/util/concurrent/ForkJoinWorkerThread", "onTermination", "(Ljava/lang/Throwable;)V");
            for(InsnNode node : AsmUtils.findReturns(method.instructions)){
                method.instructions.insertBefore(node, new MethodInsnNode(
                    INVOKESTATIC,
                    "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                    "threadEnd",
                    "()V",
                    false
                ));
            }
    
            stats.incrementStat("ForkJoinWorkerThread");
            changed = true;
        }
        
        for(MethodNode method : classNode.methods){
            InsnList instructions = method.instructions;
            Set<MethodInsnNode> threadInstructions = new HashSet<>();
    
            for(AbstractInsnNode instruction : instructions){
                if(instruction instanceof MethodInsnNode){
                    MethodInsnNode methodNode = (MethodInsnNode)instruction;
                    if(
                        methodNode.getOpcode() == INVOKESPECIAL &&
                        methodNode.owner.equals("java/lang/Thread") &&
                        methodNode.name.equals("<init>")
                    ){
                        threadInstructions.add(methodNode);
                    }
                }
            }
    
            changed |= !threadInstructions.isEmpty();
            stats.addStat("inits", threadInstructions.size());
    
            for(MethodInsnNode initNode : threadInstructions){
                // Easy mode
                if(initNode.desc.equals("(Ljava/lang/Runnable;)V")){
                    instructions.insertBefore(initNode, new MethodInsnNode(
                        INVOKESTATIC,
                        "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                        "runnable",
                        "(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
                        false
                    ));
                // Handled elsewhere.
                }else if(initNode.desc.equals("(Ljava/lang/String;)V")){
                }else if(initNode.desc.equals("(Ljava/lang/Runnable;Ljava/lang/String;)V")){
                    /*
    NEW java/lang/Thread
    DUP
    ALOAD 0
    GETFIELD com/mojang/realmsclient/gui/screens/RealmsLongRunningMcoTaskScreen.taskThread : Lcom/mojang/realmsclient/gui/LongRunningTask;
    LDC "Realms-long-running-task"
    INVOKESPECIAL java/lang/Thread.<init> (Ljava/lang/Runnable;Ljava/lang/String;)V
    ASTORE 1
                     */
                    
                    InsnList patch = new InsnList();
                    patch.add(new InsnNode(SWAP));
                    patch.add(new MethodInsnNode(
                        INVOKESTATIC,
                        "net/gudenau/minecraft/fps/fixes/RPMallocFixes",
                        "runnable",
                        "(Ljava/lang/Runnable;)Ljava/lang/Runnable;",
                        false
                    ));
                    patch.add(new InsnNode(SWAP));
                    instructions.insertBefore(initNode, patch);
                }else{
                    throw new RuntimeException(String.format(
                        "Failed to transform %s.%s%s, unknown Thread constructor <init>%s",
                        classNode.name,
                        method.name,
                        method.desc,
                        initNode.desc
                    ));
                }
            }
        }
        
        return changed;
    }
}
