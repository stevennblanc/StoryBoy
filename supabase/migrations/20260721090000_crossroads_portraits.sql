-- The Ashen Crossroads gains character portraits and class-gated content (0.2.0).
update public.books set
  version = '0.2.0',
  description = 'Choose a warrior, a ranger, or a hedge-witch, then face the ash-hound at the warded gate - your ability scores and your class decide which paths open.',
  node_count = 10,
  file_size_bytes = 992053,
  features = array['Character choice', 'Ability scores', 'Class-gated paths', 'Stat-driven combat']
where id = 'ashen_crossroads';
