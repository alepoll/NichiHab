#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_nichihab_app_ui_commercial_CommercialSecurityConfig_getNativeApiKey(
        JNIEnv* env,
        jobject /* this */) {
    
    // Fake Key for demonstration: sk-test-1234
    // XOR key used to encode: 0x5A
    // In production, the user will replace these bytes with their actual XORed Gemini API Key.
    // Script to generate:
    // python -c "key='sk-ant-real-key'; print(','.join([hex(ord(c) ^ 0x5A) for c in key]))"
    
    // Encoded bytes of "sk-test-1234" XOR 0x5A
    unsigned char encoded[] = {
        0x29, 0x31, 0x77, 0x2e, 0x3f, 0x29, 0x2e, 0x77, 0x6b, 0x68, 0x69, 0x6e
    };
    
    int length = sizeof(encoded) / sizeof(encoded[0]);
    std::string decoded = "";
    
    for (int i = 0; i < length; i++) {
        decoded += (char)(encoded[i] ^ 0x5A);
    }
    
    return env->NewStringUTF(decoded.c_str());
}
