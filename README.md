# Padho

**Read in your language.**

Padho is a distraction-free Android PDF reader designed to translate English PDFs into Indian languages as you read.

## Vision

Open a PDF and read. Translation should feel like an invisible part of the reading experience, not a separate workflow.

## Initial languages

- Hindi
- Gujarati
- Marathi
- Tamil

## Product principles

- **Reading first:** keep controls quiet and unobtrusive.
- **Fast by design:** prioritize the visible page and prefetch upcoming pages.
- **Stay in flow:** avoid modal dialogs, blocking spinners, and repetitive setup.
- **Preserve originals:** never modify the source PDF.
- **Cache intelligently:** reuse translations across sessions.
- **Build for growth:** support additional languages and document types over time.

## MVP scope

- Android app using Kotlin and Jetpack Compose
- Open local text-based PDFs
- Automatic page-level translation
- Remember the preferred language
- Hindi, Gujarati, Marathi, and Tamil
- Original/translated view toggle
- Memory and persistent translation caches
- Background prefetch for nearby pages
- Light and dark reading themes

OCR for scanned PDFs is a later phase, after the text-based reading experience is reliable.

## Architecture direction

```text
PDF document
    |
    v
PDF renderer + text extractor
    |
    v
Text segmentation
    |
    v
Translation manager <----> Translation provider(s)
    |
    v
Translation cache
    |
    v
Distraction-free reader
```

The reader should depend on a provider interface rather than a single translation vendor. Local and cloud translation engines can be evaluated against speed, quality, cost, language coverage, and offline support.

## Performance strategy

1. Display the original page immediately when possible.
2. Start translating as soon as text is extracted.
3. Prioritize the currently visible page.
4. Prefetch the next page and nearby pages.
5. Cache translated segments and complete page results.
6. Cancel or reprioritize work when the reader jumps to another page.
7. Measure extraction, translation, and rendering latency separately.

## Planned repository layout

```text
app/                  Android application
core/pdf/             PDF rendering and text extraction
core/translation/     Translation contracts and scheduling
core/cache/           Translation cache
core/reader/          Reader state and presentation logic
docs/                 Product, architecture, and roadmap
```

## Status

Early development. See [the product definition](docs/PRODUCT.md), [architecture](docs/ARCHITECTURE.md), and [roadmap](docs/ROADMAP.md).

## License

To be decided before public distribution.
