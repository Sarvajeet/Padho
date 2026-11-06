# Padho Roadmap

## Phase 0: Foundation
- [x] Android project scaffold
- [x] Kotlin and Jetpack Compose baseline
- [x] PDF picker and document access
- [x] Basic page renderer
- [x] Product and architecture documentation

## Phase 1: Translation vertical slice
- [x] Extract text from a text-based PDF
- [ ] Improve text segmentation and reading order
- [ ] Define a replaceable translation provider interface
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
- [ ] Cache size limits and invalidation
- [ ] Performance instrumentation

## Phase 3: Reading experience
- [ ] Persist preferred language
- [x] Minimal reader controls
- [x] Light and dark themes
- [ ] Typography and spacing controls
- [ ] Smooth page transitions
- [ ] Reading progress and reopen support
- [ ] Accessibility review for Indic scripts

## Phase 4: Expansion
- [ ] OCR for scanned PDFs
- [ ] Additional Indian languages
- [ ] Offline translation evaluation
- [ ] EPUB and other document formats
- [ ] Glossary and vocabulary support

## Release readiness
- [ ] Add Gradle wrapper
- [ ] Add automated unit tests
- [ ] Verify clean Android build in CI
- [ ] Test with real PDFs on physical devices
