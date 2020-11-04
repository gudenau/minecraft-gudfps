package net.gudenau.minecraft.fps.transformer;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.gudenau.minecraft.asm.api.v1.AsmUtils;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class SimdTransformer implements Transformer{
    private static final boolean CLIENT;
    
    private static final Type Sprite$Interpolation;
    
    static{
        FabricLoader loader = FabricLoader.getInstance();
        CLIENT = loader.getEnvironmentType() == EnvType.CLIENT;
        MappingResolver mapper = loader.getMappingResolver();
        
        if(CLIENT){
            Sprite$Interpolation = Type.getObjectType(mapper.mapClassName(
                "intermediary", "net.minecraft.class_1058$class_4728"
            ).replaceAll("\\.", "/"));
        }else{
            Sprite$Interpolation = null;
        }
    }
    
    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "simd");
    }
    
    @Override
    public boolean handlesClass(String name, String transformedName){
        if(CLIENT){
            return Sprite$Interpolation.getClassName().equals(name);
        }
        
        return false;
    }
    
    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        if(CLIENT){
            if(Sprite$Interpolation.getInternalName().equals(classNode.name)){
                return transformSprite$Interpolation(classNode);
            }
        }
        
        return false;
    }
    
    private boolean transformSprite$Interpolation(ClassNode classNode){
        AsmUtils.findMethod(classNode, "apply", "()V").ifPresent((method)->{
            InsnList instructions = method.instructions;
            instructions.clear();
            LabelNode start = new LabelNode();
            instructions.add(start);
            instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "net/gudenau/minecraft/fps/cpu/client/SimdSprite",
                "interpolateSprite",
                "(Lnet/gudenau/minecraft/fps/mixin/client/Sprite$InterpolationAccessor;)V",
                false
            ));
            instructions.add(new InsnNode(Opcodes.RETURN));
            LabelNode end = new LabelNode();
            instructions.add(end);
            method.maxLocals = 1;
            method.maxStack = 1;
            method.localVariables.clear();
            method.localVariables.add(new LocalVariableNode(
                "this",
                Sprite$Interpolation.getDescriptor(),
                null,
                start,
                end,
                0
            ));
        });
        
        return true;
    }
}
