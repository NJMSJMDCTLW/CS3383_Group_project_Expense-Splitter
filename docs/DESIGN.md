DRAFT — to be reviewed by the analysis/docs member
Decisions in section 6 agreed with the analysis/docs member on 2026-11-21 (reported by the developer).

# Design Proposal: Group Expense Splitter

Scope: model classes, `SplitStrategy`, `ExpenseService`, `ServiceListener`, and the MVP contracts of the GUI.
Only what the four GUI tabs (Members, Expenses, Balances, Settle Up) and Undo / New / Open / Save need.
This is a design only; no implementation code.

## 1. Conventions
- All classes are immutable except `Group`, which holds the mutable state.
- Money is `long` cents. Display uses 2 decimals. `Money.toString()` and `Transfer.toString()` contain
  no currency symbol; the symbol is known only to `gui.MoneyFormatter`.
- Two exceptions are used, both in the package `com.splitter.exception` (section 1.1):
  - `ValidationException(message)` (unchecked) for rejected input. Its message is shown to the user in a `JOptionPane`.
  - `StorageException(message, cause)` (checked) for I/O or file-format failures.
- Packages: `exception`, `model`, `split`, `service`, `command`, `storage`, `gui`. Dependencies follow CLAUDE.md.
- Every generated class gets `@AIGenerated`.

### 1.1 `exception` package
Depends on nothing; every other package may use it. It holds `ValidationException extends RuntimeException`
and `StorageException extends Exception`. The exceptions are not in `service` because `model`, `split` and
`storage` throw them, and `model` must not depend on `service` (that would create a dependency cycle).

## 2. `model` package

### 2.1 `Money` (final value class, wraps `long cents`)
| Method | Input → Output | Validation / errors |
|---|---|---|
| `static Money ofCents(long)` | cents → Money | none (negative allowed, for balances) |
| `static Money parse(String)` | "12.5", "12.50", "12" → Money | accepts only non-negative plain decimals with at most 2 decimals; `ValidationException` if null, blank, a leading minus, non-numeric, more than 2 decimals, or overflow |
| `static final Money ZERO` | – | – |
| `long cents()` | → cents | – |
| `Money plus(Money)` / `minus(Money)` | → Money | `ArithmeticException` is wrapped as `ValidationException` on overflow |
| `Money negate()` | → Money | – |
| `int signum()` | → -1, 0 or 1 | – |
| `compareTo`, `equals`, `hashCode` | – | – |
| `String toString()` | → "12.34" or "-12.34" (no currency symbol) | – |

Negative `Money` arises only from `ofCents`, `minus` and `negate`.

### 2.2 `Member` (immutable, identity is the comparison key)
The comparison key is the trimmed name lower-cased with `Locale.ROOT`. `equals`, `hashCode`, `Group.hasMember`
and the duplicate-name check all use this key. `getName()` keeps the user's own casing (trimmed).

| Method | Output | Validation |
|---|---|---|
| `Member(String name)` | Member | `ValidationException` if null, blank after trim, longer than 30 characters after trim, or containing a line break |
| `String getName()` | name (trimmed, user's casing) | – |
| `equals`, `hashCode`, `toString` | by comparison key (`toString` returns the name) | – |

### 2.3 `Group` (mutable aggregate: members and expenses)
| Method | Output | Validation |
|---|---|---|
| `Group(String name)` | Group | name not blank, no line breaks |
| `String getName()` | name | – |
| `void addMember(Member)` | – | duplicate (same comparison key) → `ValidationException` |
| `void removeMember(Member)` | – | not in group → error; member is the payer or a participant (including a zero share) of any existing expense → error |
| `List<Member> getMembers()` | unmodifiable, in insertion order | – |
| `boolean hasMember(Member)` | boolean | – |
| `Expense addExpense(Expense)` | the stored expense | payer or any participant not in group → error; id already present → error. Inserts by id, so the list stays sorted by id |
| `void removeExpense(int id)` | – | unknown id → error |
| `List<Expense> getExpenses()` | unmodifiable, sorted by id | – |
| `int allocateExpenseId()` | the current counter value; the counter then advances by one | – |
| `int getNextExpenseId()` | the counter value, without advancing (used when saving) | – |
| `void restoreNextExpenseId(int next)` | – | used when loading; `next` must be ≥ 1 and greater than every expense id currently in the group, otherwise `ValidationException` |

- The counter starts at 1. Expense ids only increase and are never reused, even after undo. Undoing an
  `AddExpenseCommand` removes the expense but does not decrement the counter. An id that was allocated but
  whose expense was rejected is simply skipped.
- Because `addExpense` inserts by id, undoing a `RemoveExpenseCommand` puts the expense back at its original position.
- The `command` package needs these public methods, because `command` may depend only on `model` (and `exception`).

### 2.4 `SplitMethod` (enum)
`EQUAL, EXACT, PERCENTAGE, SHARES`. It also has `getDisplayName()` for the combo box.

### 2.5 `Expense` (immutable)
| Method | Output | Validation |
|---|---|---|
| `Expense(int id, String description, Member payer, Money total, SplitMethod method, Map<Member,Money> shares)` | Expense | id ≥ 1; description not blank and without line breaks; payer not null; total > 0 and at most 100,000,000 cents (1,000,000.00); shares not empty; every share ≥ 0; the shares sum exactly to total; otherwise `ValidationException` |
| `getId`, `getDescription`, `getPayer`, `getTotal`, `getMethod` | – | – |
| `Map<Member,Money> getShares()` | unmodifiable, ordered | – |
| `List<Member> getParticipants()` | keys of shares | – |

An expense stores the computed shares. That makes balances and saved files independent of the strategy code.
A participant with a zero share stays in `shares` with `Money.ZERO`. Such a member still counts as a
participant, so the member cannot be removed from the group.
The total limit (100,000,000 cents) is exposed as `public static final long Expense.MAX_TOTAL_CENTS`; `ExpenseService.addExpense` (4.2) reuses it instead of defining its own.

### 2.6 `Transfer` (immutable)
| Method | Output | Validation |
|---|---|---|
| `Transfer(Member from, Member to, Money amount)` | Transfer | from ≠ to; amount > 0 |
| `getFrom`, `getTo`, `getAmount` | – | – |
| `toString()` | "A pays B 12.34" (no currency symbol) | – |

## 3. `split` package (Strategy and Factory)

### 3.1 `SplitStrategy`
```
Map<Member, Money> split(Money total, List<ParticipantInput> inputs)
SplitMethod method()
```
- **Input:** the list order is the participant order, and it is the rounding order. The value of each
  `ParticipantInput` (3.3) has a meaning that depends on the strategy:

| Strategy | Meaning of the value |
|---|---|
| `EQUAL` | ignored |
| `EXACT` | the amount in currency units, at most 2 decimals |
| `PERCENTAGE` | a percent, at most 2 decimals |
| `SHARES` | a share count (weight) |

- **Output:** ordered map (participant order), member → Money. It always sums exactly to `total`.
- **Rounding rule:** each participant first gets the floor of the exact share, in cents:

| Strategy | Base amount for each participant |
|---|---|
| `EQUAL` | floor(total / n) |
| `PERCENTAGE` | floor(total × pct / 100) |
| `SHARES` | floor(total × weight / sum of weights) |
| `EXACT` | the given amount; no rounding |

  The leftover cents are the total minus the sum of the base amounts (always fewer than the number of
  participants). They are given one cent each in participant order, starting from the first participant, but
  only to participants whose percentage or weight is greater than zero (for `EQUAL`: everyone). `EXACT` has
  no leftover. The calculation uses `BigDecimal` without intermediate rounding.
- **Validation errors (`ValidationException`):**
  - Common to all: total ≤ 0; inputs empty; the same member appears twice in the list; a null member or value.
  - `EXACT`: any amount < 0, more than 2 decimals, or the amounts don't sum to total.
  - `PERCENTAGE`: any value < 0, more than 2 decimals, or the sum is not equal to 100 (compared exactly with
    `BigDecimal.compareTo`, no tolerance; so 33.33 × 3 is rejected and users should use `EQUAL`).
  - `SHARES`: any value < 0, or all values are zero.
  - `EQUAL`: none beyond the common ones.

### 3.2 `SplitStrategyFactory`
```
static SplitStrategy forMethod(SplitMethod m)
```
It throws `ValidationException` for null. Implementations are `EqualSplit`, `ExactSplit`, `PercentageSplit` and `SharesSplit`.

### 3.3 `ParticipantInput`
An immutable record `ParticipantInput(Member member, BigDecimal value)` in package `split`. Both components must
be non-null (`ValidationException` otherwise). One `BigDecimal` is used for all strategies, and its meaning
depends on the strategy (see decision record D6).

## 4. `service` package

### 4.1 `ServiceListener` (Observer)
```
void onDataChanged()
```
It is called after any successful change: a member added or removed, an expense added or removed, undo, new, or open. It is not called when a call fails validation. Listeners (the presenters, section 4.4) re-read state through the service.

### 4.2 `ExpenseService` (the only service type the GUI uses)

**Listeners**
| Method | Notes |
|---|---|
| `void addListener(ServiceListener)` | null → `ValidationException` |
| `void removeListener(ServiceListener)` | – |

**Members tab**
| Method | Output | Errors |
|---|---|---|
| `void addMember(String name)` | – | name rules of 2.2; duplicate name |
| `void removeMember(Member m)` | – | not in group; payer or participant of any expense |
| `List<Member> getMembers()` | list, insertion order | – |

**Expenses tab**
| Method | Output | Errors |
|---|---|---|
| `Expense addExpense(String description, Member payer, Money amount, SplitMethod method, List<ParticipantInput> inputs)` | stored expense | blank description; amount ≤ 0 or above 100,000,000 cents; empty participants; payer not in group; any participant not in group; plus the strategy errors from 3.1. The payer does not have to be a participant |
| `void removeExpense(int id)` | – | unknown id (undoable) |
| `List<Expense> getExpenses()` | list, sorted by id | – |

**Balances and Settle Up tabs**
| Method | Output | Rule |
|---|---|---|
| `Map<Member,Money> getBalances()` | ordered by member insertion order; includes members with balance zero; balance = paid − owed; positive = is owed | the sum of all balances is always 0 |
| `List<Transfer> getSettlements()` | greedy: the largest debtor pays the largest creditor, repeat | at most n−1 transfers; members with balance zero do not appear; ties between equal balances are broken by member insertion order, so the result is deterministic |

Settle-up only suggests transfers. Payments are not recorded; after people have paid, the user starts a New group.

**Undo, and New / Open / Save**
| Method | Output | Errors |
|---|---|---|
| `boolean canUndo()` | boolean | – |
| `void undo()` | – | nothing to undo → `ValidationException` |
| `void newGroup(String groupName)` | – | blank name; clears undo history |
| `void save(Path file) throws StorageException` | – | null path; I/O failure → `StorageException` |
| `void open(Path file) throws StorageException` | – | missing, unreadable, empty, wrong-version or corrupt file → `StorageException` with a clear message; a failed open leaves the current data unchanged; clears undo history |
| `String getGroupName()` | name | – |

**Notes**
- The implementation, `ExpenseServiceImpl`, takes a `Repository` and a `CommandHistory` in its constructor.
- Undo covers expense add and expense remove only (`AddExpenseCommand`, `RemoveExpenseCommand`). There is no undo
  for member changes and no Redo.
- The service allocates the expense id (`Group.allocateExpenseId()`), builds the `Expense`, and hands it to the command.

### 4.3 `storage.Repository`
```
void save(Group g, Path file) throws StorageException
Group load(Path file) throws StorageException
```
The format is CSV with no third-party library. It stores the group name, the next expense id, the members and
the computed shares of each expense (not the original inputs). Proposed layout:
```
EXPENSE_SPLITTER,1                              <- format-version marker (first line)
GROUP,<name>
NEXT_ID,<n>
MEMBER,<name>
EXPENSE,<id>,<description>,<payer>,<method>,<totalCents>
SHARE,<expenseId>,<member>,<cents>
```
- Fields are quoted and escaped, so commas and quotes in text are safe. Descriptions and member names may not
  contain line breaks (`ValidationException` when they are created).
- An empty file, a wrong version, an unknown record type, a bad number or an inconsistent file (for example shares
  that do not sum to the total, or an unknown member) throws `StorageException`.
- `load` restores the next expense id with `Group.restoreNextExpenseId`.

### 4.4 `gui` package (MVP)
The gui package uses MVP. It may depend on `service`, `model` and `exception`: `ExpenseService` exposes the model
value classes (`Member`, `Money`, `Expense`, `SplitMethod`, `Transfer`) and `ParticipantInput`. The Swing panels and the
main frame implement the view interfaces. The presenters depend on `ExpenseService` and on their view interface,
implement `ServiceListener`, and must not import `javax.swing` or `java.awt`. Swing components never implement
`ServiceListener`. Presenters catch `ValidationException` and `StorageException` and pass the message to the view's
`showError`. The application's main class wires service, presenters and views (not designed here).

**Helpers (no Swing)**
| Class | Methods | Notes |
|---|---|---|
| `InputParser` | `Money parseMoney(String)`, `BigDecimal parseDecimal(String)`, `SplitMethod parseMethod(String displayName)`, `List<ParticipantInput> parseInputs(List<ParticipantText>)` | text → typed values; throws `ValidationException` |
| `ParticipantText` | record `(Member member, String text)` | raw text typed for one participant; the list order is the participant order |
| `MoneyFormatter` | `String format(Money)`, `String format(Transfer)` | the only place that knows the currency symbol (default "$"); `format(Transfer)` gives "A pays B $12.34" |

**Views** (Swing code only collects input and displays results; amounts are displayed with `MoneyFormatter`)
| View | Methods |
|---|---|
| `MembersView` | `showMembers(List<Member>)`, `showError(String)` |
| `ExpensesView` | `showMembers(List<Member>)` (payer combo box and participant checkboxes), `showExpenses(List<Expense>)`, `clearForm()`, `showError(String)` |
| `BalancesView` | `showBalances(Map<Member,Money>)`, `showError(String)` |
| `SettleUpView` | `showSettlements(List<Transfer>)`, `showError(String)` |
| `MainView` | `showGroupName(String)`, `setUndoEnabled(boolean)`, `showError(String)`, `askGroupName(): Optional<String>`, `confirmDiscardChanges(): boolean` (text: "Unsaved changes will be lost"), `chooseFileToOpen(): Optional<Path>`, `chooseFileToSave(): Optional<Path>` |

**Presenters** (every presenter has `start()`, which calls `service.addListener(this)` and shows the first state, and `stop()`, which removes the listener)
| Presenter | Methods | Service calls used |
|---|---|---|
| `MembersPresenter` | `onAddMember(String name)`, `onRemoveMember(Member)`, `onDataChanged()` | `addMember`, `removeMember`, `getMembers` |
| `ExpensesPresenter` | `onAddExpense(String description, Member payer, String amountText, SplitMethod method, List<ParticipantText>)`, `onRemoveSelected(int expenseId)`, `onDataChanged()` | `addExpense`, `removeExpense`, `getMembers`, `getExpenses` |
| `BalancesPresenter` | `onDataChanged()` | `getBalances` |
| `SettleUpPresenter` | `onDataChanged()` | `getSettlements` |
| `MainPresenter` | `onNew()`, `onOpen()`, `onSave()`, `onUndo()`, `onDataChanged()` | `newGroup`, `open`, `save`, `undo`, `canUndo`, `getGroupName`, `getMembers`, `getExpenses` |

`MainPresenter.onNew()` and `onOpen()` ask `view.confirmDiscardChanges()` first whenever the current group has any
member or expense (there is no dirty flag). The Expenses tab has a "Remove selected" button, which calls
`onRemoveSelected`. The Undo menu item is labelled to show that it covers expenses only.

## 5. PlantUML class diagram
NOTE: the diagram below has not been updated for the decisions in section 6. A teammate will redraw it; see section 8.
```plantuml
@startuml
skinparam classAttributeIconSize 0
package "model" {
  class Money <<value>> {
    -cents: long
    +{static} ofCents(long): Money
    +{static} parse(String): Money
    +plus(Money): Money
    +minus(Money): Money
    +negate(): Money
    +signum(): int
    +cents(): long
  }
  class Member <<value>> {
    -name: String
    +getName(): String
  }
  enum SplitMethod { EQUAL\nEXACT\nPERCENTAGE\nSHARES }
  class Expense <<immutable>> {
    -id: int
    -description: String
    +getShares(): Map<Member,Money>
    +getParticipants(): List<Member>
  }
  class Transfer <<value>> {
    +getFrom(): Member
    +getTo(): Member
    +getAmount(): Money
  }
  class Group {
    -name: String
    +addMember(Member)
    +removeMember(Member)
    +addExpense(Expense): Expense
    +removeExpense(int)
    +nextExpenseId(): int
    +getMembers(): List<Member>
    +getExpenses(): List<Expense>
  }
}
package "split" {
  interface SplitStrategy {
    +split(Money, LinkedHashMap<Member,BigDecimal>): Map<Member,Money>
    +method(): SplitMethod
  }
  class EqualSplit
  class ExactSplit
  class PercentageSplit
  class SharesSplit
  class SplitStrategyFactory {
    +{static} forMethod(SplitMethod): SplitStrategy
  }
}
package "command" {
  interface Command { +execute()\n+undo() }
  class AddExpenseCommand
  class RemoveExpenseCommand
  class CommandHistory {
    +run(Command)
    +undo()
    +canUndo(): boolean
    +clear()
  }
}
package "storage" {
  interface Repository {
    +save(Group, Path)
    +load(Path): Group
  }
  class FileRepository
}
package "service" {
  interface ServiceListener { +onDataChanged() }
  interface ExpenseService {
    +addListener(ServiceListener)
    +removeListener(ServiceListener)
    +addMember(String)
    +removeMember(Member)
    +getMembers(): List<Member>
    +addExpense(String,Member,Money,SplitMethod,LinkedHashMap): Expense
    +removeExpense(int)
    +getExpenses(): List<Expense>
    +getBalances(): Map<Member,Money>
    +getSettlements(): List<Transfer>
    +canUndo(): boolean
    +undo()
    +newGroup(String)
    +save(Path)
    +open(Path)
    +getGroupName(): String
  }
  class ExpenseServiceImpl
  class ValidationException
  class StorageException
}
package "gui" {
  class MainWindow
  class MembersPanel
  class ExpensesPanel
  class BalancesPanel
  class SettleUpPanel
}

Group "1" o-- "*" Member
Group "1" o-- "*" Expense
Expense --> Member : payer
Expense --> Money : total
Expense --> SplitMethod
Transfer --> Member
Transfer --> Money
SplitStrategy <|.. EqualSplit
SplitStrategy <|.. ExactSplit
SplitStrategy <|.. PercentageSplit
SplitStrategy <|.. SharesSplit
SplitStrategyFactory ..> SplitStrategy : creates
Command <|.. AddExpenseCommand
Command <|.. RemoveExpenseCommand
AddExpenseCommand --> Group
RemoveExpenseCommand --> Group
CommandHistory o-- Command
Repository <|.. FileRepository
ExpenseService <|.. ExpenseServiceImpl
ExpenseServiceImpl --> Group
ExpenseServiceImpl --> CommandHistory
ExpenseServiceImpl --> Repository
ExpenseServiceImpl ..> SplitStrategyFactory
ExpenseServiceImpl o-- ServiceListener : notifies
ExpenseService ..> ValidationException
ExpenseService ..> StorageException
MainWindow *-- MembersPanel
MainWindow *-- ExpensesPanel
MainWindow *-- BalancesPanel
MainWindow *-- SettleUpPanel
MembersPanel ..> ExpenseService
ExpensesPanel ..> ExpenseService
BalancesPanel ..> ExpenseService
SettleUpPanel ..> ExpenseService
MembersPanel ..|> ServiceListener
ExpensesPanel ..|> ServiceListener
BalancesPanel ..|> ServiceListener
SettleUpPanel ..|> ServiceListener
@enduml
```

## 6. Decisions
Decisions agreed with the analysis/docs member on 2026-11-21 (reported by the developer).

| # | Decision |
|---|---|
| Q1 | Removing a member who is the payer or a participant of any expense is blocked (`ValidationException`). |
| Q2 | Undo covers only expense add and expense remove (`AddExpenseCommand`, `RemoveExpenseCommand`). No undo for member changes, no Redo. |
| Q3 | Names are trimmed, unique case-insensitively, and at most 30 characters. |
| Q4 | Percentages may have up to 2 decimals. The sum is compared exactly with `BigDecimal.compareTo` to 100, with no tolerance (so 33.33 × 3 is rejected; users should use `EQUAL`). |
| Q5 | Single currency. The symbol lives only in `gui.MoneyFormatter` (default "$"). `Money.toString()` and `Transfer.toString()` contain no currency symbol. |
| Q6 | The payer does not have to be a participant. |
| Q7 | Save format is CSV with no third-party library. It stores the group name, the next expense id and the computed shares (not the original inputs). The first line is a format-version marker. Fields are quoted and escaped; descriptions and member names may not contain line breaks. A corrupt, empty or wrong-version file gives a `StorageException` with a clear message. |
| Q8 | No dirty flag. `MainPresenter` asks for confirmation ("Unsaved changes will be lost") before New or Open whenever the current group has any member or expense. |
| Q9 | One group at a time. |
| Q10 | Settlement ties are broken by member insertion order, so results are deterministic. |
| Q11 | No expense editing; remove and re-add. |
| Q12 | A single `ServiceListener.onDataChanged()` is enough. |
| Q13 | `StorageException` is checked, `ValidationException` is unchecked. Presenters catch both and show the message in the view. |
| Q14 | A single expense total may not exceed 1,000,000.00 (100,000,000 cents); larger gives a `ValidationException`. |
| N1 | Participants with a zero share stay in `Expense.shares` with `Money.ZERO`. They count as participants, so they also block member removal. |
| N2 | Expense ids only increase and are never reused, even after undo. `Group` keeps a separate counter, and the saved file stores the next id. `Group` keeps expenses sorted by id; `addExpense` inserts by id and rejects a duplicate id, so undoing a `RemoveExpenseCommand` restores the original position. Undoing an `AddExpenseCommand` does not decrement the counter (see 2.3). |
| N3 | Settle-up only suggests transfers. Payments are not recorded; after people pay, the user starts a New group. |
| N4 | The Expenses tab has a "Remove selected" button (calls `removeExpense`, undoable). |
| N5 | No date field on expenses. |

### 6.1 Unresolved points (for the team)
- **Undo of a removed expense after its member was removed.** Q1 blocks removing a member only while an expense
  uses that member. Sequence: add expense E (payer A); remove E; remove member A (allowed, E no longer exists);
  undo. Undoing `RemoveExpenseCommand` calls `Group.addExpense(E)`, which must reject E because A is no longer a
  member. The undo then fails with a `ValidationException`. The design does not yet say what should happen.
- **Weights in `SHARES`.** The decisions do not say whether share counts must be whole numbers or may be decimals.
  The text above allows any non-negative `BigDecimal` (at least one non-zero).
- **Group name rules.** The decisions give name rules only for members. The text above uses "not blank, no line
  breaks" for the group name, so that CSV stays one record per line. No length limit is set.
- **Description rules.** Only "no line breaks" is required (Q7). No length limit is set.
- **`ParticipantText`.** The new record `gui.ParticipantText` was added so that
  presenters can receive raw text; it was not in the decision list.

## 7. Decision records
DRAFT — for the analyst to review.

**D1. Exceptions in their own package (`com.splitter.exception`)**
- Decision: `ValidationException` (unchecked) and `StorageException` (checked) live in a package that depends on nothing.
- Alternatives considered: keep them in `service`; one exception type for everything.
- Consequences: `model`, `split` and `storage` can throw them without depending on `service`, so there is no dependency cycle. One more package appears in the UML.

**D2. Money as `long` cents**
- Decision: all amounts are `long` cents wrapped in `Money`.
- Alternatives considered: `double`; `BigDecimal` everywhere.
- Consequences: no floating-point error and exact sums. Percent and share arithmetic needs `BigDecimal` in the strategies, and a maximum amount (Q14) is needed to avoid overflow.

**D3. Greedy settle-up**
- Decision: the largest debtor pays the largest creditor, repeated; at most n−1 transfers.
- Alternatives considered: searching for the globally minimal number of transfers.
- Consequences: simple and deterministic. It is not always the global minimum, because that problem is NP-hard. This is a documented design decision.

**D4. CSV instead of JSON**
- Decision: the save file is CSV, written and parsed by hand.
- Alternatives considered: JSON with a hand-written parser; Java serialization.
- Consequences: no external dependency and a simple format. Quoting and escaping must be implemented and tested, and descriptions and names may not contain line breaks.

**D5. Monotonic expense ids**
- Decision: ids only increase and are never reused; `Group` keeps the counter and the file stores it; expenses are kept sorted by id.
- Alternatives considered: id = list index; reusing the highest id after undo.
- Consequences: undo of a removal restores the original position, and ids are stable. Gaps in the id sequence are possible.

**D6. One `BigDecimal` per `ParticipantInput`, with strategy-dependent meaning**
- Decision: a single `BigDecimal value` means nothing, an amount, a percent or a weight, depending on the strategy.
- Alternatives considered: one input type per strategy; a sealed hierarchy of inputs.
- Consequences: one `split` signature and a simple GUI-to-service path. The meaning of the value is not enforced by the type system, so each strategy must validate it and the tests must cover every strategy.

**D7. No dirty flag; confirmation dialog instead**
- Decision: `MainPresenter` asks for confirmation before New or Open whenever the group has any member or expense.
- Alternatives considered: a dirty flag in the service (`isDirty()`).
- Consequences: a smaller `ExpenseService` interface. The prompt also appears when the data was just saved.

## 8. Diagram changes needed
The PlantUML diagram in section 5 was not edited. After these decisions it must show:
- A new package `exception` containing `ValidationException` and `StorageException`, removed from `service`.
  `StorageException` extends `Exception`; `ValidationException` extends `RuntimeException`.
- No arrow from `model`, `split` or `storage` to `service`. Arrows to `exception` instead.
- A new record `ParticipantInput(member, value)` in `split`. `SplitStrategy.split` and `ExpenseService.addExpense` take
  `List<ParticipantInput>` instead of `LinkedHashMap<Member,BigDecimal>`.
- `Group` with the new methods: `allocateExpenseId`, `getNextExpenseId`, `restoreNextExpenseId`, `hasMember`.
- `Repository.save` and `Repository.load`, `ExpenseService.save` and `ExpenseService.open` declare `throws StorageException`.
- `FileRepository` is the CSV implementation.
- No `AddMemberCommand`; only `AddExpenseCommand` and `RemoveExpenseCommand`.
- The `gui` package shows the view interfaces `MembersView`, `ExpensesView`, `BalancesView`, `SettleUpView`, `MainView`
  (implemented by the Swing panels and frame), the presenters `MembersPresenter`, `ExpensesPresenter`,
  `BalancesPresenter`, `SettleUpPresenter`, `MainPresenter` (each depends on `ExpenseService` and on its view interface,
  and implements `ServiceListener`), and the helpers `InputParser`, `ParticipantText` and `MoneyFormatter`.
- The Swing panels do not implement `ServiceListener` and do not call `ExpenseService` directly.
- `gui` depends on `service`, `model` and `exception`.
