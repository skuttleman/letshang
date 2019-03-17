CREATE TABLE known_associates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v1mc(),
    user_id UUID NOT NULL REFERENCES users,
    associate_id UUID NOT NULL REFERENCES users,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE known_associates
    ADD CONSTRAINT known_associates_user_id_not_associate_id CHECK (user_id != associate_id);

CREATE UNIQUE INDEX known_associates_unique_user_id_and_associate_id ON known_associates (least(user_id, associate_id), greatest(user_id, associate_id));
