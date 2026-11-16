package de.kurvenflug.app

import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.*
import de.kurvenflug.core.model.Pro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Play-Fassung: Kurvenflug Pro als Einmal-Produkt über Google Play Billing (Pflicht im Play Store).
 * „Verfügbar“ erst, wenn das Produkt in der Play Console angelegt ist – vorher bleibt alles frei.
 * Käufe kennt die Play-Store-App auch ohne Internet; die App selbst braucht keine Internet-Berechtigung.
 */
object ProShops {
    fun create(activity: ComponentActivity): ProShop = PlayShop(activity)
}

private class PlayShop(private val activity: ComponentActivity) : ProShop, PurchasesUpdatedListener, DefaultLifecycleObserver {
    override val available = MutableStateFlow(false)
    override val price = MutableStateFlow<String?>(null)
    override val purchased = MutableStateFlow(false)
    override val acceptsCodes = false
    override val publicKey = ""
    private var details: ProductDetails? = null

    private val client = BillingClient.newBuilder(activity.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init { activity.lifecycle.addObserver(this) }

    override fun onStart(owner: LifecycleOwner) = connect()
    override fun onDestroy(owner: LifecycleOwner) { runCatching { client.endConnection() } }

    private fun connect() {
        if (client.isReady) { refresh(); return }
        runCatching {
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh() }
                override fun onBillingServiceDisconnected() {}
            })
        }
    }

    private fun refresh() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(Pro.PLAY_PRODUCT).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, list ->
            val d = list.productDetailsList.firstOrNull()
            if (result.responseCode == BillingClient.BillingResponseCode.OK && d != null) {
                details = d
                price.value = d.oneTimePurchaseOfferDetails?.formattedPrice
                available.value = true
            }
        }
        restore()
    }

    override fun restore() {
        if (!client.isReady) { connect(); return }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases)
        }
    }

    override fun buy() {
        val d = details ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        runCatching { client.launchBillingFlow(activity, params) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) handle(purchases)
    }

    private fun handle(purchases: List<Purchase>) {
        val mine = purchases.filter { Pro.PLAY_PRODUCT in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        purchased.value = mine.isNotEmpty()
        // Kauf bestätigen, sonst erstattet Google ihn nach 3 Tagen automatisch
        mine.filter { !it.isAcknowledged }.forEach { p ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
        }
    }
}
