DELETE FROM location_responses;
DELETE FROM moment_responses;
DELETE FROM locations;
DELETE FROM moments;
DELETE FROM invitations;
DELETE FROM hangouts;
DELETE FROM users;

INSERT INTO users
    (first_name, last_name, handle, email, mobile_number)
VALUES
    ('Ben', 'Allred', 'skuttleman', 'skuttleman@gmail.com', '4437979211'),
    ('Sven', 'Sanchez', 'votesanchez', 'evil@alter.ego', '9999999999'),
    ('Zoe', 'Bartlett', 'slowyzoe', 'zoe@bartlett.net', '6092438667'),
    ('Charles', 'Xavier', 'proffessor-x', 'prof@xmansion.edu', '7024913106'),
    ('Biff', 'Tannen', 'biff33', 'biff@thug.bully', '5227024933'),
    ('William', 'Zabka', 'therealbillyzabka', 'kick@karate.globe', '5334390218'),
    ('Billy', 'Zabka', 'zabka', 'zabkafan@stalkers.org', '6104399761'),
    ('Jill', 'Abercrombe', 'jillybean', 'jill85@yahoo.com', '2179773016'),
    ('Katie', 'Thompson', 'k8_kitty', 'k.thom@greatykatie.net', '3362084115'),
    ('Sarah', 'Gunderston', 'sgunderston', 'sgunderston@comcast.net', '5526563774'),
    ('Dirk', 'Diggler', 'thedirk', 'dirk@longjohns.xxx', '4493024417'),
    ('Martif','Pharti', 'partiboy','ambassador@domincan.republic','7333951247');

INSERT INTO hangouts
    (name, created_by)
VALUES
    ('Evil event', (SELECT id FROM users WHERE handle = 'votesanchez')),
    ('bluto', (SELECT id FROM users where handle = 'skuttleman')),
    ('Dude', (SELECT id FROM users where handle = 'skuttleman'));

INSERT INTO invitations
    (hangout_id, search, user_id, created_by, response, match_type)
VALUES
    ((SELECT id FROM hangouts WHERE name = 'Dude'), NULL, (SELECT id FROM users where handle = 'slowyzoe'), (SELECT id FROM users where handle = 'skuttleman'), 'none', 'exact'),
    ((SELECT id FROM hangouts WHERE name = 'bluto'), NULL, (SELECT id FROM users where handle = 'slowyzoe'), (SELECT id FROM users where handle = 'skuttleman'), 'none', 'exact'),
    ((SELECT id FROM hangouts WHERE name = 'bluto'), NULL, (SELECT id FROM users where handle = 'therealbillyzabka'), (SELECT id FROM users where handle = 'skuttleman'), 'none', 'exact'),
    ((SELECT id FROM hangouts WHERE name = 'bluto'), NULL, (SELECT id FROM users where handle = 'proffessor-x'), (SELECT id FROM users where handle = 'skuttleman'), 'none', 'exact'),
    ((SELECT id FROM hangouts WHERE name = 'bluto'), '123456789', (SELECT id FROM users where handle = 'thedirk'), (SELECT id FROM users where handle = 'skuttleman'), 'none', 'mobile-number');
