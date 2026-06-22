package com.nichihab.app.ui.commercial

import com.nichihab.app.util.Constants

object CommercialSecurityConfig {
    
    init {
        // Carga la librería compilada de C++
        System.loadLibrary("security-lib")
    }

    // Llama a la función JNI en C++ para ensamblar la llave
    private external fun getNativeApiKey(): String

    // 1. Central API Key Config (Ahora segura y ofuscada)
    val GLOBAL_API_KEY: String
        get() = getNativeApiKey()
        
    const val GLOBAL_PROVIDER = Constants.PROVIDER_GEMINI

    // 2. Feature Flags
    const val ENABLE_INPUT_GUARD = true
    const val ENABLE_HARDENED_PROMPTS = true
    const val ENABLE_2MIN_EDIT_WINDOW = true
    const val MAX_TASK_CHARS = 150
}
