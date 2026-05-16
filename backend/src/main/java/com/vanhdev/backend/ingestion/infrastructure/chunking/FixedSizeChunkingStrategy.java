package com.vanhdev.backend.ingestion.infrastructure.chunking;

import com.vanhdev.backend.ingestion.domain.ChunkingStrategy;
import com.vanhdev.backend.ingestion.domain.TextChunk;
import com.vanhdev.backend.shared.config.ChunkingProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    // Approximation: 1 token ≈ 4 characters (OpenAI tokenizer heuristic for English).
    // A proper cl100k tokenizer would be accurate but adds latency; this is acceptable
    // for chunk boundary estimation — exact token counts are not required for ANN search.
    private static final int CHARS_PER_TOKEN = 4;

    private final int chunkSizeChars;
    private final int overlapChars;

    public FixedSizeChunkingStrategy(ChunkingProperties props) {
        this.chunkSizeChars = props.chunkSizeTokens() * CHARS_PER_TOKEN;
        this.overlapChars = props.overlapTokens() * CHARS_PER_TOKEN;
    }

    @Override
    public List<TextChunk> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSizeChars, text.length());

            // Prefer splitting at a paragraph/sentence boundary within the last 20%
            // of the window rather than cutting mid-word — improves embedding quality.
            if (end < text.length()) {
                int boundarySearch = Math.max(start + (chunkSizeChars * 4 / 5), start + 1);
                int boundary = findSplitBoundary(text, boundarySearch, end);
                if (boundary > start) {
                    end = boundary;
                }
            }

            String content = text.substring(start, end).strip();
            if (!content.isBlank()) {
                int estimatedTokens = (int) Math.ceil((double) content.length() / CHARS_PER_TOKEN);
                chunks.add(new TextChunk(index++, content, estimatedTokens));
            }

            // Overlap ensures sentences straddling a chunk boundary are captured by both chunks.
            start = end - overlapChars;
            if (start <= 0 || start >= text.length()) break;
        }

        return chunks;
    }

    // Search backwards from `end` for the last paragraph or sentence boundary
    private int findSplitBoundary(String text, int searchFrom, int end) {
        // Prefer paragraph break
        int paragraphBreak = text.lastIndexOf("\n\n", end);
        if (paragraphBreak >= searchFrom) return paragraphBreak + 2;

        // Fall back to sentence end
        for (int i = end - 1; i >= searchFrom; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length() && text.charAt(i + 1) == ' ') {
                return i + 1;
            }
        }
        return end;
    }
}