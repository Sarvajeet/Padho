# Development Notes

## Working translation vertical slice

The Android scaffold now includes:
- Compose welcome screen and language selection for Hindi, Gujarati, Marathi, and Tamil
- Android system PDF picker and native `PdfRenderer` page display for scanned/textless pages
- PDF text extraction using PDFBox-Android for text-based PDFs
- On-device translation using Google ML Kit Translation
- Current-page translation with an in-memory translation cache for the current app session
- Original-English text toggle, page navigation, and basic light/dark reader mode

## Important limitations

- ML Kit translation models are downloaded on first use, so the first translation needs internet access and may take time. Subsequent translation runs use the downloaded model on-device.
- Translation currently displays extracted text in a clean reading view; it does not preserve the PDF's exact visual layout.
- Scanned PDFs without selectable text are not OCR'd yet; the original page image is shown with an explanatory note.
- Cache is currently in memory only. Persistent cache, prefetching, saved language preferences, and more robust loading/error states are still planned.
- Translation is limited to the four initial target languages. Validate language model availability and translation quality on representative documents before release.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Allow Gradle to sync, then run the `app` configuration on an Android device or emulator. A Gradle wrapper is not included yet.
