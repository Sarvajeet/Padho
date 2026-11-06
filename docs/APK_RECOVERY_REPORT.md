# Padho APK Recovery Report

## Artifact

- Input: `Padho.apk`
- Size: approximately 44 MiB
- SHA-256: `572521b9169ac24bff73efeb2196a23547d91ccd9304a51f51a20b4cf266473b`
- Package namespace: `com.padho.reader`
- DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`
- Android Gradle Plugin metadata: `8.7.3`

## Confirmed application architecture

DEX class and method metadata confirms these components:

- Application: `PadhoApp`, `MainActivity`, `AppContainer`
- PDF: `PdfTextExtractor`, `PdfPageRenderer`, `Block`, `BlockType`, `PageContent`
- Translation: `TranslationEngine`, `MlKitEngine`, `LanguageManager`, `TargetLang`, `Segmenter`
- Pipeline: `PagePipeline`, `PageState`
- Cache: `TranslationCache`
- Preferences: `SettingsRepository`, `Settings`, `ReaderTheme`
- Library: `LibraryRepository`, `BookEntry`
- UI: onboarding language picker, library, reader, language list and theme

The APK contains PDFBox-Android resources under `com.tom_roush`, Google ML Kit translation classes, Jetpack Compose classes, and native translation libraries for ARM64 and ARM32.

## Translation pipeline reconstructed from DEX references

The following call path is directly supported by method references in the compiled bytecode:

`PdfTextExtractor.extract(page)`
→ `PdfTextExtractor.buildBlocks(lines)`
→ `PagePipeline.produce(page)`
→ `PagePipeline.translateBlock(block)`
→ `Segmenter.chunks(block.original)`
→ one coroutine per returned chunk
→ `TranslationEngine.translate(chunk, targetLanguage)`
→ `MlKitEngine.translate(text, targetLanguage)`
→ translated chunks joined with a single space
→ page result cached.

Important implementation details established from DEX:

1. `PdfTextExtractor.LineCollector` has both `collect` and `writeString` methods. The extractor groups line text before calling `buildBlocks`.
2. `buildBlocks` calculates font-size statistics and compares line Y positions/heights; it calls `appendDehyphenated` and flushes accumulated text into `Block` objects.
3. `Segmenter.chunks` uses Android ICU `BreakIterator.getSentenceInstance(Locale)`. The DEX default-argument method sets the chunk limit to **400 characters**. Therefore the shipped binary does contain sentence-boundary segmentation; it is not simply splitting every word on whitespace.
4. `PagePipeline.translateBlock` calls `Segmenter.chunks`, creates an async task for every returned chunk, awaits all tasks, then joins their outputs with a hard-coded single space. This can lose original whitespace and paragraph formatting.
5. `MlKitEngine.translate` calls ML Kit's `Translator.translate(String)` with the full string supplied by the pipeline. ML Kit itself is not called once per token in this method.
6. The ML Kit source language is English (`en`); target language is taken from `TargetLang`.
7. Cache key metadata includes document fingerprint, language, and engine identifier. A segmentation/prompt version is not evident in the recovered key signature.

## What this means for the reported word-by-word symptom

The binary does not support the simple diagnosis that the translator API is invoked once for each word. It has sentence segmentation and translates each chunk as a string.

The likely failure points to investigate next are:

- PDF extraction may produce very small `Block.original` values, potentially one word or short fragments, before the sentence segmenter ever receives them.
- Sentence segmentation may run independently inside each block. If extraction splits one paragraph into many blocks, sentence context cannot be recovered by the segmenter.
- Chunks are translated concurrently and reassembled with a hard-coded single-space separator. This can damage paragraph/newline structure and discourse flow.
- The offline ML Kit model is a general on-device translation model, not a literary translation system. Even when given full sentences, it may not preserve tone, idioms, and book-level context.

These are evidence-based hypotheses, not yet confirmed runtime observations. The exact runtime values passed into `Segmenter.chunks` and `MlKitEngine.translate` still need instrumentation or a decompiler-level reconstruction.

## Confirmed repository baseline

The `claude-apk-review` branch contains Gradle configuration, manifest, documentation, and app dependency declarations, but the expected original Kotlin implementation files such as `Segmenter.kt` and `PagePipeline.kt` are not present at their expected paths. The branch is therefore not currently a complete buildable reconstruction of the APK.

## Recovery limitations

The current execution environment has Java and ZIP tooling but no JADX, APKTool, Android SDK build tools, or DEX decompiler installed. The three DEX files have been parsed for class definitions, method signatures, method references, and string constants. This is useful structural reverse engineering, but it is not equivalent to a complete Kotlin decompilation.

Do not claim the original project is restored, the translation defect is fixed, or a replacement APK has been built until source reconstruction, Android build, and device tests are complete.

## Recovery code added on claude-apk-review\n\nThe branch now contains a first-pass, isolated Kotlin implementation (not yet wired into the recovered reader):\n\n- `ContextualChunker.kt`: sends a short paragraph as one unit and splits oversized paragraphs only at ICU sentence boundaries. It keeps an over-limit sentence intact rather than splitting it into words.\n- `ContextualTranslationProvider.kt`: defines a provider contract carrying paragraph text, source/target languages, neighboring context, and an optional glossary.\n- `ParagraphTranslationCoordinator.kt`: processes long-paragraph chunks in order and supplies neighboring chunks as context to providers that support it.\n- Unit tests cover whole-paragraph requests, sentence-boundary chunking, exact source offsets, oversized sentences, and context propagation.\n\nThese files are a recovery prototype. They are not connected to the APK-derived `PagePipeline` yet, and no Android build or test execution has been possible in this environment.\n\n## Corrective design

- Reconstruct paragraph-level semantic blocks before translation.
- Combine adjacent body-text blocks when layout metadata indicates the same paragraph; do not merge headings, list items, columns, captions, or footnotes.
- Preserve source paragraph identity and explicit newline separators.
- Prefer one complete paragraph per provider request.
- For oversized paragraphs, split only at sentence boundaries, with a preceding-sentence context overlap and deterministic reassembly.
- Never split by whitespace or translate individual tokens.
- Keep ML Kit as an offline fallback, but evaluate a context-aware provider for literary quality.
- Preserve names, numbers, quotations, emphasis, paragraph boundaries, and terminology.
- Version the segmentation strategy in cache keys so old fragmented translations are not reused.

## Regression benchmark

Create golden passages for English-to-Hindi, Gujarati, Marathi, and Tamil covering idioms, metaphors, pronoun references, long sentences, dialogue, lists, technical terms, and page-break continuation. Evaluate meaning fidelity, fluency, omissions/additions, terminology consistency, and reading order.

## Branch safety

All recovery work belongs on `claude-apk-review`. Keep `main` unchanged until the reconstructed app passes functional and translation-quality verification.
