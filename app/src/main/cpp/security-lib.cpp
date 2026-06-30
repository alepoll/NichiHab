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
    
    // Encoded bytes of "AIzaSyBytBoFL-0CbFG-fzPX15-y8sCNQp5drJs" XOR 0x5A
    unsigned char encoded[] = {
        0x1b, 0x13, 0x20, 0x3b, 0x9, 0x23, 0x18, 0x23, 0x2e, 0x18, 0x35, 0x1c, 0x16, 0x77, 0x6a, 0x19, 0x38, 0x1c, 0x1d, 0x77, 0x3c, 0x20, 0xa, 0x2, 0x6b, 0x6f, 0x77, 0x23, 0x62, 0x29, 0x19, 0x14, 0xb, 0x2a, 0x6f, 0x3e, 0x28, 0x10, 0x29
    };
    
    int length = sizeof(encoded) / sizeof(encoded[0]);
    std::string decoded = "";
    
    for (int i = 0; i < length; i++) {
        decoded += (char)(encoded[i] ^ 0x5A);
    }
    
    return env->NewStringUTF(decoded.c_str());
}
