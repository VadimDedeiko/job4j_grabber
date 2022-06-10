create database sql schema;
create table post(
id serial primary key,
name varchar(50),
text varchar(50),
link varchar(50),
created timestamp
)