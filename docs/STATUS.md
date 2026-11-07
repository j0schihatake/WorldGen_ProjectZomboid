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
