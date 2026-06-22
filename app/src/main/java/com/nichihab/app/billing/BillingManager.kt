package com.nichihab.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val integrityGuard: IntegrityGuard
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val SKU_PRO_MONTHLY = "NichiHab_pro_monthly"
        const val SKU_PRO_YEARLY = "NichiHab_pro_yearly"
    }

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connectToBilling()
    }

    private fun connectToBilling() {
        if (billingClient.isReady) {
            checkProStatus()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful")
                    checkProStatus()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, retrying...")
                // In production, add a delay or retry policy here
            }
        })
    }

    /**
     * Queries active purchases and updates the Pro status.
     */
    fun checkProStatus() {
        if (!billingClient.isReady) {
            connectToBilling()
            return
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                
                val isTrusted = integrityGuard.isEffectivelyTrusted()
                val isPro = hasActiveSub && isTrusted
                
                if (hasActiveSub && !isTrusted) {
                    Log.w(TAG, "Active subscription found, but device is UNTRUSTED. Blocking Pro features.")
                }

                _isProUser.value = isPro
                com.nichihab.app.ui.commercial.CommercialTierManager.isProUser = isPro
                
                // Acknowledge purchases if needed
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                }
            } else {
                Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            }
        }
    }

    /**
     * Launch purchase flow for a specific SKU.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            Log.e(TAG, "Billing client not ready to purchase")
            return
        }

        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                // For subscriptions, we need to select an offer token
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""

                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                Log.e(TAG, "Failed to query product details or empty list")
                android.widget.Toast.makeText(
                    activity,
                    "Product not found. Ensure NichiHab_pro_monthly is active in Google Play Console.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            val hasActiveSub = purchases.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasActiveSub) {
                // Check integrity on new purchase
                integrityGuard.forceRecheck()
                
                val isTrusted = integrityGuard.isEffectivelyTrusted()
                val isPro = hasActiveSub && isTrusted

                if (hasActiveSub && !isTrusted) {
                    Log.w(TAG, "New purchase successful, but device is UNTRUSTED. Blocking Pro features.")
                }

                _isProUser.value = isPro
                com.nichihab.app.ui.commercial.CommercialTierManager.isProUser = isPro
            }
            
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User cancelled purchase flow")
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
        }
    }

    // Commercial Tier Gates (Delegated to CommercialTierManager for the most part, but keeping legacy bridges)
    fun canAddMoreTasks(currentCount: Int): Boolean = _isProUser.value || currentCount < 1
    fun canAddMoreTimeWindows(currentCount: Int): Boolean = _isProUser.value || currentCount < 1
    fun canUseHardMode(): Boolean = _isProUser.value
    fun canViewFullHistory(): Boolean = _isProUser.value
    fun canRestoreStreak(): Boolean = _isProUser.value
}
