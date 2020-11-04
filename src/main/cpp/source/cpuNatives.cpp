#include <cstdint>

extern "C"{
    #include <cpuid.h>
}

#include "net_gudenau_minecraft_fps_cpu_CpuNatives.h"

#define UNUSED(var) ((void)(var))
#define UNUSED_JAVA do{UNUSED(env); UNUSED(klass);}while(false)

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IJ
(jint function, jlong registers){
    auto eax = (uint32_t*)registers + 0;
    auto ebx = (uint32_t*)registers + 1;
    auto ecx = (uint32_t*)registers + 2;
    auto edx = (uint32_t*)registers + 3;
    __get_cpuid(
        function,
        eax,
        ebx,
        ecx,
        edx
    );
}
JNIEXPORT void JNICALL Java_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IJ
(JNIEnv* env, jclass klass, jint function, jlong registers){
    UNUSED_JAVA;
    JavaCritical_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IJ(function, registers);
}

extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IIJ
(jint function, jint subFunction, jlong registers){
    auto eax = (uint32_t*)registers + 0;
    auto ebx = (uint32_t*)registers + 1;
    auto ecx = (uint32_t*)registers + 2;
    auto edx = (uint32_t*)registers + 3;
    __get_cpuid_count(
        function,
        subFunction,
        eax,
        ebx,
        ecx,
        edx
    );
}
JNIEXPORT void JNICALL Java_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IIJ
(JNIEnv* env, jclass klass, jint function, jint subFunction, jlong registers){
    UNUSED_JAVA;
    JavaCritical_net_gudenau_minecraft_fps_cpu_CpuNatives_cpuid__IIJ(function, subFunction, registers);
}

