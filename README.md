# PersonalWorldGen

Личный отдельный проект расширения основного мира Project Zomboid B42.21.
Цель: новые поселения, лагеря и фермы с сохранением обычной карты и работой
Viewpoint, Knox NPC и PersonalKnox. Сейчас это исходники защитной политики,
не установленный и не работающий в игре генератор.

Первый компонент `ProtectedCells` проверяет весь прямоугольник будущего участка
с отступом, включая границы клеток. Занятые клетки берутся из проверенного
набора карт. Проверка не создаёт игровые объекты, не читает мир и имеет лимит
числа обращений к индексу. Это защита от одного класса конфликтов, не гарантия
полной совместимости. Запуск теста: `python3 tools/check.py`.

Референс — [Ungodly Random Map Generator](https://steamcommunity.com/sharedfiles/filedetails/?id=3790727872)
StixsworldHD: изучен как пример поведения (подгрузка участков, очередь операций,
проверки). Он не является зависимостью, не форкается и рядом не запускается;
его код в проект не включается. Собственный генератор пишем на Java.

[ProcTown](https://steamcommunity.com/sharedfiles/filedetails/?id=3825812871)
по описанию автора генерирует отдельный мир с городами; это другой режим,
его нельзя считать расширением Knox Country. Автор прямо предупреждает о
очень ранней версии. В общую сборку он не включён.

Разработка только локальная, Git без remote. Публикация сборки и исходников —
только после согласования с пользователем; перед публикацией пользователь
связывается с автором URMG. Не включать чужой код, карты, здания или ресурсы
в поставку без соответствующего разрешения.

## Профиль защищённых клеток

```sh
python3 tools/export_profile.py
```

Скрипт читает `mods/default.txt`, находит включённые карты Workshop, собирает
клетки из `*.lotheader` и пишет `profile.tsv` с версией игры. Файл
`profile.tsv` — локальный (в `.gitignore`); `MapProfileIO` загружает его в Java,
`ProtectedCells` использует как запрет записи. На текущем наборе — 130 клеток.

## PWG-DB-001 — SQLite checkpoint

- `PersonalWorldGen.db` на каждое сохранение: `regions`, `plans`, `writes`, `settings`, `meta`;
- seed `.sql`, миграция версии, backup API;
- `profile.tsv` в git/JAR;
- `writes` пишут camp/road/building/wall, `plans` — start/once/test-site;
- unit PASS;
- игровой прогон отложен до 300 изменений.

## PWG-DB-001 — SQLite на каждое сохранение

- Реализация: `WorldDatabase` в `getSaveFolder()/PersonalWorldGen.db`,
  `schema.sql`; таблицы `regions`, `plans`, `writes`; API `worldDbPutPlan`,
  `worldDbPutRegion`, `worldDbStatus`.
- Unit: `World database tests passed: plans upsert, regions and deduplicated writes.`
- Статус: код и unit PASS в staging-копии; автозапись генерации и игра ещё нет.

## PWG-06.6 — запись только по явному разрешению

- Причина: игровые write-методы были доступны без release-guard; случайный вызов
  мог писать в пользовательский мир.
- Реализация: `writesEnabled=false` по умолчанию, `writeBudget` 1–512,
  `status/worldgenSettings` показывают режим; все place/generate методы
  возвращают `writes-disabled writes=off worldWrites=0`.
- Критерии: unit-тесты `writes=off`, явное включение, игровой тест только
  в disposable dev-копии.
- Реализация бюджета: `WriteBudget` (limit 1–512), `generateTestSite`
  возвращает `budget=… used=…`; `generateStart/generateOnce` используют общий
  бюджет. Unit-тест PASS; игровой тест после guard ещё не выполнен.
- Статус: подтверждено в игре 1.18.6: `writes=off`, тест с бюджетом 8 дал
  `worldWrites=8 budget=8 used=8`, 115/0, status=OK.

## PWG-06.1 — ограниченный реестр сгенерированных регионов

- Причина: `generateOnce`/`restoreGenerated` хранят ID в неограниченном `Set`;
  `RegionRegistry` существует, но не используется. При долгой игре реестр растёт,
  а повторная генерация после загрузки не ограничена.
- Реализация: перевести API на `RegionRegistry` (лимит 256), сохранить порядок,
  дедупликацию и вытеснение старых ID; добавить unit-тесты реестра.
- Критерии: повторный `generateOnce` не генерирует; `restoreGenerated` не создаёт
  дубликаты; старые ID вытесняются; `generatedRegions` остаётся CSV.
- Статус: подтверждено в игре 11.11.2026 на 1.18.5 (113/0):
  `worldgen-registry` PASS и `worldgen-plan campOps=8 buildingOps=768 roadPoints=77`,
  `status=OK`, exit code 0.

## PWG-06.2 — CSV-сериализация реестра регионов

- Причина: API вручную склеивает/разбирает CSV, а `RegionRegistry` не умеет
  сохранять и восстанавливать своё состояние; это дублирует логику и теряет
  проверку лимита при импорте.
- Реализация: `RegionRegistry.csv()` и `restoreCsv()`; API использует их.
- Критерии: round-trip, trim, дубликаты, лимит 256, пустой CSV.
- Статус: реализовано, unit-тесты PASS; проверка `worldgen-registry` PASS в игре.

## PWG-06.3 — проверка дороги на protected-клетки

- Причина: `SettlementPlanner.planValidated` проверял лагерь и здание, но не точки
  дороги; дорога могла пройти через защищённую клетку.
- Реализация: после `PlanValidator` проверяется каждая точка `settlement.road()`;
  при нарушении — `protected_road@x,y`.
- Критерии: дорога через protected-клетку отклоняется, тест покрывает случай.
- Статус: unit-тесты PASS; полный план 1.18.5 в игре содержит roadPoints=77.

## PWG-06.5 — тестируемый planNear без игрока

- Причина: `planNearPlayer` требует игровой объект и профиль из ресурса, поэтому
  полный settlement 1.18.4 проверялся только в игре.
- Реализация: `planNear(px,py,seed,ProtectedCells)` — чистая функция; игровой
  метод только извлекает координаты и профиль.
- Критерии: unit-тест свободного места (camp/building/road) и protected-отказа.
- Статус: подтверждено в игре: `worldgen-plan` вернул campOps/buildingOps/roadPoints
  на 1.18.5, 113/0, status=OK.

## PWG-06.4 — planNearPlayer планирует весь settlement

- Причина: `planNearPlayer` возвращал только `campOps`, хотя PWG-06 требует
  профиль → участки → дороги → здания → проверку.
- Реализация: camp + building + дорога через `SettlementPlanner.planValidated`;
  вывод содержит `campOps`, `buildingOps`, `roadPoints` и `worldWrites=0`.
- Критерии: при свободном месте — все три компонента; при protected — отказ.
- Статус: подтверждено в игре 1.18.5: `campOps=8 buildingOps=768 roadPoints=77`,
  `protected=false worldWrites=0`.

## PWG-04.1 — дорога с обходом препятствий

- Причина: `RoadPlanner.connect` строит только L-маршрут и не учитывает
  защищённые/занятые клетки; PWG-04 требует учёта препятствий.
- Реализация: `connectAvoiding` — прямой L, альтернативный L и детерминированные
  обходные точки вокруг bounding box; пустой список, если прохода нет.
- Критерии: маршрут не содержит blocked-точек, сохраняет связность, ограничен
  `MAX_POINTS`, одинаков для одного seed/входа.
- Статус: реализовано, unit-тесты PASS; полный settlement содержит roadPoints=77
  в игровом `worldgen-plan`.

## Игровой пакет

- `42/` — B42-пакет `PersonalWorldGen`;
- `tools/build_mod.py` собирает `42/media/java/client/PersonalWorldGen.jar`;
- Java-ядро 1.18.6: writes=off по умолчанию, бюджет записи, чистый planNear, полный settlement, реестр 256 ID, дороги с обходом/protected
  планы, каталог 1.17; `worldWrites=0` без явного тестового вызова.
