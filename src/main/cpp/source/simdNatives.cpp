#include "types.h"

extern "C"{
    #include <cpuid.h>
    #include <emmintrin.h>
}

#include <cstdlib>
#include <cstdio>

#include "net_gudenau_minecraft_fps_cpu_SimdNatives.h"

#define UNUSED(var) ((void)(var))
#define UNUSED_JAVA do{UNUSED(env); UNUSED(klass);}while(false)

// Based on work from Soonts on StackOverflow
// https://stackoverflow.com/questions/64165250/8-bit-lerp-with-sse
extern "C" JNIEXPORT void JNICALL JavaCritical_net_gudenau_minecraft_fps_cpu_SimdNatives_lerpBytes
(jlong aVoid, jlong bVoid, jlong resultVoid, jint count, jint progress, jint total){
    const u32 fp = progress * 0x100 / total;
    __m128i mulA = _mm_set1_epi16((u16)fp);
    __m128i mulB = _mm_set1_epi16((u16)(0x100 - fp));

    //u32* a32 = (u32*)aVoid;
    //u32* b32 = (u32*)bVoid;
    //u32* result32 = (u32*)resultVoid;

    __m128i* a128 = (__m128i*)aVoid;
    __m128i* b128 = (__m128i*)bVoid;
    __m128i* result128 = (__m128i*)resultVoid;
    
    const __m128i lowMask = _mm_set1_epi16(0xFF);

    for(int i = 0; i < count; i += 16){
        /*
        __m128i a = _mm_setr_epi32(
            a32[i], a32[i + 1], a32[i + 2], a32[i + 3]
        );
        __m128i b = _mm_setr_epi32(
            b32[i], b32[i + 1], b32[i + 2], b32[i + 3]
        );
        */
       __m128i a = _mm_load_si128(a128 + i);
       __m128i b = _mm_load_si128(b128 + i);

        // Split vectors into pairs
        __m128i lowA = _mm_and_si128(a, lowMask);
        __m128i highA = _mm_srli_epi16(a, 8);
        __m128i lowB = _mm_and_si128(b, lowMask);
        __m128i highB = _mm_srli_epi16(b, 8);

        // Multiply
        lowA = _mm_mullo_epi16(lowA, mulA);
        highA = _mm_mullo_epi16(highA, mulA);
        lowB = _mm_mullo_epi16(lowB, mulB);
        highB = _mm_mullo_epi16(highB, mulB);

        // Add products
        __m128i low = _mm_adds_epu16(lowA, lowB);
        __m128i high = _mm_adds_epu16(highA, highB);

        // Repack
        low = _mm_srli_epi16(low, 8);
        high = _mm_andnot_si128(lowMask, high);
        __m128i result = _mm_or_si128(low, high);
        _mm_store_si128(result128 + i, result);
    }
}
JNIEXPORT void JNICALL Java_net_gudenau_minecraft_fps_cpu_SimdNatives_lerpBytes
(JNIEnv* env, jclass klass, jlong a, jlong b, jlong result, jint count, jint progress, jint total){
    UNUSED_JAVA;
    JavaCritical_net_gudenau_minecraft_fps_cpu_SimdNatives_lerpBytes(a, b, count, result, progress, total);
}