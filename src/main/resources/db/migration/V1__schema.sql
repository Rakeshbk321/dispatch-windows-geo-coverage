CREATE TABLE zone (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            TEXT NOT NULL UNIQUE,
    name            TEXT NOT NULL,
    window_capacity INT  NOT NULL CHECK (window_capacity > 0)
);

-- A commune points to at most one zone, so zones can't overlap. NULL zone_id means "known but not covered".
CREATE TABLE commune (
    code    TEXT PRIMARY KEY,
    name    TEXT NOT NULL,
    region  TEXT NOT NULL,
    zone_id BIGINT REFERENCES zone (id)
);

-- CLP has no decimals.
CREATE TABLE time_block (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    start_time TIME          NOT NULL,
    end_time   TIME          NOT NULL,
    cost       NUMERIC(10, 0) NOT NULL CHECK (cost >= 0),
    CHECK (end_time > start_time)
);

CREATE TABLE dispatch_window (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    zone_id       BIGINT NOT NULL REFERENCES zone (id),
    delivery_date DATE   NOT NULL,
    time_block_id BIGINT NOT NULL REFERENCES time_block (id),
    capacity      INT    NOT NULL,
    booked_count  INT    NOT NULL DEFAULT 0,
    CONSTRAINT uq_window_zone_date_block UNIQUE (zone_id, delivery_date, time_block_id),
    -- Last line of defence against over-booking, even if application code is wrong.
    CONSTRAINT ck_window_booked_within_capacity CHECK (booked_count >= 0 AND booked_count <= capacity)
);

CREATE TABLE booking (
    id            UUID PRIMARY KEY,
    window_id     BIGINT         NOT NULL REFERENCES dispatch_window (id),
    order_id      TEXT           NOT NULL,
    street        TEXT           NOT NULL,
    street_number TEXT           NOT NULL,
    commune_code  TEXT           NOT NULL REFERENCES commune (code),
    -- Snapshot of the time block cost at booking time, so later price changes don't rewrite history.
    price         NUMERIC(10, 0) NOT NULL,
    status        TEXT           NOT NULL CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    created_at    TIMESTAMPTZ    NOT NULL,
    cancelled_at  TIMESTAMPTZ
);

-- One confirmed booking per order; a cancelled order can be booked again.
CREATE UNIQUE INDEX uq_booking_confirmed_order ON booking (order_id) WHERE status = 'CONFIRMED';

CREATE INDEX ix_booking_window ON booking (window_id);
