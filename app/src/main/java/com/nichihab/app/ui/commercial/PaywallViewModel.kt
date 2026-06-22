package com.nichihab.app.ui.commercial

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.nichihab.app.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    fun launchPurchaseFlow(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, BillingManager.SKU_PRO_MONTHLY)
    }
}
