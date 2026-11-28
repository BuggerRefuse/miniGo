INSERT IGNORE INTO admin_user (
    username,
    password_hash,
    display_name,
    status
) VALUES (
    'admin',
    '$2a$10$Yd6bqeSycUn/68MWpuo0Ou9PAEDU28AP..diK8u33hsWULR4o.7D6',
    '系统管理员',
    'ACTIVE'
);
