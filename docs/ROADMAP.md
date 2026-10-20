# Padho Roadmap

## Phase 0: Foundation
- [ ] Android project scaffold
- [ ] Kotlin and Jetpack Compose baseline
- [ ] PDF picker and document access
- [ ] Basic page renderer
- [ ] Product and architecture documentation

## Phase 1: Translation vertical slice
- [ ] Extract text from a text-based PDF
- [ ] Build text segmentation and ordering
- [ ] Define translation provider interface
- [ ] Implement one target language end to end
- [ ] Render translated content in a readable page view
- [ ] Add original/translated toggle
- [ ] Add Hindi, Gujarati, Marathi, and Tamil support

## Phase 2: Speed and resilience
- [ ] Memory cache
- [ ] Persistent cache
- [ ] Current-page-first scheduling
- [ ] Next-page prefetch
- [ ] Cancellation and reprioritization
- [ ] Retry and failure behavior
- [ ] Performance instrumentation

## Phase 3: Reading experience
- [ ] Persist preferred language
- [ ] Minimal reader controls
- [ ] Light and dark themes
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
