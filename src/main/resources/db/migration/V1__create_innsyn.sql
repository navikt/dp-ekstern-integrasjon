create table innsyn
(
    id         serial primary key,
    innsyn_ref uuid                                   not null,
    created_on timestamp with time zone default now() not null,
    created_by text                                   not null,
    data       json                                   null,
    status     text                                   null
)