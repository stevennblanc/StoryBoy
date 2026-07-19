-- StoryBoy store: catalogue, user profiles, and purchases.
-- Prices are stored in USD; 0 means the book is listed as Free.

-- -------------------------------------------------------------------
-- Books catalogue
-- -------------------------------------------------------------------
create table public.books (
  id text primary key,
  title text not null,
  author text not null,
  genre text,
  description text not null default '',
  about text,
  version text not null,
  price_usd numeric(8,2) not null default 0 check (price_usd >= 0),
  language text not null default 'English',
  publisher text not null default 'StoryBoy',
  published_on date,
  node_count integer,
  ending_count integer,
  file_size_bytes bigint,
  features text[] not null default '{}',
  download_path text not null,
  poster_path text,
  banner_path text,
  is_published boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.books is
  'StoryBoy store catalogue. *_path columns are origin-relative paths under the web host (e.g. /store/foo.gbk).';

alter table public.books enable row level security;

create policy "Anyone can read published books"
  on public.books for select
  using (is_published);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger books_set_updated_at
  before update on public.books
  for each row execute function public.set_updated_at();

-- -------------------------------------------------------------------
-- Profiles (one row per auth user)
-- -------------------------------------------------------------------
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  display_name text,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Users can view their own profile"
  on public.profiles for select
  using ((select auth.uid()) = id);

create policy "Users can update their own profile"
  on public.profiles for update
  using ((select auth.uid()) = id);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'display_name', split_part(new.email, '@', 1))
  );
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- -------------------------------------------------------------------
-- Purchases (the user's store library; free books today, priced later)
-- -------------------------------------------------------------------
create table public.purchases (
  user_id uuid not null references auth.users (id) on delete cascade,
  book_id text not null references public.books (id) on delete cascade,
  price_paid_usd numeric(8,2) not null default 0,
  acquired_at timestamptz not null default now(),
  primary key (user_id, book_id)
);

alter table public.purchases enable row level security;

create policy "Users can view their own purchases"
  on public.purchases for select
  using ((select auth.uid()) = user_id);

-- Client inserts are only allowed for free books; paid checkout would go
-- through a trusted backend path instead of this policy.
create policy "Users can acquire free published books"
  on public.purchases for insert
  with check (
    (select auth.uid()) = user_id
    and price_paid_usd = 0
    and exists (
      select 1 from public.books b
      where b.id = book_id
        and b.is_published
        and b.price_usd = 0
    )
  );

create policy "Users can remove their own purchases"
  on public.purchases for delete
  using ((select auth.uid()) = user_id);
