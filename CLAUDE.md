# Group Expense Splitter — CS3383 Group Project

## Project context
- Course: CS3383 (Agentic Coding / AI for Software Engineering), group project, 4 members.
- I am the **only developer**: I write all production code (core logic and GUI). The other 3 members own
  analysis/docs (UML, design report), testing (all JUnit tests, bug reports) and project management.
- Product: a desktop app that records shared expenses within a group, splits them with different
  methods, shows who owes whom, and suggests transfers to settle up.
- Grading cares about: UML design and design patterns, clear documentation of **AI-generated vs.
  human-refined code**, and real bugs found by testing. Keep this in mind for every change.

## Scope of Claude's work (important)
- **Write production code only. Do NOT create or edit test files** (`src/test/**`). A teammate writes all
  tests against the public signatures in `docs/DESIGN.md`.
- Verify your work with `mvn compile`. Run `mvn test` / `mvn verify` only when I ask, or when test files
  from my teammate already exist. Never delete, skip or weaken an existing test; report failures instead.
- Do not edit diagrams. Do not write documents under `docs/` unless the prompt tells you to.
- Public classes and method signatures must match `docs/DESIGN.md` exactly. If you think a signature
  should change, stop and tell me; do not change it on your own.

## Tech stack
- Java 17, Maven, JUnit 5, JaCoCo, **Swing** for the GUI. No third-party libraries.
- `mvn package` must produce a runnable jar: `java -jar target/expense-splitter.jar`.

## Architecture
Package root: `com.splitter`. **`docs/DESIGN.md` is the authority for class and method contracts.**
If a prompt conflicts with it, follow DESIGN.md and tell me.

| Package     | Responsibility | May depend on |
|-------------|----------------|---------------|
| `exception` | `ValidationException` (unchecked), `StorageException` (checked) | nothing |
| `model`     | Member, Group, Expense, Money, Transfer (value objects, no UI/file logic) | exception |
| `split`     | `SplitStrategy`, `ParticipantInput`, Equal / Exact / Percentage / Shares, `SplitStrategyFactory` | model, exception |
| `command`   | `Command`, `AddExpenseCommand`, `RemoveExpenseCommand`, `CommandHistory` (undo) | model, exception |
| `storage`   | `Repository` interface + CSV implementation | model, exception |
| `service`   | `ExpenseService` interface + implementation, `ServiceListener` | model, split, command, storage, exception |
| `gui`       | Swing views, presenters, `InputParser`, `MoneyFormatter` | service, model, exception |
| `meta`      | `@AIGenerated` annotation | nothing |

Hard rules:
- **No business logic in `gui`.** Views only collect input and show results. Anything unit-testable
  belongs in `service` / `split` / `model`.
- Only `gui` and the entry point `App` may import `javax.swing` or `java.awt` (`App` needs `SwingUtilities.invokeLater`). Presenters must not import them either.
- `model`, `split`, `command`, `storage` must never depend on `service` or `gui`.
- The GUI talks to `ExpenseService` (an interface), never to implementation classes.
- GUI is MVP: view interfaces implemented by Swing panels; presenters depend on the service and on a view
  interface. **Presenters implement `ServiceListener`; Swing components never do.**
- Build order: core logic first (bottom-up), GUI last on top of the real service.

Design patterns (keep them recognisable, they are presented and graded):
Strategy (split methods), Command (undo), Factory (strategy creation), Observer (`ServiceListener.onDataChanged`), MVP (GUI).

## Domain rules
- Money is `long` cents, never `double`. Display with 2 decimals. The currency symbol exists only in
  `gui.MoneyFormatter`; `Money.toString()` and `Transfer.toString()` contain no symbol.
- `Money.parse` accepts only non-negative decimals with at most 2 decimal places.
- Member identity: key = trimmed name lower-cased with `Locale.ROOT`; `getName()` keeps the user's casing.
  Names: trimmed, not blank, max 30 chars, no line breaks. Duplicate names (by key) are rejected.
- A single expense total may not exceed 1,000,000.00. Descriptions may not contain line breaks.
- Rounding: EQUAL floor(total/n); PERCENTAGE floor(total*pct/100); SHARES floor(total*weight/sum);
  EXACT has none. Leftover cents (always fewer than the number of participants) go one cent each in
  participant order, only to participants with weight > 0 (EQUAL: everyone). Shares always sum to the total.
- Percentages: up to 2 decimals, sum compared exactly to 100 (no tolerance).
- Validation (clear message, `ValidationException`): amount <= 0 or over the limit, no participants,
  payer not a member (payer need not be a participant), exact amounts not summing to total, percentages
  not summing to 100, all shares zero, duplicate member, removing a member who is payer/participant of any expense.
- Participants with a zero share stay in `Expense.shares` with `Money.ZERO`.
- Expense ids only increase and are never reused (even after undo); the saved file stores the next id.
  Group keeps expenses sorted by id and rejects duplicate ids, so undo of a removal restores the position.
- Undo covers expense add/remove only (no member undo, no redo).
- Settle-up is greedy (largest debtor pays largest creditor, repeat): at most n-1 transfers, not
  guaranteed globally minimal (NP-hard), deterministic (ties by member insertion order). It only suggests
  transfers; payments are not recorded. Members with zero balance do not appear in transfers.
  Balances are ordered by member insertion order, include zero balances, and always sum to 0.
- Exceptions: `ValidationException` unchecked, `StorageException` checked. Presenters catch both and show
  the message in the view (`JOptionPane`), never a stack trace.
- Storage: CSV, no library. First line is a format-version marker, fields quoted/escaped, stores group
  name, next expense id and computed shares. Corrupt, empty or wrong-version files raise `StorageException`.

## GUI requirements
Main window with tabs:
1. **Members**: add / remove members.
2. **Expenses**: table + form (description, payer combo box, amount, participant checkboxes, split method
   combo box; inputs change per method) and a **Remove selected** button (undoable).
3. **Balances**: net balance per member (positive = is owed, negative = owes).
4. **Settle Up**: list of suggested transfers "A pays B $x".
Menu/toolbar: New, Open, Save, Undo (Ctrl+Z, labelled as undoing the last expense change).
New and Open ask for confirmation ("Unsaved changes will be lost") whenever the group has any member or expense.

## Submission requirements
The course requires a fixed deliverable structure (Source/, Release/, Docs/, README.md, ActivityLog.xlsx,
packaged as Group_#_Project.zip). The specification is in `docs/DELIVERABLES.md`; read it before creating
or editing anything under `docs/`, the README, scripts or packaging. Never write members' self-assessments
or ActivityLog hours, and never invent bugs, releases, names or dates.

## How to work with me (rules for Claude)
- Work in small steps. For anything touching more than ~3 files, show a plan first and wait for my OK.
- Do not add features or requirements I did not ask for. If something is unclear, ask.
- Mark every class or method you generate with `@AIGenerated(tool = "Claude Code", prompt = "<prompt id>")`
  (annotation in `com.splitter.meta`). Keep it when I later edit the code; I fill in `refinedBy` and
  `changes` myself.
- After each generated change, append one row to `docs/AI_USAGE_LOG.md`:
  date | prompt id | files | what was generated | open issues you noticed.
- Git: do not commit unless I ask. When I ask, use the prefix `[AI]` and the prompt id, e.g.
  `[AI] P03 Add split strategies`. Never commit my manual edits under `[AI]`; I use `[REFINE]` or `[HUMAN]`.
- If you notice a bug in existing code, do not silently fix it. Describe it (steps to reproduce,
  expected vs actual) so it can go into the bug report, and fix only if I agree.
