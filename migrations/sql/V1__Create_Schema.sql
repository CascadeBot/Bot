CREATE TYPE slot_type AS ENUM ('CUSTOM_CMD', 'PROVIDED', 'TEXT', 'AUTO_REPLY');
CREATE TYPE script_lang AS ENUM ('JS', 'TEXT');
CREATE TYPE option_type AS ENUM ('SUB_COMMAND', 'SUBCOMMAND_GROUP', 'USER', 'ATTACHMENT', 'ROLE', 'STRING', 'NUMBER', 'CHANNEL', 'BOOLEAN');
CREATE TYPE custom_command_type as ENUM ('SLASH', 'CONTEXT_USER', 'CONTEXT_MESSAGE');

CREATE TABLE guild
(
    guild_id bigint primary key
);

CREATE TABLE guild_slot
(
    slot_id   uuid default gen_random_uuid() primary key,
    slot_type slot_type not null,
    guild_id  bigint not null,
    enabled   bool,

    constraint guild_commands_fk FOREIGN KEY (guild_id) REFERENCES guild (guild_id) ON DELETE CASCADE
);

CREATE TABLE custom_command
(
    slot_id               uuid primary key,
    name                  varchar(32)         not null,
    description           varchar(100),
    marketplace_reference uuid,
    type                  custom_command_type not null,
    script_lang           script_lang         not null,
    entrypoint            uuid,

    constraint slot_id_fk FOREIGN KEY (slot_id) REFERENCES guild_slot (slot_id) ON DELETE CASCADE
);

CREATE TABLE script_file
(
    script_id uuid default gen_random_uuid() primary key,
    slot_id   uuid            not null,
    file_name varchar(255)    not null,
    script    text default '' not null,

    constraint slot_id_fk FOREIGN KEY (slot_id) REFERENCES custom_command (slot_id) ON DELETE CASCADE
);

CREATE TABLE command_option
(
    option_id    uuid default gen_random_uuid() primary key,
    name         varchar(32) not null,
    description  varchar(100) not null,
    type         option_type not null,
    constraints  jsonb,
    autocomplete bool,
    entrypoint   uuid
);

CREATE TABLE command_option_join
(
    slot_id   uuid,
    option_id uuid,

    primary key (slot_id, option_id),
    constraint slot_id_fk foreign key (slot_id) references custom_command (slot_id),
    constraint option_id_fk foreign key (option_id) references command_option (option_id)
);

CREATE TABLE option_join
(
    parent_option uuid,
    child_option  uuid,
    primary key (parent_option, child_option),
    constraint parent_option_fk foreign key (parent_option) references command_option (option_id),
    constraint child_option_fk foreign key (child_option) references command_option (option_id)
);


CREATE TABLE auto_responder
(
    slot_id    uuid primary key,
    text       jsonb   not null,
    match_text text[] not null,

    constraint slot_id_fk FOREIGN KEY (slot_id) REFERENCES guild_slot (slot_id) ON DELETE CASCADE
);
