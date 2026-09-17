# 🎵 QuarkTube - Android YouTube & Background Music Player

<p align="center">
  <img src="app/src/main/ic_launcher-web.png" width="110" height="110" alt="QuarkTube Logo" />
</p>

<p align="center">
  <strong>Ứng dụng Android nghe nhạc và xem video YouTube không quảng cáo, hỗ trợ phát nền khi tắt màn hình, giao diện Jetpack Compose mượt mà và tối ưu pin.</strong>
</p>

<p align="center">
  <a href="https://github.com/chuantranvn/soundtube-android/releases">
    <img src="https://img.shields.io/badge/Release-APK%20Latest-E50914?style=flat-square&logo=github&logoColor=white" alt="Latest Release" />
  </a>
  <a href="https://github.com/chuantranvn/soundtube-android/actions/workflows/deploy-supabase.yml">
    <img src="https://github.com/chuantranvn/soundtube-android/actions/workflows/deploy-supabase.yml/badge.svg?branch=main" alt="Build Status" />
  </a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android Version" />
  <img src="https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" />
</p>

---

## 🌟 Tính năng nổi bật

- 🎧 **Phát âm thanh chạy nền (Background Playback):** Tiếp tục nghe nhạc khi khóa màn hình hoặc chuyển sang ứng dụng khác.
- 🔔 **Media Notification & Lock Screen Controls:** Trình phát tích hợp đầy đủ nút Play/Pause, Next, Previous trên thanh thông báo và màn hình khóa.
- 📺 **Trình phát Video toàn màn hình (Fullscreen Player):** Hỗ trợ đổi chiều xoay ngang, điều khiển tiến độ mượt mà.
- 🔍 **Tìm kiếm & Khám phá nội dung:** Tìm kiếm video, bài hát, nghệ sĩ nhanh chóng thông qua YouTube Innertube API độc lập, không cần Google Play Services (GMS).
- ⚡ **Giao diện Jetpack Compose chuẩn YouTube:** Giao diện tối hiện đại, thanh điều hướng (Home, Shorts, Subscriptions, You), Bottom Sheet Mini-player vuốt tiện lợi.
- 🔄 **Đồng bộ hóa cá nhân (YouTube Sync):** Hỗ trợ đăng nhập để lấy danh sách đăng ký và lịch sử xem.
- 🚀 **Tự động Build & Deploy:** Tích hợp CI/CD tự động xuất bản file APK lên cả **GitHub Releases** và **Supabase Storage**.

---

## 📲 Tải về ứng dụng (Downloads)

Bạn có thể tải bản cài đặt APK mới nhất theo 2 nguồn:

| Kênh tải | Đường dẫn | Mô tả |
| :--- | :--- | :--- |
| 📦 **GitHub Releases** | [**Tải bản mới nhất tại GitHub Releases**](https://github.com/chuantranvn/soundtube-android/releases) | Đầy đủ changelog, mã commit và các phiên bản |
| ☁️ **Supabase Storage CDN** | `QuarkTube-release.apk` | Link trực tiếp từ Supabase Bucket tốc độ cao |

---

## 🛠️ Công nghệ & Thư viện sử dụng

- **Ngôn ngữ:** Kotlin 1.9+
- **Giao diện:** Jetpack Compose, Material 3, Material Icons Extended
- **Kiến trúc:** MVVM (Model-View-ViewModel), StateFlow, Coroutines
- **Media & Audio:** AndroidX Media (`MediaBrowserServiceCompat`, `MediaSessionCompat`), Android Foreground Service
- **Mạng:** OkHttp 4.12
- **Tải ảnh:** Coil Compose 2.6
- **CI/CD:** GitHub Actions, Ubuntu Runner, OpenJDK 17 Temurin, Supabase REST API, `softprops/action-gh-release`

---

## 🏗️ Hướng dẫn cài đặt & Build từ mã nguồn

### Yêu cầu môi trường
- **Android Studio:** Hedgehog (2023.1.1) hoặc mới hơn
- **JDK:** OpenJDK 17
- **Android SDK:** Compile SDK 34, Min SDK 26 (Android 8.0 trở lên)

### Các bước thực hiện

1. **Clone repository:**
   ```bash
   git clone https://github.com/chuantranvn/soundtube-android.git
   cd soundtube-android
   ```

2. **Cấp quyền thực thi Gradlew (Linux/macOS):**
   ```bash
   chmod +x gradlew
   ```

3. **Build bản Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   *File APK sinh ra tại:* `app/build/outputs/apk/debug/QuarkTube-debug.apk`

4. **Build bản Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```
   *File APK sinh ra tại:* `app/build/outputs/apk/release/QuarkTube-release.apk`

---

## 🤖 CI/CD Workflow & Biến môi trường

Khi `push` vào nhánh `main` hoặc tạo tag phát hành `v*`, quy trình GitHub Actions tại [.github/workflows/deploy-supabase.yml](.github/workflows/deploy-supabase.yml) sẽ tự động:
1. Thiết lập môi trường JDK 17 & Gradle Cache.
2. Build `assembleRelease`.
3. Tạo 2 bản APK: bản cố định `QuarkTube-release.apk` và bản gắn timestamp `QuarkTube-YYYY-MM-DD_HHhMM-sha.apk`.
4. Đẩy file lên **Supabase Storage** (yêu cầu cấu hình Repository Secrets):
   - `SUPABASE_URL`: Đường dẫn Project Supabase.
   - `SUPABASE_SERVICE_ROLE_KEY`: Service Role Secret Key của Supabase.
   - `SUPABASE_BUCKET`: Tên Bucket lưu file APK.
5. Tạo bản phát hành **GitHub Release** và đính kèm trực tiếp các file APK.

---

## 📄 Bản quyền (License)

Dự án phục vụ mục đích học tập và nghiên cứu cá nhân.
Mọi bản quyền nội dung âm nhạc và video thuộc về các chủ sở hữu trên nền tảng YouTube.
