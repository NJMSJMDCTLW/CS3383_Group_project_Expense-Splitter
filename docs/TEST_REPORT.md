# Test Report — Group Expense Splitter

DRAFT — tester deliverable. All tests are in English. Structure: 81 test cases across the six packages
`model`, `split`, `service`, `command`, `storage`, `gui` (11–15 per package; raised from the initial
10-per-package draft to cover the full HANDOVER Part 2 checklist).

## 1. Test strategy

Bottom-up integration, matching the dependency direction of the design:
`exception` → `model` → `split` → `service` (helpers + `ExpenseServiceImpl`) → `command` → `storage` → `gui`.
Lower layers are tested first against their own contracts; `service` and `gui` are tested on top of the real
lower layers, with a fake `Repository` and fake views at the boundary so no real file or Swing window is used.

Techniques used:

| Technique | Example |
|---|---|
| Boundary value analysis | 30/31-char names, id 0/1, total 0 / 1,000,000.00 / 1,000,000.01, 2 vs 3 decimals, `restoreNextExpenseId` bounds, UTF-8 BOM bytes, long overflow |
| Equivalence classes | blank/null/negative/non-numeric amounts; line-break names; empty/duplicate participants; zero-share participants; CSV error branches |
| Decision table | `Group.removeMember` (payer / participant / unused), `SplitStrategyFactory` method → strategy, `MainPresenter` confirm-or-not, settle-up ties |
| State-based | id allocation and undo, sorted insertion, undo of a removal restoring position, LIFO undo, failed `open` keeping data |
| Observer branch | listener notified once on success, never on failure, never on `save`; duplicate listener ignored |

Every expected value was computed by hand from the rules in `CLAUDE.md` and `docs/DESIGN.md`, not copied
from the code.

## 2. Test index (by package / rule)

### `model` — `ModelTest` (14)
Money parse/format/arithmetic/overflow; Member length, validation, identity; Group name rules, duplicate
member, `removeMember` decision table, `addExpense` membership, id allocation + `restoreNextExpenseId`
bounds, sorted insertion; Expense total bounds, description validation, share validation (empty/negative/
sum≠total); Transfer rules and `toString`.

### `split` — `SplitTest` (14)
EQUAL 10c/3, 100c/3, 100.00/3 leftover examples and invalid input; EXACT sum, invalid, too-large; PERCENTAGE
99c 50/30/20 and 80/0/20, exact-sum rejection, 3-decimal and negative; SHARES 0,1,1 → 0,51,50, all-zero,
negative; factory decision table; R1 (1.005 rejected).

### `service` — `ServiceTest` (14)
Balances + settle-up canonical cases (A pays 90 for 3; two creditors one debtor; circular debt cancels;
tie broken by insertion order; no expenses → no transfers/all zero); ids survive undo; undo of removal
restores position; undo empty; remove member clears history (R2); new group clears history; listener
notification branches; addListener(null) / duplicate listener; unmodifiable collections; failed `open`
keeps data.

### `command` — `CommandTest` (11)
Add/Remove execute+undo; null and unknown-id rejections; `undo` before `execute` → `IllegalStateException`;
`run` makes a command undoable; empty-stack undo; null command; failed execute not remembered; `clear`;
LIFO order of multiple undos.

### `storage` — `FileRepositoryTest` (15, `@TempDir`)
Round trip; next id preserved; commas/quotes; Chinese names; UTF-8 BOM bytes EF BB BF; no-BOM load; missing
and empty files; wrong version / unknown type; shares not summing; duplicate id and SHARE-before-EXPENSE;
bad number; duplicate share for one member; unclosed quote; non-UTF-8 file; SHARE for unknown member.

### `gui` — `GuiTest` (13)
`InputParser` money (valid, spaces, empty, negative, 3-decimal, huge) and decimal; inputs blank→0; method;
`MoneyFormatter` ($ and -$); `ExpensesPresenter` clears only on success, remove errors; `MembersPresenter`
add/remove/error; `BalancesPresenter`/`SettleUpPresenter` start; `MainPresenter` confirms before New/Open
only when data exists and keeps data when the user declines.

## 3. Current count

| Package | Test class | Tests |
|---|---|---|
| model | ModelTest | 14 |
| split | SplitTest | 14 |
| service | ServiceTest | 14 |
| command | CommandTest | 11 |
| storage | FileRepositoryTest | 15 |
| gui | GuiTest | 13 |
| **total** | | **81** |

(`mvn test`: `Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`.)

## 4. Coverage

JaCoCo per-package coverage is produced by `mvn verify` → `target/site/jacoco/index.html`. It will be
summarised here after the code freeze.

## 5. MC/DC table

To be added for the `Expense` constructor conditions in a later revision.

## 6. Notes / gaps

- Package-private helpers `SplitSupport`, `BalanceCalculator`, `SettlementCalculator` are exercised
  indirectly through `split`/`service`; there is no dedicated same-package test class for them in this
  structure (could be added if the team wants direct coverage).
- Documented difference (not a bug): `Money.parse` is textual (1–2 decimals, `"12.500"` rejected) while the
  split two-decimal check is numeric (`12.500` accepted).
- The `getExpenses()` unmodifiable check and `save` write-failure atomicity are not asserted explicitly.
- No production bug found so far; further manual exploratory testing is still needed to reach the
  ">10 real bugs" target of the marking scheme.
