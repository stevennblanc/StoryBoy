-- Seed The Ashen Crossroads: an original character-choice demo for the
-- ability-score and character-selection systems.
insert into public.books (
  id, title, author, genre, description, about, version, price_usd,
  language, publisher, published_on, node_count, ending_count,
  file_size_bytes, features, download_path, poster_path, banner_path
) values (
  'ashen_crossroads',
  'The Ashen Crossroads',
  'StoryBoy',
  'Fantasy',
  'Choose a warrior, a ranger, or a hedge-witch, then face the ash-hound at the warded gate - your ability scores decide which approach works best.',
  'A short demo of StoryBoy''s character systems. Pick one of three pre-made protagonists, each with their own ability scores and starting gear, then meet the ash-hound at a warded gate. The warrior''s Strength, the ranger''s Dexterity, and the witch''s Intellect each make a different approach shine - the story meets you as the character you chose. Built to show author-defined ability scores (score to modifier) and character selection.',
  '0.1.0',
  0,
  'English',
  'StoryBoy',
  '2026-07-20',
  9,
  2,
  83761,
  array['Character choice', 'Ability scores', 'Stat-driven combat', 'Skill checks'],
  '/store/the-ashen-crossroads.gbk',
  '/store/the-ashen-crossroads-poster.jpg',
  '/store/the-ashen-crossroads-banner.jpg'
)
on conflict (id) do update set
  title = excluded.title, author = excluded.author, genre = excluded.genre,
  description = excluded.description, about = excluded.about, version = excluded.version,
  node_count = excluded.node_count, ending_count = excluded.ending_count,
  file_size_bytes = excluded.file_size_bytes, features = excluded.features,
  download_path = excluded.download_path, poster_path = excluded.poster_path, banner_path = excluded.banner_path;
