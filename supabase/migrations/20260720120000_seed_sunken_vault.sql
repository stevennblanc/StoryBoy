-- Seed the Sunken Vault test adventure: an original dungeon crawl that
-- exercises character stats, the check node, and combat.
insert into public.books (
  id, title, author, genre, description, about, version, price_usd,
  language, publisher, published_on, node_count, ending_count,
  file_size_bytes, features, download_path, poster_path, banner_path
) values (
  'the_sunken_vault',
  'The Sunken Vault',
  'StoryBoy',
  'Dungeon Crawl',
  'A short solo delve that teaches StoryBoy''s adventure systems: character stats, a luck check, and a real fight where your health matters.',
  'A compact test dungeon for StoryBoy''s roleplaying systems. You track Health and Armor Class, risk a luck check on a crumbling bridge, and face the gloamworm in round-based combat where a bad roll can end your delve. Built to prove the engine handles stats, checks, and combat before larger adventures use them.',
  '0.1.0',
  0,
  'English',
  'StoryBoy',
  '2026-07-20',
  10,
  3,
  119807,
  array['Character stats', 'Luck checks', 'Combat', 'Health & death'],
  '/store/the-sunken-vault.gbk',
  '/store/the-sunken-vault-poster.jpg',
  '/store/the-sunken-vault-banner.jpg'
)
on conflict (id) do update set
  title = excluded.title,
  author = excluded.author,
  genre = excluded.genre,
  description = excluded.description,
  about = excluded.about,
  version = excluded.version,
  node_count = excluded.node_count,
  ending_count = excluded.ending_count,
  file_size_bytes = excluded.file_size_bytes,
  features = excluded.features,
  download_path = excluded.download_path,
  poster_path = excluded.poster_path,
  banner_path = excluded.banner_path;
