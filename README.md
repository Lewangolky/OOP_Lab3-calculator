# Лабораторна робота 3 — Науковий калькулятор для Android

Android-застосунок «Калькулятор» з науковими функціями та графічним інтерфейсом.
Kotlin, Material 3, AGP 9.5 / Gradle 9.8, minSdk 24, targetSdk 36.

## Можливості

- арифметика з пріоритетами операцій і дужками, неявне множення (`2π`, `3(1+2)`);
- `sin`, `cos`, `tan` та обернені (`2nd`), у градусах або радіанах (`DEG`/`RAD`);
- `ln`, `log`, `eˣ`, `10ˣ`, `√`, `∛`, `x²`, `x³`, `xʸ`, `x!`, `%`, константи `π` і `e`;
- живий попередній результат під час введення, `Ans` — останній результат;
- історія обчислень (меню → «Історія»), стан переживає поворот екрана та знищення процесу;
- окремі розкладки для портретної та альбомної орієнтації, світла/темна тема, Material You.

## Структура

| Пакет / файл | Призначення |
|---|---|
| `engine/` | Обчислювальне ядро без залежностей від Android: `Tokenizer` → `ExpressionParser` (рекурсивний спуск) → `CalculatorEngine`; помилки — `CalculationException` з типом `CalcError` |
| `format/ResultFormatter` | Форматування чисел за локаллю (роздільники, наукова нотація, округлення до 12 значущих цифр) |
| `ui/CalculatorViewModel` | Логіка клавіш, історія, `StateFlow<CalculatorUiState>`, збереження в `SavedStateHandle` |
| `ui/MainActivity` | Відображення стану, меню, діалоги історії/мови/«Про застосунок» |
| `CalculatorApp` | Ініціалізація, логування необроблених винятків |

## Логування

SLF4J API + [logback-android](https://github.com/tony19/logback-android), конфігурація в `assets/logback.xml`:

- `src/main/assets/logback.xml` — **release**: у Logcat лише `WARN+`, у файл `INFO+`;
- `src/debug/assets/logback.xml` — **debug** (перекриває попередній): `DEBUG` для `com.oop.calculator`;
- файловий апендер: `files/logs/calculator.log` у приватному сховищі, ротація за днем і розміром (1 МБ, до 5 МБ сумарно);
- тег Logcat `SciCalc`: `adb logcat -s SciCalc`;
- у JVM-тестах logback-android замінено на `slf4j-simple` (`src/test/resources/simplelogger.properties`).

Рівні: `DEBUG` — натискання клавіш і життєвий цикл, `INFO` — обчислення, зміна мови/одиниць, `WARN` — помилки обчислення, `ERROR` — необроблені винятки.

## Інтернаціоналізація (i18n)

- Мови: англійська (за замовчуванням, `values/`), українська (`values-uk/`), німецька (`values-de/`).
- Увесь текст — у `strings.xml`; математичні позначення клавіш — у `values/keys.xml` з `translatable="false"`.
- Множина через `<plurals>` (українська має форми `one`/`few`/`many`/`other`), параметризовані рядки `%1$s`.
- Помилки ядра — це `enum CalcError`, а текст для користувача підбирає UI з ресурсів.
- Числа форматуються за локаллю (`1,234.5` / `1 234,5` / `1.234,5`), клавіша десяткового роздільника показує `.` або `,`.
- Описи клавіш для TalkBack (`cd_*`) також перекладено.
- Вибір мови в застосунку (меню → «Мова»): `AppCompatDelegate.setApplicationLocales`; на Android 13+ мова видна й у системних налаштуваннях застосунку — `locales_config` генерує AGP (`generateLocaleConfig`), на старіших версіях вибір зберігає `AppLocalesMetadataHolderService`.
- Нова мова = файл `values-<код>/strings.xml` + код у масиві `supported_language_tags`.

## Збірка та запуск

Потрібні JDK 17+ та Android SDK (шлях у `local.properties` або `ANDROID_HOME`).

```bash
./gradlew testDebugUnitTest   # модульні тести ядра, форматера та ViewModel
./gradlew lintDebug           # Android Lint
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug        # встановити на підключений пристрій/емулятор
```

Або відкрити теку в Android Studio й запустити конфігурацію `app`.

### Запуск в емуляторі (без Android Studio)

Потрібні пакети SDK `emulator` і `system-images;android-36;google_apis;x86_64`
та апаратна віртуалізація (Windows Hypervisor Platform). Скрипт сам створить
віртуальний Pixel 7 з Android 16 (якщо його ще немає), запустить емулятор,
дочекається завантаження, збере й відкриє застосунок:

```powershell
.\run-emulator.ps1
```

Параметри: `-AvdName`, `-AvdHome` (де зберігати віртуальний пристрій, ~6 ГБ), `-SystemImage`, `-Device`.
