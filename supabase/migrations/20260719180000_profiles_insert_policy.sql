-- Allow clients to upsert their own profile row. Android cannot send PATCH
-- through HttpURLConnection, so it updates profiles with an upsert POST,
-- which requires insert permission on the user's own row.
create policy "Users can insert their own profile"
  on public.profiles for insert
  with check ((select auth.uid()) = id);
