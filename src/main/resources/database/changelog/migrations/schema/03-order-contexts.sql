--comment: create the order_contexts table to stage order data consumed from Orders events before orchestration begins
CREATE TABLE assignments.order_contexts (
    id                 UUID           NOT NULL DEFAULT gen_random_uuid(),

    branch_id          VARCHAR(36),
    committed_quote_id UUID           NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL,
    is_reassignment    BOOLEAN        NOT NULL DEFAULT FALSE,
    order_id           UUID           NOT NULL,
    organization_id    VARCHAR(36)    NOT NULL,
    pickup_latitude    DECIMAL(9, 6)  NOT NULL,
    pickup_longitude   DECIMAL(9, 6)  NOT NULL,
    updated_at         TIMESTAMPTZ,
    vehicle_class      VARCHAR(20)    NOT NULL,

    CONSTRAINT pk_order_contexts PRIMARY KEY (id)
);

--comment: add check constraints for order_contexts
ALTER TABLE assignments.order_contexts
    ADD CONSTRAINT chk_order_contexts_vehicle_class
        CHECK (vehicle_class IN ('BICYCLE', 'CAR', 'E_BIKE', 'MOTORCYCLE', 'VAN'));

--comment: add unique constraints for order_contexts
ALTER TABLE assignments.order_contexts
    ADD CONSTRAINT uq_order_contexts_order_id UNIQUE (order_id);

--comment: create indexes for order_contexts
CREATE INDEX idx_order_contexts_organization_id ON assignments.order_contexts (organization_id);

--comment: add table and column comments for order_contexts
COMMENT ON TABLE assignments.order_contexts IS 'Staged order context consumed from Orders events. Written on receipt of orders.order.entered_pending_assignment or orders.order.reassignment_requested, before the Workforce shortlist arrives. Provides a durable reference for the full orchestration lifecycle.';
COMMENT ON COLUMN assignments.order_contexts.id IS 'Primary key - UUID surrogate identifier';
COMMENT ON COLUMN assignments.order_contexts.branch_id IS 'Branch scope - null for organisation-wide orders';
COMMENT ON COLUMN assignments.order_contexts.committed_quote_id IS 'Quote ID from Routing - must exist before assignment orchestration can start';
COMMENT ON COLUMN assignments.order_contexts.created_at IS 'Timestamp when order context was staged';
COMMENT ON COLUMN assignments.order_contexts.is_reassignment IS 'True when staged from orders.order.reassignment_requested, false for initial assignment';
COMMENT ON COLUMN assignments.order_contexts.order_id IS 'Order identifier from Orders service - unique per staged context';
COMMENT ON COLUMN assignments.order_contexts.organization_id IS 'Organisation scope - carried for audit and access control';
COMMENT ON COLUMN assignments.order_contexts.pickup_latitude IS 'Pickup location latitude - passed to Routing matrix as destination';
COMMENT ON COLUMN assignments.order_contexts.pickup_longitude IS 'Pickup location longitude - passed to Routing matrix as destination';
COMMENT ON COLUMN assignments.order_contexts.updated_at IS 'Timestamp of last update';
COMMENT ON COLUMN assignments.order_contexts.vehicle_class IS 'Required vehicle capability class - passed to Routing matrix request';
