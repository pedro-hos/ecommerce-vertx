create table if not exists product (
	id bigint not null auto_increment, 
	name varchar(100) not null, 
	price float(10) not null, 
	stock int not null, 
	primary key (id)
);