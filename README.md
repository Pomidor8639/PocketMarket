# PocketMarket

```text
+-------------------------------------------------------------+
|                        POCKETMARKET                         |
|      Minecraft Bedrock Edition: Mods, Textures, APK Mods    |
+-------------------------------------------------------------+
```

PocketMarket представляет собой независимый каталог и магазин модификаций для мобильной версии Minecraft (Bedrock Edition). Проект включает нативное Android-приложение, написанное на Java с черным интерфейсом и фирменной типографикой Minecraft, а также централизованный REST API сервер.

---

## Скачать приложение (APK)

Готовая сборка приложения скомпилирована и доступна для прямой загрузки:

* **Прямая ссылка с сервера**: [Скачать PocketMarket.apk (v1.0.0)](http://87.120.187.254:3000/files/PocketMarket.apk)
* **Файл в репозитории**: [releases/PocketMarket.apk](releases/PocketMarket.apk)
* **Размер файла**: ~5.8 МБ
* **Минимальная версия Android**: Android 7.0 (API 24)
* **Целевая версия Android**: Android 14.0 (API 34)

---

## Основные возможности

* **Полностью на Java**: Клиент реализован на нативном стеке Android SDK без сторонних тяжелых фреймворков.
* **Черная стилистика**: Интерфейс выполнен в глубокой темной палитре (`#0C0D11`, `#131620`, `#171B26`) с акцентными цветами Minecraft.
* **Пиксельный шрифт Minecraft**: Интегрирован шрифт `minecraft.otf`, обеспечивающий аутентичный визуальный стиль меню игры.
* **Векторная графика Google**: Все иконки интерфейса используют оптимизированные векторные ресурсы Google Material Icons.
* **Интерактивные отзывы и рейтинг**: Возможность оценивать модификации и оставлять комментарии прямо из приложения.

---

## Поддерживаемые категории

Приложение разделяет контент на три ключевые категории с адаптивной логикой установки:

| Категория | Форматы файлов | Тип установки | Описание |
| :--- | :--- | :--- | :--- |
| **Моды** | `.mcaddon`, `.mcpack` | Автоматический импорт в Minecraft | Геймплейные дополнения: мебель, рюкзаки, новые мобы, боссы и реалистичная физика |
| **Текстурпаки** | `.mcpack` | Автоматический импорт в Minecraft | Пакеты ресурсов высокого разрешения, шейдеры с реалистичной водой и X-Ray |
| **АПК Моды** | `.apk` | Android PackageInstaller | Модифицированные клиенты игры: Mod Menu, OptiFine FPS-бустер, шейдерные патчи |

---

## Архитектура механизмов установки

### 1. Импорт модов и текстур (`.mcpack` / `.mcaddon`)
* Файл скачивается во внутреннее хранилище приложения.
* Создается безопасный `content://` URI через `androidx.core.content.FileProvider`.
* Запускается `Intent.ACTION_VIEW` с типом данных `application/octet-stream`.
* Приоритетно вызывается пакет `com.mojang.minecraftpe`. Minecraft запускается и сразу начинает автоматический импорт без необходимости ручного переноса файлов по папкам.

### 2. Установка автономных сборок (`.apk`)
* Приложение проверяет системное право `REQUEST_INSTALL_PACKAGES`.
* Если разрешение не выдано, открывается системное окно настроек `ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
* После подтверждения запускается системный установщик Android через `Intent.ACTION_VIEW` с типом `application/vnd.android.package-archive`.

---

## Серверная часть (REST API)

Бэкенд функционирует на базе Node.js и развернут на сервере в качестве фоновой службы `systemd`.

* **Адрес API**: `http://87.120.187.254:3000`
* **Веб-каталог в браузере**: `http://87.120.187.254:3000/`

### Спецификация эндпоинтов:

| Метод | Эндпоинт | Назначение |
| :--- | :--- | :--- |
| `GET` | `/api/status` | Проверка состояния сервера, количества элементов и общего числа загрузок |
| `GET` | `/api/categories` | Список категорий (Моды, Текстурпаки, АПК Моды) |
| `GET` | `/api/items` | Получение каталога. Фильтры: `category`, `search`, `sort`, `page`, `limit` |
| `GET` | `/api/items/:id` | Детальная карточка конкретного элемента |
| `POST` | `/api/items/:id/download` | Регистрация факта загрузки и генерация прямой ссылки на файл |
| `POST` | `/api/items/:id/rate` | Отправка оценки (от 1 до 5) и текстового комментария |
| `POST` | `/api/items` | Добавление новой модификации через API |

---

## Структура проекта

```text
PocketMarket/
|-- .gitignore
|-- LICENSE
|-- README.md
|-- build.gradle
|-- gradle.properties
|-- settings.gradle
|-- gradlew
|-- gradlew.bat
|-- gradle/
|   `-- wrapper/
|       |-- gradle-wrapper.jar
|       `-- gradle-wrapper.properties
|-- releases/
|   `-- PocketMarket.apk
`-- app/
    |-- build.gradle
    `-- src/main/
        |-- AndroidManifest.xml
        |-- assets/fonts/minecraft.otf
        |-- res/
        |   |-- drawable/        (Иконки Google Material и фоны карточек)
        |   |-- font/            (Шрифты Minecraft)
        |   |-- layout/          (Разметки главного экрана и карточек)
        |   |-- values/          (Цвета, темы, строки)
        |   `-- xml/             (Конфигурация FileProvider)
        `-- java/com/pocketmarket/client/
            |-- MainActivity.java
            |-- DetailActivity.java
            |-- adapter/
            |   |-- ModAdapter.java
            |   `-- ReviewAdapter.java
            |-- api/
            |   `-- PocketMarketApi.java
            |-- installer/
            |   `-- ModInstaller.java
            |-- model/
            |   |-- ModItem.java
            |   `-- Review.java
            `-- utils/
                `-- FontUtils.java
```

---

## Сборка из исходного кода

### Требования
* JDK 17
* Android SDK (API 34, Build Tools 34.0.0+)

### Команды сборки:

1. Клонирование репозитория:
```bash
git clone https://github.com/Pomidor8639/PocketMarket.git
cd PocketMarket
```

2. Сборка Debug APK через Gradle Wrapper:
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

Собранный файл будет находиться по пути:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Лицензия
Проект распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).
