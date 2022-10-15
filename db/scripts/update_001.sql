create table if not exists post
(
    id          serial primary key,
    name        varchar(250) not null,
    link        varchar(500) not null unique ,
    description text,
    created     timestamp not null
)