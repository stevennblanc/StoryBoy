-- The Sunken Vault gains three playable delvers with portraits and class-gated
-- routes through the vault (0.6.0).
update public.books set
  version = '0.6.0',
  description = 'Choose a sellsword, a tomb-robber, or a tide-touched witch, then delve a flooded vault: buy and loot gear, map the ruin room by room, and fight to the Tide-Priest at its heart - with paths only your delver can walk.',
  node_count = 95,
  file_size_bytes = 2102589,
  features = array['Character choice', 'Ability scores', 'Class-gated paths', 'Revealing map', 'Shop & gold', 'Equipment', 'Illustrated scenes']
where id = 'the_sunken_vault';
