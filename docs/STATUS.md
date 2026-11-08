# PersonalWorldGen — статус

## 0.1.0 — игровой пакет

- B42-пакет `42/`, Java-ядро 0.1.0, ZombieBuddy.
- `PersonalWorldGenAPI.status()` и `planDemo(42)` только планируют данные.
- `worldWrites=0`: мир пока не изменяется.
- `tools/check.py` — все тесты; `tools/build_mod.py` — JAR.

## 0.1.0 — проверка в игре

- Штатный `tools/launch_dev.sh --menu-only --profile worldgen ...` поднял игру.
- Лог: `[PersonalWorldGen] Java core 0.1.0 loaded; core=0.1.0 worldWrites=0 mode=planning`.
- JAR одобрен, мод загружен в ZombieBuddy; игра выключена.

## 0.3.0 — профиль карт в игре

- В JAR включён `personal/worldgen/profile.tsv`.
- В игре: `core=0.3.0 profileCells=130 profileVersion=42.21.0 worldWrites=0`.
- Игра выключена.

## 0.4.0 — первая запись объекта

- `@Exposer.LuaClass` на `PersonalWorldGenAPI`: ZombieBuddy теперь отдаёт API в Lua.
- `placeMarker` пишет один предмет `Base.Hammer` на клетку игрока.
- Проверка: `marker=placed worldWrites=1`.
- В обычном запуске автоматической записи нет; только явный вызов API.
- Мир сценария тестовый; штатный мир не изменялся.

## 0.5.0 — лагерь из плана

- `placeCamp`: 8 операций плана → 8 предметов в тестовом мире.
- `campOps=8 placed=8 worldWrites=8`.
- Автоматической записи в обычном запуске нет.

## 1.2.0 — полный лагерь у края

- План у края уменьшен до 16×16 и 2 объектов, чтобы целиком попадать в загруженные чанки.
- Игра: `bg commit placed=2 remaining=0 worldWrites=2`.

## 1.3.0 — трансформации паттернов

- `MapPattern.rotate/mirror`; лог `pattern base=3 rotated=2x3 mirrored=3`.

## 1.4.0 — библиотека паттернов

- `PatternLibrary`: ограниченная библиотека, детерминированный выбор.
- Паттерны дороги и здания.
- Лог `library ...`.

## 1.5.0 — проверка и трансформации паттернов

- `transpose`, `rotate180`, `valid`, `serialize`.
- Библиотека: `has`, `remove`, `clear`, `all`.
- Лог `engine ...`.

## 1.6.0 — операции с паттернами и реестр регионов

- `parse`, `hash`, `trim`, `countKinds`, `bounds`.
- `RegionRegistry`: remember/has/forget/clear/first/last/at/list/summary.
