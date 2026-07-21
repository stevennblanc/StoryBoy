-- Tier I of the full Sunken Vault: the lost crew, Odo's ledger, and Sethe
-- Marlow under the lintel (0.9.0).
update public.books set
  version = '0.9.0',
  description = 'Five went into the flooded vault a month ago and none came out. Choose a sellsword, a tomb-robber, or a tide-touched witch, and go down after them.',
  node_count = 110,
  file_size_bytes = 2106447
where id = 'the_sunken_vault';
