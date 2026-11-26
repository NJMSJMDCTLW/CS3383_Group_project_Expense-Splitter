# Group Expense Splitter

**CS3383 Group Project — Group 19**
Version 1.0.0 · 26 November 2026

A desktop application for groups that share expenses. It records who paid, splits each
expense in one of four ways, keeps a running balance for every member, and suggests the
transfers that settle the group up.

---

## Team

| Name | Student ID | Role |
|---|---|---|
| Ng Ka Yeung | 59299349 | Project Manager |
| Wang Man Nga | 59291980 | Developer |
| Yip Man Wai | 59292809 | Tester |
| Wong Yuet Ki | 59293820 | Analysis and Design |

---

## Overview

When several people pay for each other — a trip, a shared flat, a group dinner — keeping
track of who owes what quickly becomes guesswork. This program keeps that record in one
place: every expense stores its payer and the exact share of every participant, so the
balances always add up to zero. The Settle Up tab then turns those balances into a short
list of payments, and the whole group can be saved to a file and opened again later.

The application is a Java 17 Swing desktop program. It uses no database, no network
connection and no third-party libraries.

---

## Features

- Create a group and manage its members.
- Record an expense with a description, a payer, an amount and a set of participants.
- Split an expense in four ways: **Equal**, **Exact amounts**, **Percentages** and **Shares**.
- See each member's net balance at any time; the balances always sum to zero.
- Get a suggested list of transfers that settles the group.
- Undo the last expense change, and save or open the group as a CSV file.

---

## Requirements

| Item | Requirement |
|---|---|
| Java | Java 17 or newer (JDK or JRE) |
| Operating system | Windows, macOS or Linux with Java installed (tested on Windows with Oracle JDK 17) |
| Disk space | About 1 MB |
| Other | No installer, no administrator rights, no database, no internet connection |

---

## Installation

The program does not need to be installed. The `Release` folder contains everything:

1. Install Java 17 or newer if it is not already available
   (<https://adoptium.net/> or <https://www.oracle.com/java/technologies/downloads/>).
2. Copy the `Group_19_Project` folder to any location.
3. Open a terminal in the `Release` folder and run:

   ```
   java -jar expense-splitter.jar
   ```

   On Windows you can also double-click `run.bat` in the same folder.

If `run.bat` cannot find Java, open a terminal in `Release` and use the `java -jar`
command above with the full path to your Java 17 `java.exe`.

---

## Quick Start

The example below takes about two minutes and covers the whole workflow.

1. Start the program. The window opens on the **Members** tab with the group name
   `New Group`.
2. Choose **File → New...**, enter `Weekend trip` and confirm.
3. On the **Members** tab, type `Alice` and click **Add**. Repeat for `Bob` and `Carol`.
4. On the **Expenses** tab, enter:
   - Description: `Dinner`
   - Payer: `Alice`
   - Amount: `120.00`
   - Split method: `Equal`
   - Participants: tick `Alice`, `Bob` and `Carol`

   Click **Add**. The expense appears in the table.
5. Open the **Balances** tab. Alice has `$80.00`, Bob has `-$40.00` and Carol has
   `-$40.00` (positive means the member should receive money).
6. Open the **Settle Up** tab. The program suggests:

   ```
   Bob pays Alice $40.00
   Carol pays Alice $40.00
   ```

7. Choose **File → Save...**, enter `trip.csv` and confirm. The file is written to disk
   and can be opened again with **File → Open...**.

---

## Usage Guide

### Members tab

Type a name and click **Add** to add a member. Select a row and click **Remove selected**
to remove one.

- Names are trimmed, must not be blank, must be at most **30 characters** and must not
  contain line breaks.
- Two names that differ only in capitalisation or surrounding spaces are treated as the
  same member.
- A member who is the payer or a participant of an existing expense cannot be removed.
  Remove those expenses first.

### Expenses tab

Enter a description, choose the payer, enter the amount, choose a split method and tick
the participants. The input fields change according to the chosen method. Click **Add**
to record the expense, or select a row and click **Remove selected** to delete one.

| Method | How the amount is divided |
|---|---|
| Equal | The total is divided equally among the participants. Leftover cents are given one each, in participant order, until none remain. |
| Exact amounts | You enter the amount for every participant. The amounts must add up to the total exactly. |
| Percentages | You enter a percentage for every participant. The percentages must add up to 100, with at most two decimals. |
| Shares | You enter a weight for every participant. Each share is proportional to its weight. The weights must be zero or positive, with at most two decimals. |

Example: `$10.00` split equally between three people gives `$3.34`, `$3.33` and `$3.33`.
`$40.00` split with weights `2 : 1 : 1` gives `$20.00`, `$10.00` and `$10.00`.

### Balances tab

Shows one row per member with the net amount that member has paid or still owes.
A positive amount means the member should receive money; a negative amount means the
member should pay. All balances in a group always add up to `$0.00`.

### Settle Up tab

Shows a short list of payments that brings every balance to zero, for example
`Bob pays Alice $40.00`. The calculation is greedy: it repeatedly matches the largest
debtor with the largest creditor, so it always produces at most one fewer transfer than
the number of members. It is not guaranteed to be the smallest possible number of
transfers.

### Undo

**Edit → Undo expense change** (or `Ctrl+Z`) reverses the last add or removal of an
expense. Only expense changes can be undone; there is no redo. Starting a new group,
opening a file or removing a member clears the undo history.

### Save, Open and New

- **File → Save...** writes the group, its members, its expenses and the next expense
  number to a CSV file. If the file name does not already end with `.csv`, the extension
  `.csv` is appended (for example, `trip.data` becomes `trip.data.csv`). The program
  asks for confirmation before replacing an existing file.
- **File → Open...** loads a saved group and replaces the current one. If the current
  group already has members or expenses, the program asks for confirmation first. A file
  that cannot be read is rejected with an error message and the current data is left
  unchanged.
- **File → New...** starts an empty group with a name you choose.

---

## Rules and Limits

| Item | Rule |
|---|---|
| Group name | Not blank, no line breaks |
| Member name | Trimmed, not blank, at most 30 characters, no line breaks, unique ignoring case and surrounding spaces |
| Expense description | Not blank, no line breaks |
| Expense amount | Greater than 0 and at most 1,000,000.00 |
| Participants | At least one; a participant may have a zero share |
| Split totals | Every share is a whole number of cents and the shares must add up to the amount exactly |
| Percentages | Must add up to 100, at most two decimals |
| Shares | Zero or positive weights, at most two decimals, not all zero |
| Expense numbers | Increase only and are never reused, even after undo |
| Undo | Expense add and remove only; no redo |
| Currency | A single currency, shown with the `$` symbol, two decimals |

---

## File Format

A saved group is a UTF-8 CSV file. The first line identifies the format and its version;
every following line is one record. The file starts with a byte order mark so that Excel
displays non-ASCII names correctly. A file without the byte order mark is still accepted.

```
EXPENSE_SPLITTER,1
GROUP,Weekend trip
NEXT_ID,2
MEMBER,Alice
MEMBER,Bob
MEMBER,Carol
EXPENSE,1,Dinner,Alice,EQUAL,12000
SHARE,1,Alice,4000
SHARE,1,Bob,4000
SHARE,1,Carol,4000
```

| Record | Fields |
|---|---|
| `EXPENSE_SPLITTER` | format marker and version |
| `GROUP` | group name |
| `NEXT_ID` | the number that will be given to the next expense |
| `MEMBER` | member name |
| `EXPENSE` | id, description, payer, split method (`EQUAL`, `EXACT`, `PERCENTAGE` or `SHARES`), total in cents |
| `SHARE` | expense id, member, share in cents |

Fields that contain a comma or a double quote are quoted, and a double quote inside a
field is written twice. Amounts are stored as whole cents, so `12000` means `$120.00`.

---

## Version History

### v1.0.0 — 26 November 2026

First full release: members, expenses with four split methods, balances, settle-up
suggestions, undo of expense changes, and CSV save and open.

Bugs found and fixed during development:

- #1 Undo could get stuck after a member was removed; removing a member now clears the undo history.
- #2 Non-ASCII member names appeared garbled in Excel; CSV files are now written with a UTF-8 BOM.
- #3 Saving to an invalid file path failed silently; the program now shows an error message.
- #5 A `NEXT_ID` of `Integer.MAX_VALUE` overflowed; the counter is now stored as a long and overflow is rejected.
- #6 Blank participant values were accepted as zero in non-Equal splits; they are now rejected.
- #7 The expense form kept values from the previous group after New/Open; it is now cleared.
- #9 Adding an expense to an empty group showed a misleading payer error; it now asks for a member first.
- #10 A file name containing a dot did not get `.csv` appended; `.csv` is now appended unless already present.
- #13 The member name field was not cleared after New/Open; it is now cleared.
- #14 The split method dropdown was not visible at startup; the form layout and minimum window size were fixed.

---

## Known Limitations and Future Work

- Settle Up uses a greedy algorithm and may not produce the smallest possible number of
  transfers.
- Undo covers expense changes only; there is no redo and no undo for member changes.
- Only one currency (`$`) and amounts with two decimals are supported.
- One group is stored per file, and only one person edits a file at a time.
- No export to PDF or Excel, no charts, and no web or mobile version.
- The user interface is in English only.
- Closing the window does not ask to save unsaved changes; use **File → Save...** before closing.

---

## Project Structure and Build

```
Group_19_Project/
├── README.md
├── ActivityLog.xlsx
├── Docs/                     reports and diagrams
├── Release/
│   ├── expense-splitter.jar  executable
│   ├── run.bat               Windows launcher
│   └── *.csv                 sample data
└── Source/                   Maven project (Java 17, JUnit 5, JaCoCo)
    ├── pom.xml
    └── src/
```

To build from source:

```
cd Source
mvn package
java -jar target/expense-splitter.jar
```

JUnit 5 and JaCoCo are configured in `pom.xml`: `mvn test` runs the test suite in
`src/test/java`, and `mvn verify` also writes the coverage report to
`target/site/jacoco/index.html`.

Building from source requires JDK 17 and Maven 3.x; running the released jar requires only Java 17.

---

## AI Usage

Parts of the source code were drafted with an AI coding assistant and then reviewed and
refined by the team. Each generated class carries an `@AIGenerated` annotation that
records the tool and the prompt identifier, commit messages use the prefixes `[AI]`,
`[REFINE]` and `[HUMAN]`, and the full record of prompts, generated files and human
changes is kept in `Docs/AI_Usage_Log.md`.
