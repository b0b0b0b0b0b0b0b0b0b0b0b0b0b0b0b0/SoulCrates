# SoulCrates

> [English version](docs/en/README.md)

**Кейсы без дюпов: Folia, анимация уровня premium, pity, reroll, сеть MySQL+Redis — один JAR, без shade.**

SoulCrates — плагин кейсов для Paper **1.21+** и **Folia**, когда открытие должно быть красивым, честным по весам и безопасным на region-потоках и прокси.

Один JAR: зависимости через Paper `libraries`, не shade. Меньше конфликтов с другими плагинами и проще обновлять.

## Зачем админу

**Folia first-class** — region-aware потоки, без `BukkitScheduler` на hot path. Для сети на Folia это базовый контракт, не опция.

**Несколько типов кейсов** — `crates/<id>.yml`: свои награды, ключи, pity, reroll, анимация, движок отображения. Не копипаст конфигов и не зоопарк плагинов.

**Мультисервер** — **MySQL** + **Redis pub/sub**: виртуальные ключи и pity-кэш синхронизируются между инстансами. Истина в SQL; Redis — зеркало и инвалидация, не «кто последний записал файл».

**Анти-дюп** — сессия открытия с lock по игроку, bulk-open под одним lock, consume ключей до старта анимации. GUI только через custom `InventoryHolder`, не по title.

**Игрокам — нормальный опыт:** preview, confirm, CSGO-спиннер, reroll с Vault-оплатой, multi-open x5/x10, магазин ключей. Сообщения — MiniMessage, HEX, градиенты; тексты в `lang/messages_*.yml`.

**Мир и NPC:** привязка кейса к блоку (`/sc setcrate`), idle-модели/дисплеи, голограммы (TextDisplay / DecentHolograms), Citizens NPC (`/sc setnpc`). Shift+RMB — preview, RMB — открытие.

**Операционка:** in-game редактор наград, `/sc stats`, `/sc locations`, broadcast редких дропов, PlaceholderAPI, API-события фаз. `/sc reload` — конфиг, lang, GUI yml, кейсы.

**Для кого:** Paper/Folia-сеть с донат/ивент кейсами, прокси с общей БД, когда важнее анти-дюп и синхрон ключей, чем «ещё одна кнопка в GUI».

## Сеть и хранение (кратко)

| Режим | Назначение |
|--------|------------|
| `SQLITE` | один сервер, быстрый старт |
| `MYSQL` | персистентность; **основа для прокси** |
| Redis + pub/sub | зеркало virtual keys + pity между инстансами |

На прокси: **MYSQL + `redis.enabled: true`**. Канал по умолчанию — `soulcrates:sync`. Публикация при записи ключей/pity; подписка обновляет локальный кэш без echo своего сервера.

## Что умеет (полный список)

### Платформа

- Paper **1.21+** и **Folia** (`folia-supported: true`), `PluginSchedulers` на всех мутациях мира/инвентаря.
- Один JAR, зависимости через Paper **`libraries`** (Elytrium Serializer, HikariCP, MySQL/SQLite, Jedis, Gson) — **не shade**.
- Async bootstrap БД: миграции и preload не блокируют region/global tick.
- Typed config (Elytrium): дефолты в Java, fresh install работает без ручного дописывания YAML.

### Кейсы и награды

- Один YAML на тип: `plugins/SoulCrates/crates/<id>.yml`.
- Пул наград: **weight**, preview-иконка, `grants` (`MATERIAL:amount`, `vault:100`), console `commands` с `{player}`, `{crate}`, `{reward}`.
- **Pity:** счётчик opens без pity-награды → гарантированный `rewardId`.
- **Broadcast:** `broadcast: true` на награде → сообщение всему серверу при claim.
- Per-crate: cooldown, permission на открытие, preview/confirm, multi-open.

### Ключи

- **Виртуальные** — в БД, кэш в памяти, `/sc givekey`, PlaceholderAPI `%soulcrates_keys_<crate>%`.
- **Физические** — предмет с PDC, custom model data, consume из инвентаря при открытии.
- **Магазин** — `/sc shop`, `shop.yml` + `gui/shop.yml`: Vault и/или item-cost за пакеты ключей.

### Открытие и анимация

- Pipeline из 3 фаз: **key insert → CSGO spinner → firework reveal** (типы и длительность в `animations` кейса).
- **Premium opening** (`config.yml` → `premiumOpening`):
  - `soulcrates.open.skip` — пропуск анимации, reroll по правилам кейса;
  - `soulcrates.open.instant` — мгновенно, без display;
  - `soulcrates.open.multi` — `/sc open <crate> <amount>`, кнопки x5/x10 в preview.
- **Reroll** — GUI после анимации: free/paid rolls, Vault cost; skip per-crate (`skipOnInstantOpen`, `skipOnSkipAnimation`, `skipOnMultiOpen`) + глобальные флаги.
- **Bulk open** — последовательные pity-rolls, summary в чат, без reroll-меню.
- API-события: `CrateOpenStartEvent`, `CrateOpenPhaseStartEvent`, `CrateOpenPhaseEndEvent`, `CrateOpenFinishEvent`.

### Отображение в мире

- **Движки:** `VANILLA_BLOCK`, `VANILLA_DISPLAY`, **ModelEngine** (`engine.type`, `modelId`, idle/close анимации).
- **Idle display** — модель/дисплей на привязанном блоке, ambient-частицы, respawn при load chunk.
- **Голограммы** — `idleDisplay.hologram`: VANILLA TextDisplay, **DecentHolograms** (reflection), задел под FancyHolograms; плейсхолдеры `{crate}`, `{crate_id}` в строках.
- **Привязка блока** — `/sc setcrate <crate>`, `/sc setcrate remove`; interact sound из config.

### NPC и интеграции

- **Citizens** — `/sc setnpc <crate>`, `/sc setnpc remove`; клик → open, Shift → preview.
- **Vault** — экономика reroll и key shop.
- **PlaceholderAPI** — `%soulcrates_*%` (см. ниже).
- **ModelEngine**, **DecentHolograms**, **Citizens** — softdepend, reflection где возможно.

### Админка и данные

- **`/sc editor`** — список кейсов, правка в GUI (награды: weight, broadcast, pity, grant from hand).
- **`/sc givekey`**, **`/sc keys`**, **`/sc stats [player]`**, **`/sc locations`**.
- **`/sc reload`** — config, lang, gui, crates, idle/hologram respawn.
- SQL: virtual keys, pity, opens, last reward, locations, npc bindings.

## Структура конфигов

После первого старта в `plugins/SoulCrates/`:

| Путь | Содержимое |
|------|------------|
| `config.yml` | БД, Redis, session timeout, idle display, broadcast, premium opening, shop toggle, aliases |
| `crates/*.yml` | Определения кейсов (engine, animations, opening, keys, reroll, pity, rewards) |
| `shop.yml` | Позиции магазина ключей (crate, amount, vault/item price) |
| `gui/*.yml` | Слоты и материалы GUI (preview, confirm, spinner, reroll, editor, shop) |
| `lang/messages_*.yml` | MiniMessage-тексты (ru + en в JAR) |
| `data/crates.db` | SQLite при `database.mode: SQLITE` |

### `config.yml` — главное

```yaml
defaultCrateId: default
cratesDirectory: crates
sessionTimeoutSeconds: 120

database:
  mode: SQLITE          # или MYSQL
  sqliteFile: data/crates.db
  mysqlHost: 127.0.0.1
  poolSize: 4

redis:
  enabled: false        # true на прокси с MYSQL
  host: 127.0.0.1
  port: 6379
  channel: soulcrates:sync
  pubSubEnabled: true

premiumOpening:
  skipAnimationPermission: soulcrates.open.skip
  instantOpenPermission: soulcrates.open.instant
  multiOpenPermission: soulcrates.open.multi
  maxMultiOpen: 10
  instantSkipsReroll: true
  multiOpenSkipsReroll: true

idleDisplay:
  enabled: true
  particles: true
  hologram:
    enabled: true
    offsetY: 2.1
    provider: VANILLA    # DECENT_HOLOGRAMS
    lines:
      - "<gold>{crate}</gold>"
      - "<gray>Click to open · Shift preview</gray>"
```

### `crates/<id>.yml` — пример

```yaml
id: default
displayName: Default Crate

engine:
  type: VANILLA_DISPLAY
  blockMaterial: ENDER_CHEST
  modelId: ""
  idleAnimation: idle

opening:
  requireKey: true
  previewEnabled: true
  keysRequired: 1
  cooldownSeconds: 0
  permission: ""
  allowMultiOpen: true

keys:
  enabled: true
  material: TRIPWIRE_HOOK
  virtualKeys: true
  physicalKeys: true

reroll:
  enabled: true
  freeRolls: 1
  maxRolls: 3
  vaultCost: 100.0
  skipOnInstantOpen: true
  skipOnSkipAnimation: false
  skipOnMultiOpen: true

pity:
  enabled: true
  threshold: 50
  rewardId: legendary

rewards:
  - id: common
    weight: 70
    displayName: Diamond Stack
    material: DIAMOND
    grants: ["DIAMOND:3"]
  - id: legendary
    weight: 5
    displayName: Netherite Ingot
    material: NETHERITE_INGOT
    grants: ["NETHERITE_INGOT:1"]
    pityEligible: true
    broadcast: true
```

## Сообщения (`plugins/SoulCrates/lang/`)

- `prefix` — префикс строк с `{prefix}`.
- Ключи — **MiniMessage**; ru и en поставляются из JAR, merge при добавлении новых ключей.
- Плейсхолдеры: `{crate}`, `{reward}`, `{player}`, `{amount}`, `{npc}`, `{seconds}` и др. по ключу.

## PlaceholderAPI

Идентификатор — `soulcrates`, формат `%soulcrates_<параметр>%`. Регистрация автоматически при наличии PlaceholderAPI.

| Плейсхолдер | Значение |
|---|---|
| `%soulcrates_active_session%` | `true` / `false` — игрок в сессии открытия |
| `%soulcrates_keys_<crateId>%` | Виртуальные ключи |
| `%soulcrates_physical_keys_<crateId>%` | Физические ключи в инвентаре (online) |
| `%soulcrates_total_keys_<crateId>%` | Сумма virtual + physical |
| `%soulcrates_opens_<crateId>%` | Счётчик открытий |
| `%soulcrates_pity_<crateId>%` | Текущий pity-счётчик |
| `%soulcrates_last_reward_<crateId>%` | Id последней выигранной награды |

## Команды

Алиасы: `sc`, `crates` (настраиваются в `config.yml` → `commandAliases`).

### Игроки

| Команда | Действие |
|---------|----------|
| `/sc open [crate] [amount]` | Открыть кейс (amount при `soulcrates.open.multi`) |
| `/sc preview [crate]` | Preview GUI |
| `/sc shop` | Магазин ключей |
| `/sc keys [crate]` | Показать ключи |
| `/sc stats [player]` | Статистика opens/pity (чужие — право admin) |

### Админы

| Команда | Действие |
|---------|----------|
| `/sc editor` | In-game редактор кейсов |
| `/sc givekey <player> <crate> [amount] [physical]` | Выдать ключи |
| `/sc setcrate <crate>` | Привязать кейс к блоку (look 5 blocks) |
| `/sc setcrate remove` | Снять привязку блока |
| `/sc setnpc <crate>` | Привязать Citizens NPC (look 5 blocks) |
| `/sc setnpc remove` | Снять привязку NPC |
| `/sc locations` | Список привязанных блоков |
| `/sc reload` | Перезагрузка конфигов |

## Права

| Право | Назначение |
|-------|------------|
| `soulcrates.command.use` | Базовая команда `/sc` |
| `soulcrates.command.open` | `/sc open` |
| `soulcrates.command.preview` | `/sc preview` |
| `soulcrates.command.shop` | `/sc shop` |
| `soulcrates.command.keys` | `/sc keys` |
| `soulcrates.command.admin` | editor, setcrate, setnpc, locations |
| `soulcrates.command.reload` | `/sc reload` |
| `soulcrates.command.givekey` | `/sc givekey` |
| `soulcrates.command.stats.others` | `/sc stats <player>` |
| `soulcrates.open.skip` | Пропуск анимации |
| `soulcrates.open.instant` | Мгновенное открытие |
| `soulcrates.open.multi` | Multi-open и bulk-кнопки |

Per-crate permission — поле `opening.permission` в `crates/<id>.yml` (пусто = достаточно `soulcrates.command.open`).

## API для разработчиков

```java
SoulCrates plugin = (SoulCrates) Bukkit.getPluginManager().getPlugin("SoulCrates");
SoulCratesApi api = plugin.core().api();

api.isLoaded();
api.giveVirtualKeys(playerId, "default", 5);
api.beginOpen(player, "default", location, 1);
api.openPreview(player, "default");
api.pity(playerId, "default");
```

События в пакете `bm.b0b0b0.soulCrates.api.event` — слушайте фазы и finish для квестов/статистики.

## Softdepend

Vault, PlaceholderAPI, ModelEngine, ItemsAdder, **Citizens**, **DecentHolograms**, FancyHolograms — опционально; без них соответствующие фичи отключаются gracefully.
