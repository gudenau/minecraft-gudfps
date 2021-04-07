package net.gudenau.minecraft.fps.transformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.fps.util.threading.SynchronizedUtils;
import net.gudenau.minecraft.fps.util.Stats;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

public class ForEachRemover implements Transformer{
    private static final Map<String, Boolean> INTERFACE_MAP = new HashMap<>();
    private static final ReentrantReadWriteLock INTERFACE_MAP_LOCK = new ReentrantReadWriteLock();
    
    private final Stats stats = Stats.getStats("forEach Remover");
    
    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "foreach_remover");
    }
    
    @Override
    public boolean handlesClass(String name, String transformedName){
        return true;
    }
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        AtomicBoolean changed = new AtomicBoolean(false);
        
        SynchronizedUtils.withWriteLock(INTERFACE_MAP_LOCK, ()->INTERFACE_MAP.put(classNode.name, (classNode.access & ACC_INTERFACE) != 0));
        
        for(MethodNode method : classNode.methods){
            InsnList instructions = method.instructions;
            AsmUtils.<InvokeDynamicInsnNode>findMatchingNodes(instructions, (node)->{
                // Get all the dynamic nodes that look right
                if(node instanceof InvokeDynamicInsnNode){
                    InvokeDynamicInsnNode invoke = (InvokeDynamicInsnNode)node;
                    return "accept".equals(invoke.name) && "()Ljava/util/function/Consumer;".equals(invoke.desc);
                }
                return false;
            }).stream()
            .filter((node)->{
                // Make sure the consumers go into a foreach
                AbstractInsnNode next = node.getNext();
                if(next instanceof MethodInsnNode){
                    MethodInsnNode insn = (MethodInsnNode)next;
                    return "forEach".equals(insn.name) && "(Ljava/util/function/Consumer;)V".equals(insn.desc);
                }
                return false;
            }).forEach((invokeDynamic)->{
                MethodInsnNode methodNode = (MethodInsnNode)invokeDynamic.getNext();
                
                // Transform them
                
                // Extra info from existing instructions
                Handle targetHandle = (Handle)invokeDynamic.bsmArgs[1];
                int targetOpcode = AsmUtils.getOpcodeFromHandle(targetHandle);
                Type targetType = (Type)invokeDynamic.bsmArgs[2];
                
                // Figure out our starting indices
                int maxLocals = method.maxLocals;
    
                List<LocalVariableNode> localVariables = method.localVariables;
                if(localVariables != null){
                    for(LocalVariableNode localVariable : method.localVariables){
                        maxLocals = Math.max(localVariable.index, maxLocals);
                    }
                }
                
                // Figure out our locals
                int localIterator = maxLocals++;
                //int localObject = maxLocals++;
                
                // Store the new max
                method.maxLocals = maxLocals;
                
                // Figure out the collection
                String collection = methodNode.owner;
                boolean isCollectionInterface;
                try{
                    // Is this really the best way?
                    Boolean bool = SynchronizedUtils.withReadLock(INTERFACE_MAP_LOCK, ()->INTERFACE_MAP.get(collection));
                    if(bool == null){
                        Class<?> collectionClass = getClass().getClassLoader().loadClass(collection.replaceAll("/", "."));
                        isCollectionInterface = collectionClass.isInterface();
                        SynchronizedUtils.withWriteLock(INTERFACE_MAP_LOCK, ()->INTERFACE_MAP.put(collection, isCollectionInterface));
                    }else{
                        isCollectionInterface = bool;
                    }
                }catch(ClassNotFoundException ignored){
                    stats.incrementStat("failed");
                    return;
                }
    
                // Build the for-each loop
                InsnList patch = new InsnList();
                LabelNode breakNode = new LabelNode();
                LabelNode continueNode = new LabelNode();
                
                // Iterator iter = collection.iterator();
                patch.add(new MethodInsnNode(
                    isCollectionInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                    collection,
                    "iterator",
                    "()Ljava/util/Iterator;",
                    isCollectionInterface
                ));
                patch.add(new VarInsnNode(ASTORE, localIterator));
                
                // while(iter.hasNext()){
                patch.add(continueNode);
                patch.add(new FrameNode(
                    F_APPEND,
                    1, new Object[]{"java/util/Iterator"},
                    0, null
                ));
                patch.add(new VarInsnNode(ALOAD, localIterator));
                patch.add(new MethodInsnNode(
                    INVOKEINTERFACE,
                    "java/util/Iterator",
                    "hasNext",
                    "()Z",
                    true
                ));
                patch.add(new JumpInsnNode(IFEQ, breakNode));
                
                //   lambda(iter.next());
                patch.add(new VarInsnNode(ALOAD, localIterator));
                patch.add(new MethodInsnNode(
                    INVOKEINTERFACE,
                    "java/util/Iterator",
                    "next",
                    "()Ljava/lang/Object;",
                    true
                ));
                
                Type targetMethod = Type.getMethodType(targetHandle.getDesc());
                Type[] targetArgs = targetMethod.getArgumentTypes();
                boolean passObject = targetArgs.length == targetType.getArgumentTypes().length;
                
                patch.add(new TypeInsnNode(
                    CHECKCAST,
                    passObject ? targetArgs[targetArgs.length - 1].getInternalName() : targetHandle.getOwner()
                ));
                patch.add(new MethodInsnNode(
                    targetOpcode,
                    targetHandle.getOwner(),
                    targetHandle.getName(),
                    targetHandle.getDesc(),
                    targetOpcode == INVOKEINTERFACE
                ));
                
                // Return value of a foreach handle is not always void
                switch (Type.getMethodType(targetHandle.getDesc()).getReturnType().getSize()) {
                    case 2:
                        patch.add(new InsnNode(POP2));
                        break;
                    case 1:
                        patch.add(new InsnNode(POP));
                    case 0:
                }
                
                // }
                patch.add(new JumpInsnNode(GOTO, continueNode));
                patch.add(breakNode);
                patch.add(new FrameNode(F_CHOP, 0, null, 0, null));
    
                instructions.insertBefore(invokeDynamic, patch);
                instructions.remove(invokeDynamic);
                instructions.remove(methodNode);
                
                changed.set(true);
                stats.incrementStat("success");
            });
        }
        
        if(changed.get()){
            flags.requestFrames();
            return true;
        }else{
            return false;
        }
    }
}
