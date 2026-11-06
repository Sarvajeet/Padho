# Padho Roadmap

## Phase 0: Foundation
- [x] Android project scaffold
- [x] Kotlin and Jetpack Compose baseline
- [x] PDF picker and document access
- [x] Basic page renderer
- [x] Product and architecture documentation

## Phase 1: Translation vertical slice
- [x] Extract text from a text-based PDF
- [x] Improve basic paragraph segmentation and sentence-aware translation chunking
- [x] Define a replaceable translation provider interface
- [x] Implement one target language end to end
- [x] Render translated content in a readable page view
- [x] Add original/translated toggle
- [x] Add Hindi, Gujarati, Marathi, and Tamil support

## Phase 2: Speed and resilience
- [ ] Memory cache for the active session
- [x] Persistent SQLite translation cache
- [x] Next-page translation prefetch
- [ ] Bounded prefetch queue and cancellation
- [ ] Retry and failure behavior
- [x] Cache size limits and age-based pruning
- [ ] Cache invalidation controls
- [ ] Performance instrumentation

## Phase 3: Reading experience
- [x] Persist preferred language
- [x] Minimal reader controls
- [x] Light and dark themes
- [ ] Typography and spacing controls
- [ ] Smooth page transitions
- [x] Remember last persistable PDF and page position; restore on launch
- [ ] Accessibility review for Indic scripts

## Phase 4: Expansion
- [x] On-device Latin OCR fallback for scanned PDFs (initial implementation)
- [ ] Additional Indian languages
- [ ] Offline translation evaluation
- [ ] EPUB and other document formats
- [ ] Glossary and vocabulary support

## Release readiness
- [ ] Add Gradle wrapper (or document CI-provided Gradle)
- [x] Add initial automated unit tests for translation chunking
- [ ] Expand automated unit tests for PDF extraction, cache pruning, and provider failures
- [ ] Verify clean Android build in CI
- [ ] Test selectable-text and scanned PDFs on physical devices


## Recent platform work
- [x] Accept PDFs opened from compatible Android apps (VIEW intent)
- [x] Accept PDFs shared from compatible Android apps (SEND intent)
- [x] Remember preferred language and theme locally
- [x] Add GitHub Actions debug APK build workflow
