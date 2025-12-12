-- Create waiters table
CREATE TABLE waiters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'WAITER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create restaurant_tables table
CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_number VARCHAR(20) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create parties table
CREATE TABLE parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id UUID NOT NULL REFERENCES restaurant_tables(id),
    waiter_id UUID NOT NULL REFERENCES waiters(id),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create guests table
CREATE TABLE guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create bills table
CREATE TABLE bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id),
    bill_number VARCHAR(50) NOT NULL UNIQUE,
    items_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    service_fee_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    service_fee_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL REFERENCES bills(id),
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create order_item_guests junction table
CREATE TABLE order_item_guests (
    order_item_id UUID NOT NULL REFERENCES order_items(id),
    guest_id UUID NOT NULL REFERENCES guests(id),
    PRIMARY KEY (order_item_id, guest_id)
);

-- Create indexes for better performance
CREATE INDEX idx_waiters_keycloak_id ON waiters(keycloak_id);
CREATE INDEX idx_waiters_username ON waiters(username);
CREATE INDEX idx_parties_table_id ON parties(table_id);
CREATE INDEX idx_parties_waiter_id ON parties(waiter_id);
CREATE INDEX idx_parties_status ON parties(status);
CREATE INDEX idx_guests_party_id ON guests(party_id);
CREATE INDEX idx_bills_party_id ON bills(party_id);
CREATE INDEX idx_bills_bill_number ON bills(bill_number);
CREATE INDEX idx_bills_status ON bills(status);
CREATE INDEX idx_order_items_bill_id ON order_items(bill_id);
