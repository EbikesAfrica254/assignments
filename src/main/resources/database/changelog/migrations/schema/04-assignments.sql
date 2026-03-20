--comment: create the assignments table as the core orchestration aggregate for dispatch attempts
CREATE TABLE assignments.assignments (
     id                  UUID         NOT NULL DEFAULT gen_random_uuid(),

     cancellation_reason TEXT,
     created_at          TIMESTAMPTZ  NOT NULL,
     failure_reason      TEXT,
     order_context_id    UUID         NOT NULL,
     status              VARCHAR(20)  NOT NULL,
     strategy            VARCHAR(20)  NOT NULL,
     updated_at          TIMESTAMPTZ,
     version             BIGINT       NOT NULL DEFAULT 0,
     winner_agent_id     VARCHAR(36),

     CONSTRAINT pk_assignments PRIMARY KEY (id)
);

--comment: add foreign key constraints for assignments
ALTER TABLE assignments.assignments
    ADD CONSTRAINT fk_assignments_order_context
        FOREIGN KEY (order_context_id) REFERENCES assignments.order_contexts (id);

--comment: add check constraints for assignments
ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_status
        CHECK (status IN ('AWAITING_RESPONSE', 'CANCELLED', 'FAILED', 'STARTED', 'SUCCEEDED'));

ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_strategy
        CHECK (strategy IN ('BROADCAST', 'PREASSIGNED', 'RANKED'));

ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_version
        CHECK (version >= 0);

ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_failure_reason_only_when_failed
        CHECK (failure_reason IS NULL OR status = 'FAILED');

ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_cancellation_reason_only_when_cancelled
        CHECK (cancellation_reason IS NULL OR status = 'CANCELLED');

ALTER TABLE assignments.assignments
    ADD CONSTRAINT chk_assignments_winner_only_when_succeeded
        CHECK (winner_agent_id IS NULL OR status = 'SUCCEEDED');

--comment: partial unique index enforcing one active attempt per order - core invariant from ADR-003
CREATE UNIQUE INDEX uq_assignments_one_active_per_order
    ON assignments.assignments (order_context_id)
    WHERE status NOT IN ('CANCELLED', 'FAILED', 'SUCCEEDED');

--comment: create indexes for assignments
CREATE INDEX idx_assignments_order_context_status ON assignments.assignments (order_context_id, status);
CREATE INDEX idx_assignments_status               ON assignments.assignments (status);

--comment: add table and column comments for assignments
COMMENT ON TABLE assignments.assignments IS 'Core orchestration aggregate. One active attempt per order at any time, enforced by partial unique index on order_context_id where status is non-terminal. Terminal states (SUCCEEDED, FAILED, CANCELLED) are immutable.';
COMMENT ON COLUMN assignments.assignments.id IS 'Primary key - UUID surrogate identifier';
COMMENT ON COLUMN assignments.assignments.cancellation_reason IS 'Human-readable reason for CANCELLED terminal state - null for all other statuses';
COMMENT ON COLUMN assignments.assignments.created_at IS 'Timestamp when assignment attempt was opened';
COMMENT ON COLUMN assignments.assignments.failure_reason IS 'Human-readable reason for FAILED terminal state - null for all other statuses';
COMMENT ON COLUMN assignments.assignments.order_context_id IS 'FK to staged order context - links attempt to originating order data';
COMMENT ON COLUMN assignments.assignments.status IS 'Lifecycle state of this attempt - transitions are monotonic toward a terminal state';
COMMENT ON COLUMN assignments.assignments.strategy IS 'Strategy currently executing for this attempt';
COMMENT ON COLUMN assignments.assignments.updated_at IS 'Timestamp of last status transition';
COMMENT ON COLUMN assignments.assignments.version IS 'Optimistic lock version - also used as basis for pessimistic SELECT FOR UPDATE during winner finalization';
COMMENT ON COLUMN assignments.assignments.winner_agent_id IS 'Keycloak user ID of the winning agent - null until status reaches SUCCEEDED';