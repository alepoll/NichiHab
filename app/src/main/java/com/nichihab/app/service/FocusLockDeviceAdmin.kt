package com.nichihab.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class NichiHabDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling this will allow you to uninstall NichiHab and bypass your blocks."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
