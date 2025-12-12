-- добавление уникального индекса на имя гостя в рамках компании
CREATE UNIQUE INDEX idx_guests_party_name_unique ON guests(party_id, name);
