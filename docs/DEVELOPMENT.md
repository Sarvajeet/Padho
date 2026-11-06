# Development Notes

## Working Android vertical slice

The app currently includes:

- Jetpack Compose welcome and reader screens.
- Language selection for Hindi, Gujarati, Marathi, and Tamil.
- Android system PDF picker with persisted read permission.
- Native `PdfRenderer` page rendering through a reusable `PdfDocumentSession`, keeping the active document open while navigating pages.
- Dedicated `PdfTextExtractor` component using PDFBox-Android for selectable-text PDFs.
- PDF text normalization that rejoins wrapped visual lines into paragraphs before translation.
- Sentence-aware translation chunking: groups complete English sentences into context-rich requests (up to 900 characters where possible), preserves paragraph boundaries, and splits oversized sentences at word boundaries.
- Replaceable `TranslationProvider` interface with `MlKitTranslationProvider` as the current on-device implementation.
- Sentence-aware translation chunking shared by foreground translation and prefetch.
- SQLite translation cache with 90-day expiry and bounded retention (500 entries / approximately 32 MB).
- Original-English / translated-text toggle.
- Page navigation and basic light/dark reader mode.
- Persistent SQLite cache for page translations.
- Background prefetch of the next page translation.

## Important limitations

- ML Kit downloads each language model on first use. The first translation therefore needs internet access and may take longer; later translations run on-device.
- Translation is shown in a clean reading surface and does not yet preserve the PDF's exact visual layout.
- Scanned PDFs without selectable text are not OCR'd yet; the original page image is shown.
- Prefetch currently warms only the next page. A bounded queue with cancellation and reprioritization is still needed.
- Text extraction still opens the PDF separately for each page; the renderer session is reused, but a shared extraction/document session remains a future optimization.
- Cache keys include the source page text's SHA-256 fingerprint and translation-engine version. Cache retention is bounded to 500 entries / approximately 32 MB, with entries older than 90 days pruned when new translations are stored.
- Translation chunking improves context versus line/word fragments, but ML Kit remains an on-device baseline rather than a guaranteed literary-quality engine. The provider interface now allows another engine to be added without coupling it to Compose UI. Validate idioms, names, dialogue, poetry, and long-form narrative against native-speaker-reviewed samples in all four languages.
- Sentence boundary detection uses English punctuation heuristics; unusual abbreviations, dialogue formatting, poetry, and PDF extraction errors can still affect grouping.

## Build locally

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Allow Gradle sync, then run the `app` configuration on an Android device or emulator.

Initial JUnit tests cover sentence chunking and paragraph preservation. A Gradle wrapper is not included yet. A GitHub Actions debug build is configured. Recent runs exposed Kotlin compilation errors that have been corrected in the latest commits; the latest run must finish successfully before treating the snapshot as build-verified or release-ready.
