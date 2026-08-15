# SoulCrates

> [Russian version](../../README.md)

SoulCrates is a production-focused crate system built around stability and performance, not just visual effects.

It is designed to stay predictable under real load: critical I/O is async, opening flow is guarded with session/location locks, and reward delivery remains safe even in edge cases. Players get the flashy experience, while server owners get controlled load and fewer support incidents.

What this means in practice:
- opening flow avoids blocking operations on hot paths;
- crate mechanics remain stable under Folia and concurrent interactions;
- lower risk of race-condition style dupe scenarios;
- one operational model for both single-server and multi-server setups.

In short: SoulCrates sells the player experience while giving you stable operations behind the scenes.

---

## Why SoulCrates

- Give whatever you want: after a win the plugin runs any console command — rank, kit, money, permissions, anything.
- Production-oriented optimization: async workflow and careful DB interaction design.
- Reliable opening flow: session/location lock strategy for concurrent safety.
- Folia-ready architecture: region-threading friendly behavior instead of legacy assumptions.
- Multiple crate scenarios: static map points and personal chest items in inventory.
- Scales from single servers to networked infrastructure.

Technical details (Folia model, MySQL/Redis sync, race-safety, Paper `libraries`) are documented below.

## Rewards

Player wins — your console commands run. No caps, no tie-in to specific plugins.

- **Items** — `grants` on the reward (`DIAMOND:3`, `NETHERITE_SWORD:1`)
- **Everything else** — `commands` on the reward: any command with `{player}`, `{uuid}`, `{crate}`, `{reward}`

Examples: `eco give {player} 10000`, `lp user {player} parent set vip`, `kit give {player} start` — whatever you put in config gets executed.

## What server owners get

- Fast setup: install JAR, bind a crate, give keys, done.
- Two placement modes: a static map point or personal chest items issued via `/sc givecrate`.
- ModelEngine 3D crates: custom models and animations instead of a plain chest block.
- Premium opening flow: CS:GO-style roulette, idle animations, holograms above crate points.
- Personal chests and WorldGuard: whitelist regions where players may place their crate (default: `spawn`).
- Split storage model: keys and progress in the database, static map points in a separate YAML you can wipe without resetting economy.
- Operational tooling: config reload, locations list, in-game editor, stats, key/crate issuing.
- When needed: PlaceholderAPI, ModelEngine, WorldGuard, Redis for networks.

## What players get

- Clean interface: open a crate and press one clear `Open` button in preview.
- Two crate formats: a static point on the map (admin bound a block — player walks up and opens) or a personal chest item (received, placed where allowed, opened by the owner).
- WorldGuard support: personal chests can only be placed in whitelisted regions — for example spawn, even when normal building is denied. Outside the whitelist, normal region rules apply.
- Every issued chest has a unique database ID. Fake items or duplicate copies cannot be opened — only the registered instance counts.
- Transparent odds and a polished opening animation.
- Safe reward delivery: if inventory is full, rewards are queued and can be claimed later.

---

## Quick start

1. Put the JAR into `plugins/` and restart the server.
2. The plugin creates `plugins/SoulCrates/` and a default crate `default`.
3. Run:

```text
/sc setcrate default
/sc givekey <player> default 10
```

4. Player right-clicks the bound block to open.
5. Preview can be tested with `/sc preview default`.

---

## Where data is stored

| Path | Content |
|---|---|
| `plugins/SoulCrates/data/crates.db` | Keys, pity, opens, claim, history, internal state |
| `plugins/SoulCrates/data/crate-locations.yml` | Static `/sc setcrate` bindings |
| `plugins/SoulCrates/crates/*.yml` | Crate definitions and rewards |

If you want to wipe only static crate points, remove `crate-locations.yml` and keep the DB intact.

---

## Configuration

On first start the plugin creates `plugins/SoulCrates/` with full configs, defaults, and inline comments for every option. Edit files on disk — a partial YAML snippet in README would only mislead you.

- `config.yml` — database, Redis, holograms, global settings
- `crates/<id>.yml` — crates, rewards, opening, keys, animations
- `shop.yml` — optional key shop
- `gui/*.yml` — menu layouts and slots
- `lang/messages_*.yml` — player-facing text

After edits: `/sc reload`.

For multi-server setups switch the database to MySQL and enable Redis in `config.yml` — one MySQL and a shared channel on every instance.

---

## Commands

- `/sc open [crate] [amount]` - open crate
- `/sc preview [crate]` - open preview
- `/sc keys [crate]` - show key balance
- `/sc virtualkeys` - open virtual keys GUI
- `/sc paykey <player> <crate> <amount>` - transfer virtual keys
- `/sc claim` - claim queued rewards
- `/sc stats [player]` - open stats
- `/sc setcrate <crate>` - bind crate to target block
- `/sc setcrate remove` - unbind crate from target block
- `/sc setnpc <crate>` - bind crate to NPC (Citizens)
- `/sc givekey <player> <crate> [amount] [physical]` - give keys
- `/sc givecrate <player> <crate> [preset] [amount]` - give physical crates
- `/sc editor` - open crate editor
- `/sc locations` - list bound locations
- `/sc reload` - reload configs

Aliases: `sc`, `crates`.

---

## Permissions

- `soulcrates.command.open` - `/sc open`
- `soulcrates.command.preview` - `/sc preview`
- `soulcrates.command.keys` - `/sc keys`
- `soulcrates.command.virtualkeys` - `/sc virtualkeys`
- `soulcrates.command.paykey` - `/sc paykey`
- `soulcrates.command.claim` - `/sc claim`
- `soulcrates.command.givekey` - give keys
- `soulcrates.command.givecrate` - give physical crates
- `soulcrates.command.admin` - crate binding, editor, locations list
- `soulcrates.command.reload` - `/sc reload`
- `soulcrates.open.multi` - multi-open through amount argument

Per-crate access can be configured via `opening.permission` in `crates/<id>.yml`.

---

## PlaceholderAPI

Examples:

- `%soulcrates_keys_<crateId>%`
- `%soulcrates_total_keys_<crateId>%`
- `%soulcrates_pity_<crateId>%`
- `%soulcrates_opens_<crateId>%`

---

## Optional plugins

- PlaceholderAPI — placeholders in menus and holograms
- ModelEngine — 3D crate models
- WorldGuard — whitelist regions for personal chests

Holograms use the built-in engine — no third-party hologram plugin required.
