INSERT INTO users (email, first_name, last_name, handle, mobile_number)
VALUES
    ('user1@example.test', 'User', 'One', 'user1', '9876543210'),
    ('user2@example.test', 'User', 'Two', 'user2', '7654321098'),
    ('user3@example.test', 'User', 'Three', 'user3', '3456789012'),
    ('user4@example.test', 'User', 'Four', 'user4', '8273645647'),
    ('user5@example.test', 'User', 'Five', 'user5', '2938475671'),
    ('user6@example.test', 'User', 'Six', 'user6', '9343234014');

INSERT INTO hangouts (name, created_by)
VALUES
    ('User 1 hangout 1', (SELECT id FROM users WHERE email = 'user1@example.test')),
    ('User 1 hangout 2', (SELECT id FROM users WHERE email = 'user1@example.test')),
    ('User 2 hangout 1', (SELECT id FROM users WHERE email = 'user2@example.test')),
    ('User 3 hangout 1', (SELECT id FROM users WHERE email = 'user3@example.test'));

INSERT INTO invitations (hangout_id, user_id, match_type, created_by)
VALUES
    ((SELECT id FROM hangouts WHERE name = 'User 1 hangout 1'), (SELECT id FROM users WHERE email = 'user2@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user1@example.test')),
    ((SELECT id FROM hangouts WHERE name = 'User 1 hangout 1'), (SELECT id FROM users WHERE email = 'user3@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user1@example.test')),
    ((SELECT id FROM hangouts WHERE name = 'User 1 hangout 1'), (SELECT id FROM users WHERE email = 'user5@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user1@example.test')),

    ((SELECT id FROM hangouts WHERE name = 'User 2 hangout 1'), (SELECT id FROM users WHERE email = 'user1@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user2@example.test')),
    ((SELECT id FROM hangouts WHERE name = 'User 2 hangout 1'), (SELECT id FROM users WHERE email = 'user5@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user2@example.test')),

    ((SELECT id FROM hangouts WHERE name = 'User 3 hangout 1'), (SELECT id FROM users WHERE email = 'user4@example.test'), 'exact', (SELECT id FROM users WHERE email = 'user2@example.test'));
