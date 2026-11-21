create table store (
  id bigint primary key,
  name text not null
);

create table menu_item (
  id bigint primary key,
  store_id bigint not null references store (id),
  name text not null,
  price bigint not null check (price >= 0)
);

create table menu_option (
  id bigint primary key,
  menu_item_id bigint not null references menu_item (id),
  name text not null,
  extra_price bigint not null check (extra_price >= 0)
);

create table stock (
  menu_item_id bigint primary key references menu_item (id),
  available int not null check (available >= 0),
  version bigint not null default 0
);

create table orders (
  id bigint generated always as identity primary key,
  store_id bigint not null references store (id),
  status text not null,
  total_minor bigint not null,
  idempotency_key text not null unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table order_line (
  id bigint generated always as identity primary key,
  order_id bigint not null references orders (id),
  menu_item_id bigint not null references menu_item (id),
  option_id bigint references menu_option (id),
  quantity int not null,
  unit_price bigint not null,
  option_extra bigint not null
);
create index order_line_order on order_line (order_id);

create table payment (
  id bigint generated always as identity primary key,
  order_id bigint not null unique references orders (id),
  payment_ref text not null unique,
  state text not null,
  amount_minor bigint not null,
  cancelled_minor bigint not null default 0,
  inquiries int not null default 0,
  card_token text not null,
  gateway_tx_id text,
  approved_at timestamptz,
  next_inquiry_at timestamptz,
  updated_at timestamptz not null default now()
);
create index payment_due on payment (next_inquiry_at) where state in ('UNKNOWN', 'HELD');

create table payment_cancel (
  id bigint generated always as identity primary key,
  payment_id bigint not null references payment (id),
  cancel_key text not null unique,
  amount_minor bigint not null,
  gateway_tx_id text,
  cancelled_at timestamptz not null
);

create table payment_conflict (
  id bigint generated always as identity primary key,
  payment_ref text not null,
  code text not null,
  detail text not null,
  created_at timestamptz not null default now()
);

create table idempotency_key (
  scope text not null,
  key text not null,
  request_hash text not null,
  status text not null,
  locked_until timestamptz not null,
  response_status int,
  response_body text,
  created_at timestamptz not null default now(),
  primary key (scope, key)
);

create table outbox (
  id bigint generated always as identity primary key,
  event_id uuid not null unique,
  aggregate_id text not null,
  event_type text not null,
  payload text not null,
  created_at timestamptz not null default now(),
  published_at timestamptz
);
create index outbox_unpublished on outbox (id) where published_at is null;

create table processed_event (
  consumer text not null,
  event_id uuid not null,
  processed_at timestamptz not null default now(),
  primary key (consumer, event_id)
);

create table kitchen_ticket (
  order_id bigint primary key,
  store_id bigint not null,
  status text not null,
  items text not null,
  updated_at timestamptz not null default now()
);

create table notification (
  id bigint generated always as identity primary key,
  event_id uuid not null unique,
  order_id bigint not null,
  message text not null,
  created_at timestamptz not null default now()
);

create table ledger_entry (
  id bigint generated always as identity primary key,
  event_id uuid not null unique,
  store_id bigint not null,
  kind text not null,
  ref text not null,
  amount_minor bigint not null,
  occurred_at timestamptz not null,
  business_date date not null
);
create index ledger_date on ledger_entry (business_date);

create table dead_letter (
  id bigint generated always as identity primary key,
  consumer text not null,
  event_id uuid not null,
  event_type text not null,
  payload text not null,
  error text not null,
  attempts int not null,
  created_at timestamptz not null default now(),
  replayed_at timestamptz
);

create table settlement (
  business_date date not null,
  store_id bigint not null,
  sales_minor bigint not null,
  cancels_minor bigint not null,
  fee_minor bigint not null,
  net_minor bigint not null,
  computed_at timestamptz not null default now(),
  primary key (business_date, store_id)
);

create table reconciliation_report (
  business_date date primary key,
  matched int not null,
  mismatch_count int not null,
  computed_at timestamptz not null default now()
);

create table reconciliation_item (
  id bigint generated always as identity primary key,
  business_date date not null,
  type text not null,
  kind text not null,
  ref text not null,
  ledger_amount bigint,
  provider_amount bigint
);
create index reconciliation_item_date on reconciliation_item (business_date);
