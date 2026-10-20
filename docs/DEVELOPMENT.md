# Development Notes

## First Android vertical slice

The current scaffold includes a Compose welcome screen, language selection for Hindi/Gujarati/Marathi/Tamil, Android's PDF document picker, native `PdfRenderer` page rendering, previous/next navigation, and a basic light/dark reader toggle.

## Current limitation

Language selection is a UI preference only. The reader currently displays the original PDF page image. Text extraction, translation-provider integration, translated text rendering, persistence, and prefetching are not implemented yet.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK Platform 35 installed. Allow Gradle to sync, then run the `app` configuration on an Android device or emulator. A Gradle wrapper is not included yet.
