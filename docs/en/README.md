# SoulCrates

> [Russian version](../../README.md)

Crates for Paper **1.21+** / **Folia**: keys, pity, reroll, SELECT mode, idle effects, MySQL+Redis for networks.

---

## Quick start

1. Drop the JAR into `plugins/`, restart the server.
2. The plugin creates `plugins/SoulCrates/` and a **`default`** crate automatically.
3. In-game (OP or permissions below):

```
/sc setcrate default
/sc givekey <name> default 10
```

4. Look at the block → **RMB** to open, **Shift+RMB** for preview.  
   Or: `/sc open default`

Done — one working crate on a block.

---

## New crate

1. Copy `plugins/SoulCrates/crates/default.yml` → `donate.yml`
2. Set `id: donate` (must match the filename) and `displayName`
3. `/sc reload`
4. `/sc setcrate donate` + `/sc givekey <name> donate 10`

In-game reward editor: `/sc editor` (existing crates only).

---

## File layout

| Path | Purpose |
|------|---------|
| `config.yml` | DB, Redis, holograms, premium permissions |
| `crates/<id>.yml` | Crate: rewards, keys, animation, opening |
| `shop.yml` + `gui/shop.yml` | Key shop (`/sc shop`) |
| `gui/*.yml` | Menu slots (preview, select, reroll…) |
| `lang/messages_*.yml` | Player messages (MiniMessage) |

After editing YAML: **`/sc reload`**.

---

## `config.yml` — essentials

```yaml
defaultCrateId: default

database:
  mode: SQLITE          # MYSQL for proxy/network

redis:
  enabled: false        # true + MYSQL on proxy

idleDisplay:
  enabled: true
  hologram:
    enabled: true
    lines:
      - "<gold>{crate}</gold>"
      - "<gray>RMB open · Shift preview</gray>"
```

**Proxy:** `database.mode: MYSQL` + `redis.enabled: true`, same channel `soulcrates:sync` on every server.

---

## `crates/<id>.yml` — skeleton

```yaml
id: donate
displayName: "<gold>Donate</gold>"

engine:
  type: VANILLA_DISPLAY
  blockMaterial: ENDER_CHEST

opening:
  requireKey: true
  previewEnabled: true
  keysRequired: 1
  rewardsMode: RANDOM       # SELECT — player picks reward in a menu
  openCost:
    enabled: false
    vaultPrice: 500.0
    keysFirst: true

keys:
  enabled: true
  virtualKeys: true
  physicalKeys: true

animations:
  preset: CLASSIC           # BLAZING, KEYSTORM, CSGO_STYLE, FIREWORKS…

idleEffects:
  - pattern: DEFAULT
    particle: REDSTONE
    color: "#ff0000"
    amount: 2

rewards:
  - id: common
    weight: 80
    displayName: "1000$"
    material: GOLD_INGOT
    grants: ["vault:1000"]
  - id: rare
    weight: 20
    displayName: "Sword"
    material: DIAMOND_SWORD
    grants: ["DIAMOND_SWORD:1"]
    broadcast: true
```

**grants:** `MATERIAL:amount`, `vault:amount`.  
Reward **commands** support `{player}`, `{crate}`, `{reward}`.

---

## Commands

| Command | Who |
|---------|-----|
| `/sc open [crate] [amount]` | player |
| `/sc preview [crate]` | player |
| `/sc shop` | player |
| `/sc keys [crate]` | player |
| `/sc virtualkeys` | player |
| `/sc paykey <player> <crate> <amount>` | player |
| `/sc claim` | player (pending rewards) |
| `/sc stats [player]` | player / admin |
| `/sc setcrate <crate>` | admin (look at block) |
| `/sc setcrate remove` | admin |
| `/sc setnpc <crate>` | admin (Citizens) |
| `/sc givekey <player> <crate> [amount] [physical]` | admin |
| `/sc editor` | admin |
| `/sc locations` | admin |
| `/sc reload` | admin |

Aliases: `sc`, `crates`.

---

## Permissions

| Permission | Grants |
|------------|--------|
| `soulcrates.command.open` | open |
| `soulcrates.command.preview` | preview |
| `soulcrates.command.shop` | shop |
| `soulcrates.command.virtualkeys` | keys GUI |
| `soulcrates.command.paykey` | transfer virtual keys |
| `soulcrates.command.admin` | setcrate, editor, setnpc |
| `soulcrates.command.givekey` | give keys |
| `soulcrates.command.reload` | reload |
| `soulcrates.open.multi` | `/sc open … 5` and bulk in preview |

Per-crate: `opening.permission` in the crate yml.

---

## PlaceholderAPI

`%soulcrates_keys_<crateId>%`, `%soulcrates_total_keys_<crateId>%`, `%soulcrates_pity_<crateId>%`, `%soulcrates_opens_<crateId>%`, etc.

---

## Softdepend

Vault, PlaceholderAPI, Citizens, ModelEngine, DecentHolograms — optional; related features simply won't run without them.
