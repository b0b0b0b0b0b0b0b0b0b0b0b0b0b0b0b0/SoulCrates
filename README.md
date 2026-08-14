# SoulCrates

> [English version](docs/en/README.md)

Кейсы для Paper **1.21+** / **Folia**: ключи, pity, reroll, SELECT-режим, idle-эффекты, MySQL+Redis для сети.

---

## Быстрый старт

1. Положи JAR в `plugins/`, перезапусти сервер.
2. Плагин сам создаст `plugins/SoulCrates/` и кейс **`default`**.
3. В игре (нужен OP или права ниже):

```
/sc setcrate default
/sc givekey <ник> default 10
```

4. Смотри на блок → **ПКМ** открыть, **Shift+ПКМ** preview.  
   Или: `/sc open default`

Готово — один рабочий кейс на блоке.

---

## Новый кейс

1. Скопируй `plugins/SoulCrates/crates/default.yml` → `donate.yml`
2. Поменяй `id: donate` (должен совпадать с именем файла) и `displayName`
3. `/sc reload`
4. `/sc setcrate donate` + `/sc givekey <ник> donate 10`

Редактор наград в игре: `/sc editor` (только для уже существующих кейсов).

---

## Что где лежит

| Путь | Зачем |
|------|--------|
| `config.yml` | БД, Redis, голограммы, premium-права |
| `crates/<id>.yml` | Кейс: награды, ключи, анимация, opening |
| `shop.yml` + `gui/shop.yml` | Магазин ключей (`/sc shop`) |
| `gui/*.yml` | Слоты меню (preview, select, reroll…) |
| `lang/messages_*.yml` | Тексты игрокам (MiniMessage) |

После правки YAML: **`/sc reload`**.

---

## `config.yml` — минимум

```yaml
defaultCrateId: default

database:
  mode: SQLITE          # MYSQL — для прокси/сети

redis:
  enabled: false        # true + MYSQL на прокси

idleDisplay:
  enabled: true
  hologram:
    enabled: true
    lines:
      - "<gold>{crate}</gold>"
      - "<gray>ПКМ — открыть · Shift — preview</gray>"
```

**Прокси:** `database.mode: MYSQL` + `redis.enabled: true`, один канал `soulcrates:sync` на всех серверах.

---

## `crates/<id>.yml` — скелет

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
  rewardsMode: RANDOM       # SELECT — игрок выбирает награду в меню
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
    displayName: "Меч"
    material: DIAMOND_SWORD
    grants: ["DIAMOND_SWORD:1"]
    broadcast: true
```

**grants:** `MATERIAL:кол-во`, `vault:сумма`.  
**commands** на награде: `{player}`, `{crate}`, `{reward}`.

---

## Команды

| Команда | Кто |
|---------|-----|
| `/sc open [кейс] [кол-во]` | игрок |
| `/sc preview [кейс]` | игрок |
| `/sc shop` | игрок |
| `/sc keys [кейс]` | игрок |
| `/sc virtualkeys` | игрок |
| `/sc paykey <игрок> <кейс> <кол-во>` | игрок |
| `/sc claim` | игрок (очередь наград) |
| `/sc stats [игрок]` | игрок / админ |
| `/sc setcrate <кейс>` | админ (смотри на блок) |
| `/sc setcrate remove` | админ |
| `/sc setnpc <кейс>` | админ (Citizens) |
| `/sc givekey <игрок> <кейс> [кол-во] [physical]` | админ |
| `/sc editor` | админ |
| `/sc locations` | админ |
| `/sc reload` | админ |

Алиасы: `sc`, `crates`.

---

## Права

| Право | Что даёт |
|-------|----------|
| `soulcrates.command.open` | открытие |
| `soulcrates.command.preview` | preview |
| `soulcrates.command.shop` | магазин |
| `soulcrates.command.virtualkeys` | GUI ключей |
| `soulcrates.command.paykey` | перевод вирт. ключей |
| `soulcrates.command.admin` | setcrate, editor, setnpc |
| `soulcrates.command.givekey` | выдача ключей |
| `soulcrates.command.reload` | reload |
| `soulcrates.open.multi` | `/sc open … 5` и bulk в preview |

Per-crate: поле `opening.permission` в yml кейса.

---

## PlaceholderAPI

`%soulcrates_keys_<crateId>%`, `%soulcrates_total_keys_<crateId>%`, `%soulcrates_pity_<crateId>%`, `%soulcrates_opens_<crateId>%` и др.

---

## Softdepend

Vault, PlaceholderAPI, Citizens, ModelEngine, DecentHolograms — опционально; без них связанные фичи просто не работают.
