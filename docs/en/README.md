# SoulCrates

> [Русская версия](../../README.md)

**Crates without dupes: Folia, premium-style animation, pity, reroll, MySQL+Redis network — single JAR, no shade.**

SoulCrates is a crate plugin for Paper **1.21+** and **Folia**, built for fair weighted rolls, polished opening UX, and safety on region threads and proxies.

Single JAR: dependencies via Paper `libraries`, no shade. Fewer conflicts with other plugins and easier updates.

## Why admins choose it

**Folia first-class** — region-aware threading, no `BukkitScheduler` on hot paths. For Folia networks this is baseline, not an optional extra.

**Multiple crate types** — `crates/<id>.yml`: own rewards, keys, pity, reroll, animation, display engine. No config copy-paste and no plugin zoo.

**Multi-server** — **MySQL** + **Redis pub/sub**: virtual keys and pity cache stay in sync across instances. SQL is the source of truth; Redis mirrors and invalidates, not “whoever wrote the file last”.

**Anti-dupe** — opening session lock per player, bulk-open under one lock, key consume before animation starts. GUI routing only via custom `InventoryHolder`, never by inventory title.

**Players get a proper experience:** preview, confirm, CSGO-style spinner, Vault-paid reroll, multi-open x5/x10, key shop. Messages use MiniMessage, HEX, gradients; texts live in `lang/messages_*.yml`.

**World and NPCs:** bind crates to blocks (`/sc setcrate`), idle models/displays, holograms (TextDisplay / DecentHolograms), Citizens NPCs (`/sc setnpc`). Shift+RMB — preview, RMB — open.

**Operations:** in-game reward editor, `/sc stats`, `/sc locations`, broadcast for rare wins, PlaceholderAPI, phase API events. `/sc reload` — config, lang, GUI yml, crates.

**Built for:** Paper/Folia networks with donation/event crates, proxy with shared DB, where anti-dupe and key sync matter more than “one more GUI button”.

## Network and storage (short)

| Mode | Purpose |
|------|---------|
| `SQLITE` | single server, quick start |
| `MYSQL` | persistence; **proxy foundation** |
| Redis + pub/sub | virtual keys + pity mirror across instances |

On a proxy: **MYSQL + `redis.enabled: true`**. Default channel — `soulcrates:sync`. Publishes on key/pity writes; subscriber refreshes local cache without echoing the same server.

## Features (full list)

### Platform

- Paper **1.21+** and **Folia** (`folia-supported: true`), `PluginSchedulers` for all world/inventory mutations.
- Single JAR, dependencies via Paper **`libraries`** (Elytrium Serializer, HikariCP, MySQL/SQLite, Jedis, Gson) — **no shade**.
- Async DB bootstrap: migrations and preload do not block region/global ticks.
- Typed config (Elytrium): defaults in Java; fresh install works without hand-editing YAML.

### Crates and rewards

- One YAML per type: `plugins/SoulCrates/crates/<id>.yml`.
- Reward pool: **weight**, preview icon, `grants` (`MATERIAL:amount`, `vault:100`), console `commands` with `{player}`, `{crate}`, `{reward}`.
- **Pity:** counter of opens without pity reward → guaranteed `rewardId`.
- **Broadcast:** `broadcast: true` on reward → server-wide message on claim.
- Per-crate: cooldown, open permission, preview/confirm, multi-open.

### Keys

- **Virtual** — stored in DB, in-memory cache, `/sc givekey`, PlaceholderAPI `%soulcrates_keys_<crate>%`.
- **Physical** — PDC item, custom model data, consumed from inventory on open.
- **Shop** — `/sc shop`, `shop.yml` + `gui/shop.yml`: Vault and/or item cost for key bundles.

### Opening and animation

- 3-phase pipeline: **key insert → CSGO spinner → firework reveal** (types and duration in crate `animations`).
- **Premium opening** (`config.yml` → `premiumOpening`):
  - `soulcrates.open.skip` — skip animation, reroll per crate rules;
  - `soulcrates.open.instant` — instant, no display;
  - `soulcrates.open.multi` — `/sc open <crate> <amount>`, x5/x10 buttons in preview.
- **Reroll** — GUI after animation: free/paid rolls, Vault cost; per-crate skip (`skipOnInstantOpen`, `skipOnSkipAnimation`, `skipOnMultiOpen`) + global flags.
- **Bulk open** — sequential pity rolls, summary chat message, no reroll menu.
- API events: `CrateOpenStartEvent`, `CrateOpenPhaseStartEvent`, `CrateOpenPhaseEndEvent`, `CrateOpenFinishEvent`.

### World display

- **Engines:** `VANILLA_BLOCK`, `VANILLA_DISPLAY`, **ModelEngine** (`engine.type`, `modelId`, idle/close animations).
- **Idle display** — model/display on bound block, ambient particles, respawn on chunk load.
- **Holograms** — `idleDisplay.hologram`: VANILLA TextDisplay, **DecentHolograms** (reflection), FancyHolograms hook stub; `{crate}`, `{crate_id}` in lines.
- **Block binding** — `/sc setcrate <crate>`, `/sc setcrate remove`; interact sound from config.

### NPCs and integrations

- **Citizens** — `/sc setnpc <crate>`, `/sc setnpc remove`; click → open, Shift → preview.
- **Vault** — reroll economy and key shop.
- **PlaceholderAPI** — `%soulcrates_*%` (see below).
- **ModelEngine**, **DecentHolograms**, **Citizens** — softdepend, reflection where possible.

### Admin and data

- **`/sc editor`** — crate list, GUI editing (rewards: weight, broadcast, pity, grant from hand).
- **`/sc givekey`**, **`/sc keys`**, **`/sc stats [player]`**, **`/sc locations`**.
- **`/sc reload`** — config, lang, gui, crates, idle/hologram respawn.
- SQL: virtual keys, pity, opens, last reward, locations, npc bindings.

## Config layout

After first start in `plugins/SoulCrates/`:

| Path | Content |
|------|---------|
| `config.yml` | DB, Redis, session timeout, idle display, broadcast, premium opening, shop toggle, aliases |
| `crates/*.yml` | Crate definitions (engine, animations, opening, keys, reroll, pity, rewards) |
| `shop.yml` | Key shop entries (crate, amount, vault/item price) |
| `gui/*.yml` | GUI slots and materials (preview, confirm, spinner, reroll, editor, shop) |
| `lang/messages_*.yml` | MiniMessage texts (ru + en bundled in JAR) |
| `data/crates.db` | SQLite when `database.mode: SQLITE` |

### `config.yml` — essentials

```yaml
defaultCrateId: default
cratesDirectory: crates
sessionTimeoutSeconds: 120

database:
  mode: SQLITE          # or MYSQL
  sqliteFile: data/crates.db

redis:
  enabled: false        # true on proxy with MYSQL
  host: 127.0.0.1
  channel: soulcrates:sync

premiumOpening:
  maxMultiOpen: 10
  instantSkipsReroll: true

idleDisplay:
  hologram:
    provider: VANILLA    # DECENT_HOLOGRAMS
```

See the Russian README for a full crate YAML example.

## Messages (`plugins/SoulCrates/lang/`)

- `prefix` — prefix for lines using `{prefix}`.
- Keys use **MiniMessage**; ru and en ship in the JAR with merge on new keys.
- Placeholders: `{crate}`, `{reward}`, `{player}`, `{amount}`, `{npc}`, `{seconds}`, etc.

## PlaceholderAPI

Identifier — `soulcrates`, format `%soulcrates_<param>%`. Auto-registers when PlaceholderAPI is present.

| Placeholder | Value |
|---|---|
| `%soulcrates_active_session%` | `true` / `false` — player in opening session |
| `%soulcrates_keys_<crateId>%` | Virtual keys |
| `%soulcrates_physical_keys_<crateId>%` | Physical keys in inventory (online) |
| `%soulcrates_total_keys_<crateId>%` | Virtual + physical |
| `%soulcrates_opens_<crateId>%` | Open counter |
| `%soulcrates_pity_<crateId>%` | Current pity counter |
| `%soulcrates_last_reward_<crateId>%` | Last won reward id |

## Commands

Aliases: `sc`, `crates` (`config.yml` → `commandAliases`).

### Players

| Command | Action |
|---------|--------|
| `/sc open [crate] [amount]` | Open crate (amount with `soulcrates.open.multi`) |
| `/sc preview [crate]` | Preview GUI |
| `/sc shop` | Key shop |
| `/sc keys [crate]` | Show keys |
| `/sc stats [player]` | Opens/pity stats (others — admin permission) |

### Admins

| Command | Action |
|---------|--------|
| `/sc editor` | In-game crate editor |
| `/sc givekey <player> <crate> [amount] [physical]` | Give keys |
| `/sc setcrate <crate>` | Bind crate to block (look 5 blocks) |
| `/sc setcrate remove` | Unbind block |
| `/sc setnpc <crate>` | Bind Citizens NPC (look 5 blocks) |
| `/sc setnpc remove` | Unbind NPC |
| `/sc locations` | List bound blocks |
| `/sc reload` | Reload configs |

## Permissions

| Permission | Purpose |
|------------|---------|
| `soulcrates.command.use` | Base `/sc` |
| `soulcrates.command.open` | `/sc open` |
| `soulcrates.command.preview` | `/sc preview` |
| `soulcrates.command.shop` | `/sc shop` |
| `soulcrates.command.keys` | `/sc keys` |
| `soulcrates.command.admin` | editor, setcrate, setnpc, locations |
| `soulcrates.command.reload` | `/sc reload` |
| `soulcrates.command.givekey` | `/sc givekey` |
| `soulcrates.command.stats.others` | `/sc stats <player>` |
| `soulcrates.open.skip` | Skip animation |
| `soulcrates.open.instant` | Instant open |
| `soulcrates.open.multi` | Multi-open and bulk buttons |

Per-crate permission — `opening.permission` in `crates/<id>.yml` (empty = `soulcrates.command.open` is enough).

## Developer API

```java
SoulCrates plugin = (SoulCrates) Bukkit.getPluginManager().getPlugin("SoulCrates");
SoulCratesApi api = plugin.core().api();

api.isLoaded();
api.giveVirtualKeys(playerId, "default", 5);
api.beginOpen(player, "default", location, 1);
api.openPreview(player, "default");
api.pity(playerId, "default");
```

Events in `bm.b0b0b0.soulCrates.api.event` — hook phases and finish for quests/stats.

## Build

```bash
./gradlew build
```

JAR: `build/libs/SoulCrates-*.jar`. ModelEngine is `compileOnly` from `libs/ModelEngine-*.jar` (not in repo — add locally to build).

## Softdepend

Vault, PlaceholderAPI, ModelEngine, ItemsAdder, **Citizens**, **DecentHolograms**, FancyHolograms — optional; missing plugins disable related features gracefully.
