package com.nichihab.app.ui.commercial

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nichihab.app.billing.BillingManager
import com.nichihab.app.billing.ProductPrice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage: StateFlow<String?> = _restoreMessage.asStateFlow()

    val isProUser: StateFlow<Boolean> = billingManager.isProUser
    val productPrices: StateFlow<ProductPrice> = billingManager.productPrices

    fun launchMonthlyPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, BillingManager.SKU_PRO_MONTHLY)
    }

    fun launchYearlyPurchase(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, BillingManager.SKU_PRO_YEARLY)
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _isRestoring.value = true
            _restoreMessage.value = null
            billingManager.checkProStatus()
            kotlinx.coroutines.delay(1000)
            val isPro = billingManager.isProUser.value
            _isRestoring.value = false
            _restoreMessage.value = if (isPro) "restore_success" else "restore_not_found"
        }
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }
}