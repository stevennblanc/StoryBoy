-- The Sunken Vault gains scene and monster artwork on its nodes (0.5.0).
update public.books set
  version = '0.5.0',
  file_size_bytes = 831769,
  features = array['Illustrated scenes', 'Revealing map', 'Shop & gold', 'Equipment', 'Character stats', '14+ combats']
where id = 'the_sunken_vault';
