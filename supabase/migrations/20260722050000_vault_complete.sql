-- The Sunken Vault reaches 1.0.0: Ismene, the post, and the scene at the mouth.
update public.books set
  version = '1.0.0',
  description = 'Five went into the flooded vault a month ago and none came out. Choose a sellsword, a tomb-robber, or a tide-touched witch, go down after them, and find out what the vault-keepers drowned themselves to hold.',
  node_count = 141,
  ending_count = 14,
  file_size_bytes = 2114015,
  features = array['Character choice', 'Companion', 'Branching endings', 'Ability scores', 'Class-gated paths', 'Tolls & bribes', 'Usable items', 'Revealing map', 'Equipment', 'Illustrated scenes']
where id = 'the_sunken_vault';
