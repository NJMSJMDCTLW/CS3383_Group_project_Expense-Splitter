DRAFT — to be reviewed by the analysis/docs member

# AI Usage Log

One row per AI-generated change. Human review and refinement columns are filled in by the team, not by Claude Code.

| Date | Prompt ID | Files | What was generated | Open issues noticed |
|---|---|---|---|---|
| 2026-11-21 | P00 | docs/DESIGN.md, docs/AI_USAGE_LOG.md | Design proposal: model classes, SplitStrategy, ExpenseService, ServiceListener, PlantUML class diagram, 14 open questions. No code. | 14 open questions listed in DESIGN.md section 6. A sequence diagram is not yet drafted. |
| 2026-11-21 | P00b | docs/DESIGN.md, CLAUDE.md, docs/AI_USAGE_LOG.md | Updated the design text and CLAUDE.md to the team decisions (Q1-Q14, N1-N5, F1-F5): exception package, monotonic expense ids, MVP views/presenters, member identity, ParticipantInput, CSV rules, decision records, list of diagram changes. Diagram block not touched. No code. | Undo of RemoveExpenseCommand can fail if the payer was removed meanwhile (DESIGN.md 6.1); SHARES weight type, group name and description limits not decided; gui.ParticipantText added by Claude. |
| 2026-11-21 | P01 | pom.xml, .gitignore, src/main/java/com/splitter/App.java, src/main/java/com/splitter/meta/AIGenerated.java, package-info.java in 8 packages (exception, model, split, service, command, storage, gui, meta) | Maven project skeleton (Java 17, JUnit 5, JaCoCo, jar manifest Main-Class com.splitter.App, finalName expense-splitter), AIGenerated annotation, App opening an empty JFrame. No features. | JaCoCo report is skipped until the first test exists; no tests yet; App window not run by hand; AIGenerated is not on package-info files or on the annotation itself. |
