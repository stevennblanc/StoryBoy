-- The Sunken Vault expands to a full 94-node dungeon (0.4.0).
update public.books set
  version = '0.4.0',
  description = 'A full solo delve through a flooded vault: buy and loot gear, sharpen your Wits, map the ruin room by room, and fight your way to the Tide-Priest at its heart - with more than one way to end it.',
  about = 'The full Sunken Vault: a 94-passage dungeon crawl that stress-tests every StoryBoy roleplaying system. Two shops and looted gear tiers, a Wits stat that opens hidden paths, seventeen map fragments that assemble a dungeon map as you explore, more than a dozen combats, luck checks and traps, and multiple endings including a true ending. Buy well, read everything, and keep your head above water.',
  node_count = 94,
  ending_count = 9,
  file_size_bytes = 263187,
  features = array['Revealing map (17 rooms)', 'Two shops & loot', 'Gear & stats', 'Wits checks', '14+ combats', 'Multiple endings']
where id = 'the_sunken_vault';
