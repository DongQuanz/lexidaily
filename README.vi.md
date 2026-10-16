# LexiDaily

[English](README.md) · [Tài liệu bộ dữ liệu](data_pipeline/README.md)

Ứng dụng Android học từ vựng tiếng Anh ngoại tuyến dành cho người Việt học General English và IELTS. LexiDaily kết hợp nội dung Anh–Việt, ôn tập ngắt quãng và hành trình học hằng ngày trong giao diện Kotlin và Jetpack Compose.

Bản MVP hiện đóng gói **3.000 mục từ/cụm từ**, hướng tới người học IELTS **6.5–8.5**. Tiến độ được lưu trên thiết bị; ứng dụng hoạt động mà không cần tài khoản hay backend.

## Trải nghiệm học

1. **Ôn tập** những từ đến hạn và củng cố từ còn khó nhớ.
2. **Học từ mới** qua flashcard, nghĩa, phát âm và ví dụ sử dụng khi có dữ liệu.
3. **Vận dụng** trong thử thách cuối ngày, mở sau khi hoàn thành 70% kế hoạch từ mới. Ứng dụng cũng hỗ trợ ngày chỉ có từ cần ôn.

Phiên học lưu câu hiện tại, trạng thái lật thẻ, nội dung đang nhập và câu trả lời đã gửi để người học tiếp tục khi bị gián đoạn.

### Tính năng

- **Mục tiêu cá nhân:** General English hoặc IELTS, số từ mỗi ngày, tham chiếu CEFR, band mục tiêu, kỹ năng và chủ đề ưu tiên.
- **Ôn tập ngắt quãng:** đánh giá Again / Hard / Good / Easy; bài tập nhận biết, định nghĩa, điền từ và tự nhớ từ; bộ Trouble Words tập trung vào từ còn yếu.
- **Thư viện tra cứu:** tìm bằng tiếng Anh hoặc tiếng Việt, lọc theo trạng thái học, CEFR, kỹ năng, chủ đề hoặc bộ từ; đánh dấu mục cần lưu. Danh sách lớn được phân trang 50 mục.
- **Ngữ cảnh và phát âm:** IPA Anh–Anh hoặc Anh–Mỹ khi có dữ liệu, Text-to-Speech hệ thống, ví dụ, collocation, một số liên kết họ từ và từ gần nghĩa.
- **Tiến độ học:** XP, streak, nhiệm vụ ngày và tuần, Grace Day, checkpoint và Mastery Seal. IELTS mastery yêu cầu hiểu ngữ cảnh và nhớ chủ động.
- **Tùy chỉnh trải nghiệm:** giao diện sáng, tối hoặc theo hệ thống; giảm chuyển động; tùy chọn rung phản hồi và nhắc học.

## Nội dung từ vựng

| Thành phần | Độ phủ |
| --- | ---: |
| Mục từ/cụm từ duy nhất / nghĩa học chính | 3.000 / 3.000 |
| Nhãn chủ đề | 34 |
| Mục có IPA | 2.812 |
| Mục có ví dụ tiếng Anh | 798 |
| Mục có collocation | 514 |
| Bản ghi collocation | 1.028 |
| Mục biên soạn riêng / mục chọn lọc từ nguồn mở | 514 / 2.486 |

Mỗi mục có nghĩa Anh–Việt, từ loại, tham chiếu CEFR và thông tin nguồn. Phân bố CEFR gồm **667 mục B1**, **1.333 mục B2**, **665 mục C1** và **335 mục C2**. Phần B1 củng cố các từ dùng phổ biến bên cạnh nội dung nâng cao.

Khoảng IELTS thể hiện đối tượng người học, **không phải band chính thức gán cho từng từ**. CEFR-J và Octanove tham chiếu theo headword và từ loại; nội dung gốc dùng ước tính biên tập có ghi rõ nguồn. Phân loại chủ đề và gợi ý kỹ năng sử dụng heuristic.

Ví dụ, IPA và các nội dung bổ sung chưa phủ toàn bộ danh sách. Phần nhập từ nguồn mở có bản dịch sinh bởi mô hình và chưa được chuyên gia thẩm định toàn bộ. Khi chưa có ví dụ phù hợp, ứng dụng chuyển bài điền từ sang tự nhớ từ qua định nghĩa.

- [Tải gói dữ liệu ZIP](data_pipeline/exports/ielts_3000.zip) — JSONL, hướng dẫn ngắn, báo cáo độ phủ và giấy phép.
- [Xem bản xuất JSONL](data_pipeline/exports/ielts_3000.jsonl).
- [Xem báo cáo độ phủ](data_pipeline/normalized/quality_report.json).
- [Đọc phương pháp, nguồn và giới hạn](data_pipeline/README.md).

Dữ liệu được đóng gói trong APK và tự động nhập vào Room. Manifest nội dung **phiên bản 2** giữ nguyên 70 ID lexeme và sense gốc để bảo toàn tiến độ khi cập nhật nội dung.

## Chạy ứng dụng

### Yêu cầu môi trường

| Thành phần | Cấu hình |
| --- | --- |
| Thiết bị hoặc emulator | Android 8.0+ / API 26+ |
| Android SDK | Compile SDK 37.0; target SDK 36 |
| Java | JDK đi kèm Android Studio; đã xác nhận với JDK 25 |
| Công cụ build | Gradle wrapper 9.6.0; Android Gradle Plugin 9.4.1 |
| Công cụ dữ liệu | Python 3; đã xác nhận với Python 3.14; chỉ dùng thư viện chuẩn |

Cài SDK tương ứng qua Android Studio. Lần build đầu cần kết nối mạng để tải các thành phần phụ thuộc.

### Android Studio

1. Clone repository và mở thư mục gốc bằng Android Studio.
2. Chọn JDK đi kèm làm Gradle JDK, cấu hình Android SDK và hoàn tất Gradle sync.
3. Chọn emulator hoặc thiết bị Android đã kết nối, sau đó chạy cấu hình `app`.
4. Trong ứng dụng, mở **Library → IELTS Packs → IELTS 3000 · 6.5–8.5** để xem bộ từ đầy đủ.

### Dòng lệnh

Trên Windows, cấu hình `JAVA_HOME` trỏ tới JDK của Android Studio, sau đó chạy:

```powershell
.\gradlew.bat :app:assembleDebug
```

Trên macOS hoặc Linux, cấu hình `JAVA_HOME` rồi chạy:

```sh
sh ./gradlew :app:assembleDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`. Khi đã bật USB debugging, dùng lệnh sau để cài hoặc cập nhật:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Đây là bản phát triển ký bằng debug key. APK và thông tin ký ứng dụng được loại khỏi Git. Đường dẫn SDK thuộc cấu hình riêng của máy, được đặt trong `local.properties` đã ignore hoặc trong biến môi trường.

## Duy trì bộ dữ liệu

Các đầu vào được lưu trong repository cho phép tạo lại dữ liệu offline bằng thư viện chuẩn Python:

```sh
python data_pipeline/scripts/build_seed.py
python data_pipeline/scripts/validate_seed.py
python -m unittest discover -s data_pipeline/validation -v
```

Công cụ tạo lại JSON chuẩn hóa, app assets, bản xuất JSONL và gói ZIP. Bộ kiểm tra xác nhận ID, nghĩa bắt buộc, trình độ, tham chiếu, từ mục tiêu trong ví dụ, giấy phép, bản xuất và tính đồng nhất giữa dữ liệu chuẩn hóa với app assets.

Giữ ID ổn định khi sửa mục từ hiện có. Tăng phiên bản manifest nội dung khi phát hành thay đổi dữ liệu. Quy trình chọn nguồn, revision upstream, checksum và điều kiện phân phối nằm trong [tài liệu bộ dữ liệu](data_pipeline/README.md).

## Kiến trúc

```text
app/
  schemas/                       Schema Room có quản lý phiên bản
  src/main/assets/               Từ vựng, bộ từ và thông báo nguồn đi kèm
  src/main/java/com/lexidaily/app/
    data/                        Entity Room, repository và công cụ nhập nội dung
    domain/                      Quy tắc ôn tập, chọn từ và tiến trình
    ui/                          Màn hình, component và theme Compose
  src/test/                      Kiểm thử JVM cho bộ quy tắc học
  src/androidTest/               Kiểm thử repository, migration và giao diện
data_pipeline/
  curated/                       Đầu vào biên soạn và lựa chọn nguồn có quản lý phiên bản
  licenses/                      Ghi công và thông báo giấy phép nguồn
  normalized/                    Dữ liệu đồng bộ với app assets
  exports/                       Bản dữ liệu JSONL và ZIP
  scripts/                       Công cụ tải, chọn, tạo và kiểm tra dữ liệu
  validation/                    Kiểm thử dữ liệu
scripts/verify.ps1               Điểm chạy kiểm tra trên Windows
```

Ứng dụng sử dụng **Kotlin, Jetpack Compose, Material 3, Room và WorkManager**. ViewModel và repository kết nối giao diện với dữ liệu cục bộ; quy tắc học nằm trong lớp domain. Nhắc học tùy chọn phụ thuộc cơ chế lập lịch Android; phát âm phụ thuộc dịch vụ và giọng TTS đã cài trên thiết bị.

## Kiểm thử

Trên Windows, chạy kiểm tra dữ liệu, JVM tests, lint và build APK ứng dụng/kiểm thử:

```powershell
.\scripts\verify.ps1
```

Chạy instrumentation trên emulator hoặc thiết bị dành riêng cho kiểm thử:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
```

Bộ kiểm thử giao diện đặt lại database để chạy onboarding. Dùng thiết bị thử nghiệm không chứa tiến độ học cần giữ. Báo cáo được tạo tại `app/build/reports/`; kết quả instrumentation cũng được lưu trong `app/build/outputs/androidTest-results/` khi chạy qua Gradle.

Bản hiện tại đã được xác nhận với **12 kiểm thử dữ liệu**, **10 kiểm thử JVM**, các bản build APK debug và instrumentation trên Android API 36 bao phủ giao diện, migration cùng tám ca repository. Ca repository chỉ ôn tập được xác nhận bằng lượt chạy có chọn lọc sau khi cập nhật fixture dữ liệu. Lượt lint đầy đủ cho revision này chưa hoàn tất. Hành vi trên điện thoại thật chưa được xác nhận.

## Phạm vi và hướng phát triển

LexiDaily hiện là MVP có hành trình học ngoại tuyến hoạt động hoàn chỉnh. Bộ lên lịch ôn dùng heuristic riêng, chưa phải FSRS đã hiệu chỉnh. Các hạng mục cần hoàn thiện gồm mở rộng và thẩm định nội dung, kiểm thử trên điện thoại thật và hoàn tất kiểm tra lint.

Widget, nhập bộ dữ liệu trong app, sao lưu và khôi phục, tương tác chòm sao nâng cao, cùng hỗ trợ viết hoặc nói bằng AI hiện chưa được triển khai.

## Giấy phép dữ liệu và ghi công

Bộ dữ liệu từ vựng phái sinh và các mục từ biên soạn bổ sung được phân phối theo **[CC BY-SA 4.0](data_pipeline/licenses/CC-BY-SA-4.0.txt)**. Các nguồn gồm thichhoc-dict, CEFR-J, Octanove, Princeton WordNet, CMUdict và Wiktionary; thông báo giấy phép tương ứng được giữ lại.

Đọc [phần ghi công đầy đủ](data_pipeline/licenses/ATTRIBUTION.txt) và [tài liệu nguồn](data_pipeline/README.md) trước khi phân phối lại dữ liệu. Điều khoản dữ liệu không thiết lập giấy phép riêng cho mã nguồn ứng dụng. Phần ghi công cũng có sẵn trong app tại **Library → IELTS Packs → Giấy phép và ghi công**.

Bộ dữ liệu không chứa bài đọc Cambridge, transcript, answer key hay tuyên bố lần xuất hiện đã xác minh trong đề IELTS. NGSL và NAWL là vị trí nguồn chưa được đóng gói thành danh sách từ.
