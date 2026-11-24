# Final Submission Requirements — CS3383 Group Project

Sources: Group_Project_Details.pptx, Final_Report_Marking_Scheme.docx, Presentation_Marking_Scheme.docx,
the `Group_##_Project` template zip, Sample Projects 1 and 2, ActivityLog_Example.xlsx.
Items marked **(inferred)** are not stated by the course files; confirm with the team / TA.

## 1. What is submitted (Canvas, one submission per group)

| Item | File name | Format |
|------|-----------|--------|
| Presentation slides | `Group_#_Presentation.pptx` | pptx or PDF only |
| Final report package | `Group_#_Project.zip` | one zip containing everything below |

- `#` = our group number. All due times are 11:59pm; exact dates are on Canvas (not in the files).
- Presentation: 15–20 minutes, 5% of the course mark. Must include a quick demo (live or screenshots).

## 2. Zip structure

```
Group_#_Project/                 (template folder is named Group_##_Project)
├── README.md
├── ActivityLog.xlsx
├── Docs/
│   ├── ReleaseSummary           1 page
│   ├── ProjectPlan              4 pages
│   ├── AnalysisDesignReport     10 pages
│   ├── TestReport
│   ├── BugReport
│   ├── SelfAssessmentReport     up to 1 page per member
│   ├── AI_Usage_Log             AI-generated vs human-refined record (needed for Implementation marks)
│   └── Javadoc/                 optional extras: InstallationGuide, UserManual, Graphs (UML as PDF)
├── Release/                     executable (.jar) + run script (.bat) + any input/config files the app needs
└── Source/                      Java source files + JUnit test source files
```

- Project Details says `Readme.txt`, the template contains `README.md`: use **README.md** (matches the template).
- Samples use .docx for documents. The course files do not state a format for Docs (inferred: .docx or PDF).
- Sample 2 has no separate Test Report (it is folded into Sample 1's single report), but Project Details lists a
  Test Report as a required document, so we provide one.

## 3. What each document must contain

| Document | Must contain |
|----------|--------------|
| **README.md** | App description; requirements (Java version); installation (simple steps); usage guide with an example; input/file formats; versions and bug fixes per version; **team member names and roles** |
| **Release Summary** (1 page) | Overview; per release: introduction, features, patches; release date table; list of files included; team members and roles |
| **Project Plan** (4 pages) | Description, objectives, scope, constraints, stakeholders; development methodology and why; team organisation; tools and configuration management (GitHub); WBS; schedule/Gantt chart; activity diagram; risks |
| **Analysis & Design Report** (10 pages) | Design constraints; requirements specification; use case diagram + use case specifications; class diagram; sequence diagrams; design principles and patterns applied **with justification and reasoning** |
| **Test Report** | Test plan and strategy (e.g. bottom-up, with justification); coverage criteria (statement, branch, condition, decision, MC/DC) and how test values were chosen; number of test cases; overall coverage report with percentages and screenshots |
| **Bug Report** | All bugs found during development (GitHub Issues can be printed/exported), see section 5 |
| **Self-Assessment** | Per member, up to 1 page: what was learned, difficulties encountered. Written by each member personally |
| **AI Usage Log** | Per generation: prompt, tool/model, files, what AI produced, human review result, human changes and why, commit refs |
| **ActivityLog.xlsx** | Two sheets: *Group Contribution Log* (weekly hours per member; **used to calculate each member's final mark**) and *Weekly Activity Log* (week, activity description, completed by). The project manager fills it in every week |

Formatting assumption for page limits: 10 pt font, single line spacing, A4, 1-inch margins.
Every report needs a cover page: team name, project title, member list, tutorial session.

## 4. Marking schemes

### Final report (100 marks)

| Item | Marks | Top band requires |
|------|-------|-------------------|
| Summary and Requirements | 10 | Precise and informative summary of the project |
| Project Plan | 10 | More complete plan with extensive details |
| Analysis and Design | 20 | Complete design report with UML class diagrams, use case etc. |
| Implementation | 20 | Satisfies all requirements, **with clear documentation of AI-generated vs. human-refined code** |
| Test Cases | 20 | Complete set with justification and testing strategies, **more than 50 test cases** (about 40 = second band) |
| Bug Report | 10 | Comprehensive reports and details, **more than 10 bugs** (more than 5 = second band) |
| Weekly Activity | 10 | Extensive documentation of weekly activity and project members |

### Presentation (7 aspects, each scored 0–10)
1. Project plan, people management, lifecycle management
2. Product introduction, prototype, demo, story line
3. Requirements and verification (use cases, test cases)
4. Software design in UML (class, sequence diagrams)
5. Software testing (test methods, test coverage)
6. Documentation and bug report
7. Design principles / patterns applied

Project Details also frames the talk around five aspects: Progress, Product, People, Process, Technology.

## 5. Bug report entry format

Fields used by Sample 2 (one entry per bug, IDs like `Bug-00001`):
Title · Status · Product · Type (Crash / Bug / Usability / Enhancement) · Component · Version · Platform ·
Importance (critical / major / minor / enhancement) · Reported (date/time, by whom) · Modified · Assignee.

Week 7 lecture adds, and a good report must also have: **Steps to Reproduce, Expected result, Actual result
(screenshot or stack trace)**, a specific title (under about 60 characters), OS / environment. We also add
*Found by* (JUnit / manual / AI review), *Introduced by* (AI-generated code / human / requirement) and *Fix commit*.

## 6. Rules for AI-drafted documents (apply to Claude Code)
- Claude Code may draft: README, Release Summary, document skeletons and diagrams (PlantUML), the Test Report
  tables and coverage summary, the Bug Report compiled from **existing** GitHub Issues, Javadoc, packaging scripts.
- Claude Code must **not** write: members' Self-Assessments, hours or activity entries in ActivityLog.xlsx,
  or any bug that did not really occur. Leave clearly marked placeholders instead.
- Never invent team names, member names, group number, dates or releases that did not happen; use placeholders.
- Every AI-drafted document starts with `DRAFT — to be reviewed by <role>` until a human has checked it,
  and each draft gets a row in the AI Usage Log.
- Numbers in marking criteria (more than 50 tests, more than 10 bugs) are reported as gaps, never padded with
  meaningless tests or fabricated bugs.
