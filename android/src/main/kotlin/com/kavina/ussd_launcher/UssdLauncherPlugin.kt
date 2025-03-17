package com.kavina.ussd_launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import android.os.Handler
import android.os.Looper

class UssdLauncherPlugin: FlutterPlugin, MethodCallHandler {
    private lateinit var channel : MethodChannel
    private lateinit var context: Context
    private lateinit var ussdSessionUnique: UssdSessionUnique
    private lateinit var ussdMultiSession: UssdMultiSession

    companion object {
        private var channel: MethodChannel? = null
        private val handler = Handler(Looper.getMainLooper())
    
        fun onUssdResult(result: String) {
            handler.post {
                channel?.invokeMethod("onUssdMessageReceived", result)
            }
        }
    
        fun setMethodChannel(methodChannel: MethodChannel) {
            channel = methodChannel
        }
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "ussd_launcher")
        channel.setMethodCallHandler(this)
        setMethodChannel(channel)
        context = flutterPluginBinding.applicationContext
        ussdSessionUnique = UssdSessionUnique(context)
        ussdMultiSession = UssdMultiSession(context)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "requestAccessibilityPermission" -> {
                val isGranted = requestAccessibilityPermission()
                result.success(isGranted)
            }
            "multisessionUssd" -> {
               try{
                    val ussdCode = call.argument<String>("ussdCode")
                val slotIndex = call.argument<Int>("slotIndex") ?: 0
                val options = call.argument<List<String>>("options") ?: emptyList()
                if (ussdCode != null) {
                    ussdMultiSession.callUSSDWithMenu(ussdCode, slotIndex, options, createHashMap(), object :
                        UssdMultiSession.CallbackInvoke {
                        override fun responseInvoke(message: String) {
                            onUssdResult(message)
                            result.success(message)
                        }
                        override fun over(message: String) {
                            onUssdResult(message)
                            result.success(message)
                        }
                    })
                } else {
                    result.error("INVALID_ARGUMENT", "USSD code is required", null)
                }
               }catch(e: Exception){
                   result.error("USSD_FAILURE", e, null)
               }
                   
            }

           
            else -> result.notImplemented()
        }
    }

    private fun requestAccessibilityPermission(): Boolean {
        if (!isAccessibilityServiceEnabled()) {
            openAccessibilitySettings()
            return false // Permission not granted, settings opened
        }
        return true // Permission granted
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )
        if (accessibilityEnabled == 1) {
            val service = "${context.packageName}/${UssdAccessibilityService::class.java.canonicalName}"
            val settingValue = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        return false
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun createHashMap(): HashMap<String, HashSet<String>> {
        val map = HashMap<String, HashSet<String>>()
        map["KEY_ERROR"] = hashSetOf("Error message")
        map["KEY_LOGIN"] = hashSetOf("Login message")
        return map
    }
}
