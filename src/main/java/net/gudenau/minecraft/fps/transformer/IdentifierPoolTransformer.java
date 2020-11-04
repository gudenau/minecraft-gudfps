package net.gudenau.minecraft.fps.transformer;

import org.objectweb.asm.Type;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import net.gudenau.minecraft.fps.util.StagingAsmUtils;
import org.objectweb.asm.tree.ClassNode;

public class IdentifierPoolTransformer implements Transformer{
    private static final Type IdentifierPool = Type.getObjectType("net/gudenau/minecraft/fps/pool/IdentifierPool");
    private static final Type Identifier;

    static{
        MappingResolver mapper = FabricLoader.getInstance().getMappingResolver();
        Identifier = Type.getObjectType(mapper
            .mapClassName("intermediary", "net.minecraft.class_2960")
            .replaceAll("\\.", "/")
        );
    }

    @Override
    public Identifier getName(){
        return new Identifier("gud_fps", "identifier_pool");
    }

    @Override
    public boolean handlesClass(String name, String transformedName){
        return !(name.equals(Identifier.getClassName()) || name.equals(IdentifierPool.getClassName()));
    }

    @Override
    public boolean transform(ClassNode classNode, Flags flags){
        return StagingAsmUtils.replaceConstructors(classNode, Identifier, IdentifierPool);
    }
}
