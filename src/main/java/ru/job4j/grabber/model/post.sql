create database sql schema;
create table post(
id serial primary key,
name varchar(250),
description text,
link varchar(250),
created timestamp
)