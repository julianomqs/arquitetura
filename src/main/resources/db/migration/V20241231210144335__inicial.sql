CREATE TABLE product (
  id INT AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE client (
  id INT AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE invoice (
  id INT AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL,
  number VARCHAR(255) NOT NULL,
  client_id INT NOT NULL,
  date_time DATETIME(3) NOT NULL,
  CONSTRAINT fk_invoice__client FOREIGN KEY (client_id) REFERENCES client(id)
);

CREATE TABLE invoice_item (
  id INT AUTO_INCREMENT PRIMARY KEY,
  version INT NOT NULL,
  quantity DECIMAL(19, 4) NOT NULL,
  unit_value DECIMAL(19, 4) NOT NULL,
  product_id INT NOT NULL,
  invoice_id INT NOT NULL,
  CONSTRAINT fk_invoice_item__product FOREIGN KEY (product_id) REFERENCES product(id),
  CONSTRAINT fk_invoice_item__invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id)
);

ALTER TABLE client ADD CONSTRAINT uk_client__name UNIQUE (name);
ALTER TABLE product ADD CONSTRAINT uk_product__name UNIQUE (name);
ALTER TABLE invoice ADD CONSTRAINT uk_invoice__number UNIQUE (number);
