# Translation Quality Specification

## Problem

The current reading experience produces word-level or overly fragmented translation. This destroys syntax, idioms, references, tone, and the author's intended meaning.

## Required behavior

Translate coherent passages, not isolated words. The unit of translation should normally be a paragraph or a short group of related sentences. Sentence boundaries are useful for alignment and display, but sentences must retain surrounding paragraph context.

## Pipeline

1. Extract text together with page coordinates and layout metadata.
2. Reconstruct reading order and group text into semantic blocks: title, heading, paragraph, list item, caption, table cell, footnote.
3. Keep each paragraph intact. Do not split on spaces, individual tokens, or arbitrary PDF text spans.
4. For long paragraphs, split only at sentence boundaries into overlapping context windows. Carry the preceding paragraph or a short context summary when a page boundary interrupts a thought.
5. Send each chunk as a complete natural-language passage with explicit source language, target language, and optional document glossary.
6. Require fluent, faithful translation. Preserve names, numbers, quotations, emphasis, list structure, and paragraph boundaries. Do not summarize, omit, or add facts.
7. Validate response structure and retry transient/provider failures. Never silently fall back to translating individual words.
8. Render translated blocks in original reading order. Preserve the original PDF as a separate view.
9. Cache by document fingerprint, page/block identity, source/target language, provider/model, segmentation version, and prompt version.

## Provider strategy

Keep the translation provider replaceable. On-device ML Kit can remain an offline/low-latency option, but it must receive complete paragraph/sentence-context strings, never isolated tokens. For the highest literary quality, evaluate a context-aware neural/LLM provider against a curated benchmark. Cloud translation must be explicit about network use, privacy, cost, and offline fallback.

## Chunking rules

- Prefer one paragraph per request when it fits provider limits.
- Keep adjacent sentences together; never translate each sentence without context when pronouns, references, or discourse markers depend on nearby text.
- For oversized paragraphs, split at sentence boundaries with a small preceding-context overlap; remove overlap from the final assembled output.
- Never split a word, Unicode grapheme cluster, or Indic combining sequence.
- Preserve headings, bullet/list items, tables, and captions as separate semantic blocks.
- Avoid joining unrelated columns or page headers/footers into body paragraphs.

## Quality benchmark

Create a golden test set for English-to-Hindi, Gujarati, Marathi, and Tamil with:
- idioms and figurative language
- pronouns and cross-sentence references
- long and compound sentences
- dialogue and quotations
- headings, lists, and footnotes
- literary/non-fiction passages and technical terms
- page-break continuation

Review for meaning fidelity, fluency, terminology consistency, omissions/additions, and reading order. Compare providers and model versions using the same passages.

## Acceptance criteria

- No word-by-word translation path remains in the reader pipeline.
- A multi-sentence paragraph is translated as a coherent passage with context.
- Page breaks do not cause isolated fragments to be translated without nearby context.
- Output preserves paragraph/list structure and all source meaning.
- Provider failure leaves the original page readable and never degrades to token-by-token output.
- Cache entries are invalidated when segmentation or translation provider/model versions change.
