# StoryBoy Store Backend

The store catalogue, user accounts, and purchases live in Supabase (project `StoryBoy`, ref `ndgguqbrhatvcqetgeks`). Static files — `.gbk` packages and artwork — stay on the web host (Vercel) for now because the Supabase free tier caps Storage uploads at 50 MB per file and Anya's package is ~54 MB.

## Tables

All tables are in the `public` schema with row level security enabled. Schema changes are made through SQL migrations in `supabase/migrations/` and applied with `supabase db push` (the linked CLI needs no database password).

### `books`

The store catalogue; source of truth for the web store. One row per gamebook.

Key columns:

- `id` — the stable book slug (matches `metadata.folder`), e.g. `the_long_shadow`
- `title`, `author`, `genre`, `description` (short), `about` (long, detail page)
- `version` — must match the packaged `story.json` version so update checks work
- `price_usd` — `0` renders as **Free**; a nonzero value renders as a price. No payment flow exists yet; the value is stored so charging later is only a policy/UI change
- `language`, `publisher`, `published_on`
- `node_count` ("passages"), `ending_count`, `file_size_bytes` — detail page stats
- `features` — text array rendered as chips, e.g. `{Memories, Puzzles, "14 endings"}`
- `download_path`, `poster_path`, `banner_path` — **origin-relative** paths (`/store/...`) resolved against the web host
- `is_published` — unpublished rows are invisible to clients (RLS)

Read access: anyone (anon) can read published rows. Writes: dashboard/service role only.

### `profiles`

One row per auth user, created automatically by the `on_auth_user_created` trigger. Holds `display_name`. Users can read and update only their own row.

### `purchases`

The user's store library. Primary key `(user_id, book_id)`, with `price_paid_usd` and `acquired_at`. RLS lets a signed-in user read, insert, and delete only their own rows, and the insert policy only accepts **free, published** books at `price_paid_usd = 0` — a future paid checkout must go through a trusted backend path instead.

## Auth

Email + password via Supabase Auth. The web app's Account tab handles sign up / sign in / sign out. New signups receive a confirmation email (Supabase's built-in sender, low rate limits). For frictionless testing, "Confirm email" can be disabled under Authentication → Sign In / Providers in the Supabase dashboard; configure custom SMTP before real users.

## Web client

`web/app.js` loads `@supabase/supabase-js` from a CDN and uses the **publishable** key (`sb_publishable_...`), which is safe to embed client-side. The secret/service keys must never appear in the repo or client code.

- Store list and detail pages read from `books`
- "Get" inserts into `purchases`; the Library view shows owned books plus any book with local reading progress
- Reading progress remains in browser `localStorage` (not yet synced)
- If Supabase is unreachable, the store falls back to `web/store/store-index.json`, so local offline dev still works

## Android

The Android app still reads `store/store-index.json` (absolute URLs). Migrating it to the Supabase catalogue (and adding auth) is future work; keep the JSON updated when books change until then.

## Updating the catalogue

For content changes (new book, new version), add a migration that upserts the row — see `supabase/migrations/20260719120100_seed_books.sql` for the pattern — then run `supabase db push`. Keep `version`, `file_size_bytes`, and the packaged `story.json` in sync.
