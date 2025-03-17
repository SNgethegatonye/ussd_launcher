package com.kavina.ussd_launcher

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telecom.TelecomManager
import java.util.ArrayDeque
import android.content.Context

class UssdMultiSession(private val context: Context) {
    
    private var ussdOptionsQueue: ArrayDeque<String> = ArrayDeque()

    private var isRunning = false
    private var callbackInvoke: CallbackInvoke? = null
    private var map: HashMap<String, HashSet<String>>? = null

    companion object {
        private const val KEY_ERROR = "KEY_ERROR"
        private const val KEY_LOGIN = "KEY_LOGIN"
    }


    fun callUSSDWithMenu(str: String, simSlot: Int, options: List<String>, hashMap: HashMap<String, HashSet<String>>, callbackInvoke: CallbackInvoke) {
        this.callbackInvoke = callbackInvoke
        this.map = hashMap
        this.ussdOptionsQueue.clear()
        this.ussdOptionsQueue.addAll(options)
        if (verifyAccesibilityAccess(context)) {
            dialUp(str, simSlot)
        } else {
            this.callbackInvoke?.over("Check your accessibility")
        }
    }

    private fun dialUp(str: String, simSlot: Int) {
        val hashMap = this.map
        if (hashMap == null || !hashMap.containsKey(KEY_ERROR) || !hashMap.containsKey(KEY_LOGIN)) {
            this.callbackInvoke?.over("Bad Mapping structure")
            return
        }
        if (str.isEmpty()) {
            this.callbackInvoke?.over("Bad ussd number")
            return
        }
        val encodedHash = Uri.encode("#")
        val ussdCode = str.replace("#", encodedHash)
        val uri = Uri.parse("tel:$ussdCode")
        if (uri != null) {
            this.isRunning = true
        }
        UssdAccessibilityService.hideDialogs = true
        context.startActivity(getActionCallIntent(uri, simSlot))

         Handler(Looper.getMainLooper()).postDelayed({
            sendNextUssdOption()
        }, 3500)
    }

    private fun sendNextUssdOption() {
        if (ussdOptionsQueue.isNotEmpty()) {
            val nextOption = ussdOptionsQueue.poll()
            if (nextOption != null) {
                sendUssdOption(nextOption)
            }
        }
        else {
            val response = UssdAccessibilityService.endSession() ?: "No response"
             this.callbackInvoke?.responseInvoke(response)
        }
    }

    private fun sendUssdOption(option: String) {
        try {
            UssdAccessibilityService.sendReply(listOf(option))
            Handler(Looper.getMainLooper()).postDelayed({
                sendNextUssdOption()
            }, 2000)
        } catch (e: Exception) {
            println(e.message)
            callbackInvoke?.over("An error occurred while sending the USSD option")
        }
    }

    private fun getActionCallIntent(uri: Uri, simSlot: Int): Intent {
        val slotKeys = arrayOf(
            "extra_asus_dial_use_dualsim",
            "com.android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "Subscription",
            "phone",
            "com.android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
        )
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("com.android.phone.force.slot", true)
            putExtra("Cdma_Supp", true)
        }
        for (key in slotKeys) {
            intent.putExtra(key, simSlot)
        }
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager?
        telecomManager?.let {
            val phoneAccounts = it.callCapablePhoneAccounts
            if (phoneAccounts.size > simSlot) {
                intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", phoneAccounts[simSlot])
            }
        }
        return intent
    }

    private fun verifyAccesibilityAccess(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (services != null) {
                return services.lowercase().contains(context.packageName.lowercase())
            }
        }
        return false
    }

    interface CallbackInvoke {
        fun responseInvoke(message: String)
        fun over(message: String)
    }
}

