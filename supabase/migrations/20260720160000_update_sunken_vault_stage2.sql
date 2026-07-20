-- The Sunken Vault gains a shop, equipment, and gold in stage 2.
update public.books set
  version = '0.2.0',
  description = 'A short solo delve that teaches StoryBoy''s adventure systems: stats, a shop and equipment, a luck check, and a real fight where gear and health matter.',
  about = 'A compact test dungeon for StoryBoy''s roleplaying systems. Spend gold at a supply stall, equip a weapon and armor that change your stats and combat, risk a luck check on a crumbling bridge, and face the gloamworm in round-based combat where being unarmored gets you killed. Built to prove the engine handles stats, shops, equipment, checks, and combat before larger adventures use them.',
  node_count = 11,
  ending_count = 3,
  file_size_bytes = 120268,
  features = array['Shop & gold', 'Equipment', 'Character stats', 'Luck checks', 'Combat & death']
where id = 'the_sunken_vault';
