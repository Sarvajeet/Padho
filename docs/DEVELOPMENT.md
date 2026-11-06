# Development Notes

## Working Android vertical slice

The app currently includes:

- Jetpack Compose welcome and reader screens.
- Language selection for Hindi, Gujarati, Marathi, and Tamil.
- Android system PDF picker with persisted read permission.
- Native `PdfRenderer` page rendering through a reusable `PdfDocumentSession`, keeping the active document open while navigating pages.
- Dedicated `PdfTextExtractor` component using PDFBox-Android for selectable-text PDFs.
- Basic text normalization for line endings and excessive blank lines, kept separate from the Compose UI.
- Google ML Kit on-device translation.
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
- Cache keys now include the source page text's SHA-256 fingerprint and a translation-engine version, preventing reuse when extracted page content changes. Cache size limits and expiry still need implementation.
- Validate ML Kit language model availability and translation quality with representative PDFs before release.

## Build locally

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Allow Gradle sync, then run the `app` configuration on an Android device or emulator.

A Gradle wrapper is not included yet. A GitHub Actions debug build is configured. Recent runs exposed Kotlin compilation errors that have been corrected in the latest commits; the latest run must finish successfully before treating the snapshot as build-verified or release-ready.
