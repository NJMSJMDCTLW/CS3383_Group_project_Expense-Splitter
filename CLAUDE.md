# Group Expense Splitter — CS3383 Group Project

## Project context
- Course: CS3383 (Agentic Coding / AI for Software Engineering), group project, 4 members.
- I am the **only developer**: I write all code (core logic and GUI). The other 3 members own
  analysis/docs, testing and project management. The tester also writes extra boundary tests by hand.
- Product: a desktop app that records shared expenses within a group, splits them with different
  methods, shows who owes whom, and suggests the minimum number of transfers to settle up.
- Grading cares about: UML design and design patterns, JUnit tests (>50, with justification and
  coverage), bug reports (>10, real ones), and **clear documentation of AI-generated vs.
  human-refined code**. Keep this in mind for every change.

## Tech stack
- Java 17, Maven, JUnit 5, JaCoCo (coverage), **Swing** for the GUI (no third-party UI libraries).
- No external runtime dependencies, so `mvn package` produces a runnable jar.

## Commands
- Build + unit tests: `mvn test`
- Tests + coverage report: `mvn verify` (report at `target/site/jacoco/index.html`)
- Package: `mvn package`, then run `java -jar target/expense-splitter.jar`

## Architecture
Package root: `com.splitter`

| Package   | Responsibility | May depend on |
|-----------|----------------|---------------|
| `model`   | Member, Group, Expense, Money (value objects, no logic about UI or files) | nothing |
| `split`   | `SplitStrategy` interface + Equal / Exact / Percentage / Shares, `SplitStrategyFactory` | model |
| `service` | `ExpenseService` interface + implementation: add expense, balances, settle-up (debt simplification) | model, split, command |
| `command` | `Command` interface, `AddExpenseCommand`, `RemoveExpenseCommand`, `CommandHistory` (undo) | model |
| `storage` | `Repository` interface + CSV/JSON file implementation | model |
| `gui`     | Swing views + controllers/presenters | **service interface only** |

Hard rules:
- **No business logic in `gui`.** Views only collect input and display results. Anything that can be
  unit-tested belongs in `service` / `split`.
- `model`, `split`, `service` must not import `javax.swing` or `java.awt`.
- The GUI talks to `ExpenseService` (an interface), never to implementation classes.
- Build order: core logic with tests first (bottom-up), GUI last on top of the real service.
- GUI observes the service (Observer pattern): the service notifies listeners when data changes,
  and views refresh themselves.

Design patterns used (keep them recognisable, they are presented and graded):
Strategy (split methods), Command (undo), Factory (strategy creation), Observer (GUI refresh), MVC/MVP (GUI).

## Domain rules
- Money is stored as `long` cents (never `double`). Display with 2 decimal places.
- Rounding: when an amount cannot be divided evenly, the leftover cents go to participants in list
  order, one cent each, so the parts always sum exactly to the total.
- Validation (reject with a clear message): amount <= 0, empty participant list, payer not in group,
  exact amounts not summing to total, percentages not summing to 100, shares all zero, duplicate member name.
- Settle-up uses greedy matching on net balances (largest debtor pays largest creditor, repeat), giving
  at most n-1 transfers. It is not guaranteed to be the global minimum (that problem is NP-hard); this is
  a documented design decision. Members with zero balance do not appear. Transfers always sum to zero.

## GUI requirements
Main window with tabs:
1. **Members** – add/remove members of the current group.
2. **Expenses** – table of expenses + form to add one: description, payer (combo box), amount,
   participants (checkboxes), split method (combo box). The input fields change with the split method
   (exact amount / percentage / shares per participant).
3. **Balances** – net balance per member (positive = is owed, negative = owes).
4. **Settle Up** – list of suggested transfers "A pays B $x".
Menu/toolbar: New, Open, Save, Undo (Ctrl+Z). Errors are shown with `JOptionPane`, never as stack traces.

## Submission requirements
The course requires a fixed deliverable structure (Source/, Release/, Docs/, README.md, ActivityLog.xlsx,
packaged as Group_#_Project.zip) and a set of documents with page limits. The full specification and
marking criteria are in `docs/DELIVERABLES.md`; read it before creating or editing anything under `docs/`,
the README, scripts, or the packaging process. Keep the repo so that packaging is a copy step.
Never write members' self-assessments or ActivityLog hours, and never invent bugs, releases, names or dates.

## How to work with me (rules for Claude)
- Work in small steps. For anything touching more than ~3 files, show a plan first and wait for my OK.
- Do not add features or requirements I did not ask for. If something is unclear, ask.
- After every change run `mvn test`. Never delete, skip or weaken an existing test to make it pass —
  report the failure instead.
- When you write tests, state for each test which requirement, branch or condition it targets.
  Point out cases you think are still untested.
- Mark every class or method you generate with `@AIGenerated(tool = "Claude Code", prompt = "<prompt id>")`
  (annotation in `com.splitter.meta`). Keep the annotation when I later edit the code; I will fill in
  `refinedBy` and `changes` myself.
- After each generated change, append one row to `docs/AI_USAGE_LOG.md`:
  date | prompt id | files | what was generated | open issues you noticed.
- Git: when I ask you to commit your generated code, use the prefix `[AI]` and mention the prompt id,
  e.g. `[AI] P03 Add Swing expense form`. Never commit my manual edits under `[AI]`; I commit those
  myself with `[REFINE]` or `[HUMAN]`.
- If you notice a bug in existing code, do not silently fix it. Describe it (steps to reproduce,
  expected vs actual) so it can go into the bug report, then fix only if I agree.
