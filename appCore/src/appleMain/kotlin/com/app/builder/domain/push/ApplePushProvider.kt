package com.app.builder.domain.push

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import cocoapods.FirebaseCore.FIRApp
import cocoapods.FirebaseMessaging.FIRMessaging
import cocoapods.FirebaseMessaging.FIRMessagingDelegateProtocol
import com.app.builder.core.telemetry.Telemetry
import com.app.builder.data.serializer.decode
import com.app.builder.domain.PushPayload

object ApplePushProvider {
    private const val TAG = "ApplePushProvider"

    /** Scope with a specialized dispatcher limited to a single thread to act as a synchronization lock. */
    private val scope = CoroutineScope(context = Dispatchers.Default.limitedParallelism(parallelism = 1) + SupervisorJob())

    /** Push delegate. */
    private val iosPushDelegate = IosPushDelegate()

    /** Configures Firebase, notification permissions and the push delegates, and starts observing for FCM token registration. */
    @Suppress("unused")
    fun configurePush() {
        runCatching {
            FIRApp.configure()
            FIRMessaging.messaging().delegate = iosPushDelegate
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.delegate = iosPushDelegate
            center.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { _, _ -> }
            UIApplication.sharedApplication().registerForRemoteNotifications()
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to configure push", throwable = it)
        }
    }

    /**
     * Hands the APNs device token to Firebase so it can be exchanged for an FCM token.
     *
     * @param deviceToken The APNs device token.
     */
    @Suppress("unused")
    fun handleApnsToken(deviceToken: NSData) {
        runCatching {
            FIRMessaging.messaging().APNSToken = deviceToken
        }.onFailure {
            Telemetry.error(tag = TAG, message = "Unable to handle APNs token", throwable = it)
        }
    }

    /**
     * Decodes a remote push's `payload` data field unless it's a visible payload.
     *
     * @param userInfo The notification's raw `userInfo` dictionary.
     * @param onResult Invoked with whether new data was fetched, so Swift can call the real `UIBackgroundFetchResult` completion handler.
     * @param init lambda to init the necessary services before processing the payload.
     */
    fun onPushPayloadReceived(userInfo: Map<Any?, *>, onResult: (Boolean) -> Unit, init: suspend () -> Unit) {
        scope.launch(context = Dispatchers.Default) {
            val payloadJson = userInfo[PushPayload.DATA_KEY] as? String ?: run {
                Telemetry.error(tag = TAG, message = "Unable to get payload")
                onResult(false)
                return@launch
            }
            val aps = userInfo["aps"] as? Map<*, *>
            if (aps?.get("alert") != null) {
                Telemetry.error(tag = TAG, message = "Unable to get aps")
                onResult(false)
                return@launch
            }
            val payload = decode<PushPayload>(value = payloadJson) ?: run {
                Telemetry.error(tag = TAG, message = "Unable to decode silent push payload")
                onResult(false)
                return@launch
            }
            init()
            val handled = PushProvider.handlePushPayload(pushPayload = payload)
            onResult(handled)
        }
    }

    /** Implements both [FIRMessagingDelegateProtocol] (token delivery) and [UNUserNotificationCenterDelegateProtocol] (foreground presentation and notification taps). */
    private class IosPushDelegate: NSObject(), FIRMessagingDelegateProtocol, UNUserNotificationCenterDelegateProtocol {

        override fun messaging(messaging: FIRMessaging, didReceiveRegistrationToken: String?) {
            scope.launch(context = Dispatchers.Default) {
                PushProvider.setToken(token = didReceiveRegistrationToken)
            }
        }

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (ULong) -> Unit
        ) {
            runCatching {
                withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound)
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to present notification", throwable = it)
            }
        }

        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            didReceiveNotificationResponse: UNNotificationResponse,
            withCompletionHandler: () -> Unit
        ) {
            runCatching {
                val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
                val payload = (userInfo[PushPayload.DATA_KEY] as? String)?.let { decode<PushPayload>(value = it) } ?: error(message = "Unable to get payload from intent")
                PushProvider.handleNotificationClick(pushPayload = payload)
                withCompletionHandler()
            }.onFailure {
                Telemetry.error(tag = TAG, message = "Unable to handle notification response", throwable = it)
            }
        }
    }
}
