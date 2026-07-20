-- The Sunken Vault map fragments become clean single-room sketches (0.4.1).
update public.books set
  version = '0.4.1',
  file_size_bytes = 299785
where id = 'the_sunken_vault';
