-- Image compression pass: packages re-encoded with JPEG artwork.
-- Versions bump to 0.3.1 and store artwork switches to .jpg paths.

update public.books set
  version = '0.3.1-test',
  file_size_bytes = 2994206,
  poster_path = '/store/the-long-shadow-poster.jpg',
  banner_path = '/store/the-long-shadow-banner.jpg'
where id = 'the_long_shadow';

update public.books set
  version = '0.3.1',
  file_size_bytes = 4954598,
  poster_path = '/store/anya-suitcase-scenic-route-poster.jpg',
  banner_path = '/store/anya-suitcase-scenic-route-banner.jpg'
where id = 'anya_suitcase_scenic_route';
