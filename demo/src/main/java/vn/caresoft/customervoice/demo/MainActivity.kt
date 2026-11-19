package vn.caresoft.customervoice.demo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.view.View
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.view.Gravity
import android.view.WindowManager
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.*
import kotlinx.coroutines.*
import vn.caresoft.customervoice.*
import vn.caresoft.customervoice.ui.CustomerVoiceUi

/** Demonstrates host login and restores the SDK encrypted session. */
class MainActivity : Activity() {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
 private lateinit var status: TextView
 private lateinit var call: Button
 private var pendingCall=false
 private lateinit var logout: Button
 private lateinit var incomingPermission: Button
 private var logoutDialog: AlertDialog?=null
 private var loginDialog: AlertDialog?=null
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val body=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
   setPadding(dp(28),dp(32),dp(28),dp(32))
   setOnApplyWindowInsetsListener { v,insets ->
    v.setPadding(dp(28)+insets.systemWindowInsetLeft,dp(32)+insets.systemWindowInsetTop,
     dp(28)+insets.systemWindowInsetRight,dp(32)+insets.systemWindowInsetBottom); insets
   }
   background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,intArrayOf(0xffe8f5f2.toInt(),0xfff4f7f7.toInt()))
  }
  fun label(text: String,size: Float)=TextView(this).apply {
   this.text=text; textSize=size; gravity=Gravity.CENTER; setTextColor(0xff182f35.toInt())
  }
  body.addView(label("Chăm sóc khách hàng",30f).apply { typeface=Typeface.DEFAULT_BOLD },space(0,16))
  body.addView(label("Chúng tôi luôn sẵn sàng hỗ trợ bạn",16f),space(0,32))
  call=Button(this).apply {
   text="Gọi CSKH"; isAllCaps=false; textSize=19f; setTextColor(Color.WHITE)
   backgroundTintList=android.content.res.ColorStateList.valueOf(0xff087e75.toInt())
   minHeight=dp(64); setOnClickListener { callSupport() }
  }
  body.addView(call,space(0,24))
  status=label("Đang kiểm tra kết nối…",14f)
  body.addView(status,space(0,0))
  incomingPermission=Button(this).apply {
   text="Cho phép cuộc gọi trên màn hình khóa"; isAllCaps=false
   setOnClickListener { CustomerVoiceUi.requestIncomingScreenPermission(this@MainActivity) }
  }
  body.addView(incomingPermission,space(16,0))
  logout=Button(this).apply {
   text="Đăng xuất"; isAllCaps=false; setTextColor(0xff64757a.toInt())
   backgroundTintList=android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
   setOnClickListener {
    logoutDialog?.dismiss()
    logoutDialog=AlertDialog.Builder(this@MainActivity).setTitle("Đăng xuất")
     .setMessage("Đăng xuất tài khoản hiện tại? Cuộc gọi đang diễn ra sẽ kết thúc.")
     .setNegativeButton("Hủy",null).setPositiveButton("Đăng xuất") { _,_ -> DemoApplication.logout() }.show()
   }
  }
  body.addView(logout,space(20,0)); setContentView(body)
  scope.launch { CustomerVoice.readiness.collect { render() } }
  scope.launch { DemoApplication.calling.collect { render() } }
  scope.launch { DemoApplication.callError.collect { render() } }
  scope.launch { DemoApplication.signingOut.collect { render() } }
  scope.launch { DemoApplication.loggingIn.collect { render() } }
  scope.launch { CustomerVoice.sessionState.collect { render() } }
  scope.launch { CustomerVoice.calls.collect { render() } }
  scope.launch { CustomerVoice.connectionStates.collect { render() } }
 }
 override fun onResume() {
  super.onResume()
  render()
  CustomerVoice.resume()
  DemoApplication.reconcilePush()
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED &&
   !getPreferences(MODE_PRIVATE).getBoolean("notificationsAsked",false)) {
   getPreferences(MODE_PRIVATE).edit().putBoolean("notificationsAsked",true).apply()
   requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),2)
  }
 }
 private fun callSupport() {
  val active=CustomerVoice.calls.value.lastOrNull { !it.terminal && !it.lost }
  if(active!=null) { CustomerVoiceUi.showCall(this,active.callId); return }
  if(CustomerVoice.sessionState.value !is SessionState.Authenticated) { showLogin(); return }
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
   pendingCall=true; requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),1); return
  }
  DemoApplication.callSupport(getString(R.string.demo_default_businessNumber))
 }
 private fun showLogin() {
  if(DemoApplication.loggingIn.value || DemoApplication.signingOut.value) return
  loginDialog?.dismiss()
  val prefs=getSharedPreferences("voice-demo-public",MODE_PRIVATE)
  val defaults=DemoApplication.publicDefaults(this)
  val body=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL; setPadding(dp(24),dp(12),dp(24),dp(8))
   importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
  }
  fun publicValue(key: String)=prefs.getString(key,defaults[key]).orEmpty()
  body.addView(TextView(this).apply {
   text="Máy chủ: ${publicValue("backend")}\nAccount: ${publicValue("account")}\nApp: ${publicValue("app")}"
   textSize=12f; setTextColor(0xff64757a.toInt())
  })
  val user=EditText(this).apply {
   hint="User ID"; contentDescription="User ID"; setSingleLine()
   inputType=InputType.TYPE_CLASS_NUMBER; setText(publicValue("user")); isSaveEnabled=false
  }
  val token=EditText(this).apply {
   hint="API token"; contentDescription="API token"; setSingleLine()
   inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
   isSaveEnabled=false; importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
   imeOptions=EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
  }
  body.addView(user); body.addView(token)
  val dialog=AlertDialog.Builder(this).setTitle("Đăng nhập")
   .setView(ScrollView(this).apply { addView(body) })
   .setNegativeButton("Hủy",null).setPositiveButton("Đăng nhập",null).create()
  loginDialog=dialog
  dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
  dialog.setOnDismissListener { token.text.clear() }
  dialog.show()
  dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
   val userId=user.text.toString().trim()
   if(!userId.matches(Regex("[0-9]{1,10}")) || userId.toLongOrNull() !in 1L..4294967295L) {
    user.error="Nhập User ID hợp lệ (1–4294967295)"; return@setOnClickListener
   }
   val runtimeToken=token.text.toString()
   if(runtimeToken.isBlank()) { token.error="Vui lòng nhập API token"; return@setOnClickListener }
   token.text.clear(); dialog.dismiss()
   DemoApplication.login(userId,runtimeToken)
  }
 }
 override fun onRequestPermissionsResult(code: Int,permissions: Array<out String>,grants: IntArray) {
  super.onRequestPermissionsResult(code,permissions,grants)
  scope.launch { CustomerVoice.checkReadiness() }
  if(code==1) {
   val requested=pendingCall; pendingCall=false
   if(requested && grants.firstOrNull()==PackageManager.PERMISSION_GRANTED) callSupport()
   else status.text="Cần cấp quyền micro để đàm thoại."
  }
 }
 private fun render() {
  val ready=CustomerVoice.readiness.value
  val active=CustomerVoice.calls.value.any { !it.terminal && !it.lost }
  val authenticated=CustomerVoice.sessionState.value is SessionState.Authenticated
  call.text=when {
   DemoApplication.loggingIn.value -> "Đang đăng nhập…"
   active -> "Trở lại cuộc gọi"
   !authenticated -> "Đăng nhập"
   else -> "Gọi CSKH"
  }
  call.isEnabled=DemoApplication.configurationReady && !DemoApplication.calling.value && !DemoApplication.signingOut.value && !DemoApplication.loggingIn.value
  logout.visibility=if(CustomerVoice.sessionState.value is SessionState.Authenticated) View.VISIBLE else View.GONE
  logout.isEnabled=!DemoApplication.signingOut.value
  incomingPermission.visibility=if(!CustomerVoiceUi.canShowIncomingScreen(this)) View.VISIBLE else View.GONE
  status.text=DemoApplication.callError.value ?: when {
   !DemoApplication.configurationReady -> "Điền cấu hình ứng dụng trong demo_config.xml rồi build lại demo."
   DemoApplication.loggingIn.value -> "Đang đăng nhập…"
   DemoApplication.calling.value -> "Đang kết nối đến CSKH…"
   CustomerVoice.connectionStates.value.values.any { it==CallConnectionState.RECONNECTING } -> "Đang kết nối lại cuộc gọi…"
   active -> "Bạn đang có cuộc gọi."
   ready.sessionStatus==SessionStatus.LOGGED_OUT -> "Nhấn Đăng nhập để sử dụng dịch vụ."
   ready.sessionStatus==SessionStatus.UNKNOWN -> "Chưa xác minh được phiên. Vui lòng kiểm tra kết nối."
   "MICROPHONE_PERMISSION_REQUIRED" in ready.reasons -> "Nhấn Gọi CSKH và cho phép sử dụng micro."
   ready.canCall && ready.canReceive -> "Sẵn sàng gọi và nhận cuộc gọi hỗ trợ."
   ready.canCall -> "Sẵn sàng gọi CSKH. Chưa sẵn sàng nhận cuộc gọi."
   else -> "Dịch vụ chưa sẵn sàng. Vui lòng thử lại sau."
  }
 }
 private fun space(top: Int,bottom: Int)=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(top); bottomMargin=dp(bottom) }
 private fun dp(n: Int)=(n*resources.displayMetrics.density).toInt()
 override fun onDestroy() { loginDialog?.dismiss(); logoutDialog?.dismiss(); scope.cancel(); super.onDestroy() }
}
