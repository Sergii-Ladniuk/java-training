DROP DATABASE if EXISTS inventory;
create DATABASE inventory;

USE inventory;
DROP TABLE IF EXISTS items  ;
create TABLE items
(
  item_id INT AUTO_INCREMENT NOT NULL ,
  name VARCHAR(255) NOT NULL ,
  price DECIMAL NOT NULL ,
  PRIMARY KEY (item_id)
);

DROP TABLE IF EXISTS categories  ;
create TABLE categories
(
  category_id INT AUTO_INCREMENT NOT NULL ,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (category_id)
);

DROP TABLE IF EXISTS items_categories  ;
CREATE TABLE `items_categories` (
  `category_id` INT NOT NULL,
  `item_id` INT NOT NULL,
  PRIMARY KEY (`category_id`,`item_id`),
  KEY `itm_idx` (`item_id`),
  CONSTRAINT `cat` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `itm` FOREIGN KEY (`item_id`) REFERENCES `items` (`item_id`) ON DELETE CASCADE ON UPDATE CASCADE
);
