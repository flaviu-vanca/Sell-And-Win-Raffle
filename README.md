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
- 🖼️ Store and browse item images (with a configurable default image)
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
| 📦 Packaging | JavaFX Maven Plugin, Launch4j artifacts in `out/artifacts/` |

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
  settings.properties         (chosen language)
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
- `ItemSales` — tickets sold, paid and owed, and money collected/outstanding for an item (money counted in cents)
- `Payments` — marking the tickets of a buyer paid or unpaid
- `DrawSession` — one sitting: how many winners, who has won, who can still win ("one prize per person")
- `DrawHistory` — append-only audit log of draws

### UI helpers

- `ConfettiCanvas` — the confetti overlay of the draw screen
- `ItemImages` — item picture, or the bundled logo when there is none

### Persistence and utilities

- `ItemDataReaderAndWriter`, `PlayerDataReaderAndWriter` — CSV reading and writing (quotes and commas in names survive a round trip; files from older versions are still read)
- `SafeFiles` — atomic writes with a `.bak` of the previous version
- `BackupService` — start-up snapshots and archiving of deleted items
- `AppPaths` — the one place that knows where data lives
- `Money` — amounts in the active language and the `currency` setting (EUR by default, for example `currency=RON` in `settings.properties`)
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

- Java 21
- Maven 3.9+
- Windows is the primary packaged target for the included `.exe` artifact

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
- Sales figures and payments
- Translation guard: English and Romanian define the same keys and placeholders, and every key used in Java or FXML exists
- Models

Current test suite: 17 test classes, 76 JUnit tests (`mvn test`). The controllers are not unit tested yet.

---

## 📦 Included Artifacts

The repository includes generated application artifacts in:

```text
out/artifacts/Sell_and_Win_Raffle_jar/
```

That folder contains:

- `Sell_and_Win_Raffle.jar`
- `Sell & Win Raffle.exe`

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
- Improve image management and default image workflows
