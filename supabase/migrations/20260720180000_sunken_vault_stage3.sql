-- The Sunken Vault gains a revealing map in stage 3.
update public.books set
  version = '0.3.0',
  description = 'A short solo delve that teaches every StoryBoy adventure system: stats, a shop and equipment, a luck check, real combat, and a map that reveals itself as you explore.',
  about = 'A compact test dungeon for StoryBoy''s roleplaying systems. Spend gold at a supply stall, equip a weapon and armor that change your stats and combat, risk a luck check on a crumbling bridge, face the gloamworm in round-based combat, and watch a dungeon map fill in room by room as you explore. Built to prove the engine handles stats, shops, equipment, checks, combat, and a revealing map before larger adventures use them.',
  file_size_bytes = 156249,
  features = array['Revealing map', 'Shop & gold', 'Equipment', 'Character stats', 'Combat & death']
where id = 'the_sunken_vault';
