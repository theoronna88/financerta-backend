CREATE TABLE credit_card_statements
(
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    userId       UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    creditCardId UUID    NOT NULL REFERENCES credit_cards (id),

    month        INTEGER NOT NULL,
    year         INTEGER NOT NULL,

    created_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP        DEFAULT CURRENT_TIMESTAMP
);