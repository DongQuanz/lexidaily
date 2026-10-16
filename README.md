# LexiDaily

**Build a daily vocabulary habit, one learning journey at a time.**

[English](README.md) · [Tiếng Việt](README.vi.md)

LexiDaily is an offline Android vocabulary app for Vietnamese learners of General English and IELTS. Each day brings a focused journey: review due words, learn new vocabulary, then put it into practice with a challenge.

Built with **Kotlin · Jetpack Compose · Material 3 · Room**.

> **Status:** Android MVP with 70 original demo words. Supports Android 8.0 (API 26) and later. Validated on an Android API 36 emulator; physical-device testing is still pending. The app currently uses a Vietnamese interface with English learning content.

## Features

- **Personal learning goals:** General English or IELTS, daily word targets, CEFR level, target band, skills and topics.
- **Daily Journey:** review → new words → challenge, with challenge access from 70% journey progress. Sessions preserve the current question, revealed card, typed draft and answers.
- **Spaced repetition:** Again / Hard / Good / Easy ratings with upcoming intervals, plus recognition, definition, cloze and typing exercises. Trouble Words brings difficult vocabulary back into focus.
- **Rich vocabulary cards:** IPA, system Text-to-Speech, English and Vietnamese meanings, examples, collocations, word families, near synonyms, bookmarks and source details.
- **Progress with purpose:** XP, streaks, daily and weekly quests, Grace Days, checkpoints and mastery seals. Reward records prevent duplicate XP; IELTS mastery requires productive recall and context understanding.
- **Searchable library:** filter by learning state, CEFR, skill, topic or pack. A constellation view reflects saved learning progress.
- **Everyday comfort:** light, dark and system themes; reduced motion; optional haptics; opt-in learning reminders.

## Get started

### Requirements

| Component | Project configuration |
| --- | --- |
| Device | Android 8.0+ / API 26+ |
| Android SDK | Compile SDK 37.0; target SDK 36 |
| Java | Tested with Android Studio's bundled JDK 25; Java 11 cannot build this project |
| Gradle | 9.6.0, included through the wrapper |
| Android Gradle Plugin | 9.4.1 |
| Python | Python 3 for content tools and validation; tested with 3.14 |

Install the matching Android SDK through Android Studio's SDK Manager. The first build needs internet access to download build dependencies.

### Run in Android Studio

1. Open the project root in Android Studio.
2. Select the bundled JDK as the Gradle JDK and let Gradle sync.
3. Select an Android emulator or connected phone and run the `app` configuration.

Android Studio creates the machine-specific `local.properties` file. It is excluded from version control. For command-line builds, configure `ANDROID_HOME` or create `local.properties` with your own SDK path.

### Build from the command line

Windows / PowerShell:

```powershell
# Adjust this path if Android Studio is installed elsewhere.
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

macOS / Linux shell equivalent:

```sh
# Set JAVA_HOME and your Android SDK location first.
sh ./gradlew :app:assembleDebug
```

The build produces `app/build/outputs/apk/debug/app-debug.apk`. With a device connected and USB debugging enabled, install it with:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can also transfer the APK to an Android phone and open it to install. This is a debug-signed build for development and testing. APKs are excluded from Git; a public download link has not been published.

## Project structure

```text
app/
  schemas/                     Versioned Room database schemas
  src/main/
    assets/                    Bundled, validated vocabulary JSON
    java/com/lexidaily/app/
      data/                    Room entities, repository and seed importer
      domain/                  Learning, scheduling and progression rules
      ui/                      Compose screens, components and theme
  src/test/                    JVM learning-engine tests
  src/androidTest/             Database, migration and UI tests
data_pipeline/
  scripts/                     Demo content builder and validator
  normalized/                  Normalized content mirrored into app assets
  validation/                  Content validation tests
scripts/verify.ps1             Local verification entry point for Windows
```

The UI is driven by a ViewModel and repository backed by Room. Learning rules live in the domain layer; WorkManager handles optional reminders. Learning data is stored locally, with no application backend or cloud account. System TTS depends on the voices installed on the device, and reminder timing is approximate.

## Vocabulary data

The bundled dataset contains **70 lexemes and 70 senses**, with original definitions and examples. CEFR levels and IELTS relevance are editorial estimates, not official certifications or per-word IELTS band scores.

The schema supports source provenance, topic links, vocabulary packs and multiple source occurrences. NGSL and NAWL source slots are present, but the full wordlists are **not bundled** and no membership in those lists is asserted for the demo words. The demo does not include Cambridge test passages or claim verified Cambridge occurrences.

Rebuild and validate the demo dataset:

```sh
python data_pipeline/scripts/build_seed.py
python data_pipeline/scripts/validate_seed.py
python -m unittest discover -s data_pipeline/validation -v
```

The builder overwrites both `data_pipeline/normalized/` and `app/src/main/assets/` with the demo dataset. For a different dataset, prepare JSON in the same structure and validate both copies before building. Keep stable IDs, update the content manifest version in both copies, and retain source and license metadata. The seed importer updates content transactionally while preserving user progress and reward history; Room migrations preserve existing learning records.

## Verification

On Windows, run the content checks, JVM tests, lint and debug APK builds together:

```powershell
.\scripts\verify.ps1
```

Run Android instrumentation separately on a dedicated emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
```

The UI test resets the app database to repeat onboarding. Use a test device without learning data you need to keep.

The test suites cover learning rules, session persistence, reward handling, database migrations, content validation and the main UI flow. Generated test and lint reports are available under `app/build/reports/` after running the checks.

## Current scope

This MVP implements the core learning journey. The scheduler uses a custom heuristic; it is not a calibrated FSRS implementation. Widgets, in-app dataset import, backup and restore, advanced constellation views, and AI-assisted writing or speaking remain future work.

