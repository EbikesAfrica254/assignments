--comment: create the shortlist table to create the Workforce shortlist snapshot per assignment attempt
CREATE TABLE assignments.shortlist(
      id            UUID          NOT NULL DEFAULT gen_random_uuid(),

      agent_id      VARCHAR(36)   NOT NULL,
      assignment_id UUID          NOT NULL,
      created_at    TIMESTAMPTZ   NOT NULL,
      is_preferred  BOOLEAN       NOT NULL DEFAULT FALSE,
      latitude      DECIMAL(9, 6) NOT NULL,
      longitude     DECIMAL(9, 6) NOT NULL,
      rank          INTEGER,
      vehicle_class VARCHAR(20)   NOT NULL,

      CONSTRAINT pk_shortlist PRIMARY KEY (id)
);

--comment: add foreign key constraints for shortlist
ALTER TABLE assignments.shortlist
    ADD CONSTRAINT fk_shortlist_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments.assignments (id);

--comment: add check constraints for shortlist
ALTER TABLE assignments.shortlist
    ADD CONSTRAINT chk_shortlist_vehicle_class
        CHECK (vehicle_class IN ('BICYCLE', 'CAR', 'MOTORCYCLE'));

ALTER TABLE assignments.shortlist
    ADD CONSTRAINT chk_shortlist_rank_positive
        CHECK (rank IS NULL OR rank > 0);

--comment: unique constraint ensuring an agent appears only once per assignment attempt
ALTER TABLE assignments.shortlist
    ADD CONSTRAINT uq_shortlist_assignment_agent UNIQUE (assignment_id, agent_id);

--comment: create indexes for shortlist
CREATE INDEX idx_shortlist_assignment_id ON assignments.shortlist(assignment_id);
CREATE INDEX idx_shortlist_rank          ON assignments.shortlist(assignment_id, rank) WHERE rank IS NOT NULL;

--comment: add table and column comments for shortlist
COMMENT ON TABLE assignments.shortlist IS 'Point-in-time snapshot of Workforce shortlist candidates per assignment attempt. Not a reservation. Provides durable reference for RANKED ordering, BROADCAST subset selection, and audit. Immutable after creation.';
COMMENT ON COLUMN assignments.shortlist.id IS 'Primary key - UUID surrogate identifier';
COMMENT ON COLUMN assignments.shortlist.agent_id IS 'Keycloak user ID of the candidate agent from Workforce shortlist';
COMMENT ON COLUMN assignments.shortlist.assignment_id IS 'FK to parent assignment attempt';
COMMENT ON COLUMN assignments.shortlist.created_at IS 'Timestamp when candidate was recorded from the shortlist event';
COMMENT ON COLUMN assignments.shortlist.is_preferred IS 'True when Workforce has designated this candidate as the preferred or pre-assigned agent. Used exclusively by PREASSIGNED strategy. At most one candidate per assignment should carry this flag.';
COMMENT ON COLUMN assignments.shortlist.latitude IS 'Agent location latitude at time of shortlist resolution - used as origin in Routing matrix request';
COMMENT ON COLUMN assignments.shortlist.longitude IS 'Agent location longitude at time of shortlist resolution - used as origin in Routing matrix request';
COMMENT ON COLUMN assignments.shortlist.rank IS 'Ordinal position after Routing matrix scoring for RANKED strategy - null for PREASSIGNED and BROADCAST';
COMMENT ON COLUMN assignments.shortlist.vehicle_class IS 'Vehicle class mapped from Workforce CapabilityClass - passed to Routing matrix request';
