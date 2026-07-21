-- The Sunken Vault's economy moves from shopping to tolls and bribes (0.8.0):
-- one trader at the mouth, all gear discoverable, and gold spent on passage.
update public.books set
  version = '0.8.0',
  description = 'Choose a sellsword, a tomb-robber, or a tide-touched witch, then delve a flooded vault: outfit yourself at the mouth, find your gear in the dark, and spend what you scavenge buying your way past the things that will not be fought.',
  node_count = 97,
  file_size_bytes = 2103274,
  features = array['Character choice', 'Ability scores', 'Class-gated paths', 'Tolls & bribes', 'Usable items', 'Revealing map', 'Equipment', 'Illustrated scenes']
where id = 'the_sunken_vault';
