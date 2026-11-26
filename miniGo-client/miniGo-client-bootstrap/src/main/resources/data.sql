INSERT IGNORE INTO app_user (
    username,
    password_hash,
    nickname,
    phone,
    email,
    status
) VALUES
    (
        'demo',
        '$2a$10$Yd6bqeSycUn/68MWpuo0Ou9PAEDU28AP..diK8u33hsWULR4o.7D6',
        '演示用户',
        '13800000001',
        'demo@minigo.local',
        'ACTIVE'
    ),
    (
        'alice',
        '$2a$10$Yd6bqeSycUn/68MWpuo0Ou9PAEDU28AP..diK8u33hsWULR4o.7D6',
        'Alice',
        '13800000002',
        'alice@minigo.local',
        'ACTIVE'
    );
