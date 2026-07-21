-- Prose promised gold the 0.8.0 economy pass had already removed (1.0.1).
update public.books set version = '1.0.1', file_size_bytes = 2114157
where id = 'the_sunken_vault';
