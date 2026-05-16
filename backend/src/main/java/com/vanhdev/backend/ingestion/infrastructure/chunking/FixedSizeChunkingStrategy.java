package com.vanhdev.backend.ingestion.infrastructure.chunking;

import com.vanhdev.backend.ingestion.domain.ChunkingStrategy;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.shared.config.ChunkingProperties;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits text by sentences, accumulates sentences until chunkSizeTokens is reached,
 * then carries the last overlapTokens worth of content into the next chunk.
 *
 * Sentence-aware splitting avoids cutting mid-sentence, which degrades retrieval quality.
 * Overlap prevents losing context at chunk boundaries.
 */
@Component
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    // 1 token ≈ 4 characters — acceptable approximation for phase 2.
    // Phase 3: replace with jtokkit for exact OpenAI tokenization.
    private static final double CHARS_PER_TOKEN = 4.0;

    private final ChunkingProperties props;

    public FixedSizeChunkingStrategy(ChunkingProperties props) {
        this.props = props;
    }

    @Override
    public List<TextChunk> chunk(String text) {
        List<String> sentences = splitIntoSentences(text);
        List<TextChunk> chunks = new ArrayList<>();
        List<String> window = new ArrayList<>();
        int windowTokens = 0;
        int chunkIndex = 0;

        for (String sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence);

            if (windowTokens + sentenceTokens > props.chunkSizeTokens() && !window.isEmpty()) {
                chunks.add(buildChunk(chunkIndex++, window, windowTokens));
                List<String> overlap = buildOverlapWindow(window);
                window = overlap;
                windowTokens = window.stream().mapToInt(this::estimateTokens).sum();
            }

            window.add(sentence);
            windowTokens += sentenceTokens;
        }

        if (!window.isEmpty()) {
            chunks.add(buildChunk(chunkIndex, window, windowTokens));
        }

        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.US);
        boundary.setText(text);
        List<String> sentences = new ArrayList<>();
        int start = boundary.first();
        for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary.next()) {
            String sentence = text.substring(start, end).strip();
            if (!sentence.isBlank()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    private List<String> buildOverlapWindow(List<String> window) {
        List<String> overlap = new ArrayList<>();
        int tokens = 0;
        for (int i = window.size() - 1; i >= 0 && tokens < props.overlapTokens(); i--) {
            overlap.addFirst(window.get(i));
            tokens += estimateTokens(window.get(i));
        }
        return overlap;
    }

    private TextChunk buildChunk(int index, List<String> sentences, int estimatedTokens) {
        return new TextChunk(index, String.join(" ", sentences), estimatedTokens);
    }

    private int estimateTokens(String text) {
        return Math.max(1, (int) (text.length() / CHARS_PER_TOKEN));
    }
}
