# Padho Product Definition

## Promise

**Read in your language.** Open an English PDF and continue reading in a preferred Indian language without repeatedly invoking translation.

## Primary journey

1. Choose a preferred reading language during setup.
2. Open a PDF from the device.
3. Render the page immediately.
4. Translate the visible page automatically.
5. Prefetch upcoming pages in the background.
6. Reopen documents with cached translations whenever possible.
7. Switch temporarily to the original text when needed.

## Experience principles

- Content is the interface. Reader controls should fade away during reading.
- Translation should never require a button press on every page.
- Avoid blocking the reader while background work continues.
- Keep language choice persistent and easy to change.
- Use subtle transitions, not attention-grabbing loading animations.
- Provide a clear way to compare original and translated text.

## V1 scope

### Reader
- Local PDF picker and document access
- Page rendering and navigation
- Zoom and basic reading controls
- Light and dark themes

### Translation
- English source PDFs
- Hindi, Gujarati, Marathi, and Tamil targets
- Automatic page translation
- Original/translated toggle
- Persistent preferred language
- Clear handling of pages with no extractable text

### Performance
- Current-page-first scheduling
- Next-page prefetch
- In-memory and persistent cache
- Cancellation/reprioritization after page jumps
- Instrumentation for page-ready and translation latency

## Out of scope for the first release

- OCR for scanned-only PDFs
- EPUB and web pages
- Summaries and chat with documents
- Voice reading
- Annotation and cloud sync
- Large language catalogue

## Success measures

- Time to first visible page
- Time to translated current page
- Percentage of page turns where translation is ready
- Translation cache hit rate
- Crash-free reading sessions
- Translation quality feedback by language
- Battery and network usage
