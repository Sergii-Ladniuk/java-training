DROP DATABASE if EXISTS inventory;
create DATABASE inventory;

DROP TABLE IF EXISTS items  ;
create TABLE items
(
  item_id INT AUTO_INCREMENT NOT NULL ,
  name VARCHAR(255) NOT NULL ,
  price DECIMAL NOT NULL ,
  category_id LONG NOT NULL,
  PRIMARY KEY (item_id)
);

DROP TABLE IF EXISTS categories  ;
create TABLE categories
(
  category_id INT AUTO_INCREMENT NOT NULL ,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (category_id)
);