-- Final artwork pass for The Sunken Vault (0.5.0): illustrated covers plus
-- scene and monster art on nodes. Corrects the packaged file size.
update public.books set
  file_size_bytes = 1513437
where id = 'the_sunken_vault';
