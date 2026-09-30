# Sell & Win Raffle 🎟️

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-2C2255?logo=java&logoColor=white)](https://openjfx.io/)
[![Build](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-JUnit%205-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)

Desktop raffle ticket management system built with **JavaFX**. Designed for local raffle campaigns with an operator-friendly workflow for managing items, selling tickets, tracking players, and running a live draw.

---

## 🧾 About

**Sell & Win Raffle** is a local-first desktop application for organizing and running raffle campaigns end-to-end:

- Manage raffle items (prizes)
- Sell tickets and assign ticket IDs automatically
- Track players and ticket inventory
- Run a live draw and announce winners

Data is stored locally using **CSV files** and item-specific image folders under the user’s home directory, keeping setup simple and backups straightforward.

---

## ✨ Key Features

- 🏷️ Create raffle items with title, description, ticket count, and ticket price
- 🖼️ Pictures without copying files by hand: choose the item's picture from anywhere on the computer (it is copied into the item's folder), add more pictures later and pick the main one from the View Item screen
- 👤 Sell tickets to players and auto-assign ticket IDs
- 💶 Price per ticket, the amount to pay shown while typing, "paid now" or owed, and a per-item summary of money collected and still outstanding; mark a buyer's tickets as paid or unpaid later
- ♻️ Reuse ticket IDs when player records are removed
- 📦 Track remaining ticket inventory per item
- 🔎 Search player status by name, phone number, or ticket ID
- 🎲 Run a live draw: the winner is picked with a secure random generator, only among tickets that were actually sold
- 🏆 Draw several winners in one sitting, optionally with "one prize per person" (everyone who already won is left out, with all their tickets)
- 🎉 Made to be shown on a projector: full screen (`F11`, `Esc` to leave), the item picture and name, slowing number roll, confetti, keyboard only (`Space`/`Enter`). Phone numbers are never shown on the draw screen
- 📜 Every draw is saved to a history file (time, item, ticket, winner)
- 🛟 Crash-safe saves (write to a temp file, then replace) and an automatic backup on every start-up
- 📊 A dashboard on the main screen: tickets sold, money collected, money still owed and what selling everything would bring, plus a progress bar and the collected/owed amount for every item
- 🎨 Dark and light theme, switched with a button on the main screen and remembered; one stylesheet whose colours are variables, so a new theme is a handful of lines
- 🌍 English and Romanian: every screen is translated, the language follows the system language at first and can be switched with the `RO`/`EN` button on the main screen (the choice is remembered in `settings.properties`). Texts live in `i18n/messages*.properties`
- 🧰 Package as a **JAR** and a Windows **.exe** (Launch4j)

---

## 🧱 Tech Stack

| Area | Details |
|------|---------|
| 💻 Language | Java 21 |
| 🧩 UI | JavaFX 21, FXML, CSS |
| 🏗️ Build | Maven |
| ✅ Testing | JUnit 5 |
| 💾 Storage | Local CSV files |
| 📦 Packaging | `jpackage` (Windows installer and portable zip with its own Java runtime), built by GitHub Actions |

---

## 🔄 Application Flow

### 1) Add a raffle item

Create a new item with:

- Title
- Description
- Number of tickets
- Ticket price

When an item is created, the app prepares:

- A folder for the item’s images
- A ticket record CSV for that item
- An entry in the main `data.csv` catalog

### 2) Add item images

Each item gets its own image folder under the application data directory. Operators can set a default image for the main dashboard and browse all images for an item in the item viewer.

### 3) Sell tickets

For a selected item, the operator can add players by entering:

- Player name
- Phone number
- Number of tickets to purchase

The app randomly assigns available ticket IDs and writes the updated player state back to the item’s record file.

### 4) Check player status

The player status screen supports searching by:

- Ticket ID
- Phone number
- Player name

### 5) Run the draw

Choose how many winners to draw and whether one person can win only once, then press **Start** and **Stop** (or `Space`). The numbers slow down and land on the winner, which is picked at the moment of Stop from the sold tickets only, so the timing of the click cannot influence it. Winners are listed as they are drawn; **New draw** starts a fresh sitting. Every winner is appended to `data/draws.csv`.

---

## 🗂️ Data Storage

At runtime, the application writes data to the user’s home directory:

```text
~/Sell & Win Raffle/
  data/
    data.csv
  records/
    <item-title>.csv
    <item-title>.csv.bak      (previous version, kept on every save)
  <item-title>/
    image files...
  settings.properties         (language, theme and currency)
  backups/
    <yyyyMMdd-HHmmss>/        (snapshot of data/ and records/ taken at each start-up, newest 20 kept)
    deleted/                  (ledgers of deleted items are moved here, never destroyed)
```

### `data/data.csv`

Stores the master item catalog with:

- Image path
- Title
- Description
- Available tickets
- Ticket price

### `data/draws.csv`

Append-only history of every draw: time, item, winning ticket, winner name and phone number.

### `records/<item-title>.csv`

Stores the ticket ledger for a single raffle item with:

- Ticket ID
- Player name
- Phone number
- Number of tickets associated with that buyer
- Paid (`true`/`false`) and the time of sale (UTC, ISO-8601)

Ledgers written by older versions have only the first four columns. They are read as they are (their sold tickets count as paid, since payments were not tracked) and get the new columns the next time they are saved.

---

## 📁 Project Structure

```text
.github/workflows/   CI (tests) and the Windows installer build
packaging/           jpackage script and the Windows icon
src/
  main/
    java/
      raffle/
        controllers/
        main/
        models/
        services/
        ui/
        utils/
    resources/
      fxml_files/
      i18n/
      icons/
      stylesheets/
  test/
    java/
      raffle/
        models/
        services/
        utils/
```

---

## 🏗️ Architecture

### Main application layer

- `raffle.main.App` — loads the JavaFX application, shows the loading screen, and navigates between views
- `raffle.main.EntryPoint` — thin entry point class used to launch the app

### Controllers

The UI is split into focused JavaFX controllers:

- `MainViewController`
- `AddItemController`
- `AddPlayerController`
- `DrawController`
- `ViewItemController`
- `PlayerStatusController`
- `LoadingController`

### Domain models

- `Item`
- `Player`

### Services

- `RaffleDraw` — picks the winner (secure random, sold tickets only, optional exclusions); no JavaFX, fully unit tested
- `SalesSummary` — the same figures for the whole raffle
- `ItemSales` — tickets sold, paid and owed, and money collected/outstanding for an item (money counted in cents)
- `Payments` — marking the tickets of a buyer paid or unpaid
- `DrawSession` — one sitting: how many winners, who has won, who can still win ("one prize per person")
- `DrawHistory` — append-only audit log of draws

### UI helpers

- `Theme` — dark/light theme (a style class on the screen root) and its saved setting
- `Dialogs` — the one place that builds alerts and Yes/No questions, so they follow the theme and the language
- `ConfettiCanvas` — the confetti overlay of the draw screen
- `ItemImages` — item picture, or the bundled logo when there is none

### Persistence and utilities

- `ItemDataReaderAndWriter`, `PlayerDataReaderAndWriter` — CSV reading and writing (quotes and commas in names survive a round trip; files from older versions are still read)
- `SafeFiles` — atomic writes with a `.bak` of the previous version
- `BackupService` — start-up snapshots and archiving of deleted items
- `AppPaths` — the one place that knows where data lives
- `Money` — amounts in the active language and the `currency` setting (EUR by default, for example `currency=RON` in `settings.properties`)
- `ImageFiles` — listing item pictures and copying new ones in without ever overwriting
- `PhoneNumbers` — phone validation and normalisation (kept as text, 7–15 digits, optional `+`)
- `Messages`, `AppSettings`, `Fxml` — localized text from the resource bundles, the saved language, and FXML loading with the active bundle

---

## ✅ Validation and Safety

Operator-facing safeguards include:

- Empty-field validation on item and player forms
- Positive-number validation for ticket counts and ticket price
- Phone number validation (7–15 digits, optional `+`; spaces and dashes are ignored)
- File accessibility checks before writing CSV files
- Confirmation dialogs before deleting items or player records
- A failed save is reported and leaves the previous data in place
- Deleting an item keeps its sales ledger in `backups/deleted/`

---

## 📋 Requirements

- To develop or build: a full JDK 21 (it includes `jpackage`) and Maven 3.9+
- To run the packaged app (see *Installing and Packaging*): nothing, it carries its own Java runtime

---

## 🚀 Running the Application

### Start in development mode

```bash
mvn clean javafx:run
```

### Run tests

```bash
mvn test
```

### Build the project

```bash
mvn clean package
```

---

## 🧪 Testing

The repository contains automated tests for:

- Draw logic (only sold tickets win, exclusions, chance proportional to tickets held, several winners, one prize per person)
- Draw history
- CSV reading and writing, including files from older versions
- Atomic saves and backups
- Phone validation, settings, money formatting and localized messages
- Sales figures (per item and for the whole raffle) and payments
- Translation guard: English and Romanian define the same keys and placeholders, and every key used in Java or FXML exists
- Models

Current test suite: 20 test classes, 89 JUnit tests (`mvn test`). The controllers are not unit tested yet.

---

## 📦 Installing and Packaging

### For users (Windows)

Download from the **Releases** page of the repository:

- `Sell-and-Win-Raffle-<version>-windows-setup.exe` — installer (per user, no administrator rights; adds a Start menu entry and a desktop shortcut; installing a newer version replaces the old one and keeps the data)
- `Sell-and-Win-Raffle-<version>-windows-portable.zip` — unzip and run `Sell and Win Raffle.exe` from any folder, no installation

Both carry their own Java runtime. The installer is not code-signed, so Windows SmartScreen may show "Windows protected your PC": choose *More info* → *Run anyway*. The data stays in `~/Sell & Win Raffle/`, outside the program folder, so updating or uninstalling never touches it.

### Build the packages (GitHub Actions)

- `.github/workflows/ci.yml` — compiles and runs the tests on every push and pull request.
- `.github/workflows/release.yml` — builds the Windows installer and the portable zip on a Windows machine:
  - push a tag such as `v1.0.0` (`git tag v1.0.0 && git push origin v1.0.0`) and the files are attached to a new GitHub Release;
  - or run it by hand from the *Actions* tab (*Windows installer* → *Run workflow*) and download the files from the run's artifacts.

### Build a package on your own machine

```bash
packaging/package.sh                # app-image: a folder with the launcher, the runtime and the app
packaging/package.sh app-image exe  # on Windows, also the installer (needs the WiX Toolset 3)
```

The result is in `target/dist`. Installers can only be built on the operating system they are for. Set `APP_VERSION` (for example `1.2.0`) to override the version from `pom.xml`.

The folder `out/artifacts/` holds old builds from before the packaging above (a plain jar and a Launch4j `.exe`, which needs Java installed). They are not updated any more.

---

## 🖥️ Screens in the Application

The JavaFX UI currently includes these views:

- Loading view
- Main view
- Add item view
- Add player view
- Draw view
- View item view
- Player status view

---

## ⚠️ Current Limitations

- CSV-based persistence (not database-backed)
- Designed for local use (not multi-user)
- Images managed through the local file system

---

## 🛣️ Future Improvements

- Replace CSV storage with a local SQLite database, with an optional remote connection
- Add sales reports and export features
- Add installer-based distribution for non-technical operators
- Track draw history and operational audit logs
