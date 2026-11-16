package de.wissensgarten.app

import de.wissensgarten.core.data.PlusConfig

/** Plus-Einstellungen aus pro/plus.properties. [purchaseAllowed]: Web/GitHub ja, Play nein (dort später Play Billing). */
fun plusConfig(purchaseAllowed: Boolean) = PlusConfig(AppInfo.PLUS_KEY, AppInfo.PLUS_CHECKOUT_YEAR, AppInfo.PLUS_CHECKOUT_FOREVER, purchaseAllowed)

/** Server-Endpunkt für Plus-Pakete (siehe MELDUNG_AN_LEITSTAND.md, Schnittstelle Plus-Paket-Endpunkt). */
const val PLUS_API = "https://wissensgarten.mathenachhilfe-online.com/wg-api/plus/"
