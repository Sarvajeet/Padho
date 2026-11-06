# Padho Architecture

## Goals

- Minimize perceived translation latency.
- Keep PDF rendering independent from translation providers.
- Preserve the original document.
- Cache results safely and reuse them.
- Support Indian scripts and variable translated-text lengths.
- Keep the reader usable while translation is pending or unavailable.

## Proposed modules

### PDF
Responsible for document access, page rendering, page dimensions, text extraction, and block/position metadata where available. The current implementation separates `PdfTextExtractor` from the UI and keeps a reusable `PdfDocumentSession` for native page rendering. `PdfPageTextExtractor` first attempts embedded/selectable text, then invokes `OcrTextExtractor` with ML Kit Latin text recognition only when the page contains fewer than 20 letters or digits. OCR renders a page bitmap at 1600 px wide and recycles it after recognition. The OCR fallback is designed for English source PDFs; complex layouts and image quality still require device testing.

### Translation
Responsible for language identifiers, segmentation, provider abstraction, request batching, retries, cancellation, and normalized results.

### Reader
Owns current page, selected language, original/translated display mode, zoom, theme, and reading progress.

### Cache
Use a memory cache for active reading and a persistent local cache for previously translated content. Cache keys should account for document identity, page, source language, target language, and translation pipeline version.

Conceptual key:

```text
documentFingerprint | pageIndex | sourceLanguage | targetLanguage | pipelineVersion
```

Avoid using only a file name as document identity. Prefer a stable fingerprint derived from document content/metadata.

### Prefetch scheduler
Suggested priority:
1. Visible page
2. Next page
3. Previous page
4. Additional nearby pages

When a user jumps pages, cancel or lower priority for stale queued work. Limit concurrency to avoid battery, memory, and network spikes.

## Translation provider contract

The application should depend on an internal interface, not a vendor-specific SDK. The contract should accept text plus source/target language and return translated text with structured errors and timing metadata.

Provider selection should be evaluated with representative PDFs for speed, quality, cost, privacy, language coverage, and offline behavior.

## Rendering strategy

Do not modify the source PDF. Keep the original rendering and translated content as separate layers or reading modes.

Exact visual replacement is difficult because translations change text length and Indic scripts have different font metrics. For the MVP, prioritize readable structure and correct reading order over pixel-perfect reproduction. Preserve headings, paragraphs, lists, and page boundaries when extraction allows.

## Failure behavior

- If translation fails, keep the original page readable.
- Retry transient failures with bounded backoff.
- Do not show a blocking error for background prefetch failures.
- Never lose the original document when translation is unavailable.
- Make cache invalidation explicit when the provider/model or segmentation version changes.

## Performance instrumentation

Measure PDF open time, text extraction time, translation request latency, render latency, cache hit rate, prefetch hit rate, and memory usage independently.
