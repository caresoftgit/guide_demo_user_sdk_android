# Lời dẫn thuyết trình User Voice

Gợi ý lời nói cho bộ slide tổng quan.

## Slide 1

Mở đầu bằng trải nghiệm khách hàng: gọi CSKH ngay trong app bằng Internet, hoặc nhận cuộc gọi tổng đài theo User ID. Đây là bộ User Voice dành khách hàng, khác SDK agent.

## Slide 2

Không có màn hình bấm số gọi ra hay trạng thái ca làm việc. DTMF trong cuộc gọi phục vụ IVR là chức năng riêng. UI Android đi kèm tự đóng khi call kết thúc.

## Slide 3

Android đã có User SDK. iOS và Web cần implementation mới. Flutter/React Native cần bridge tới native SDK trên từng hệ điều hành. Gateway hiện validation/session/push Android, cần mở rộng platform/provider.

## Slide 4

Gateway không truyền quyền chọn SIP peer/trunk/room cho client. MySQL giữ account/app/registration/session hashes. Redis giữ call/busy/winner/idempotency. LiveKit xử lý media.

## Slide 5

Host xác thực user. Account API token xác thực tích hợp account chứ không chứng minh riêng danh tính user. Gateway cấp access/refresh riêng. Android lưu session bằng Keystore. Không hardcode account token vào APK/JS bundle.

## Slide 6

App truyền đầu số nguyên bản. Gateway dựng Request-URI/To SDKIN_<voice_server_id>_<business_number>. From vẫn là user_id dạng số. Chỉ ACTIVE xác nhận đã kết nối, không blind retry create khi timeout.

## Slide 7

Tổng đài INVITE tới user_id, From mang đầu số dịch vụ. Webhook đã ký xác nhận SIP participant. Event và push có thể cùng tới, client luôn lấy snapshot và kiểm tra hạn. Gateway claim atomic một winner khi answer.

## Slide 8

Android đã có FCM, Telecom, foreground notification và full-screen permission. iOS cần APNs VoIP/PushKit/CallKit cho User SDK, không copy nguyên Android lifecycle. Web đóng tab/browser không có bảo đảm native. FCM accepted không đồng nghĩa delivery.

## Slide 9

SDK gọi API gateway để xin quyền/control. Media token ngắn hạn theo room/identity. SDK kết nối LiveKit bằng WebRTC. SIP bridge nối RTP tới tổng đài. Kiểm tra mạng, codec, microphone, route và NAT/TURN riêng.

## Slide 10

Các trạng thái có thể kết thúc sớm do hủy/từ chối/timeout/lỗi. Android dùng ledger/version không hồi sinh terminal. Outbound CREATING RINGING ACTIVE ENDED. Inbound RINGING CONNECTING ACTIVE ENDED.

## Slide 11

API Android checkReadiness trả isLoggedIn/canCall/canReceive và reasons. UNKNOWN do chưa xác minh mạng không nên tự logout. Kết quả có hạn, Android observation tối đa 30 giây nếu không xác minh mới. Full-screen permission kiểm tra riêng.

## Slide 12

Account API token có phạm vi rộng, không đóng gói trong client. Production cần cơ chế credential an toàn và host xác thực user. Session token dùng TLS và lưu bảo mật. Gateway xác thực signed webhook và source SIP/RTP. Không đưa keys vào slides/Git.

## Slide 13

Mô tả kiến trúc đích, không công bố đã hỗ trợ tất cả. Android AAR hiện có. iOS cần native User SDK. Flutter/RN cần native SDK cho cả hai OS rồi bridge. Web cần JS client và background contract.

## Slide 14

Đầu tiên xác định account/App và route/hotline. Cấu hình push đúng package/BundleID. Khởi tạo ở Application/OS lifecycle. Login gắn auth host, đăng ký push sau login/restore. Kiểm thử hai chiều, media, nền và logout. Android guide có từng bước và demo sạch cấu hình.

## Slide 15

Thử foreground/background/locked screen, two-way media, mute speaker DTMF, multi device winner, loss reconnect, login logout. Gateway restart dọn owned rooms kết thúc call cũ, không dial lại. Một coordinator/state scope, chưa cam kết scale ngang/SLA.

## Slide 16

Android core0.3.1/UI0.3.2/demo0.3.3 và gateway0.2.2 là bộ hiện tại. Hai chiều/speaker/locked screen đã từng kiểm thử thực tế Android. Chưa cam kết implementation User SDK iOS/Web/Flutter/RN. Cần adapter platform và user scoped credential design cho production.
