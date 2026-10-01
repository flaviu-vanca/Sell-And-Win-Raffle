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

Data is stored locally in a single **SQLite database file** (plus item-specific image folders) under the user’s home directory, so nothing has to be installed or configured, and every change is saved as one all-or-nothing transaction. All data can be exported as CSV files at any time.

---

## ✨ Key Features

- 🏷️ Create raffle items with title, description, ticket count, and ticket price
- ✏️ Edit an item later (**Edit Item**): description, ticket price and number of tickets. More tickets are added at the end; fewer only while the last ones are unsold. Every ticket remembers the price it was sold at, so changing the price never changes what earlier buyers paid or owe
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
- 🛟 Crash-safe saves (every change is one database transaction) and an automatic backup on every start-up
- 🗃️ Data in one SQLite file; the CSV files of earlier versions are converted automatically on the first start (and left untouched), and an **Export CSV** button writes everything back out as CSV files for a spreadsheet
- 🖨️ Printed documents, as pages that open in the browser (print them, or choose *Save as PDF*): a **sales report** (the figures of every item, the buyers of each item with their tickets and what they paid or owe, a list of payments still to collect, the winners), a **receipt** for a purchase (select the buyer on the sales screen, press *Receipt*) and a **certificate** for every winner drawn (*Certificate* on the draw screen). The name of the organizer on them comes from the optional `organizer=` line in `settings.properties`
- 📊 A dashboard on the main screen: tickets sold, money collected, money still owed and what selling everything would bring, plus a progress bar and the collected/owed amount for every item
- 🎨 Dark and light theme, switched with a button on the main screen and remembered; one stylesheet whose colours are variables, so a new theme is a handful of lines
- 🌍 English and Romanian: every screen is translated, the language follows the system language at first and can be switched with the `RO`/`EN` button on the main screen (the choice is remembered in `settings.properties`). Texts live in `i18n/messages*.properties`
- 🧰 Windows **installer** and portable zip with their own Java runtime, built by GitHub Actions

---

## 🧱 Tech Stack

| Area | Details |
|------|---------|
| 💻 Language | Java 21 |
| 🧩 UI | JavaFX 21, FXML, CSS |
| 🏗️ Build | Maven |
| ✅ Testing | JUnit 5 |
| 💾 Storage | SQLite (`sqlite-jdbc`), CSV import and export |
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
    raffle.db                 (everything: items, tickets, draw history)
  <item-title>/
    image files...
  settings.properties         (language, theme, currency and the optional organizer name)
  reports/
    <yyyyMMdd-HHmmss>-sales-report.html, ...-receipt.html, ...-certificates.html   (the printed documents, kept)
  exports/
    <yyyyMMdd-HHmmss>/        (what the Export CSV button writes: data/ and records/ in the CSV layout below)
  backups/
    <yyyyMMdd-HHmmss>/        (snapshot of data/ taken at each start-up, newest 20 kept)
    deleted/                  (ledgers of deleted items are kept here as CSV, never destroyed)
```

### The database

`data/raffle.db` is a SQLite file with three tables: `items` (title, description, picture, price), `tickets` (one row per ticket of an item: buyer name and phone, paid, time of sale and the price it was sold for) and `draws` (the draw history). How many tickets a buyer holds is worked out from the tickets, not stored. The layout is versioned (`PRAGMA user_version`): a database made by an older version of the application is brought up to date step by step when it is opened (step 2 added the price of each ticket), and one made by a *newer* version is refused instead of being damaged.

### Moving from the CSV files of earlier versions

If there is no database yet but `data/data.csv` exists, the first start converts the CSV files into `data/raffle.db` (into a temporary file that only gets its real name once everything was copied and checked), tells you how many items and tickets were moved, and **leaves the CSV files exactly as they were**. If the conversion fails, the CSV files stay in use for that session and a message says why, so nothing is lost and the application never starts empty. A copy of the data is also made in `backups/` before every start.

### CSV files

The CSV layout is still used for the conversion above, for the **Export CSV** button and for the archived ledgers of deleted items:

#### `data/data.csv`

The master item catalog:

- Image path
- Title
- Description
- Available tickets
- Ticket price

#### `data/draws.csv`

Append-only history of every draw: time, item, winning ticket, winner name and phone number.

#### `records/<item-title>.csv`

The ticket ledger of one raffle item:

- Ticket ID
- Player name
- Phone number
- Number of tickets associated with that buyer
- Paid (`true`/`false`), the time of sale (UTC, ISO-8601) and the price the ticket was sold for

Ledgers written by older versions have fewer columns. They are read as they are (sold tickets without payment information count as paid, since payments were not tracked; tickets without a recorded price count at the item's price) and get the new columns the next time they are saved. When a price is changed, sold tickets without a recorded price are pinned to the price the item had before.

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
        storage/
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
        storage/
        ui/
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
- `SalesService` — sells tickets (each at the item's price at that moment), takes them back and records payments (validates, reads the ledger, changes it, saves it); `TicketSales` holds the pure ticket logic behind it
- `ItemService` — adds, edits and deletes items (folder for the pictures, unsold tickets, the rules of an edit); `overview()` lists the items with their sales figures
- `TicketLookup` — finds tickets by number, phone or name for the status screen
- `ValidationException` — carries the key of the message to show, so services know nothing about language or screens

### Reports

`raffle.reports` makes the printed documents. `ReportService` works out what they show from the repository (the sales report with buyers grouped per person and every ticket at the price it was sold for, the receipt of one purchase, the winners' certificates); `HtmlReports` turns that into complete HTML pages in the active language, with everything typed by the operator escaped; `ReportFiles` saves them in `reports/` and `Documents` opens them in the browser. Pages are HTML on purpose: any computer can show and print them, *Save as PDF* is in every browser, and no PDF library has to be packaged.

### Storage

All data goes through the `RaffleRepository` interface (`raffle.storage`), so the screens and services do not know where the data lives.

- `RaffleRepository` — items, ticket ledgers and the draw history; every call either completes or fails and leaves the data as it was
- `SqliteRaffleRepository` — the database (one transaction per change); `SqliteSchema` holds the versioned table layout and its migrations
- `CsvRaffleRepository` — the CSV file layout described under *Data Storage*, used for import, export and as a fallback
- `CsvTransfer` — copies everything from one repository to another (CSV import, CSV export)
- `Storage` — opens the database at start-up, converting the CSV files of an earlier version when there is no database yet

### UI helpers

- `Theme` — dark/light theme (a style class on the screen root) and its saved setting
- `Dialogs` — the one place that builds alerts and Yes/No questions, so they follow the theme and the language
- `ConfettiCanvas` — the confetti overlay of the draw screen
- `ItemImages` — item picture, or the bundled logo when there is none

### Persistence and utilities

- `ItemDataReaderAndWriter`, `PlayerDataReaderAndWriter` — CSV reading and writing used by the CSV repository (quotes and commas in names survive a round trip; files from older versions are still read)
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
- Positive-number validation for ticket counts and ticket price (the price can be typed as `12.50` or `12,50`)
- Phone number validation (7–15 digits, optional `+`; spaces and dashes are ignored)
- A file that is open in another program (Excel) makes the save fail with a clear message instead of losing data
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
- The printed documents: grouping of buyers, who owes what, one receipt per purchase, certificates, escaping of names, English and Romanian text and no untranslated key
- Selling and taking back tickets (random distinct tickets, several purchases by one buyer, totals per buyer), items (creation, duplicates, pictures, deletion with archived ledger) and the status lookup
- The storage contract: the CSV and the SQLite repository must pass the same tests; the database layout and its versioning, the conversion of CSV files on the first start (including a failing one), and CSV export/import round trips
- CSV reading and writing, including files from older versions
- Atomic saves and backups
- Phone validation, settings, money formatting and localized messages
- Sales figures (per item and for the whole raffle) and payments
- Translation guard: English and Romanian define the same keys and placeholders, and every key used in Java or FXML exists
- Models

Current test suite: 31 test classes, 224 JUnit tests (`mvn test`). The controllers are not unit tested yet; they only show what the services return.

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
  - or run it by hand from the *Actions* tab (*Windows installer* → *Run workflow*) and download the files from the run's artifacts (the button appears once this workflow file is on the default branch);
  - it also builds (without publishing) whenever something in `packaging/`, the workflow itself, `pom.xml` or `module-info.java` changes.

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
- Edit item view
- Add player view
- Draw view
- View item view
- Player status view

---

## ⚠️ Current Limitations

- One operator at a time: the database file is opened by one running copy of the application
- Designed for local use (not multi-user)
- Images managed through the local file system

---

## 🛣️ Future Improvements

- An optional remote connection (several operators on one raffle)
- QR codes on receipts, if they will be scanned at the draw
