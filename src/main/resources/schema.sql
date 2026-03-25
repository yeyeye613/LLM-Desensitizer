CREATE TABLE IF NOT EXISTS sensitive_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pattern_name VARCHAR(255),
    regex VARCHAR(255),
    is_enabled BOOLEAN,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sensitive_rules (pattern_name, regex, is_enabled, description) VALUES 
('Email', '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}', true, 'Email address pattern'),
('Phone', '\\d{3}-\\d{3}-\\d{4}', true, 'US Phone number pattern');
