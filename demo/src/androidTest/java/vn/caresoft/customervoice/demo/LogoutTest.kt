package vn.caresoft.customervoice.demo

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import vn.caresoft.customervoice.*
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class LogoutTest {
 @Test fun freshDemoDoesNotDialAndConfirmedLogoutClearsHostSuppliedSession()=runBlocking {
  val instrumentation=InstrumentationRegistry.getInstrumentation(); val context=instrumentation.targetContext
  val revoked=AtomicInteger(); val dials=AtomicInteger(); val server=MockWebServer()
  val deny=java.util.concurrent.atomic.AtomicBoolean(false)
  val lastLogin=java.util.concurrent.atomic.AtomicReference<Pair<String,String>>()
  var currentUser="123"
  server.dispatcher=object: okhttp3.mockwebserver.Dispatcher() {
   override fun dispatch(r: RecordedRequest): MockResponse=when {
    r.path!!.endsWith("/sessions") -> {
     val body=r.body.readUtf8(); lastLogin.set(body to r.getHeader("Authorization").orEmpty())
     if(deny.get()) MockResponse().setResponseCode(401).setBody("""{"error":{"code":"UNAUTHORIZED"}}""")
     else {
      currentUser=org.json.JSONObject(body).getString("user_id")
      MockResponse().setBody("""{"server_time":"2026-11-17T00:00:00Z","data":{"user_id":"$currentUser","session_id":"session-$currentUser","access_token":"fixture","refresh_token":"fixture","expires_at":"2099-01-01T00:00:00Z","refresh_expires_at":"2099-02-01T00:00:00Z"}}""")
     }
    }
    r.path!!.endsWith("/sessions/current") -> { revoked.incrementAndGet(); MockResponse().setResponseCode(204) }
    r.path!!.endsWith("/events") -> MockResponse().withWebSocketUpgrade(object: WebSocketListener(){})
    r.path!!.endsWith("/configuration") -> MockResponse().setBody("""{"data":{"session_id":"session-$currentUser","user_id":"$currentUser","voice_ready":true,"push_registered":true},"server_time":"2026-11-17T00:00:00Z"}""")
    r.path!!.endsWith("/calls") && r.method=="POST" -> { dials.incrementAndGet(); MockResponse().setResponseCode(500) }
    else -> MockResponse().setBody("""{"data":{"calls":[]}}""")
   }
  }
  server.start()
  fun find(view: View,text: String): Button? {
   if(view is Button && view.text.toString()==text) return view
   if(view is ViewGroup) for(i in 0 until view.childCount) find(view.getChildAt(i),text)?.let {return it}
   return null
  }
  var activityForLogin: MainActivity?=null
  fun editables(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
   (if(node.isEditable) listOf(node) else emptyList())+(0 until node.childCount).flatMap { node.getChild(it)?.let(::editables).orEmpty() }
  fun fill(node: AccessibilityNodeInfo,text: String) {
   val args=android.os.Bundle().apply {putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text)}
   assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args))
  }
  fun confirm(text: String) {
   val root=instrumentation.uiAutomation.rootInActiveWindow
   assertTrue(root.findAccessibilityNodeInfosByText(text).last {it.isClickable}.performAction(AccessibilityNodeInfo.ACTION_CLICK))
   instrumentation.waitForIdleSync()
  }
  suspend fun openLogin(): List<AccessibilityNodeInfo> {
   withTimeout(5000) { while(DemoApplication.loggingIn.value || DemoApplication.signingOut.value) delay(50) }
   instrumentation.runOnMainSync {
    val button=find(activityForLogin!!.window.decorView,"Đăng nhập")
    assertNotNull("logged-out demo must offer login",button); button!!.performClick()
   }
   instrumentation.waitForIdleSync()
   return editables(instrumentation.uiAutomation.rootInActiveWindow).also { assertEquals(2,it.size) }
  }
  android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS")).close()
  instrumentation.runOnMainSync { CustomerVoice.initialize(context,VoiceConfig("http://127.0.0.1:${server.port}","20","logout",allowDebugLoopback=true)) }
  CustomerVoice.logout()
  DemoApplication.configurationReady=true
  val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  activityForLogin=activity as MainActivity
  try {
   instrumentation.runOnMainSync { find(activity.window.decorView,"Đăng nhập")!!.performClick() }
   instrumentation.waitForIdleSync(); confirm("Hủy")
   assertEquals(0,dials.get()); assertEquals(SessionState.LoggedOut,CustomerVoice.sessionState.value)
   CustomerVoice.login("123","fixture"); delay(300)
   instrumentation.runOnMainSync {
    val button=find(activity.window.decorView,"Đăng xuất")
    assertNotNull("home must expose logout for the current session",button); button!!.performClick()
   }
   instrumentation.waitForIdleSync()
   val root=instrumentation.uiAutomation.rootInActiveWindow
   val positive=root.findAccessibilityNodeInfosByText("Đăng xuất").last {it.isClickable}
   assertTrue(positive.performAction(AccessibilityNodeInfo.ACTION_CLICK))
   withTimeout(5000) { while(CustomerVoice.sessionState.value is SessionState.Authenticated) delay(50) }
   assertFalse(CustomerVoice.readiness.value.isLoggedIn)
   withTimeout(5000) { while(revoked.get()==0) delay(50) }
   assertEquals(1,revoked.get()); assertEquals(0,dials.get())
   val token="runtime-demo-fixture"
   deny.set(true)
   var fields=openLogin()
   fill(fields[0],"321"); fill(fields[1],token); confirm("Đăng nhập")
   withTimeout(5000) { while(lastLogin.get().second!="Bearer $token") delay(50) }
   withTimeout(5000) { while(CustomerVoice.sessionState.value is SessionState.Authenticated) delay(50) }
   withTimeout(5000) { while(DemoApplication.loggingIn.value) delay(50) }; instrumentation.waitForIdleSync()
   assertFalse(CustomerVoice.readiness.value.isLoggedIn)
   fields=openLogin()
   assertTrue("token must not survive failed login/reopening",fields[1].isShowingHintText || fields[1].text.isNullOrEmpty())
   fill(fields[0],"321"); fill(fields[1],token)
   deny.set(false); confirm("Đăng nhập")
   withTimeout(5000) { while((CustomerVoice.sessionState.value as? SessionState.Authenticated)?.session?.userId!="321") delay(50) }
   val sent=lastLogin.get()
   assertEquals("321",org.json.JSONObject(sent.first).getString("user_id"))
   assertEquals("Bearer $token",sent.second); assertFalse(sent.first.contains(token))
   assertFalse(context.getSharedPreferences("voice-demo-public",android.content.Context.MODE_PRIVATE).all.values.any {it.toString().contains(token)})
   instrumentation.runOnMainSync { find(activity.window.decorView,"Đăng xuất")!!.performClick() }
   instrumentation.waitForIdleSync(); confirm("Đăng xuất")
   withTimeout(5000) { while(CustomerVoice.sessionState.value is SessionState.Authenticated) delay(50) }
   withTimeout(5000) { while(revoked.get()<2) delay(50) }
   assertEquals(0,dials.get())
   fields=openLogin(); assertTrue(fields[1].isShowingHintText || fields[1].text.isNullOrEmpty()); confirm("Hủy")
  } finally { instrumentation.runOnMainSync { activity.finish() }; runCatching { CustomerVoice.logout() }; server.shutdown() }
 }
}
