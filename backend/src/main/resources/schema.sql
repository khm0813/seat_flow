-- Shows table
CREATE TABLE IF NOT EXISTS shows (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    venue VARCHAR(255) NOT NULL,
    show_date TIMESTAMP NOT NULL,
    total_seats INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seat inventory table with unique constraint for concurrency control
CREATE TABLE IF NOT EXISTS seat_inventory (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_id VARCHAR(10) NOT NULL, -- e.g., 'A1', 'B5', 'C10'
    row_name VARCHAR(5) NOT NULL, -- e.g., 'A', 'B', 'C'
    seat_number INTEGER NOT NULL, -- e.g., 1, 5, 10
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, HOLD, CONFIRMED
    price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(show_id, seat_id) -- Critical constraint for concurrency control
);

-- Reservations table
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_inventory_id BIGINT NOT NULL REFERENCES seat_inventory(id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL, -- For demo purposes, simple string ID
    status VARCHAR(20) NOT NULL DEFAULT 'HOLD', -- HOLD, CONFIRMED, CANCELLED
    hold_expires_at TIMESTAMP, -- For HOLD status
    total_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    idempotency_key VARCHAR(255), -- For preventing duplicate requests
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(idempotency_key) -- Prevent duplicate reservations
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_seat_inventory_show_id ON seat_inventory(show_id);
CREATE INDEX IF NOT EXISTS idx_seat_inventory_status ON seat_inventory(status);
CREATE INDEX IF NOT EXISTS idx_reservations_show_id ON reservations(show_id);
CREATE INDEX IF NOT EXISTS idx_reservations_user_id ON reservations(user_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations(status);
CREATE INDEX IF NOT EXISTS idx_reservations_hold_expires_at ON reservations(hold_expires_at);

-- Insert demo data
INSERT INTO shows (id, title, venue, show_date, total_seats) VALUES
(1, 'SeatFlow Demo Concert', 'Main Theater', '2025-12-25 19:30:00', 50)
ON CONFLICT (id) DO NOTHING;

-- Insert demo seat inventory (5 rows x 10 seats = 50 seats)
INSERT INTO seat_inventory (show_id, seat_id, row_name, seat_number, status, price)
SELECT
    1 as show_id,
    CONCAT(row_letter, seat_num) as seat_id,
    row_letter as row_name,
    seat_num as seat_number,
    'AVAILABLE' as status,
    CASE
        WHEN row_letter IN ('A', 'B') THEN 100.00
        WHEN row_letter = 'C' THEN 80.00
        ELSE 60.00
    END as price
FROM
    (SELECT UNNEST(ARRAY['A', 'B', 'C', 'D', 'E']) as row_letter) rows
CROSS JOIN
    (SELECT generate_series(1, 10) as seat_num) seats
ON CONFLICT (show_id, seat_id) DO NOTHING;