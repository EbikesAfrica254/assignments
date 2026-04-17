--comment: create the offers table to track individual offers issued to agents during an assignment attempt
CREATE TABLE assignments.offers (
   id                   UUID        NOT NULL DEFAULT gen_random_uuid(),

   agent_id             VARCHAR(36) NOT NULL,
   assignment_id        UUID        NOT NULL,
   created_at           TIMESTAMPTZ NOT NULL,
   expiry_failure_count INTEGER     NOT NULL DEFAULT 0,
   expires_at           TIMESTAMPTZ NOT NULL,
   responded_at         TIMESTAMPTZ,
   status               VARCHAR(20) NOT NULL,
   updated_at           TIMESTAMPTZ,
   version              BIGINT      NOT NULL DEFAULT 0,

   CONSTRAINT pk_offers PRIMARY KEY (id)
);

--comment: add foreign key constraints for offers
ALTER TABLE assignments.offers
    ADD CONSTRAINT fk_offers_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments.assignments (id);

--comment: add check constraints for offers
ALTER TABLE assignments.offers
    ADD CONSTRAINT chk_offers_status
        CHECK (status IN ('ACCEPTED', 'CANCELLED', 'CREATED', 'DECLINED', 'EXPIRED', 'EXPIRY_FAILED'));

ALTER TABLE assignments.offers
    ADD CONSTRAINT chk_offers_version
        CHECK (version >= 0);

ALTER TABLE assignments.offers
    ADD CONSTRAINT chk_offers_expires_at_after_created_at
        CHECK (expires_at > created_at);

ALTER TABLE assignments.offers
    ADD CONSTRAINT chk_offers_responded_at_only_when_terminal
        CHECK (responded_at IS NULL OR status IN ('ACCEPTED', 'DECLINED'));

ALTER TABLE assignments.offers
    ADD CONSTRAINT chk_offers_expiry_failure_count_non_negative
        CHECK (expiry_failure_count >= 0);

--comment: create indexes for offers
CREATE INDEX idx_offers_assignment_status ON assignments.offers (assignment_id, status);
CREATE INDEX idx_offers_expiry            ON assignments.offers (expires_at) WHERE status = 'CREATED';
CREATE INDEX idx_offers_agent_id          ON assignments.offers (agent_id);

--comment: add table and column comments for offers
COMMENT ON TABLE assignments.offers IS 'One row per offer issued during an assignment attempt. Multiple offers exist for RANKED (sequential) and BROADCAST (concurrent) strategies. Offer states are monotonic - no terminal-to-non-terminal transitions permitted.';
COMMENT ON COLUMN assignments.offers.id IS 'Primary key - UUID surrogate identifier';
COMMENT ON COLUMN assignments.offers.agent_id IS 'Keycloak user ID of the agent this offer was sent to';
COMMENT ON COLUMN assignments.offers.assignment_id IS 'FK to parent assignment attempt';
COMMENT ON COLUMN assignments.offers.created_at IS 'Timestamp when offer was created';
COMMENT ON COLUMN assignments.offers.expiry_failure_count IS 'Number of failed expiry processing attempts - offer transitions to EXPIRY_FAILED when this reaches the configured threshold';
COMMENT ON COLUMN assignments.offers.expires_at IS 'Offer TTL - polled by expiry scheduler to transition CREATED offers past this timestamp to EXPIRED';
COMMENT ON COLUMN assignments.offers.responded_at IS 'Timestamp of agent accept or decline response - null for EXPIRED, CANCELLED, and EXPIRY_FAILED';
COMMENT ON COLUMN assignments.offers.status IS 'Current offer state - monotonic toward a terminal state';
COMMENT ON COLUMN assignments.offers.updated_at IS 'Timestamp of last status transition';
COMMENT ON COLUMN assignments.offers.version IS 'Optimistic lock version - guards against duplicate concurrent accept or decline on the same offer row';
