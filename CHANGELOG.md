# Changelog

## 1.0.0 (not released yet)

The first version meant to be installed and used at an event. All of it came in through
[pull request #1](https://github.com/flaviu-vanca/Sell-And-Win-Raffle/pull/1), in the order below (each step is one or
more commits that can be read on their own).

### Reliability
- Saves are crash-safe (temporary file, then replace; a `.bak` of the previous version), and a snapshot of the data is
  taken at every start (the newest 20 are kept). A failed backup never stops the application.
- Phone numbers are text, validated (7 to 15 digits, optional `+`), instead of numbers that overflowed.
- Deleting an item moves its ticket ledger to `backups/deleted/`; the title can be used again.
- The draw uses `SecureRandom`, picks the winner among sold tickets only, at the moment of Stop, and logs every winner.
- Errors go to `logs/app.log` as well as the console (the installed application has no console).

### Look and feel
- Every screen in English and Romanian (`i18n/messages*.properties`), a language button, a test that both bundles have
  the same keys and placeholders and that every key used exists.
- The draw as a show: full screen (`F11`), confetti, several winners, "one prize per person", keyboard only. No phone
  numbers on the draw screen.
- Dark and light theme from one stylesheet (colours are variables), a sales dashboard on the main screen, item pictures
  managed from inside the application.

### Money
- Price per ticket, "paid now" or owed, the amount to pay while typing, figures per item and for the whole raffle, mark
  a buyer paid or unpaid later. Amounts in the active language and the `currency` setting (EUR by default).
- Every ticket records the price it was sold for: changing the price of an item never changes what earlier buyers paid
  or owe.
- Edit Item: description, price and number of tickets (more tickets are added at the end; fewer only while the last ones
  are unsold).

### Storage
- All data goes through a `RaffleRepository` interface; the screens do not touch files. The rules (selling, taking back,
  creating and deleting items, finding tickets) live in services that are unit tested.
- One SQLite file, `data/raffle.db`, one transaction per change, a versioned layout with step-by-step migrations (a
  database from a newer version is refused, not damaged).
- On the first start without a database, the CSV files of earlier versions are converted into a temporary file that gets
  its name only when everything was copied and checked. The CSV files are left untouched; if the conversion fails the CSV
  stays in use and a message says why.
- *Export CSV* writes everything back out as CSV files in `exports/`.
- The CSV and the SQLite repository pass the same contract tests.

### Printed documents
- Sales report, receipt per purchase, certificate per winner: HTML pages in the active language that open in the browser
  (print, or *Save as PDF*). Names are escaped. The optional `organizer=` line in `settings.properties` puts the
  organizer's name on them. They are kept in `reports/`.

### Packaging
- `packaging/package.sh` builds a self-contained application (own Java runtime, Romanian number and date formats
  included) with `jpackage`.
- GitHub Actions: tests on every push and pull request; the *Windows installer* workflow builds the `.exe` installer and
  a portable zip on every pull request (as an artifact), by hand, and on a `v*` tag (published as a GitHub Release).

## Verification status

| What | Status |
|------|--------|
| 230 JUnit tests in 32 classes | Pass locally and in CI (Linux) |
| The same tests on Windows | Pass in the *Windows installer* workflow (it caught two Windows-only test problems, both fixed) |
| Every feature on screen (Linux, test data) | Done, including the first-start CSV conversion, a database migration from layout 1 to 2, the packaged application and the printed documents rendered to PDF |
| The installer on a real Windows computer | **Not done.** The author of this work could not run Windows; the person who owns the project has to install it and try it (see *Releasing* in the README) before it is handed to anyone |
| A browser opening the printed documents by itself | **Not seen.** The files are written correctly; opening them relies on the computer's default browser |
| Code signing | Not done (needs a certificate) |

## Decisions taken

- Data in SQLite by default; CSV is for importing, exporting and as a fallback when a conversion fails. Not two equal
  stores.
- Printed documents are HTML, not PDF: no PDF library has to be packaged (it would break the `jlink` runtime), and every
  browser can save a page as PDF.
- No QR code on receipts: nothing in the application would read it.
- The old builds in `out/artifacts/` (a plain jar and a Launch4j `.exe`) are left in the repository and are not updated.
  Delete them once a release exists.

## Open items

- **Remote connection** (not started). The scenario decides the design: several operators on one raffle at the same time
  (a shared database, with conflicts to handle), or one own server the application connects to. It is still to be decided.
- QR codes on receipts, only if they will be scanned at the draw.
- Controllers have no unit tests of their own; they only show what the services return.
