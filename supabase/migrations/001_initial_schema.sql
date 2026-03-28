-- ============================================================
-- Hobbeast – Complete Supabase schema migration
-- Run in Supabase SQL editor or via supabase db push
-- ============================================================

-- ─── Extensions ──────────────────────────────────────────────────────────────
create extension if not exists "uuid-ossp";
create extension if not exists "pg_trgm";   -- for full-text search
create extension if not exists "postgis";   -- for geo queries (optional)

-- ─── Profiles ────────────────────────────────────────────────────────────────
create table if not exists profiles (
  id                  uuid references auth.users primary key,
  email               text,
  display_name        text not null default '',
  bio                 text,
  avatar_url          text,
  interests           text[] default '{}',
  location            text,
  latitude            double precision,
  longitude           double precision,
  distance_km         int not null default 25,
  location_sharing    boolean not null default true,
  is_organizer        boolean not null default false,
  organizer_verified  boolean not null default false,
  profile_visibility  text not null default 'public' check (profile_visibility in ('public','friends','private')),
  push_token          text,
  push_token_updated_at timestamptz,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now()
);

-- Auto-create profile on signup
create or replace function handle_new_user()
returns trigger language plpgsql security definer as $$
begin
  insert into profiles (id, email, display_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data->>'display_name', ''));
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure handle_new_user();

-- ─── Venues ───────────────────────────────────────────────────────────────────
create table if not exists venues (
  id                    uuid primary key default uuid_generate_v4(),
  name                  text not null,
  description           text,
  address               text not null default '',
  latitude              double precision not null,
  longitude             double precision not null,
  image_url             text,
  category              text not null default '',
  tags                  text[] default '{}',
  phone                 text,
  website               text,
  opening_hours         text,
  rating                double precision,
  is_partner            boolean not null default false,
  partner_capabilities  text[] default '{}',
  provider_id           text,
  source                text not null default 'hobbeast',
  created_at            timestamptz not null default now()
);

-- ─── Events ───────────────────────────────────────────────────────────────────
create table if not exists events (
  id                    uuid primary key default uuid_generate_v4(),
  title                 text not null,
  description           text not null default '',
  start_time            timestamptz not null,
  end_time              timestamptz,
  location              text not null default '',
  address               text,
  latitude              double precision,
  longitude             double precision,
  organizer_id          uuid references profiles(id) on delete set null,
  organizer_name        text,
  image_url             text,
  category              text not null default '',
  tags                  text[] default '{}',
  max_capacity          int,
  attendee_count        int not null default 0,
  is_private            boolean not null default false,
  is_free               boolean not null default true,
  price                 double precision,
  source                text not null default 'hobbeast',
  external_id           text,
  external_url          text,
  venue_id              uuid references venues(id) on delete set null,
  trip_plan_id          uuid,
  is_trending           boolean not null default false,
  is_featured           boolean not null default false,
  is_early_access       boolean not null default false,
  community_pulse_score double precision,
  created_at            timestamptz not null default now(),
  updated_at            timestamptz not null default now()
);

-- Full-text search index
create index if not exists idx_events_title_trgm on events using gin (title gin_trgm_ops);
create index if not exists idx_events_category on events (category);
create index if not exists idx_events_start_time on events (start_time);
create index if not exists idx_events_organizer on events (organizer_id);
create index if not exists idx_events_source on events (source);

-- ─── Ticket Tiers ────────────────────────────────────────────────────────────
create table if not exists ticket_tiers (
  id          uuid primary key default uuid_generate_v4(),
  event_id    uuid references events(id) on delete cascade,
  name        text not null,
  price       double precision not null default 0,
  currency    text not null default 'HUF',
  available   int,
  sold        int not null default 0
);

-- ─── Attendees ────────────────────────────────────────────────────────────────
create table if not exists attendees (
  id              uuid primary key default uuid_generate_v4(),
  event_id        uuid references events(id) on delete cascade,
  user_id         uuid references profiles(id) on delete cascade,
  user_name       text not null default '',
  state           text not null default 'going'
                    check (state in ('interested','going','waitlisted','checked_in','declined','none')),
  ticket_tier_id  uuid references ticket_tiers(id) on delete set null,
  invite_code     text unique,
  checked_in_at   timestamptz,
  joined_at       timestamptz not null default now(),
  unique (event_id, user_id)
);

create index if not exists idx_attendees_event on attendees (event_id);
create index if not exists idx_attendees_user on attendees (user_id);
create index if not exists idx_attendees_state on attendees (event_id, state);

-- Auto-update attendee_count
create or replace function update_attendee_count()
returns trigger language plpgsql as $$
begin
  update events
  set attendee_count = (
    select count(*) from attendees
    where event_id = coalesce(new.event_id, old.event_id)
    and state in ('going', 'checked_in')
  )
  where id = coalesce(new.event_id, old.event_id);
  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_attendee_count on attendees;
create trigger trg_attendee_count
  after insert or update or delete on attendees
  for each row execute procedure update_attendee_count();

-- ─── Organizer Messages ───────────────────────────────────────────────────────
create table if not exists organizer_messages (
  id            uuid primary key default uuid_generate_v4(),
  event_id      uuid references events(id) on delete cascade,
  type          text not null default 'general'
                  check (type in ('reminder','logistics_update','event_update','cancellation','general')),
  subject       text not null default '',
  body          text not null default '',
  target_states text[] default '{}',
  scheduled_at  timestamptz,
  sent_at       timestamptz,
  status        text not null default 'draft'
                  check (status in ('draft','scheduled','sent','failed')),
  created_at    timestamptz not null default now()
);

-- ─── Trip Plans ───────────────────────────────────────────────────────────────
create table if not exists trip_plans (
  id          uuid primary key default uuid_generate_v4(),
  event_id    uuid references events(id) on delete cascade,
  title       text not null default '',
  start_point jsonb not null default '{}',
  end_point   jsonb not null default '{}',
  waypoints   jsonb not null default '[]',
  route_type  text not null default 'car',
  distance    double precision,
  duration    int,
  geometry    text,
  created_at  timestamptz not null default now()
);

-- ─── Event Views (analytics) ──────────────────────────────────────────────────
create table if not exists event_views (
  event_id        uuid references events(id) on delete cascade,
  user_id         uuid references profiles(id) on delete cascade,
  source          text not null default 'organic',
  view_count      int not null default 1,
  last_viewed_at  timestamptz not null default now(),
  primary key (event_id, user_id)
);

-- ─── Event Analytics (materialized view) ─────────────────────────────────────
create or replace view event_analytics as
select
  e.id                                          as event_id,
  count(distinct v.user_id)                     as unique_viewers,
  sum(v.view_count)                             as total_views,
  count(distinct v.user_id)                     as detail_opens,
  count(distinct a.id) filter (where a.state = 'going')       as going_count,
  count(distinct a.id) filter (where a.state = 'interested')  as interested_count,
  count(distinct a.id) filter (where a.state = 'waitlisted')  as waitlisted_count,
  count(distinct a.id) filter (where a.state = 'checked_in')  as checked_in_count,
  case
    when count(distinct v.user_id) = 0 then 0
    else round(
      count(distinct a.id) filter (where a.state in ('going','checked_in'))::numeric /
      count(distinct v.user_id) * 100, 2
    )
  end                                           as conversion_rate
from events e
left join event_views v on v.event_id = e.id
left join attendees a   on a.event_id = e.id
group by e.id;

-- ─── Community Pulse (hub intelligence) ──────────────────────────────────────
create table if not exists community_pulse (
  id               uuid primary key default uuid_generate_v4(),
  scene_label      text not null,
  category         text not null default '',
  activity_score   double precision not null default 0,
  recurring_formats text[] default '{}',
  suggested_events  text[] default '{}',
  is_underserved   boolean not null default false,
  latitude         double precision,
  longitude        double precision,
  computed_at      timestamptz not null default now()
);

create index if not exists idx_pulse_category on community_pulse (category);

-- ─── Row Level Security ───────────────────────────────────────────────────────
alter table profiles         enable row level security;
alter table events           enable row level security;
alter table attendees        enable row level security;
alter table organizer_messages enable row level security;
alter table trip_plans       enable row level security;
alter table ticket_tiers     enable row level security;
alter table venues           enable row level security;

-- Profiles: users can read public profiles, edit own
create policy "Public profiles are viewable" on profiles for select using (profile_visibility = 'public');
create policy "Users can view own profile" on profiles for select using (auth.uid() = id);
create policy "Users can update own profile" on profiles for update using (auth.uid() = id);

-- Events: public events readable by all authenticated users
create policy "Authenticated users can read public events" on events
  for select using (auth.role() = 'authenticated' and (not is_private or organizer_id = auth.uid()));
create policy "Organizers can insert events" on events for insert with check (auth.uid() = organizer_id);
create policy "Organizers can update own events" on events for update using (auth.uid() = organizer_id);
create policy "Organizers can delete own events" on events for delete using (auth.uid() = organizer_id);

-- Attendees: users manage own, organizers read all for their events
create policy "Users can read own attendances" on attendees for select using (auth.uid() = user_id);
create policy "Organizers can read event attendees" on attendees for select
  using (exists (select 1 from events e where e.id = event_id and e.organizer_id = auth.uid()));
create policy "Users can manage own attendance" on attendees
  for all using (auth.uid() = user_id);

-- Trip plans: own only
create policy "Users manage own trip plans" on trip_plans for all using (
  exists (select 1 from events e where e.id = event_id and e.organizer_id = auth.uid())
);

-- Venues: readable by all authenticated
create policy "Venues readable by authenticated" on venues for select using (auth.role() = 'authenticated');

-- Community pulse: readable by all
alter table community_pulse enable row level security;
create policy "Pulse readable by authenticated" on community_pulse for select using (auth.role() = 'authenticated');
