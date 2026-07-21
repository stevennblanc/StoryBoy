-- The Ashen Crossroads replaces its placeholder cover with painted poster and
-- banner art (0.2.2).
update public.books set
  version = '0.2.2',
  file_size_bytes = 1677672
where id = 'ashen_crossroads';
