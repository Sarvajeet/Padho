# Padho APK Recovery Report

## Artifact verified

- Input: `Padho.apk`
- Size: approximately 44 MiB
- SHA-256: `572521b9169ac24bff73efeb2196a23547d91ccd9304a51f51a20b4cf266473b`
- Android package namespace observed in DEX: `com.padho.reader`
- DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`
- Android Gradle Plugin metadata: `8.7.3`

## Confirmed application architecture from compiled symbols

The APK contains recoverable class and method metadata for these app modules:

- Application: `PadhoApp`, `MainActivity`, `AppContainer`
- PDF: `PdfTextExtractor`, `PdfPageRenderer`, `Block`, `BlockType`, `PageContent`
- Translation: `TranslationEngine`, `MlKitEngine`, `LanguageManager`, `TargetLang`, `Segmenter`
- Pipeline: `PagePipeline`, `PageState`
- Cache: `TranslationCache`
- Preferences: `SettingsRepository`, `Settings`, `ReaderTheme`
- Library: `LibraryRepository`, `BookEntry`
- UI: onboarding language picker, library screen, reader screen, common language list, theme

The APK includes PDFBox-Android resources under `com.tom_roush`, Google ML Kit translation classes, and native translation libraries for ARM64 and ARM32. Jetpack Compose symbols are present.

## Translation path to investigate

Compiled symbols establish the following likely path:

`PdfTextExtractor -> PageContent/Block -> PagePipeline.translateBlock -> TranslationEngine/MlKitEngine -> TranslationCache -> ReaderScreen`

The presence of a dedicated `Segmenter` class is important. The reported word-by-word behavior may originate in PDF text extraction producing fragmented blocks, segmentation splitting too aggressively, or the page pipeline invoking the engine once per fragment. The APK symbol scan alone cannot determine which exact method is responsible; method-level decompilation and runtime tracing are still required.

## Recovery limitations

An APK does not contain the original Gradle project, source-control history, comments, or necessarily original source names. DEX retains class names and method metadata, but a full source reconstruction requires a DEX decompiler. This working environment currently has no JADX/APKTool binary and cannot reach external download hosts, so a complete Kotlin decompilation and reproducible Android build have not yet been completed.

Do not describe the original project as restored, do not claim the translation defect is fixed, and do not publish a replacement APK until a reconstructed project builds and passes device tests.

## Next recovery steps

1. Obtain a DEX decompiler in the build environment and decompile all three DEX files.
2. Inspect `Segmenter`, `PdfTextExtractor`, `PagePipeline`, `MlKitEngine`, and `TranslationCache` first.
3. Determine whether PDF text spans are grouped into lines and paragraphs before segmentation.
4. Trace the exact input passed to ML Kit. Confirm whether calls receive words, sentence fragments, sentences, or paragraphs.
5. Reconstruct the Android project and dependencies, then preserve the recovered UI and navigation.
6. Replace token-level translation with semantic paragraph chunks. For oversized paragraphs, split at sentence boundaries with context overlap.
7. Add golden regression tests for English to Hindi, Gujarati, Marathi, and Tamil.
8. Build, install, and test on Android before creating a release APK.

## Branch safety

All recovery work belongs on `claude-apk-review`. Keep `main` unchanged until the reconstructed app has passed functional and translation-quality verification.
