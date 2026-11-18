# Tích hợp User Voice SDK vào Android

SDK 0.3.1 · UI 0.3.2 · Android API 26+ · Kotlin/JDK 17.

## 1. Chuẩn bị thông tin

| Thông tin | Lấy ở đâu | Dùng ở đâu |
|---|---|---|
| HTTPS gateway URL | Caresoft cấp cho môi trường của bạn | `VoiceConfig.backendUrl` — chỉ base URL, không thêm account/path API |
| Account ID | Caresoft cấp | `VoiceConfig.accountId`, chuỗi số |
| Caresoft App ID | Tạo ở bước 2 | `VoiceConfig.appId` |
| Android applicationId | Project Android của bạn | Phải khớp cấu hình Caresoft và Firebase |
| Đầu số CSKH được cấp | Caresoft cấp sau khi định tuyến tổng đài | Ứng dụng tự cấu hình rồi truyền vào `call()` |
| User ID khách hàng | Luồng xác thực của ứng dụng bạn | `login()`, chuỗi số trong khoảng 1–4294967295 |
| Account API token | Caresoft cấp qua kênh bảo mật | Tham số `tenantApiToken` của `login()` |

Không có API danh sách đầu số hoặc `getDestinations()`. App truyền đầu số nguyên bản, không thêm `sip:`, domain hay prefix định tuyến. Không có màn hình trạng thái agent hoặc yêu cầu bật trạng thái sẵn sàng thủ công.

## 2. Tạo App và cấu hình Android push trên Caresoft

### 2.1. Tạo App

1. Đăng nhập trang quản trị Caresoft đúng domain và môi trường của bạn.
2. Vào **Admin Settings → Cơ bản → Voice SDK**. Nếu không thấy mục này, nhờ quản trị viên cấp quyền Voice SDK.
3. Tạo app, nhập tên ứng dụng và bật trạng thái **Hoạt động**.
4. Lưu, sao chép **App ID** Caresoft sinh để dùng ở `VoiceConfig.appId`. App ID này khác Android applicationId và Firebase App ID.
5. Mở **Cấu hình push** của app, thêm cấu hình **Android (FCM)**.

### 2.2. Chuẩn bị Firebase

1. Mở Firebase Console, chọn/tạo project cho ứng dụng.
2. Thêm Android app với package name bằng **applicationId cuối cùng của bản build**, kể cả suffix nếu có.
3. Tải `google-services.json` về máy phát triển; đặt trong module app theo bước 4. Không dùng file từ package/project khác.
4. Bật Firebase Cloud Messaging API (HTTP v1) cho project.
5. Trong **Project settings → Service accounts**, lấy service-account JSON có quyền gửi FCM cho project. File này chỉ dùng cho cấu hình server Caresoft ở bước kế tiếp.

### 2.3. Điền cấu hình push trên Caresoft

| Trường | Giá trị |
|---|---|
| Platform/provider | Android / FCM |
| Environment | `default` |
| Application ID | Android applicationId của app, phân biệt hoa thường |
| Firebase Project ID | `project_id` của Firebase project tương ứng |
| File credentials | Service-account JSON dùng gửi FCM HTTP v1 |
| Trạng thái | Hoạt động |

Lưu cấu hình và giữ cả app lẫn push config hoạt động. Nếu màn hình quản trị không có trường environment riêng, dùng cấu hình Android mặc định. Không dùng `sandbox`/`production` của APNs cho Android.

`google-services.json` chứa cấu hình Firebase client; **service-account JSON chứa private key của server**. Không đóng gói service-account JSON trong APK hoặc đưa vào Git/chat/log. App không truyền `push_config_id` hay credentials FCM vào SDK; server tự chọn cấu hình đúng account/App ID/package.

Khi đổi package/project, cập nhật đồng bộ Firebase, Caresoft và bản build; đăng nhập rồi đăng ký push lại. Sau khi đổi credentials, kiểm tra một cuộc gọi thật.

## 3. Thêm thư viện

Sao chép **toàn bộ** `maven/` của repo này vào project, ví dụ `<project>/voice-maven/`; giữ cả POM và module metadata để Gradle lấy dependency bắc cầu. Không chỉ chép AAR vào `libs/`.

Trong `settings.gradle` (Groovy), thêm vào danh sách repository hiện có:

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url uri("voice-maven") }
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
}
```

Trong module ứng dụng:

```groovy
android {
    compileSdk 35
    defaultConfig { minSdk 26; targetSdk 35 }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation 'vn.caresoft:customer-voice-android-ui:0.3.2'
    implementation 'com.google.firebase:firebase-messaging:24.1.0'
}
```

UI kéo theo core 0.3.1. Nếu tự xây giao diện, dùng `vn.caresoft:customer-voice-android:0.3.1` thay dependency UI. Không thêm đồng thời bản SDK từ source và AAR.

## 4. Cấu hình Firebase client và manifest

1. Đặt `google-services.json` đúng package vào module ứng dụng; giữ ngoài Git.
2. Khai báo Google Services plugin trong project, ví dụ `id 'com.google.gms.google-services' version '4.4.2' apply false`; apply `id 'com.google.gms.google-services'` ở module ứng dụng.
3. Khai báo lớp `Application` của bạn và một `FirebaseMessagingService` trong manifest:

```xml
<application android:name=".MyApplication" android:allowBackup="false">
    <service android:name=".VoiceMessagingService" android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>
</application>
```

Nếu ứng dụng đã có Firebase service, forward voice data vào SDK trong service đó; không khai báo service thứ hai cạnh tranh cùng intent.

SDK tự merge permission/service/receiver cần cho Internet, microphone, Telecom, foreground call và full-screen intent. Host vẫn phải xin quyền **RECORD_AUDIO** lúc app đang mở, **POST_NOTIFICATIONS** trên Android 13+, và kiểm tra quyền thông báo của ứng dụng/kênh cuộc gọi. Nếu thêm quyền Bluetooth để liệt kê thiết bị trên phiên bản Android yêu cầu, xin quyền tương ứng trong ứng dụng.

## 5. Khởi tạo một lần trong Application

Đoạn dưới dùng các biến cấu hình do app cung cấp từ bước 1:

```kotlin
import vn.caresoft.customervoice.CustomerVoice
import vn.caresoft.customervoice.VoiceConfig
import vn.caresoft.customervoice.ui.CustomerVoiceUi

// Trong Application.onCreate(), trên main thread:
CustomerVoiceUi.install(this, supportName = "Chăm sóc khách hàng")
CustomerVoice.initialize(this, VoiceConfig(
    backendUrl = gatewayBaseUrl,
    accountId = accountId,
    appId = caresoftAppId,
    notificationTitle = "Chăm sóc khách hàng",
    incomingIntent = CustomerVoiceUi.incomingIntent(this)
))
```

Khởi tạo cả khi process được FCM đánh thức, trước khi xử lý message. Dùng HTTPS có chứng thư hợp lệ; không tắt kiểm tra TLS. Đăng xuất trước khi đổi account/App ID/gateway. SDK tự khôi phục session được lưu bằng Android Keystore; `SessionState.Authenticated` chưa thay thế kiểm tra readiness trên server.

## 6. Đăng nhập bằng phiên ứng dụng cấp

Sau khi ứng dụng của bạn đã xác thực khách hàng, gọi từ coroutine:

```kotlin
val session = CustomerVoice.login(userId, tenantApiToken)
```

`userId` phải là ID khách hàng đã xác thực của ứng dụng, không lấy tùy ý từ input không tin cậy. Account token xác thực tích hợp account, không chứng minh riêng danh tính khách hàng. Không hardcode token account trong source, resources, BuildConfig hoặc APK phát hành; không ghi token/session vào log. Thống nhất cách cấp thông tin đăng nhập với Caresoft trước khi phát hành production.

Theo dõi `CustomerVoice.sessionState`. Khi `LoggedOut` hoặc `LoginRequired`, hiển thị luồng đăng nhập của app và truyền lại hai tham số trên. Khi mất mạng, không tự xóa phiên hoặc tự đăng nhập lặp lại. Tác vụ đăng nhập/cuộc gọi nên thuộc service/application scope để không bị hủy khi Activity xoay hoặc đóng.

## 7. Đăng ký và xử lý FCM

Sau login thành công **và sau restore phiên**, lấy token hiện tại rồi gọi:

```kotlin
FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
    appScope.launch { CustomerVoice.updatePushToken(token) }
}
```

`appScope` do app quản lý (`SupervisorJob` + main dispatcher). Bắt lỗi trong coroutine và thử lại khi kết nối phục hồi/app resume; chỉ đăng ký khi session đang authenticated. Nếu session đổi trong lúc chờ Firebase, bỏ kết quả cũ rồi lấy/đăng ký lại cho session mới. Demo có [PushRegistration.kt](../demo/src/main/java/vn/caresoft/customervoice/demo/PushRegistration.kt) minh họa kiểm tra này.

Trong Firebase service:

```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    val handled = CustomerVoice.handleRemoteMessage(message.data)
    if (!handled) {
        // Xử lý loại thông báo khác của ứng dụng.
    }
}

override fun onNewToken(token: String) {
    // Chuyển token cho tầng app: đăng ký nếu authenticated;
    // nếu chưa đăng nhập thì lấy token mới nhất sau login/restore.
}
```

Forward nguyên `message.data`, không tự mở màn hình/answer từ payload và không xử lý cùng message hai lần. SDK kiểm tra session/app/hạn dùng rồi lấy trạng thái cuộc gọi từ server. ACK đăng ký push không bảo đảm FCM đã tới thiết bị. Nếu cần ngừng nhận push nhưng giữ session, gọi `unregisterPushToken()`.

## 8. Kiểm tra đăng nhập và sẵn sàng

```kotlin
val ready = CustomerVoice.checkReadiness()
if (ready.canCall) {
    // Cho phép nút Gọi CSKH.
}
if (!ready.canReceive) {
    // Dựa vào ready.reasons để hướng dẫn đăng nhập/cấp quyền/đăng ký push.
}
```

Theo dõi `CustomerVoice.readiness` để cập nhật UI. `isLoggedIn` là session đã được server xác minh gần đây; `canCall` thêm điều kiện voice/micro/không bận; `canReceive` thêm push registration và notification. `UNKNOWN` là chưa xác minh được, không đồng nghĩa đã đăng xuất. Trạng thái có thời hạn, cần kiểm tra trước thao tác; readiness không bảo đảm chất lượng media hay khả năng bật màn hình khóa.

## 9. Nút Gọi CSKH và giao diện cuộc gọi

```kotlin
// businessNumber: đầu số do ứng dụng tự cấu hình từ thông tin Caresoft cấp.
val ready = CustomerVoice.checkReadiness()
if (ready.canCall) CustomerVoice.call(businessNumber)
```

Đầu số là chuỗi 1–32 chữ số, có thể có `+` đầu chuỗi. Không tự retry `call()` nếu timeout/mất response vì có thể tạo cuộc gọi trùng; đối chiếu `CustomerVoice.calls` trước. Chỉ hiển thị đã kết nối khi snapshot có `state == "ACTIVE"`.

UI cài ở bước 5 tự hiển thị cuộc gọi vào/ra khi app ở foreground; incoming background dùng thông báo cuộc gọi. UI có trả lời, từ chối, kết thúc, tắt micro, đổi loa và DTMF trong cuộc gọi. Activity cuộc gọi tự đóng khi cuộc gọi kết thúc và quay về màn hình trước. Không cần xây lại các màn hình này hoặc thêm màn hình bấm số chủ động.

Android 14+ có quyền riêng để hiển thị cuộc gọi full-screen:

```kotlin
if (!CustomerVoiceUi.canShowIncomingScreen(activity)) {
    CustomerVoiceUi.requestIncomingScreenPermission(activity)
}
```

Mở yêu cầu này từ thao tác rõ ràng của người dùng. Cho phép notification và kênh cuộc gọi; kiểm tra full-screen trên thiết bị thật. SDK không bỏ qua PIN và không bảo đảm nhận cuộc gọi sau khi người dùng Force stop app. Khi OS không cho full-screen, dùng thông báo cuộc gọi còn được phép.

## 10. Đăng xuất và nghiệm thu

```kotlin
CustomerVoice.logout()
```

Gọi khi đăng xuất tài khoản ứng dụng. SDK kết thúc call và dọn session local; nếu server không xác nhận được logout, báo lỗi kết nối thay vì tuyên bố đã thu hồi server thành công. Không lưu lại account token để tự đăng nhập sau logout.

Chạy lần lượt [checklist nghiệm thu](testing.md). Dùng [demo/README.md](../demo/README.md) để build app mẫu bằng cấu hình của bạn.
