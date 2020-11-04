package net.gudenau.minecraft.fps.cpu;

import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryUtil.memAddress;

public class CpuFeatures{
    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
    private static Optional<Vendor> VENDOR = null;
    
    @SuppressWarnings("OptionalAssignedToNull")
    public static Vendor getVendor(){
        if(VENDOR == null){
            int ebx;
            int ecx;
            int edx;
            try(MemoryStack stack = MemoryStack.stackPush()){
                IntBuffer registers = stack
                    .malloc(Integer.BYTES, 4 * Integer.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
                cpuid(0, registers);
                ebx = registers.get(1);
                ecx = registers.get(2);
                edx = registers.get(3);
            }
            for(Vendor vendor : Vendor.values()){
                if(
                    vendor.ebx == ebx &&
                    vendor.ecx == ecx &&
                    vendor.edx == edx
                ){
                    VENDOR = Optional.of(vendor);
                    return vendor;
                }
            }
            VENDOR = Optional.empty();
        }
        return VENDOR.orElse(null);
    }
    
    private static Set<Feature> FEATURES = null;
    public static Set<Feature> getFeatures(){
        if(FEATURES != null){
            return FEATURES;
        }
    
        int ecx;
        int edx;
        try(MemoryStack stack = MemoryStack.stackPush()){
            IntBuffer registers = stack
                .malloc(Integer.BYTES, 4 * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
            cpuid(1, registers);
            ecx = registers.get(2);
            edx = registers.get(3);
        }
        
        Set<Feature> features = new HashSet<>();
        for(Feature feature : Feature.values()){
            int register = feature.register == Register.ECX ? ecx : edx;
            if((register & feature.mask) != 0){
                features.add(feature);
            }
        }
        return FEATURES = Collections.unmodifiableSet(features);
    }
    
    public static boolean hasFeature(Feature feature){
        return getFeatures().contains(feature);
    }
    
    public enum Vendor{
        AMD_OLD("AMDisbetter!"),
        AMD("AuthenticAMD"),
        INTEL("GenuineIntel"),
        VIA("CentaurHauls"),
        TRANSMETA_OLD("TransmetaCPU"),
        TRANSMETA("GenuineTMx86"),
        CYRIX("CyrixInstead"),
        CENTAUR("CentaurHauls"),
        NEXGEN("NexGenDriven"),
        UMC("UMC UMC UMC "),
        SIS("SiS SiS SiS "),
        NSC("Geode by NSC"),
        RISE("RiseRiseRise"),
        VORTEX("Vortex86 SoC"),
        VIA2("VIA VIA VIA "),
        VMWARE("VMwareVMware"),
        XEN("XenVMMXenVMM"),
        HYPERV("Microsoft Hv"),
        PARALLELS(" lrpepyh vr ");
    
        private final int ebx;
        private final int edx;
        private final int ecx;
        
        Vendor(String name){
            byte[] data = name.getBytes(StandardCharsets.US_ASCII);
            if(data.length != 12){
                throw new IllegalArgumentException("Invalid CPU Vendor name");
            }
            
            int register = 0;
            for(int i = 0; i < 4; i++){
                register |= (data[i]) << (8 * i);
            }
            ebx = register;
            register = 0;
            for(int i = 0; i < 4; i++){
                register |= (data[i + 4]) << (8 * i);
            }
            edx = register;
            register = 0;
            for(int i = 0; i < 4; i++){
                register |= (data[i + 8]) << (8 * i);
            }
            ecx = register;
        }
    }
    
    private static void cpuid(int function, IntBuffer registers){
        CpuNatives.cpuid(function, memAddress(registers));
    }
    
    private static void cpuid(int function, int subFunction, IntBuffer registers){
        CpuNatives.cpuid(function, subFunction, memAddress(registers));
    }
    
    private enum Register{
        ECX, EDX
    }
    
    public enum Feature{
        SSE3(Register.ECX, 0),
        PCLMUL(Register.ECX, 1),
        DTES64(Register.ECX, 2),
        MONITOR(Register.ECX, 3),
        DS_CPL(Register.ECX, 4),
        VMX(Register.ECX, 5),
        SMX(Register.ECX, 6),
        EST(Register.ECX, 7),
        TM2(Register.ECX, 8),
        SSSE3(Register.ECX, 9),
        CID(Register.ECX, 10),
        FMA(Register.ECX, 12),
        CX16(Register.ECX, 13),
        ETPRD(Register.ECX, 14),
        PDCM(Register.ECX, 15),
        PCIDE(Register.ECX, 17),
        DCA(Register.ECX, 18),
        SSE4_1(Register.ECX, 19),
        SSE4_2(Register.ECX, 20),
        X2APIC(Register.ECX, 21),
        MOVBE(Register.ECX, 22),
        POPCNT(Register.ECX, 23),
        AES(Register.ECX, 25),
        XSAVE(Register.ECX, 26),
        OSXSAVE(Register.ECX, 27),
        AVX(Register.ECX, 28),
        
        FPU(Register.EDX, 0),
        VME(Register.EDX, 1),
        DE(Register.EDX, 2),
        PSE(Register.EDX, 3),
        TSC(Register.EDX, 4),
        MSR(Register.EDX, 5),
        PAE(Register.EDX, 6),
        MCE(Register.EDX, 7),
        CX8(Register.EDX, 8),
        APIC(Register.EDX, 9),
        SEP(Register.EDX, 11),
        MTRR(Register.EDX, 12),
        PGE(Register.EDX, 13),
        MCA(Register.EDX, 14),
        CMOV(Register.EDX, 15),
        PAT(Register.EDX, 16),
        PSE36(Register.EDX, 17),
        PSN(Register.EDX, 18),
        CLF(Register.EDX, 19),
        DTES(Register.EDX, 21),
        ACPI(Register.EDX, 22),
        MMX(Register.EDX, 23),
        FXSR(Register.EDX, 24),
        SSE(Register.EDX, 25),
        SSE2(Register.EDX, 26),
        SS(Register.EDX, 27),
        HTT(Register.EDX, 28),
        TM1(Register.EDX, 29),
        IA64(Register.EDX, 30),
        PBE(Register.EDX, 31);
    
        private final Register register;
        private final int mask;
    
        Feature(Register register, int bit){
            this.register = register;
            this.mask = 1 << bit;
        }
    }
}
