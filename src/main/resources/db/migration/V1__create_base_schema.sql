-- Enum типы
CREATE TYPE waiter_role AS ENUM ('ADMIN', 'WAITER');
CREATE TYPE table_status AS ENUM ('AVAILABLE', 'OCCUPIED', 'RESERVED');
CREATE TYPE party_status AS ENUM ('ACTIVE', 'CLOSED');
CREATE TYPE bill_status AS ENUM ('OPEN', 'CLOSED', 'CANCELLED');

-- Таблица официантов
CREATE TABLE waiters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role waiter_role NOT NULL DEFAULT 'WAITER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_waiters_keycloak_id ON waiters(keycloak_id);
CREATE INDEX idx_waiters_username ON waiters(username);
CREATE INDEX idx_waiters_active ON waiters(active);

-- Таблица столов ресторана
CREATE TABLE restaurant_tables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_number VARCHAR(20) UNIQUE NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    status table_status NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tables_status ON restaurant_tables(status);
CREATE INDEX idx_tables_number ON restaurant_tables(table_number);

-- Таблица компаний (Party)
CREATE TABLE parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_id UUID NOT NULL REFERENCES restaurant_tables(id) ON DELETE RESTRICT,
    waiter_id UUID NOT NULL REFERENCES waiters(id) ON DELETE RESTRICT,
    status party_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT check_closed_at_if_closed CHECK (
        (status = 'CLOSED' AND closed_at IS NOT NULL) OR
        (status = 'ACTIVE' AND closed_at IS NULL)
    )
);

CREATE INDEX idx_parties_table_id ON parties(table_id);
CREATE INDEX idx_parties_waiter_id ON parties(waiter_id);
CREATE INDEX idx_parties_status ON parties(status);

-- Таблица гостей
CREATE TABLE guests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_guests_party_id ON guests(party_id);

-- Таблица счетов
CREATE TABLE bills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id) ON DELETE RESTRICT,
    bill_number VARCHAR(50) UNIQUE NOT NULL,
    items_total NUMERIC(10, 2) NOT NULL DEFAULT 0,
    service_fee_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    service_fee_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    status bill_status NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_bills_party_id ON bills(party_id);
CREATE INDEX idx_bills_status ON bills(status);
CREATE INDEX idx_bills_bill_number ON bills(bill_number);
CREATE INDEX idx_bills_created_at ON bills(created_at);

-- Таблица позиций заказа
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bill_id UUID NOT NULL REFERENCES bills(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_bill_id ON order_items(bill_id);

-- Связь позиций заказа с гостями (N:M)
CREATE TABLE order_item_guests (
    order_item_id UUID NOT NULL REFERENCES order_items(id) ON DELETE CASCADE,
    guest_id UUID NOT NULL REFERENCES guests(id) ON DELETE CASCADE,
    PRIMARY KEY (order_item_id, guest_id)
);

CREATE INDEX idx_order_item_guests_guest_id ON order_item_guests(guest_id);
