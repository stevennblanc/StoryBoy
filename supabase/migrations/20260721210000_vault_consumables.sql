-- The Sunken Vault gains usable items: healing you can carry, buy, and spend
-- (0.7.0). Also fixes a shop selling an item the book never defined.
update public.books set
  version = '0.7.0',
  file_size_bytes = 2102959,
  features = array['Character choice', 'Ability scores', 'Class-gated paths', 'Usable items', 'Revealing map', 'Shop & gold', 'Equipment', 'Illustrated scenes']
where id = 'the_sunken_vault';
