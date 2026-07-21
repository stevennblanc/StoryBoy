-- Both launch books now open with a prologue and a "How to play" section (a lore
-- node that returns to the prologue), so the rules are front-loaded the way a
-- printed gamebook does it.
update public.books set
  version = '0.5.1',
  node_count = 95,
  file_size_bytes = 1514206
where id = 'the_sunken_vault';

update public.books set
  version = '0.2.1',
  node_count = 12,
  file_size_bytes = 992558
where id = 'ashen_crossroads';
