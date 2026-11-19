package vn.caresoft.customervoice.demo

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import vn.caresoft.customervoice.*
import vn.caresoft.customervoice.ui.CustomerVoiceUi
import kotlinx.coroutines.flow.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CustomerVoiceUi.install(this)
        configurationReady = runCatching { configureSaved(this) }.getOrDefault(false)
        appScope.launch {
            CustomerVoice.events.collect { event ->
                if(event.type=="connection_changed" && event.code==CallConnectionState.CONNECTED.name) reconcilePush()
                if(event.type=="error" && event.code=="NETWORK_RECOVERY_TIMEOUT")
                    _callError.value="Cuộc gọi đã kết thúc vì mất kết nối quá lâu. Vui lòng kiểm tra mạng rồi gọi lại."
            }
        }
        appScope.launch {
            CustomerVoice.sessionState.map { (it as? SessionState.Authenticated)?.session?.sessionId }
                .distinctUntilChanged().collect { session ->
                    if(session!=null) reconcilePush()
                }
        }
    }
    companion object {
        var configurationReady = false
            internal set
        private val appScope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
        private val _calling=MutableStateFlow(false)
        val calling=_calling.asStateFlow()
        private val _callError=MutableStateFlow<String?>(null)
        val callError=_callError.asStateFlow()
        private val _signingOut=MutableStateFlow(false)
        val signingOut=_signingOut.asStateFlow()
        private val _loggingIn=MutableStateFlow(false)
        val loggingIn=_loggingIn.asStateFlow()
        fun login(userId: String, tenantApiToken: String) {
            if(_loggingIn.value || _signingOut.value || CustomerVoice.sessionState.value is SessionState.Authenticated) return
            _loggingIn.value=true; _callError.value=null
            appScope.launch {
                try {
                    CustomerVoice.login(userId,tenantApiToken)
                    appContext.getSharedPreferences("voice-demo-public",Context.MODE_PRIVATE).edit().putString("user",userId).apply()
                    reconcilePush()
                    CustomerVoice.checkReadiness()
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) {
                    _callError.value=if(e is IllegalArgumentException || (e as? VoiceException)?.status in listOf(400,401,403))
                        "Thông tin đăng nhập không hợp lệ. Vui lòng kiểm tra User ID và API token."
                        else "Chưa đăng nhập được. Vui lòng kiểm tra kết nối và thử lại."
                } finally { _loggingIn.value=false }
            }
        }
        fun logout() {
            if(_signingOut.value) return
            _signingOut.value=true; _callError.value=null
            appScope.launch {
                try { CustomerVoice.logout() }
                catch(e: CancellationException) { throw e }
                catch(_: Exception) { _callError.value="Đã đăng xuất trên thiết bị. Chưa xác nhận được việc kết thúc phiên trên máy chủ." }
                finally { _signingOut.value=false }
            }
        }
        fun callSupport(number: String) {
            if(_calling.value || _signingOut.value || _loggingIn.value) return
            _calling.value=true; _callError.value=null
            appScope.launch {
                try {
                    val readiness=CustomerVoice.checkReadiness()
                    if(!readiness.canCall) {
                        _callError.value=if(!readiness.isLoggedIn) "Vui lòng kiểm tra kết nối hoặc đăng nhập lại từ ứng dụng."
                            else "Chưa thể bắt đầu cuộc gọi. Vui lòng kiểm tra quyền micro và kết nối."
                    } else CustomerVoice.call(number)
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) { if(CustomerVoice.sessionState.value is SessionState.Authenticated) _callError.value=CustomerVoiceUi.message(e) }
                finally { _calling.value=false }
            }
        }
        private val push=PushRegistration(appScope,
            { (CustomerVoice.sessionState.value as? SessionState.Authenticated)?.session?.sessionId },
            { suspendCancellableCoroutine { continuation ->
                FirebaseMessaging.getInstance().token.addOnSuccessListener { if(continuation.isActive) continuation.resume(it) }
                    .addOnFailureListener { if(continuation.isActive) continuation.resumeWithException(it) }
            } }, { CustomerVoice.updatePushToken(it) })
        fun reconcilePush() {
            appScope.launch {
                if(::appContext.isInitialized && FirebaseApp.getApps(appContext).isNotEmpty()) push.reconcile()
            }
        }
        private lateinit var appContext: Context

        fun publicDefaults(context: Context)=mapOf(
            "backend" to context.getString(R.string.demo_default_backend),
            "account" to context.getString(R.string.demo_default_account),
            "user" to context.getString(R.string.demo_default_user),
            "app" to context.getString(R.string.demo_default_app),
            "businessNumber" to context.getString(R.string.demo_default_businessNumber),
            "firebaseAppId" to context.getString(R.string.demo_default_firebaseAppId),
            "firebaseApiKey" to context.getString(R.string.demo_default_firebaseApiKey),
            "firebaseProject" to context.getString(R.string.demo_default_firebaseProject),
            "firebaseSender" to context.getString(R.string.demo_default_firebaseSender))
        fun configureSaved(context: Context): Boolean {
            appContext=context.applicationContext
            val p=context.getSharedPreferences("voice-demo-public",Context.MODE_PRIVATE)
            val defaults=publicDefaults(context)
            fun value(key: String)=p.getString(key,defaults[key].orEmpty()).orEmpty()
            val backend=value("backend"); val account=value("account"); val app=value("app")
            if(backend.isBlank() || account.isBlank() || app.isBlank()) return false
            val launch=CustomerVoiceUi.incomingIntent(context)
            CustomerVoice.initialize(context,VoiceConfig(backend,account,app,notificationTitle="Chăm sóc khách hàng",incomingIntent=launch,allowDebugLoopback=p.getBoolean("loopback",false)))
            val firebaseAppId=value("firebaseAppId")
            if(firebaseAppId.isNotBlank() && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context,FirebaseOptions.Builder().setApplicationId(firebaseAppId)
                    .setApiKey(value("firebaseApiKey")).setProjectId(value("firebaseProject"))
                    .setGcmSenderId(value("firebaseSender")).build())
            }
            return true
        }
    }
}
class DemoMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) { CustomerVoice.handleRemoteMessage(message.data) }
    override fun onNewToken(token: String) {
        DemoApplication.reconcilePush()
    }
}
