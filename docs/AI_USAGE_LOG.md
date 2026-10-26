# Appendix A: AI Usage Log (Component 2.4 — 20%)

**Project 2 — StockFlow Inventory Management System (Swing GUI + embedded Apache Derby)**
**Student:** Yuheng Hu (26366447)

This appendix records how I collaborated with GenAI while extending the Project 1 console
system into the Project 2 desktop application. It covers the requirements review, the design
decisions, the database and DAO layer, the Swing GUI, the JUnit 4 tests, debugging, the
report figures and the documentation. The rows are representative rather than exhaustive;
every output listed below was reviewed, corrected or rejected before it reached the submitted
system or this report. The critical reflection sections (2.6 and 3.7) were written entirely
by me without GenAI text.

## A.1 GenAI tools used

| Tool | Model / version | Mainly used for |
|---|---|---|
| DeepSeek Harness (coding agent) | DeepSeek chat model, “flash” variant; agent orchestration with separate GUI and test sub-agents | Reading the assessment PDF, comparing the existing Project 1 system with the Project 2 criteria, proposing architecture and database designs, generating the Derby/JDBC DAO layer, generating the Swing GUI and JUnit 4 tests, debugging build/runtime problems, and drafting the report figures and documentation |

Only the DeepSeek Harness coding agent was used for the Project 2 work. No image-generation
model was used for the diagrams: the figures are drawn by the Pillow script
`docs/design/generate_design_diagrams.py`, which I reviewed and iterated.

## A.2 Prompt log

| # | Stage | Main prompt (summary) | What GenAI produced | What I changed, rejected or verified and why |
|---|---|---|---|---|
| 1 | Requirements | “Read the translated assessment PDF and list the Project 2 criteria with weights. Compare the current Project 1 system against them and list the gaps.” | A P1/P2 requirement table with the missing GUI, Derby, DAO, JUnit, Git and report items, and the statement that the current system meets only Project 1 | I checked every row against the PDF. Used it as the Project 2 backlog. |
| 2 | Scope | “Propose a substantive Project 2 extension for this inventory system; compare purchase orders, multi-warehouse and an activity log.” | Recommended the purchase-order / replenishment workflow; argued the other two add entities without enough verification value | Accepted purchase orders because they reuse the existing supplier, product and transaction aggregates and exercise new database reads and writes. Rejected multi-warehouse and activity-log scope as out of proportion for an individual project. |
| 3 | Design — GUI | “Which Java GUI toolkit should be used for a Maven project that must build and run without manual configuration?” | Recommended Swing (JDK built-in); listed JavaFX as an alternative with extra module/dependency configuration | Accepted Swing: no runtime dependency and a single `mvn clean package` build. JavaFX rejected because it adds launcher configuration that conflicts with the “no manual setup” criterion. |
| 4 | Design — database/DAO | “Design an embedded Derby schema and JDBC DAO layer for the existing CSV repositories; keep the service interfaces stable and avoid unnecessary framework configuration.” | A seven-table schema and seven DAO interfaces, a `DatabaseManager` Singleton, and a transaction boundary (`ConnectionProvider` / `TransactionRunner`), with a plain-JDBC implementation | Kept the DAO/Singleton/transaction design and rejected the Hibernate alternative: seven simple tables do not justify the extra dependency and configuration. I checked that the service layer only sees DAO interfaces. |
| 5 | Implementation — persistence | “Implement the schema, JDBC DAOs and first-run seeding; verify the full object graph against an empty database.” | DAO implementations, `SchemaInitializer`, `DatabaseManager` and the demo-data seeder | During the smoke run I found two defects: the code initially used the removed `org.apache.derby.jdbc.EmbeddedDriver` class (10.16 registers `org.apache.derby.iapi.jdbc.AutoloadedDriver`), and two DAOs bound the primary-key/unit columns at the wrong parameter offsets. I corrected both and re-ran the smoke test until the seeded counts and CRUD operations were correct. |
| 6 | Implementation — GUI | “Build the Swing MVC GUI: login, role-based tabs, dashboard, product CRUD, stock movements, purchase orders, reports and administration; views must not contain business logic.” | 44 GUI files in `ui/view`, `ui/controller`, `ui/table` and `ui/component`, plus `LoginDialog` and `MainFrame` | I reviewed the structure and required that admin-only actions are guarded in both the GUI and the services. After a visual check I had the AI fix the purchase-order split-pane clipping, the currency formatting and the login error presentation (inline red message). |
| 7 | Design — Observer | “How should the GUI refresh the dashboard after a stock change without coupling the service to Swing?” | A `StockLevelObserver` implementation in the GUI that receives notifications from `StockService` and posts the refresh with `SwingUtilities.invokeLater` | Accepted; I noted that `StockService` has no `removeObserver`, so the GUI observer exposes `deactivate()` and `MainFrame` calls it on sign-out. |
| 8 | Business rules — purchasing | “Define the purchase-order statuses, the partial-receipt behaviour and the role rules.” | A draft/ordered/partially-received/received/cancelled lifecycle, receiving all or part of each line, and admin-only submit/cancel | I accepted the state model, but defined the permissions myself: any signed-in user may create a draft and receive a delivery (the warehouse workflow), while submit/cancel remains an administrator decision. |
| 9 | Testing | “Generate a JUnit 4 suite (at least five meaningful cases) covering JDBC DAO operations and the critical business rules, using an isolated embedded Derby in-memory database.” | 8 test classes / 35 tests covering DAO CRUD and date queries, authentication and roles, stock in/out/adjustment, referential integrity, the purchase-order workflow (partial and full receipt) and report aggregation | I reviewed the assertions and names. Kept all 35 because each one checks a rule or a database operation; the requirement is a minimum of five. |
| 10 | Implementation — transactions | “Make receiving a purchase order atomic across the order, its lines, the product quantities and the stock-transaction log.” | A thread-local connection in `DatabaseManager` and a `TransactionRunner.runInTransaction(...)` boundary; receiving uses one transaction and can roll back | Accepted. Verified with tests for partial receipt, full receipt and rejection of invalid quantities, and by checking that a failed stock-in does not leave a half-updated order. |
| 11 | Build/debugging | “Package the application so that `java -jar` works with no classpath and no database setup.” | The Maven Shade configuration, including the Derby engine and the JDBC service file | The first attempt failed because transitive plugin dependencies were missing in the offline build environment and the JDBC `META-INF/services` file would not be merged. I added the `ServicesResourceTransformer`, checked the shaded jar manifest (`Main-Class`, Derby driver service) and launched it from an empty directory. |
| 12 | Report figures | “Generate a use-case diagram, class diagram, ER diagram and MVC diagram for the Project 2 report in the same print style as the Project 1 figures.” | A Pillow-only generator producing four PNGs with measured text, 2× supersampling and print-size assertions | I iterated on the layouts: fixed the ER text overlap and column widths, removed the use-case note collision and reduced the class diagram height to one page. Checked that every label is at least 7.5 pt at 6.3 in. |
| 13 | Rejected alternative | “Should the console UI be kept next to the new Swing GUI?” | The agent initially suggested keeping the CUI as an optional mode to preserve Project 1 behaviour | Rejected: two user interfaces duplicate action handling and test fixtures and would look like an unrefactored leftover. The console menus, CSV repositories and console scripts were removed; the P1 code remains in the Project 1 submission. |
| 14 | Documentation | “Draft the README, the test evidence and the AI usage log for the Project 2 submission.” | Drafts of the README, `docs/TEST_EVIDENCE.md` and this appendix | I verified every command and captured result, corrected the build/run instructions, and wrote the critical reflections myself. The AI log is reviewed and edited to reflect what actually happened, including the rejected items above. |

## A.3 Human vs AI contribution summary

| Part of the project | Mainly human-designed | Mainly AI-generated (then reviewed) | Notes |
|---|---|---|---|
| Topic, scope and requirement priorities | ✓ | | Same topic as Project 1; the purchase-order extension and the exclusions were my decisions. |
| Architecture: Swing + Derby + JDBC DAO + MVC | ✓ | | I selected the options from the AI comparison and rejected JavaFX and Hibernate. |
| Database schema and keys | | ✓ | AI draft; I checked the seven tables, foreign keys and the audit-only `USERS` usage against the code. |
| Domain model | ✓ (Project 1) | ✓ (PurchaseOrder / PurchaseOrderLine) | The Project 1 model is my design; the new entities were AI-extended and reviewed. |
| DAO/JDBC implementation | | ✓ | I found and fixed the driver-class and parameter-binding defects during review and smoke tests. |
| Purchase-order workflow rules | ✓ | | Statuses, permissions and partial-receipt rules defined by me; AI implemented them. |
| Swing GUI implementation | | ✓ | AI generated the code from my UI/feature specification; I checked the layout and role behaviour and requested fixes. |
| JUnit 4 tests | | ✓ | AI generated 35 tests; I reviewed the assertions and kept the suite. |
| Report figures | | ✓ | AI-written Pillow generator; I directed the layout corrections and verified print legibility. |
| Report prose and documentation | | ✓ (drafts) | AI-assisted drafts; I reviewed, corrected and rewrote where needed. |
| Critical reflections (2.6 and 3.7) | ✓ | | Written entirely by me, with no GenAI text. |

## A.4 Declaration

I confirm that the GenAI use above is recorded transparently: the system was developed with
the DeepSeek Harness coding agent, every generated artefact was reviewed before it was kept,
and the errors and rejected suggestions are reported rather than hidden. The critical
reflections in sections 2.6 and 3.7 were written by me without GenAI-generated text. All
figures in `docs/design/` were produced by the checked-in generator script and inspected for
print quality.
