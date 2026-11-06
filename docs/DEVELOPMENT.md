# Development Notes

## Working Android vertical slice

The app currently includes:

- Jetpack Compose welcome and reader screens.
- Language selection for Hindi, Gujarati, Marathi, and Tamil.
- Android system PDF picker with persisted read permission.
- Native `PdfRenderer` page rendering.
- PDFBox-Android text extraction for selectable-text PDFs.
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
- The app currently extracts text by opening the PDF for each page. A reusable document session should be added for large PDFs.
- Cache size limits, expiry, and translation-engine versioning still need implementation.
- Validate ML Kit language model availability and translation quality with representative PDFs before release.

## Build locally

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Allow Gradle sync, then run the `app` configuration on an Android device or emulator.

A Gradle wrapper is not included yet. The project has not been verified by a clean CI build, so run a local Gradle sync/build before treating this snapshot as release-ready.
