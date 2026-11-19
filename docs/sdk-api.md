# API Android SDK

Core 0.4.0, namespace `vn.caresoft.customervoice`; UI 0.4.0, namespace `vn.caresoft.customervoice.ui`. Các hàm network/control là `suspend`; gọi trong coroutine và bắt `VoiceException`/lỗi kết nối. Không log credential hoặc payload nhạy cảm.

| API | Công dụng |
|---|---|
| `initialize(context, VoiceConfig)` | Khởi tạo trong Application, khôi phục session lưu an toàn |
| `login(userId, tenantApiToken): SessionInfo` | Đăng nhập khách hàng do host xác thực |
| `logout()` | Kết thúc cuộc gọi, xóa local và yêu cầu thu hồi session |
| `updatePushToken(token)` | Đăng ký FCM cho phiên hiện tại sau login/restore/rotation |
| `unregisterPushToken()` | Gỡ đăng ký push hiện tại |
| `handleRemoteMessage(data): Boolean` | Forward FCM data; true nếu SDK nhận xử lý |
| `checkReadiness(): VoiceReadiness` | Kiểm tra khả năng gọi/nhận của phiên hiện tại |
| `call(businessNumber): CallSnapshot` | Gọi đầu số ứng dụng tự cấu hình |
| `answer(callId)` / `reject(callId)` | Trả lời/từ chối inbound |
| `hangup(callId)` | Kết thúc cuộc gọi của thiết bị |
| `setMuted(callId, muted)` | Đặt trạng thái micro |
| `getAvailableAudioRoutes(callId)` | Danh sách `AudioRoute` hiện có |
| `selectAudioRoute(callId, route)` | Chọn một route từ danh sách hiện có |
| `sendDtmf(callId, digit)` | Gửi một ký tự `0–9`, `*`, `#` trong cuộc gọi |
| `resume()` | Đồng bộ phiên/cuộc gọi khi app về foreground; không tự tạo hoặc trả lời call |
| `isCallPresentable(callId)` | Kiểm tra cuộc gọi còn được hiển thị trên thiết bị |
| `currentTime()` | Đồng hồ hiệu chỉnh theo server để so hạn cuộc gọi |

`VoiceConfig`: `backendUrl`, `accountId`, `appId`, tùy chọn `notificationTitle`, `incomingIntent`. `allowDebugLoopback` chỉ dành test local debug; không dùng trong bản phát hành.

## Dữ liệu quan sát

| Flow | Ý nghĩa |
|---|---|
| `sessionState` | `LoggedOut`, `Authenticated(session)`, `LoginRequired(reason)` |
| `readiness` | `sessionStatus`, `isLoggedIn`, `canCall`, `canReceive`, `userId`, `reasons`, `checkedAt`, `controlConnected` |
| `calls` | Danh sách snapshot, gồm cuộc gọi terminal; lọc `!terminal && !lost` để tìm cuộc gọi thiết bị còn điều khiển |
| `connectionStates` | Map call ID → `CONNECTING`, `CONNECTED`, `RECONNECTING` của media; độc lập với server call state |
| `audioStates` | Map call ID → `CallAudioState(muted, routes)`; `route.selected` phản ánh route Android đã báo |
| `events` | `VoiceEvent(type, callId, code)` để cập nhật/xử lý lỗi |

`SessionStatus`: `LOGGED_OUT`, `VALID`, `UNKNOWN`. Các reason readiness: `LOGIN_REQUIRED`, `SESSION_UNVERIFIED`, `VOICE_NOT_READY`, `MICROPHONE_PERMISSION_REQUIRED`, `NOTIFICATIONS_DISABLED`, `PUSH_NOT_REGISTERED`, `CALL_IN_PROGRESS`. `controlConnected` độc lập với session hợp lệ; socket ngắt không nhất thiết là logout.

`CallSnapshot` có `callId`, `direction`, `state`, `version`, `businessNumber`, các deadline, `deviceStatus`, `isOwner`, `endedAt`, `endReason`. Chỉ `ACTIVE` xác nhận kết nối; không dùng việc `call()` thành công để báo đã đàm thoại. Khi `terminal`/`lost`, ngừng điều khiển và đóng UI tương ứng. Không suy quyền trả lời inbound từ `isOwner` (field này dành outbound).

## Giao diện có sẵn

| API | Công dụng |
|---|---|
| `CustomerVoiceUi.install(application, supportName)` | Cài bộ quan sát/hiển thị UI, gọi một lần trên main thread |
| `CustomerVoiceUi.incomingIntent(context)` | PendingIntent cho `VoiceConfig.incomingIntent` |
| `CustomerVoiceUi.showCall(context, callId)` | Mở/trở lại Activity cuộc gọi |
| `CustomerVoiceUi.canShowIncomingScreen(context)` | Kiểm tra quyền full-screen incoming |
| `CustomerVoiceUi.requestIncomingScreenPermission(activity)` | Mở Android settings xin quyền này |
| `CustomerVoiceCallView(context).bind(callId)` | Nhúng view cuộc gọi vào màn hình riêng |
| `view.unbind()` | Dọn observer khi bỏ view |

Activity SDK tự đóng khi call kết thúc. Nếu nhúng `CustomerVoiceCallView`, host tự điều hướng/đóng màn hình theo `CustomerVoice.calls`. Không gọi `answer()` hai lần từ UI của host nếu đã để UI/notification SDK xử lý.

Thứ tự triển khai và ví dụ có ở [hướng dẫn tích hợp](https://github.com/caresoftgit/guide_demo_user_sdk_android/blob/main/docs/android-integration-guide.md).

## Khôi phục mạng và kết thúc local

SDK quan sát mạng mặc định và sự cố LiveKit. Khi mất mạng hoặc chuyển Wi-Fi/di
động trong cuộc gọi đã kết nối, `connectionStates[callId]` chuyển thành
`RECONNECTING`, phát `connection_changed` với code tương ứng. SDK lấy media grant
mới rồi khôi phục transport trong **cùng call/room**, giữ trạng thái mute; không
POST tạo call mới. Chỉ báo `CONNECTED` sau khi kết nối nhận và publisher ICE/DTLS cùng sẵn sàng.
Server snapshot `ACTIVE` vẫn là mốc đàm thoại; media connected không đổi state này.

Không phục hồi trong **30 giây liên tục**, SDK dừng mic/media/Telecom, đóng call UI,
phát `NETWORK_RECOVERY_TIMEOUT` và cố gửi hangup. Callback mạng lặp không kéo dài
hạn; response/callback cũ không hồi sinh call đã đóng. Socket điều khiển ngắt riêng
không đóng media còn hoạt động. Gateway hỗ trợ grace ACTIVE 30 giây từ bản 0.3.1.

`hangup`/`reject` dọn local và đóng UI trước khi chờ HTTP. Nếu control request thất
bại, host báo lỗi phù hợp; không khôi phục call local đã đóng. `calls` giữ snapshot
terminal/lost, nhưng loại call đã dừng local mà server chưa trả terminal.
Host dùng `isCallPresentable` thay vì chỉ kiểm tra token hoặc server ACTIVE.
