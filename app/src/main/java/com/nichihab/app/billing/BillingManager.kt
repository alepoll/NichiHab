package com.nichihab.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.nichihab.app.data.local.datastore.SettingsDataStore
import com.nichihab.app.ui.commercial.CommercialTierManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ProductPrice(
    val monthly: String = "$4.99/mes",
    val yearly: String = "$29.99/año",
    val monthlyMicros: Long = 4990000,
    val yearlyMicros: Long = 29990000
)

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val SKU_PRO_MONTHLY = "NichiHab_pro_monthly"
        const val SKU_PRO_YEARLY = "NichiHab_pro_yearly"
        private const val RETRY_DELAY_MS = 5000L
        private const val MAX_RETRIES = 5
    }

    private val _isProUser = MutableStateFlow(false)
    val isProUser: StateFlow<Boolean> = _isProUser.asStateFlow()

    private val _productPrices = MutableStateFlow(ProductPrice())
    val productPrices: StateFlow<ProductPrice> = _productPrices.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        restorePersistedProStatus()
        connectToBilling(0)
    }

    private fun restorePersistedProStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val persisted = settingsDataStore.isProUser.first()
            if (persisted) {
                _isProUser.value = true
                CommercialTierManager.isProUser = true
                Log.d(TAG, "Pro status restored from DataStore")
            }
        }
    }

    private fun connectToBilling(retryCount: Int = 0) {
        if (billingClient.isReady) {
            checkProStatus()
            queryProductPrices()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup successful")
                    checkProStatus()
                    queryProductPrices()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                if (retryCount < MAX_RETRIES) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(RETRY_DELAY_MS)
                        connectToBilling(retryCount + 1)
                    }
                }
            }
        })
    }

    private fun queryProductPrices() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_PRO_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SKU_PRO_YEARLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var monthlyPrice = "$4.99/mes"
                var yearlyPrice = "$29.99/año"
                var monthlyMicros = 4990000L
                var yearlyMicros = 29990000L

                for (details in productDetailsList) {
                    val offerDetails = details.subscriptionOfferDetails?.firstOrNull()
                    val price = offerDetails?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    if (price != null) {
                        val formatted = "${price.formattedPrice}/${if (details.productId == SKU_PRO_MONTHLY) "mes" else "año"}"
                        val micros = price.priceAmountMicros
                        if (details.productId == SKU_PRO_MONTHLY) {
                            monthlyPrice = formatted
                            monthlyMicros = micros
                        } else {
                            yearlyPrice = formatted
                            yearlyMicros = micros
                        }
                    }
                }

                _productPrices.value = ProductPrice(monthlyPrice, yearlyPrice, monthlyMicros, yearlyMicros)
            }
        }
    }

    fun checkProStatus() {
        if (!billingClient.isReady) {
            connectToBilling(0)
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

                _isProUser.value = hasActiveSub
                CommercialTierManager.isProUser = hasActiveSub

                persistProStatus(hasActiveSub)

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

    private fun persistProStatus(pro: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            settingsDataStore.setIsProUser(pro)
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        CoroutineScope(Dispatchers.IO).launch {
            val integrityResult = IntegrityGuard.verifyPurchaseToken(context, purchase.purchaseToken)
            if (!integrityResult.isIntegrityVerified) {
                Log.w(TAG, "Integrity check warning for purchase ${purchase.products}: ${integrityResult.errorMessage}")
            }
        }

        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged successfully")
            }
        }
    }

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
                Toast.makeText(
                    activity,
                    "Product not found. Ensure products are active in Google Play Console.",
                    Toast.LENGTH_LONG
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
                _isProUser.value = true
                CommercialTierManager.isProUser = true
                persistProStatus(true)
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

    fun disconnect() {
        billingClient.endConnection()
    }
}