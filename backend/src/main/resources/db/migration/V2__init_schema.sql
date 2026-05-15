CREATE TABLE tenants (
                         id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                         name       VARCHAR(255) NOT NULL,
                         slug       VARCHAR(100) NOT NULL UNIQUE,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_id     UUID         NOT NULL REFERENCES tenants(id),
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- email uniqueness is scoped to tenant, not global
                       UNIQUE (tenant_id, email)
);

-- Refresh tokens stored server-side to support revocation.
-- Only the SHA-256 hash is stored — raw token never touches the DB.
CREATE TABLE refresh_tokens (
                                id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                tenant_id  UUID        NOT NULL REFERENCES tenants(id),
                                token_hash VARCHAR(64) NOT NULL UNIQUE,
                                expires_at TIMESTAMPTZ NOT NULL,
                                revoked_at TIMESTAMPTZ,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE documents (
                           id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                           tenant_id         UUID         NOT NULL REFERENCES tenants(id),
                           uploaded_by       UUID         NOT NULL REFERENCES users(id),
                           title             VARCHAR(500) NOT NULL,
                           original_filename VARCHAR(500) NOT NULL,
    -- storage_path is an internal server path, never exposed to clients directly
                           storage_path      VARCHAR(1000) NOT NULL,
                           mime_type         VARCHAR(100) NOT NULL,
                           status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                           error_message     TEXT,
                           created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                           updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- tenant_id is denormalized here intentionally:
-- vector similarity queries filter by tenant without joining documents
CREATE TABLE document_chunks (
                                 id          UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
                                 document_id UUID     NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 tenant_id   UUID     NOT NULL REFERENCES tenants(id),
                                 chunk_index INTEGER  NOT NULL,
                                 content     TEXT     NOT NULL,
    -- 1536 dimensions matches text-embedding-3-small; changing model = full re-index
                                 embedding   vector(1536),
                                 token_count INTEGER,
                                 created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_sessions (
                               id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id    UUID         NOT NULL REFERENCES users(id),
                               tenant_id  UUID         NOT NULL REFERENCES tenants(id),
                               title      VARCHAR(500),
                               created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_messages (
                               id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                               session_id          UUID        NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
                               role                VARCHAR(20) NOT NULL,
                               content             TEXT        NOT NULL,
    -- Persisted immediately — foundation for citation display, audit trail, analytics
                               retrieved_chunk_ids UUID[]      NOT NULL DEFAULT '{}',
                               prompt_tokens       INTEGER,
                               completion_tokens   INTEGER,
                               created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Auth indexes
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Document indexes
CREATE INDEX idx_documents_tenant_status ON documents(tenant_id, status);
CREATE INDEX idx_document_chunks_tenant_document ON document_chunks(tenant_id, document_id);

-- Chat indexes
CREATE INDEX idx_chat_sessions_user_tenant ON chat_sessions(user_id, tenant_id);
CREATE INDEX idx_chat_messages_session_created ON chat_messages(session_id, created_at);

-- HNSW index for ANN vector search — added now so it exists before Phase 3 data
-- m=16, ef_construction=64 are sensible defaults for ~100k chunks
CREATE INDEX idx_document_chunks_embedding_hnsw
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);