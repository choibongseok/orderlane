insert into store (id, name) values (1, 'Alder Street Cafe'), (2, 'Pine Corner Bakery');

insert into menu_item (id, store_id, name, price) values
  (1, 1, 'Americano', 4500),
  (2, 1, 'Latte', 5000),
  (3, 1, 'Cold Brew (limited)', 5500),
  (4, 2, 'Croissant', 3800),
  (5, 2, 'Sourdough Loaf (limited)', 9000);

insert into menu_option (id, menu_item_id, name, extra_price) values
  (101, 1, 'Large', 500),
  (102, 1, 'Extra shot', 700),
  (103, 2, 'Large', 500),
  (104, 3, 'Large', 500);

insert into stock (menu_item_id, available) values (1, 200), (2, 200), (3, 1), (4, 100), (5, 1);
