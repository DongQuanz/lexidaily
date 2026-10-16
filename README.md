# LexiDaily

[Tiếng Việt](README.vi.md) · [Dataset documentation](data_pipeline/README.md)

An offline Android vocabulary app for Vietnamese learners of General English and IELTS. LexiDaily combines English–Vietnamese vocabulary, spaced review and a daily learning journey in a native Kotlin and Jetpack Compose interface.

The current MVP bundles **3,000 vocabulary entries** for learners targeting **IELTS 6.5–8.5**. Learning progress stays on the device; an application account or backend is not required.

## The learning experience

1. **Review** words that are due, with additional practice for difficult vocabulary.
2. **Learn** new words through flashcards, meanings, pronunciation and available usage examples.
3. **Apply** what you learned in a daily challenge, unlocked after completing 70% of the new-word plan. Review-only days are also supported.

Sessions retain the current question, revealed card, typed draft and submitted answers so learners can resume interrupted practice.

### Features

- **Personal study goals:** General English or IELTS, daily word targets, CEFR references, target band, preferred skills and topics.
- **Spaced review:** Again / Hard / Good / Easy ratings, recognition, definition, cloze and typed-recall exercises, plus a Trouble Words collection.
- **Vocabulary library:** search English or Vietnamese, filter by learning state, CEFR, skill, topic or pack, and bookmark entries. Large lists use 50-entry pages.
- **Usage and pronunciation:** British or American IPA where available, system Text-to-Speech, examples, collocations, selected word families and near synonyms.
- **Saved progress:** XP, streaks, daily and weekly quests, Grace Days, checkpoints and mastery seals. IELTS mastery requires contextual understanding and productive recall.
- **Appearance and reminders:** light, dark and system themes, reduced motion, optional haptics and opt-in reminders.

## Included vocabulary

| Content | Coverage |
| --- | ---: |
| Unique vocabulary entries / primary senses | 3,000 / 3,000 |
| Topic labels | 34 |
| Entries with IPA | 2,812 |
| Entries with English examples | 798 |
| Entries with collocations | 514 |
| Collocation records | 1,028 |
| Original editorial / selected open-dictionary entries | 514 / 2,486 |

Every entry includes English and Vietnamese meanings, a part of speech, a CEFR reference and provenance. CEFR coverage is **667 B1**, **1,333 B2**, **665 C1** and **335 C2** entries. B1 content supports common vocabulary alongside more advanced study.

The IELTS range describes the intended learner, **not an official band assigned to each word**. CEFR-J and Octanove references annotate headwords and parts of speech; original entries use clearly identified editorial estimates. Topic and skill recommendations are heuristic.

Examples, IPA and other enrichment do not yet cover every entry. Imported translations include model-generated material and have not all received expert review. When a suitable example is unavailable, the app uses definition-based recall instead of an empty cloze exercise.

- [Download the dataset bundle](data_pipeline/exports/ielts_3000.zip) — JSONL, a short guide, coverage report and license notices.
- [Read the JSONL export](data_pipeline/exports/ielts_3000.jsonl).
- [Inspect the coverage report](data_pipeline/normalized/quality_report.json).
- [Read the methodology, sources and limitations](data_pipeline/README.md).

Vocabulary is bundled inside the APK and imported automatically into Room. Content manifest **version 2** preserves the original 70 lexeme and sense IDs so existing learning progress can survive content updates.

## Run the app

### Requirements

| Component | Configuration |
| --- | --- |
| Android device or emulator | Android 8.0+ / API 26+ |
| Android SDK | Compile SDK 37.0; target SDK 36 |
| Java | Android Studio's bundled JDK; validated with JDK 25 |
| Build tools | Gradle 9.6.0 wrapper; Android Gradle Plugin 9.4.1 |
| Content tools | Python 3; validated with Python 3.14; standard library only |

Install the matching SDK through Android Studio. The first build requires internet access to resolve build dependencies.

### Android Studio

1. Clone this repository and open its root folder in Android Studio.
2. Select the bundled JDK as the Gradle JDK, configure the Android SDK and complete Gradle sync.
3. Choose an emulator or connected Android device and run the `app` configuration.
4. In the app, open **Library → IELTS Packs → IELTS 3000 · 6.5–8.5** to browse the full study pack.

### Command line

On Windows, configure `JAVA_HOME` for Android Studio's JDK, then run:

```powershell
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux, configure `JAVA_HOME` and run:

```sh
sh ./gradlew :app:assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. With USB debugging enabled, install or update it using:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

This produces a debug-signed development build. APKs and signing material are excluded from Git. SDK paths belong in the local, ignored `local.properties` file or the developer's environment.

## Maintain the dataset

The checked-in inputs support an offline rebuild with Python's standard library:

```sh
python data_pipeline/scripts/build_seed.py
python data_pipeline/scripts/validate_seed.py
python -m unittest discover -s data_pipeline/validation -v
```

The builder regenerates normalized JSON, bundled app assets, the JSONL export and the ZIP bundle. Validation checks identities, required meanings, levels, references, example targets, licenses, exports and agreement between normalized content and app assets.

Keep stable IDs when editing existing entries. Increment the content manifest version when releasing a content change. Source selection, upstream revisions, checksums and redistribution terms are documented in the [dataset guide](data_pipeline/README.md).

## Architecture

```text
app/
  schemas/                       Versioned Room schemas
  src/main/assets/               Bundled vocabulary, packs and source notices
  src/main/java/com/lexidaily/app/
    data/                        Room entities, repository and content importer
    domain/                      Review, selection and progression rules
    ui/                          Compose screens, components and themes
  src/test/                      JVM learning-engine tests
  src/androidTest/               Repository, migration and UI tests
data_pipeline/
  curated/                       Versioned editorial inputs and source selection
  licenses/                      Attribution and upstream license notices
  normalized/                    Content mirrored into app assets
  exports/                       Portable JSONL and ZIP dataset
  scripts/                       Fetch, select, build and validate tools
  validation/                    Dataset tests
scripts/verify.ps1               Windows verification entry point
```

The application uses **Kotlin, Jetpack Compose, Material 3, Room and WorkManager**. A ViewModel and repository connect the UI to local storage; learning rules live in the domain layer. Optional reminders depend on Android scheduling, while speech output depends on installed TTS services and voices.

## Verification

Run the data checks, JVM tests, lint and debug app/test builds on Windows:

```powershell
.\scripts\verify.ps1
```

Run instrumentation on a dedicated emulator or test device:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
```

The UI suite resets the application database to exercise onboarding. Use an isolated test device without learning data that needs to be retained. Reports are generated under `app/build/reports/`; instrumentation results are also written under `app/build/outputs/androidTest-results/` when run through Gradle.

The current implementation has been checked with **12 dataset tests**, **10 JVM tests**, debug APK builds and Android API 36 instrumentation covering the UI, migrations and eight repository cases. The review-only repository case was verified through a targeted rerun after updating its dataset fixture. A complete lint run for this revision is still outstanding. Physical-device behavior has not yet been verified.

## Scope and next steps

LexiDaily is an MVP with a working offline learning journey. Its review scheduler is a custom heuristic, not a calibrated FSRS implementation. Areas for further development include vocabulary coverage and editorial quality, physical-device validation and complete lint verification.

Widgets, in-app dataset import, backup and restore, advanced constellation interactions, and AI-assisted writing or speaking are not currently implemented.

## Data licensing and attribution

The adapted vocabulary dataset and original lexical additions are distributed under **[CC BY-SA 4.0](data_pipeline/licenses/CC-BY-SA-4.0.txt)**. Upstream sources include thichhoc-dict, CEFR-J, Octanove, Princeton WordNet, CMUdict and Wiktionary; their applicable notices are retained.

Read the [full attribution](data_pipeline/licenses/ATTRIBUTION.txt) and [source documentation](data_pipeline/README.md) before redistributing data. These data terms do not establish a separate license for the application code. Attribution is also available in the app through **Library → IELTS Packs → Giấy phép và ghi công**.

No Cambridge test passages, transcripts, answer keys or verified IELTS test-occurrence claims are bundled. NGSL and NAWL are unbundled source slots, not included wordlists.
