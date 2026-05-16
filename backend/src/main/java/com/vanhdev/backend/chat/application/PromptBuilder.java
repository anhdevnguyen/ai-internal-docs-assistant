package com.vanhdev.backend.chat.application;

import com.vanhdev.backend.retrieval.domain.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Constructs the LLM prompt from retrieved document context.
 * Centralised here so prompt engineering decisions are visible and changeable
 * in one place. Prompt quality directly determines RAG answer quality.
 * Design constraints encoded in the system prompt:
 * 1. AI must not answer from general knowledge — only from provided context.
 * 2. Fallback phrase is explicit, so the client can detect and handle "no answer" cases.
 * 3. Context window budget is managed by the caller (ChatOrchestrationService),
 *    not here — this builder trusts that the chunks list is already within budget.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_TEMPLATE = """
            You are an internal enterprise knowledge assistant.
            Answer the user's question ONLY using the Context provided below.
            If the answer cannot be found in the Context, respond with exactly:
            "I cannot find the answer in the provided documents."
            Do NOT use any external knowledge. Do NOT make up information.
            Be concise and precise. Cite the document source inline when possible.
            
            Context:
            {context}
            """;

    /**
     * @param chunks ordered list of retrieved chunks (most relevant first)
     * @return fully constructed system prompt ready to send to the LLM
     */
    public String buildSystemPrompt(List<RetrievedChunk> chunks) {
        String context = buildContextBlock(chunks);
        return SYSTEM_TEMPLATE.replace("{context}", context);
    }

    private String buildContextBlock(List<RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "(No relevant documents found)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append("Source: ").append(chunk.documentTitle()).append("\n")
                    .append(chunk.content())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }
}