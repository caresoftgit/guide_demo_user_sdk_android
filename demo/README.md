# Chạy demo Android

Demo 0.4.0 dùng core 0.4.0/UI 0.4.0 từ `../maven/`. Có đăng nhập/đăng xuất, nút Gọi CSKH và giao diện cuộc gọi có sẵn. Không có tài khoản hay server test đi kèm.

## 1. Tạo cấu hình

Hoàn tất [tạo App và FCM trên Caresoft](../docs/android-integration-guide.md#2-tạo-app-và-cấu-hình-android-push-trên-caresoft) trước.

Mở `src/main/res/values/demo_config.xml`, điền:

| Resource | Giá trị |
|---|---|
| `demo_default_backend` | HTTPS gateway base URL Caresoft cấp |
| `demo_default_account` | Account ID dạng chuỗi số |
| `demo_default_app` | Caresoft App ID |
| `demo_default_businessNumber` | Đầu số được cấp cho ứng dụng |
| `demo_default_user` | Có thể để trống để nhập lúc login |
| `demo_default_firebaseAppId` | `client_info.mobilesdk_app_id` trong client đúng package của google-services.json |
| `demo_default_firebaseApiKey` | `api_key[].current_key` của client đó |
| `demo_default_firebaseProject` | `project_info.project_id` |
| `demo_default_firebaseSender` | `project_info.project_number` |

Demo khởi tạo Firebase bằng `FirebaseOptions` trong source, vì vậy **không cần apply Google Services plugin hoặc chép google-services.json** cho riêng demo này. Ứng dụng thực tế có thể dùng cách plugin trong hướng dẫn tích hợp. Không thêm service-account JSON vào demo.

Package mặc định là `com.example.customervoicedemo`. Đăng ký đúng package này trên Firebase/Caresoft, hoặc build với `-PvoiceDemoApplicationId=YOUR_APPLICATION_ID` để dùng package của bạn. Tham số này không đổi package Firebase tự động; bạn vẫn phải điền cấu hình Firebase đúng client/package.

Không có resource cho API token; nhập lúc đăng nhập, token không được lưu. Không commit file cấu hình đã điền của bạn lên fork công khai.

## 2. Build và cài

Từ **thư mục gốc repo**, dùng JDK 17 và Android SDK 35. Đặt `ANDROID_HOME` trỏ tới Android SDK hoặc tạo `local.properties` chứa `sdk.dir` của máy bạn.

```sh
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
```

Nếu chưa điền cấu hình, app hiện yêu cầu cấu hình và khóa nút đăng nhập. Sau khi sửa XML, build/cài lại. Không cài đè lên ứng dụng thật trùng package nếu chưa bảo đảm phù hợp chữ ký và dữ liệu.

## 3. Thử luồng sử dụng

1. Mở demo, cho phép thông báo/microphone. Với Android 14+, bật quyền cuộc gọi trên màn hình khóa khi app hiển thị nút hướng dẫn.
2. Bấm **Đăng nhập**, nhập User ID khách hàng và account API token được cấp. Token được che và xóa khỏi dialog khi đóng; demo không lưu token account.
3. Chờ trạng thái sẵn sàng. Bấm **Gọi CSKH** để gọi đầu số đã cấu hình; kiểm tra đàm thoại, loa, micro và kết thúc cuộc gọi.
4. Nhờ phía tổng đài gọi vào User ID đó. Kiểm tra lúc app mở, ở nền, khóa màn hình. Kết thúc phải quay về màn hình trước cuộc gọi.
5. Chuyển Wi-Fi ↔ di động trong call: hiện Đang kết nối lại rồi phục hồi, giữ mute. Mất mạng trên 30 giây: call đóng về Home.
6. Bấm **Đăng xuất**, xác nhận; app quay lại nút đăng nhập. Đăng nhập lại với thông tin cần kiểm thử.

Demo có thể khôi phục session đã lưu an toàn sau khi mở lại app. Trong app thực tế, thay dialog demo bằng luồng đăng nhập đã xác thực của bạn; truyền cùng các tham số SDK.

Xem [nghiệm thu và xử lý lỗi](../docs/testing.md).
