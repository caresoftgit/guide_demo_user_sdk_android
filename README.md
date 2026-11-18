# Caresoft User Voice SDK — Android

Hướng dẫn tích hợp tính năng **Gọi CSKH** và nhận cuộc gọi cho ứng dụng khách hàng Android.

| Thành phần | Phiên bản |
|---|---|
| SDK | 0.3.1 |
| Giao diện cuộc gọi | 0.3.2 |
| Demo | 0.3.3 |

1. Làm theo [hướng dẫn tích hợp](docs/android-integration-guide.md): tạo App trên Caresoft, cấu hình Firebase, thêm SDK, đăng nhập và nhận/gọi điện.
2. Chạy [demo](demo/README.md) để đối chiếu luồng tích hợp.
3. Dùng [API SDK](docs/sdk-api.md) khi cần tùy chỉnh giao diện.
4. Kiểm tra các [tình huống nghiệm thu](docs/testing.md) trên thiết bị thật.

Để giới thiệu giải pháp trên nhiều nền tảng, dùng [mô tả hệ thống có sơ đồ](docs/system-overview.md) và [bộ Word/PDF/PowerPoint](presentation/README.md). Phần này trình bày gateway dùng chung, client theo nền tảng và trạng thái triển khai của từng phần.

Repo kèm thư mục `maven/` chứa AAR, POM, dependency metadata của đúng hai phiên bản SDK/UI nêu trên. Demo sử dụng trực tiếp thư mục này; không cần clone source gateway hoặc SDK để build demo. Cần Internet để tải Android/Firebase/LiveKit và các dependency bên ngoài.

Yêu cầu: Android 8.0/API 26+, JDK 17, Android SDK 35. Dùng Android Studio hoặc Gradle wrapper đi kèm.

Không có tài khoản, token, đầu số, Firebase project hoặc server dùng sẵn trong repo. Nhận cấu hình của ứng dụng bạn từ Caresoft trước khi chạy.

Source SDK: [user_voice_sdk_android](https://github.com/caresoftgit/user_voice_sdk_android).
