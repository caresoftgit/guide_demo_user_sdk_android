# Kiểm thử trước khi phát hành

Dùng thiết bị Android thật có Google Play Services, Internet và cấu hình của app bạn. Chuẩn bị một phía tổng đài được phép gọi để đối chiếu. Emulator/build thành công không thay thế kiểm thử SIP, FCM, âm thanh hoặc màn hình khóa.

## Thực hiện lần lượt

1. **Login:** thông tin đúng đăng nhập được; sai token/app/package bị từ chối. Token không xuất hiện trong log hoặc dữ liệu plain text.
2. **Restore:** đóng/mở app, SDK khôi phục phiên; app đăng ký FCM lại và kiểm tra readiness. `UNKNOWN` khi offline không tự biến thành logout.
3. **Gọi ra:** bấm Gọi CSKH một lần, phía tổng đài nhận đúng đầu số. Đàm thoại hai chiều, mute/unmute, loa trong/ngoài, tai nghe nếu hỗ trợ, DTMF với IVR.
4. **Nhận cuộc gọi:** thử lúc app foreground, background và khóa màn hình; kiểm tra notification, màn hình nhận, trả lời, từ chối, cuộc gọi nhỡ/hủy trước khi trả lời. Khi mở khóa, không hiện cuộc gọi cũ đã hết hạn.
5. **Kết thúc:** thử cả hai bên gác máy; màn hình cuộc gọi đóng, trở về màn hình trước, không còn foreground notification/cuộc gọi Android.
6. **Quyền:** từ chối micro/notification/full-screen; app hiển thị hướng dẫn phù hợp. Cấp lại quyền và kiểm tra lại. Không tự trả lời khi chưa đủ quyền.
7. **Mạng:** ngắt mạng khi ringing/active, đổi Wi-Fi/mobile; không tạo cuộc gọi lặp khi lỗi. Kiểm tra cuộc gọi được dọn khi mất kết nối kéo dài.
8. **Nhiều thiết bị:** cùng user trên hai thiết bị, một máy trả lời thì máy kia dừng ringing, không được gác cuộc gọi máy thắng.
9. **Logout/login:** logout cả khi rảnh và có call; không còn điều khiển/nhận call bằng session cũ. Login lại được, đăng ký lại push. Không giữ token account trong form.

## Khi có lỗi

| Hiện tượng | Kiểm tra |
|---|---|
| Chưa đăng nhập được | HTTPS/TLS, account/App ID/token, user dạng số, app/config hoạt động, applicationId cuối cùng khớp Android config |
| `PUSH_NOT_REGISTERED` | FCM token mới nhất, đăng ký sau session authenticated, đúng project/package, kết quả API đăng ký |
| `NOTIFICATIONS_DISABLED` | Quyền app và kênh cuộc gọi; Android 13+ runtime notification permission |
| Khóa máy không bật UI | Quyền full-screen Android 14+, notification/kênh, cài đặt pin của thiết bị; kiểm tra app có bị Force stop không |
| `VOICE_NOT_READY`/503 | Nhờ Caresoft kiểm tra gateway/media/định tuyến, không tự gọi lại liên tục |
| `VOICE_SERVER_NOT_CONFIGURED` | Nhờ Caresoft kiểm tra cấu hình định tuyến account ở gateway |
| Gọi được nhưng mất tiếng | Quyền micro, thiết bị âm thanh đang chọn, mạng và media route phía gateway/tổng đài |
| Timeout khi tạo call | Quan sát snapshot hiện tại, không blind retry `call()` |

Khi gửi báo lỗi, cung cấp phiên bản SDK/UI/app, Android/model, thời điểm và múi giờ, bước tái hiện và mã lỗi đã che dữ liệu. Không gửi API token, access/refresh/media token, FCM token hoặc private key.
