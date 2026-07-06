create extension if not exists pgcrypto;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'wedding-media',
  'wedding-media',
  false,
  52428800,
  array['image/jpeg', 'image/png', 'image/gif', 'video/mp4', 'video/webm']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create table if not exists public.wedding_gallery_config (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

create table if not exists public.wedding_media (
  id uuid primary key default gen_random_uuid(),
  storage_path text not null unique,
  filename text not null,
  mime_type text not null,
  size_bytes bigint not null default 0,
  event_name text not null default 'Charles & Jessica Hartmann Wedding',
  created_at timestamptz not null default now()
);

alter table public.wedding_gallery_config enable row level security;
alter table public.wedding_media enable row level security;

insert into public.wedding_gallery_config (key, value)
values
  ('gallery_title', 'Charles & Jessica Hartmann'),
  ('gallery_subtitle', 'Wedding Gallery'),
  ('wedding_date', 'August 22, 2026')
on conflict (key) do update set
  value = excluded.value,
  updated_at = now();
