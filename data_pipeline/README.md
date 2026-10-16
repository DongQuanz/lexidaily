# IELTS 3000 — English–Vietnamese vocabulary data

Bộ dữ liệu gồm **3.000 mục từ/cụm từ duy nhất**, dành cho người học hướng tới
IELTS **6.5–8.5**. Đây là bản dữ liệu học tập có kiểm tra tự động; **chưa được
chuyên gia ngôn ngữ thẩm định toàn bộ**. Mỗi mục có một từ loại và một nghĩa học
chính. Một mục có thể thuộc nhiều chủ đề. Không quy đổi CEFR thành band của từ.

## Sử dụng

- [Gói ZIP](exports/ielts_3000.zip): JSONL, hướng dẫn ngắn, manifest, báo cáo độ
  phủ và giấy phép, để sử dụng độc lập với mã nguồn ứng dụng.
- [Bản JSONL đầy đủ](exports/ielts_3000.jsonl): mỗi dòng là một mục độc lập,
  UTF-8, phù hợp để nhập vào ứng dụng hoặc hệ thống flashcard.
- [JSON chuẩn hóa](normalized/manifest.json): bộ dữ liệu được ứng dụng Android
  nhập trực tiếp, đồng bộ với `app/src/main/assets/`.
- [Báo cáo độ phủ](normalized/quality_report.json): số mục theo CEFR, chủ đề,
  phiên âm và nội dung bổ sung.
- [Ghi công và giấy phép](licenses/ATTRIBUTION.txt).

Ví dụ cấu trúc của một dòng JSONL:

```json
{
  "id": "mitigate",
  "senseId": "mitigate_verb_01",
  "lemma": "mitigate",
  "partOfSpeech": "verb",
  "cefrLevel": "C1",
  "meaningVi": "giảm nhẹ",
  "definitionEn": "To make a harmful effect less severe.",
  "exampleEn": "Planting trees can mitigate urban heat.",
  "ipaBr": "/ˈmɪtɪɡeɪt/",
  "topics": ["environment"],
  "collocations": ["mitigate risks", "mitigate the impact"],
  "lexicalDataSource": "lexidaily_demo"
}
```

Ví dụ trên rút gọn một số trường nguồn và trạng thái. `exampleVi` để trống nếu
chưa có bản dịch ví dụ. `exampleEn`, phiên âm và collocation thiếu cũng để trống,
không điền câu mẫu hoặc phiên âm suy đoán để làm đẹp số liệu. `spellingVariants`
chứa các biến thể chính tả đã nhận diện; chúng không được tính thành mục mới.

## Độ phủ bản hiện tại

| Thành phần | Số lượng |
| --- | ---: |
| Mục từ/cụm từ, mỗi mục một nghĩa chính | 3.000 |
| Nội dung biên soạn riêng, gồm 70 mục gốc | 514 |
| Mục chọn lọc từ từ điển mở | 2.486 |
| B1, chủ yếu để củng cố từ dùng phổ biến | 667 |
| B2 | 1.333 |
| C1 | 665 |
| C2 | 335 |
| Mục có ví dụ tiếng Anh | 798 |
| Mục có collocation | 514 |
| Tổng collocation | 1.028 |
| Mục có phiên âm IPA | 2.812 |

Số phiên âm chính xác được ghi trong báo cáo độ phủ sau khi build. Không phải
mọi mục đều có IPA. IPA nhập từ thichhoc/CMUdict được lưu ở `ipaAm`, không gán
thành giọng Anh–Anh. Những mục không có IPA vẫn có thể dùng TTS hệ thống.

34 nhãn chủ đề gồm Education, Environment, Technology, Health, Science, Work,
Business, Economics, Society, Government, Crime, Transport, Urbanisation,
Housing, Culture, Media, Advertising, Tourism, Food, Agriculture, Energy,
Climate, Nature, Psychology, Communication, Family, Children, Ageing,
Globalisation, History, Art, Research, Academic language và General English.
Phần Academic language là nhóm học bổ trợ rộng, không phải một danh sách học
thuật chuẩn hóa được chứng nhận. Phân loại chủ đề của phần nhập là heuristic.

## Chọn từ và độ tin cậy

1. Giữ toàn bộ 70 ID lexeme và sense cũ để bảo toàn tiến độ khi nâng nội dung
   từ manifest 1 lên 2. ID không phụ thuộc thứ tự dòng hay chỉ số danh sách.
2. Bổ sung nội dung gốc theo chủ đề IELTS, cách lập luận và mô tả số liệu.
   Nhãn CEFR của phần này được ghi rõ là **ước tính biên tập**, không phải EVP.
3. Với nguồn mở, chỉ lấy mục có cặp nghĩa Anh–Việt được nguồn khai báo song
   song, hoặc có đúng một gloss Anh và một nghĩa Việt. Không ghép nghĩa thứ
   nhất của hai danh sách đa nghĩa độc lập.
4. Loại mục nguồn đánh dấu độ tin cậy `low` hoặc `medium`. Việc nguồn ghi
   `high` vẫn không bảo đảm dịch đúng. Đã kiểm tra mẫu và sửa các lỗi xác định
   được; không tuyên bố đã rà soát thủ công cả 3.000 mục.
5. CEFR-J và Octanove được nối theo **headword và từ loại**, không phải xác
   nhận từng nghĩa. Một số nhãn sẽ khác đánh giá của các từ điển khác.
6. Ví dụ WordNet được lấy khi có độ trùng khớp nghĩa đủ lớn và chứa nguyên dạng
   mục từ. Có locator synset để kiểm tra lại. Ghép nghĩa này vẫn là tự động.
7. Từ hiếm, cách dùng cũ, khẩu ngữ hẹp và biến thể chính tả trùng đã nhận diện
   được giảm ưu tiên hoặc loại. Không nhân số lượng bằng các biến thể chia thì.

Đây là **nguồn tham khảo học tập**, không phải bộ từ chính thức của IELTS, EVP,
NGSL hay NAWL. Không có bài đọc Cambridge, transcript, audio đề thi hay answer
key. Không khẳng định tần suất xuất hiện trong đề IELTS. `frequencySource` là
`Not ranked`; điểm độ khó/ưu tiên chỉ là heuristic cho việc chọn bài học.

`sources/occurrences.json` lưu membership từ điển, tham chiếu CEFR, locator ví
dụ và **gợi ý kỹ năng biên tập**. Các record có `skill` thuộc nguồn `CUSTOM`
LexiDaily chỉ là khuyến nghị học, không phải số lần xuất hiện ở đề thật.
Reading được gợi ý cho toàn bộ; các kỹ năng còn lại dựa trên mức tham chiếu và
độ đầy đủ của ngữ cảnh/phiên âm. Các record từ điển/CEFR không mang kỹ năng thi.

Trong ứng dụng, bài CLOZE chỉ dùng khi ví dụ chứa nguyên dạng mục từ. Khi thiếu
ngữ cảnh, bài chuyển sang tự nhớ từ bằng định nghĩa. Bài này không được tính
như đã luyện ngữ cảnh, do đó chưa đủ điều kiện IELTS Mastered chỉ bằng cách gõ.
Thư viện và danh sách chủ đề hiển thị 50 mục mỗi trang để sử dụng được với 3.000
mục; tìm kiếm và lọc vẫn chạy trên toàn bộ dữ liệu.

## Tạo lại và kiểm tra

Build thông thường hoàn toàn offline, chỉ dùng Python standard library:

```sh
python data_pipeline/scripts/build_seed.py
python data_pipeline/scripts/validate_seed.py
python -m unittest discover -s data_pipeline/validation -v
```

`build_seed.py` tạo cả JSON chuẩn hóa, bản assets, JSONL và gói ZIP. Nội dung phiên bản
này là manifest **2**. Khi thay đổi dữ liệu của một bản đã phát hành, tăng
`VERSION` để các máy đã cài nhập bản mới. Không chạy lại công cụ demo cũ.

Để nghiên cứu hoặc tạo lại bước lựa chọn từ nguồn gốc:

```sh
python data_pipeline/scripts/fetch_sources.py
python data_pipeline/scripts/freeze_selection.py
python data_pipeline/scripts/build_seed.py
```

Hai lệnh đầu chỉ cần khi thay đổi lựa chọn nguồn. Tải nguồn vào `raw/` (không
đưa vào Git). Commit upstream và SHA-256 nằm trong
[`curated/source_lock.json`](curated/source_lock.json). Bản chọn có locator tên
file, số dòng, entry ID, chỉ số nghĩa và loại ghép nghĩa. Công cụ fetch kiểm tra
checksum, không âm thầm chấp nhận một gói nguồn đã thay đổi. Snapshot JSONL và
các đầu ra văn bản dùng xuống dòng LF để checksum ổn định khi chuyển giữa
Windows, macOS và Linux.

Kiểm tra tự động bao gồm số lượng, ID, nghĩa bắt buộc, CEFR/POS, từ mục tiêu
trong ví dụ, liên kết nguồn, membership trùng, thông tin giấy phép, bản xuất,
tính ổn định 70 ID cũ và byte đồng nhất giữa normalized/assets. Đây không phải
bài kiểm tra tính đúng nghĩa hoặc tính tự nhiên của từng mục.

Kết quả xác nhận ngày **16/10/2026**: 12 kiểm thử dữ liệu, 10 kiểm thử JVM và
các bản build APK debug đạt. Kiểm thử Android API 36 bao phủ luồng giao diện,
migration và tám ca repository; ca chỉ ôn tập được xác nhận bằng lượt chạy có
chọn lọc sau khi cập nhật fixture. Thư viện 3.000 mục và flashcard đã được xem
lại qua ảnh kiểm thử. Lint đầy đủ cho revision hiện tại chưa được xác nhận.

## Nguồn và quyền sử dụng

- Từ điển Anh–Việt thichhoc.com, revision
  `4d6e92e8bcf8e3e762410c2b0a9f98fea8e62e5b`,
  [thichhoc-dict](https://github.com/thichhoc-org/thichhoc-dict).
  CC BY-SA 4.0; chứa dữ liệu WordNet, CMUdict, Wiktionary và phần dịch sinh bởi
  mô hình. Không sử dụng bản từ điển thương mại trôi nổi.
- Yukio Tono / Tono Laboratory, Tokyo University of Foreign Studies,
  **CEFR-J Wordlist 1.5**; **Octanove Labs Vocabulary Profile C1/C2 1.0**,
  qua [Open Language Profiles](https://github.com/openlanguageprofiles/olp-en-cefrj),
  revision `d4e45b75b38f27b30dfc5c44d8c571aec7e7092f`.
- Princeton **WordNet 3.0** cho ví dụ, qua
  [nltk_data](https://github.com/nltk/nltk_data/blob/gh-pages/packages/corpora/wordnet.zip).
  Từ điển thichhoc dùng WordNet 3.1; phân biệt hai nguồn này trong ghi công.
- Tiêu chí định hướng chương trình học:
  [IELTS Writing Band Descriptors](https://ielts.org/cdn/ielts-guides/ielts-writing-band-descriptors.pdf).
  Chú trọng phạm vi từ, độ chính xác và dùng phù hợp ngữ cảnh; không suy ra một
  bảng gán band cho từng từ từ tài liệu này.

Bản dữ liệu phái sinh và nội dung từ vựng gốc bổ sung được phát hành theo
**CC BY-SA 4.0**. Giữ ghi công, thông báo đã chỉnh sửa và các notice kèm theo
khi phân phối. Quyền của mã nguồn ứng dụng được xử lý riêng. Các thông báo
nguồn đầy đủ nằm trong `licenses/` và có thể đọc trong app tại
**Library → IELTS Packs → Giấy phép và ghi công**.
