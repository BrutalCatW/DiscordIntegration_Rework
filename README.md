# DiscordIntegration

Мод интеграции Discord с сервером Minecraft 1.7.10.

## 📋 Описание

DiscordIntegration — серверный мод, связывающий Discord с Minecraft. Сообщения из чата, события входа/выхода игроков, смерти, получение достижений и команды ретранслируются между Discord и сервером.

Мод работает через FakeUser [(DiscordFakeUser / 828653ca-0185-43d4-b26d-620a7f016be6)](https://mcuuid.net/?q=828653ca-0185-43d4-b26d-620a7f016be6), которому можно выдать права через vanilla OP или другие мод с пермиссиями.

Базируется на [Chikachi/DiscordIntegration](https://github.com/Chikachi/DiscordIntegration).

## ✨ Функционал

- Ретрансляция чата Minecraft ↔ Discord
- Поддержка нескольких Discord-каналов
- Настройки на уровне каждого канала и каждой dimensions
- Ретрансляция событий: вход/выход игроков, смерти, достижения
- Выполнение команд из Discord через FakeUser
- Привязка Discord-аккаунта к аккаунту Minecraft (link/unlink)
- IMC (Inter-Mod Communication) для интеграции с другими модами
- Команда `/discord` с подкомандами: `config`, `online`, `tps`, `unstuck`, `uptime`, `link`, `unlink`

## 🔧 Изменения в rework

- Добавлена поддержка HTTP прокси для подключения к Discord (host, port, аутентификация)
- Мигрирована build система на GTNH convention (`com.gtnewhorizons.gtnhconvention`)
- Добавлен `gradle.properties` с параметрами мода
- JDA 3.8.3_464 добавлена как локальная зависимость (`libs/`)
- Добавлен access transformer (`discordintegration_at.cfg`)
- Переорганизованы импорты и отформатирован код во всех файлах

## 🚀 Установка

1. Скачайте собранный jar
2. Скопируйте в папку `mods/` сервера (клиенту не требуется)
3. Требуется Minecraft Forge для 1.7.10

Руководство по получению токена и channel ID: [wiki](https://github.com/Chikachi/ChikachiDiscord/wiki/How-to-get-a-token-and-channel-ID-for-Discord)

## 🛠️ Сборка

```bash
./gradlew build
```

Требования: Java 8, Forge для Minecraft 1.7.10.

## 🔗 Ссылки

- [Оригинал Chikachi/DiscordIntegration](https://github.com/Chikachi/DiscordIntegration)
- [Wiki по настройке Discord](https://github.com/Chikachi/ChikachiDiscord/wiki/How-to-get-a-token-and-channel-ID-for-Discord)

## 📜 Лицензия

GNU Affero General Public License v3.0 (AGPL-3.0) — [текст лицензии](http://www.gnu.org/licenses/agpl-3.0.html)

*Мод не аффилиирован с Discord Inc.*
