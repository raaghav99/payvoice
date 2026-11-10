package com.payvoice

import android.content.Context

/** SharedPreferences-backed "Only selected apps" filter. Default OFF (all apps). */
object AppFilter {
    private const val PREFS = "payvoice_prefs"
    private const val KEY_ONLY_SELECTED = "only_selected_apps"

    val DEFAULT_APPS: Set<String> = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        // Common SMS apps
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.messaging"
    )

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ONLY_SELECTED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ONLY_SELECTED, enabled).apply()
    }

    /** Pure rule: everything is allowed unless the filter is on. */
    fun allows(filterEnabled: Boolean, packageName: String): Boolean =
        !filterEnabled || packageName in DEFAULT_APPS

    fun isAllowed(context: Context, packageName: String): Boolean =
        allows(isEnabled(context), packageName)
}
