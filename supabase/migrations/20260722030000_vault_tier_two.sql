-- Tier II: Cass Harrow, the keepers' records, and Ruen Ash (0.10.0).
update public.books set
  version = '0.10.0',
  node_count = 127,
  file_size_bytes = 2110004,
  features = array['Character choice', 'Companion', 'Ability scores', 'Class-gated paths', 'Tolls & bribes', 'Usable items', 'Revealing map', 'Equipment', 'Illustrated scenes']
where id = 'the_sunken_vault';
