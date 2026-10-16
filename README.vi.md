# LexiDaily

**Xây dựng thói quen học từ vựng qua từng hành trình mỗi ngày.**

[English](README.md) · [Tiếng Việt](README.vi.md)

LexiDaily là ứng dụng Android học từ vựng ngoại tuyến dành cho người Việt học General English và IELTS. Mỗi ngày, ứng dụng đưa người học qua một hành trình rõ ràng: ôn từ đến hạn, học từ mới, rồi vận dụng qua thử thách.

Được xây dựng bằng **Kotlin · Jetpack Compose · Material 3 · Room**.

> **Trạng thái:** bản MVP Android với 70 từ demo được biên soạn riêng. Hỗ trợ Android 8.0 (API 26) trở lên. Đã kiểm thử trên emulator Android API 36; chưa kiểm thử trên điện thoại thật. Giao diện hiện sử dụng tiếng Việt kết hợp nội dung học tiếng Anh.

## Tính năng

- **Mục tiêu cá nhân:** chọn General English hoặc IELTS, số từ mỗi ngày, trình độ CEFR, band mục tiêu, kỹ năng và chủ đề.
- **Daily Journey:** ôn tập → từ mới → thử thách; mở thử thách từ mức hoàn thành 70% hành trình. Phiên học lưu câu hiện tại, trạng thái lật thẻ, nội dung đang gõ và câu trả lời.
- **Ôn tập ngắt quãng:** đánh giá Again / Hard / Good / Easy với khoảng ôn tiếp theo; bài tập nhận biết, định nghĩa, điền từ và tự gõ. Trouble Words giúp tập trung vào những từ còn yếu.
- **Flashcard đầy đủ thông tin:** IPA, phát âm bằng Text-to-Speech hệ thống, nghĩa Anh–Việt, ví dụ, collocation, họ từ, từ gần nghĩa, đánh dấu lưu và nguồn dữ liệu.
- **Phần thưởng gắn với tiến bộ:** XP, streak, nhiệm vụ ngày và tuần, Grace Day, checkpoint và Mastery Seal. Sổ thưởng ngăn cộng XP trùng; IELTS mastery yêu cầu nhớ chủ động và hiểu ngữ cảnh.
- **Thư viện dễ tra cứu:** lọc theo trạng thái học, CEFR, kỹ năng, chủ đề hoặc bộ từ. Bản đồ chòm sao phản ánh tiến độ đã lưu.
- **Trải nghiệm hằng ngày:** giao diện sáng, tối và theo hệ thống; giảm chuyển động; tùy chọn rung phản hồi và nhắc học.

## Bắt đầu

### Yêu cầu môi trường

| Thành phần | Cấu hình dự án |
| --- | --- |
| Thiết bị | Android 8.0+ / API 26+ |
| Android SDK | Compile SDK 37.0; target SDK 36 |
| Java | Đã kiểm tra với JDK 25 đi kèm Android Studio; Java 11 không build được dự án |
| Gradle | 9.6.0, sử dụng wrapper có sẵn |
| Android Gradle Plugin | 9.4.1 |
| Python | Python 3 cho công cụ nội dung và kiểm tra dữ liệu; đã kiểm tra với 3.14 |

Cài Android SDK tương ứng qua SDK Manager trong Android Studio. Lần build đầu cần kết nối mạng để tải các thành phần phụ thuộc.

### Chạy bằng Android Studio

1. Mở thư mục gốc của dự án trong Android Studio.
2. Chọn JDK đi kèm Android Studio làm Gradle JDK và chờ Gradle sync.
3. Chọn emulator hoặc điện thoại đã kết nối, sau đó chạy cấu hình `app`.

Android Studio tạo tệp `local.properties` chứa cấu hình riêng của máy; tệp này không được đưa vào Git. Nếu build bằng dòng lệnh, cấu hình `ANDROID_HOME` hoặc tạo `local.properties` với đường dẫn SDK của máy đang dùng.

### Build bằng dòng lệnh

Windows / PowerShell:

```powershell
# Điều chỉnh đường dẫn nếu Android Studio được cài ở vị trí khác.
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Lệnh tương đương trên macOS / Linux:

```sh
# Cấu hình JAVA_HOME và đường dẫn Android SDK trước khi chạy.
sh ./gradlew :app:assembleDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`. Khi đã kết nối thiết bị và bật USB debugging, cài bằng lệnh:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cũng có thể chuyển APK sang điện thoại Android rồi mở tệp để cài. Đây là bản ký bằng debug key, phục vụ phát triển và kiểm thử. APK được loại khỏi Git; hiện chưa có đường dẫn tải công khai.

## Cấu trúc dự án

```text
app/
  schemas/                     Schema Room được lưu theo phiên bản
  src/main/
    assets/                    JSON từ vựng đã kiểm tra, đóng gói cùng app
    java/com/lexidaily/app/
      data/                    Entity Room, repository và seed importer
      domain/                  Quy tắc học, lên lịch ôn và tiến trình
      ui/                      Màn hình Compose, component và theme
  src/test/                    Kiểm thử JVM cho bộ quy tắc học
  src/androidTest/             Kiểm thử database, migration và giao diện
data_pipeline/
  scripts/                     Công cụ tạo và kiểm tra dữ liệu demo
  normalized/                  Dữ liệu chuẩn hóa, đồng bộ với app assets
  validation/                  Kiểm thử tính hợp lệ của dữ liệu
scripts/verify.ps1             Điểm chạy kiểm tra tổng hợp trên Windows
```

Giao diện sử dụng ViewModel và repository với Room. Quy tắc học nằm trong lớp domain; WorkManager xử lý nhắc học tùy chọn. Tiến độ học được lưu cục bộ, không có backend ứng dụng hoặc tài khoản đám mây. TTS phụ thuộc giọng đọc đã cài trên thiết bị; nhắc học không bảo đảm chạy đúng phút.

## Dữ liệu từ vựng

Bộ dữ liệu đi kèm có **70 lexeme và 70 sense**, với định nghĩa và ví dụ được biên soạn riêng. Nhãn CEFR và mức liên quan IELTS là ước tính biên tập, không phải chứng nhận chính thức hoặc điểm band IELTS cho từng từ.

Schema hỗ trợ truy xuất nguồn, liên kết chủ đề, bộ từ và nhiều lần xuất hiện theo nguồn. Có sẵn vị trí nguồn NGSL và NAWL nhưng **chưa đóng gói toàn bộ hai danh sách**, đồng thời không khẳng định từ demo thuộc các danh sách này. Bộ demo không chứa bài đọc Cambridge hoặc khai báo lần xuất hiện Cambridge đã xác minh.

Tạo lại và kiểm tra dữ liệu demo:

```sh
python data_pipeline/scripts/build_seed.py
python data_pipeline/scripts/validate_seed.py
python -m unittest discover -s data_pipeline/validation -v
```

Công cụ tạo dữ liệu ghi đè cả `data_pipeline/normalized/` và `app/src/main/assets/` bằng bộ demo. Với bộ dữ liệu khác, chuẩn bị JSON cùng cấu trúc và kiểm tra cả hai bản trước khi build. Giữ ID ổn định, tăng phiên bản manifest nội dung ở cả hai bản, đồng thời duy trì thông tin nguồn và giấy phép. Seed importer cập nhật nội dung trong transaction, giữ tiến độ và lịch sử thưởng; migration Room giữ các bản ghi học hiện có.

## Kiểm thử

Trên Windows, chạy kiểm tra dữ liệu, JVM tests, lint và build các APK debug:

```powershell
.\scripts\verify.ps1
```

Chạy kiểm thử Android riêng trên emulator dành cho kiểm thử:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
```

UI test đặt lại database ứng dụng để chạy lại onboarding. Dùng thiết bị thử nghiệm không có tiến độ học cần giữ.

Các bộ kiểm thử bao phủ quy tắc học, lưu phiên, xử lý phần thưởng, migration database, kiểm tra dữ liệu và luồng giao diện chính. Báo cáo kiểm thử và lint được tạo trong `app/build/reports/` sau khi chạy các lệnh kiểm tra.

## Phạm vi hiện tại

Bản MVP triển khai hành trình học cốt lõi. Bộ lên lịch ôn sử dụng heuristic riêng, chưa phải FSRS đã hiệu chỉnh. Widget, import dữ liệu trong app, sao lưu và khôi phục, chòm sao nâng cao, cùng hỗ trợ viết hoặc nói bằng AI thuộc các giai đoạn tiếp theo.

