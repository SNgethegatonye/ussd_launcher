package com.kavina.ussd_launcher

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log


class UssdAccessibilityService : AccessibilityService() {
    companion object {
        private var instance: UssdAccessibilityService? = null
        private var pendingMessages: ArrayDeque<String> = ArrayDeque()
        var hideDialogs = false

        fun sendReply(messages: List<String>) {
            println("Setting pending messages: $messages")
            pendingMessages.clear()
            pendingMessages.addAll(messages)
            instance?.performReply()
        }

        fun endSession(): String? {
           return instance?.closeDialog()
        }

        fun cancelSession() {
            instance?.let { service ->
                val rootInActiveWindow = service.rootInActiveWindow
                val cancelButton = rootInActiveWindow?.findAccessibilityNodeInfosByViewId("android:id/button2")
                cancelButton?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } ?: run {
            }
        }
    }

    private fun closeDialog(): String? {
        val rootInActiveWindow = this.rootInActiveWindow ?: return null

        val messageText = findUssdMessage(rootInActiveWindow)
        Log.d("CloseDialog", "Message text: $messageText")

        val button = findConfirmButton(rootInActiveWindow)
        if (button != null) {
            val clickSuccess = button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            println("Click action performed:------------------ $clickSuccess")
            return messageText
        }

        return messageText
    }

    private fun performReply() {
        try{
            val rootInActiveWindow = this.rootInActiveWindow ?: return
            if (pendingMessages.isEmpty()) return

            val message = pendingMessages.removeFirstOrNull()
            println("Performing reply with message: $message")

            println("Root in active window: $rootInActiveWindow")

            val editText = findInputField(rootInActiveWindow)

            if (editText != null) {
                val bundle = Bundle()
                bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                val setTextSuccess = editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                Log.d("performReply", "Set text action performed: $setTextSuccess")
            } else {
                Log.e("performReply", "Input field not found")
            }
        }catch(e: Exception){
            Log.e("performReply", "Error in performReply: $e")
        }
    }

    private fun findInputField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val editTexts = findNodesByClassName(root, "android.widget.EditText")
        return editTexts.firstOrNull()
    }

    private fun findConfirmButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val buttons = findNodesByClassName(root, "android.widget.Button")
        return buttons.firstOrNull {
            it.text?.toString()?.lowercase() in listOf("send", "ok", "submit", "confirm", "cancel")
        }
    }


    private fun findNodesByClassName(root: AccessibilityNodeInfo?, className: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (root == null) return result

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.className?.toString() == className) {
                result.add(node)
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
        }
        return result
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event?.let {
                if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    handleWindowStateChanged(it)
                }
            }
        } catch (e: Exception) {
            println("UssdAccessibilityService ::: Error in onAccessibilityEvent ::: ${e.localizedMessage}")
        }
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        if (hideDialogs) performGlobalAction(GLOBAL_ACTION_BACK)

        logEventDetails(event)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            event.source?.let { nodeInfo ->
                handleContentChanged(nodeInfo)
                nodeInfo.recycle()
            } ?: println("Event source is null")
        }
    }

    private fun logEventDetails(event: AccessibilityEvent) {
        // println("Accessibility event received: ${event.eventType}")
        // println("Event source: ${event.source}")
        // println("Event class name: ${event.className}")
        // println("Event package name: ${event.packageName}")
        println("Event text: ${event.text}")
    }

    private fun handleContentChanged(nodeInfo: AccessibilityNodeInfo) {
        val ussdMessage = findUssdMessage(nodeInfo)
        if (!ussdMessage.isNullOrEmpty()) {
            UssdLauncherPlugin.onUssdResult(ussdMessage)
        }


        for (i in 0 until nodeInfo.childCount) {
            nodeInfo.getChild(i)?.let { childNode ->
                findUssdMessage(childNode)?.takeIf { it.isNotEmpty() }?.let { message ->
                    UssdLauncherPlugin.onUssdResult(message)
                }
            }
        }

        if (pendingMessages.isNotEmpty()) {
            performReply()
        } else {
            closeDialog()
        }
    }


    private fun findUssdMessage(node: AccessibilityNodeInfo): String? {

        if(node.text != null){
            if (node.className?.toString() == "android.widget.TextView") {
                return node.text.toString()
            }

            return node.text.toString()
        }

        return null
    }

    override fun onInterrupt() {
        // cancelSession()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        println("UssdAccessibilityService connected")
    }
}
