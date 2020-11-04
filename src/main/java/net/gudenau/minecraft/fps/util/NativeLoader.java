package net.gudenau.minecraft.fps.util;

import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class NativeLoader{
    public static void load(){
        Path path = FabricLoader.getInstance().getGameDir().resolve("../src/main/cpp/out/linux/amd64/natives.so");
        if(Files.exists(path)){
            System.load(path.toAbsolutePath().toString());
        }
    }
}
