-- Illustrative coverage, not real delivery data. La Serena mirrors the challenge's reference screen.
INSERT INTO zone (code, name, window_capacity) VALUES
    ('LS-CQ',   'La Serena - Coquimbo',      5),
    ('SCL-CO',  'Santiago Centro-Oriente',   8),
    ('VAL-VDM', 'Valparaíso - Viña del Mar', 5);

INSERT INTO commune (code, name, region, zone_id) VALUES
    ('la-serena',     'La Serena',     'Coquimbo',      (SELECT id FROM zone WHERE code = 'LS-CQ')),
    ('coquimbo',      'Coquimbo',      'Coquimbo',      (SELECT id FROM zone WHERE code = 'LS-CQ')),
    ('santiago',      'Santiago',      'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('providencia',   'Providencia',   'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('las-condes',    'Las Condes',    'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('nunoa',         'Ñuñoa',         'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('vitacura',      'Vitacura',      'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('la-reina',      'La Reina',      'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('lo-barnechea',  'Lo Barnechea',  'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('recoleta',      'Recoleta',      'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('independencia', 'Independencia', 'Metropolitana', (SELECT id FROM zone WHERE code = 'SCL-CO')),
    ('valparaiso',    'Valparaíso',    'Valparaíso',    (SELECT id FROM zone WHERE code = 'VAL-VDM')),
    ('vina-del-mar',  'Viña del Mar',  'Valparaíso',    (SELECT id FROM zone WHERE code = 'VAL-VDM')),
    ('concon',        'Concón',        'Valparaíso',    (SELECT id FROM zone WHERE code = 'VAL-VDM')),
    ('quilpue',       'Quilpué',       'Valparaíso',    (SELECT id FROM zone WHERE code = 'VAL-VDM')),
    ('villa-alemana', 'Villa Alemana', 'Valparaíso',    (SELECT id FROM zone WHERE code = 'VAL-VDM')),
    -- Known communes no zone delivers to, so the "not covered" path can be tried in every region.
    ('vicuna',        'Vicuña',        'Coquimbo',      NULL),
    ('ovalle',        'Ovalle',        'Coquimbo',      NULL),
    ('maipu',         'Maipú',         'Metropolitana', NULL),
    ('puente-alto',   'Puente Alto',   'Metropolitana', NULL),
    ('quillota',      'Quillota',      'Valparaíso',    NULL);

-- Back-to-back two-hour blocks from 08:00 to 22:00, the same price all day as on the reference screen.
INSERT INTO time_block (start_time, end_time, cost) VALUES
    ('08:00', '10:00', 2990),
    ('10:00', '12:00', 2990),
    ('12:00', '14:00', 2990),
    ('14:00', '16:00', 2990),
    ('16:00', '18:00', 2990),
    ('18:00', '20:00', 2990),
    ('20:00', '22:00', 2990);
