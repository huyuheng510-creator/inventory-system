# Project 2 report figures (`docs/design`)

Four print-ready PNG figures for the COMP603/ENSE600 **Project 2** report, plus a
reproducible Pillow generator. The figures use the same visual language as the Project 1
figures (Arial, opaque white background, 2× supersampled then Lanczos-resampled, UML
notation, no shadows or transparency) so the two reports look consistent.

| Figure | File | Pixels | Printed size at 6.3 in | Smallest text |
|---|---|---|---|---|
| 1 | `figure1_use_case_p2.png` | 1800 × 2120 | 6.3 × 7.42 in | 7.8 pt |
| 2 | `figure2_class_diagram_p2.png` | 2001 × 3001 | 6.3 × 9.45 in (full page) | 7.7 pt |
| 3 | `figure3_er_diagram_p2.png` | 1900 × 1660 | 6.3 × 5.50 in | 7.6 pt |
| 4 | `figure4_mvc_architecture_p2.png` | 1650 × 1620 | 6.3 × 6.19 in | 7.7 pt |

`printed_pt = font_px × (6.3 / canvas_width_px) × 72`. Every label in every figure is at
least 7.5 pt when the figure is inserted 6.3 in wide, which is the content width of the
report template.

## Figure inventory and suggested captions

### Figure 1 — `figure1_use_case_p2.png`

Project 2 use-case model. The boundary is
“StockFlow Inventory Management System (Swing GUI + Derby)”. Administrator generalises
Warehouse Staff (drawn at the top), so the administrator inherits every staff use case and
adds Manage Products/Categories/Suppliers, Adjust Stock, Manage Users and Submit/Cancel
Purchase Order. UC-01 to UC-14 extend the Project 1 model; **UC-15 View Dashboard,
UC-16 New Purchase Order, UC-17 Submit/Cancel Purchase Order and UC-18 Receive Delivery
are new in Project 2**. Three include relationships carry the stock rules (Record Stock-Out
includes Check Stock Availability; Record Stock-In/Out, Adjust Stock and Receive Delivery
include Update Stock Level) and Export Report extends Generate Reports.

Suggested caption:

> Figure 1: Use-case diagram for the Project 2 StockFlow system (UC-15 to UC-18 are the
> Project 2 additions).

### Figure 2 — `figure2_class_diagram_p2.png`

Final class diagram of the Project 2 design, matched to the built code. Four package bands
show the dependency direction `ui` → `service` → `dao` → `model`; the `dao` band also
contains the database infrastructure (`DatabaseManager`, `SchemaInitializer`,
`ConnectionProvider`, `TransactionRunner`). GUI views and table models are summarised in a
note; the controllers, services, DAO interfaces/JDBC implementation and domain entities
(including the new `PurchaseOrder` / `PurchaseOrderLine` / `PurchaseOrderStatus`, plus the
extension classes `AlertHistoryService` and `StockAlert`) are shown
with their relationships: realisation (dashed hollow triangle), generalisation (solid hollow
triangle), association with multiplicities and dependency (dashed open arrow). Getter/setter
and constructor noise is intentionally omitted.

Suggested caption:

> Figure 2: Final class diagram of the Project 2 design (MVC layer, service layer, JDBC DAO
> layer and domain model).

### Figure 3 — `figure3_er_diagram_p2.png`

Entity–relationship diagram of the embedded Apache Derby schema created by
`SchemaInitializer` from `src/main/resources/db/schema.sql`. Seven tables, primary/foreign/
unique keys, data types and crow's-foot cardinalities are shown. `USERS` is not referenced
by a foreign key: `operator_username` and `created_by` store the signed-in username as an
audit value. `SUPPLIERS` carries the two lead-time columns `avg_lead_days` and
`delivery_count`, which the purchase-order receipt path maintains.

Suggested caption:

> Figure 3: Entity–relationship diagram of the embedded Derby database (seven tables).

### Figure 4 — `figure4_mvc_architecture_p2.png`

Layered MVC architecture: View (Swing panels, dialogs, table models) → Controller
(`ui.controller`) → Model (services + domain) → Data access (DAO interfaces + JDBC
implementations) → embedded Derby. The arrows show the call direction and the Observer
notification path back to the view. The bottom note maps the patterns used: MVC, DAO,
Singleton, Factory, Observer, Strategy and Unit of Work.

Suggested caption:

> Figure 4: MVC and layered architecture of the Project 2 system with the design patterns
> used in each layer.

## Where to insert the figures in the report

| Figure | Suggested report placement |
|---|---|
| Figure 1 | Section 3 (Software Design), use-case subsection — replace the Project 1 use case figure or add it as the updated Project 2 version |
| Figure 2 | Section 3, class/design subsection — replaces the Project 1 iteration-2 class diagram; add one paragraph explaining the change from CSV repositories to JDBC DAOs and from CUI menus to Swing MVC |
| Figure 3 | New “Database design” subsection (Component 2.2) — refer to `schema.sql` and the DAO tests for verification |
| Figure 4 | Architecture subsection (Component 2.3.2) — use it to argue the layered architecture and pattern allocation |

A short paragraph that can accompany Figure 2:

> The Project 2 design keeps the Project 1 domain model and business rules but replaces the
> CSV repository implementations with JDBC DAOs over an embedded Derby database and replaces
> the console menus with a Swing MVC layer. `DatabaseManager` (Singleton) owns the database
> configuration and transaction boundary; the GUI controllers depend only on the service
> layer; the services depend only on DAO interfaces; and `ApplicationContext` remains the
> single composition root.

## Notation

* Solid line, hollow triangle: generalisation (Administrator → Warehouse Staff).
* Dashed line, hollow triangle: realisation (`GuiStockObserver` implements
  `StockLevelObserver`, `JdbcProductDao` implements `ProductDao`, `DatabaseManager`
  implements `ConnectionProvider`/`TransactionRunner`).
* Solid line: association; multiplicities are written at the ends.
* Dashed line, open arrow: dependency or `«include»` / `«extend»` (use-case figure).
* Crow's foot: “many” end of an ER relationship; `1` marks the parent end.
* ER key markers: **PK** primary key, **FK** foreign key, **UQ** unique constraint.

## Regeneration

From the `inventory-system` directory:

```powershell
& "C:\Users\21308\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe" `
  "docs\design\generate_design_diagrams.py"
```

Render one figure only with `--only fig1`, `--only fig2`, `--only fig3` or `--only fig4`.
Requirements: Python 3.10+ with **Pillow only** (no matplotlib, no Graphviz, no PlantUML);
TrueType fonts from `C:\Windows\Fonts` (Arial, with Segoe UI/DejaVu fallbacks).
`pillow_uml.py` is the Project 1 Pillow UML toolkit (rendering primitives) kept next to the
generator so the figures remain reproducible without any external diagram tool.

The generator prints the effective point sizes (`member` and `title`) and asserts that the
figure stays readable at 6.3 in and at most one A4 page tall. Every label is measured with
`font.getlength()` before it is placed, so no text is clipped.

## Verification performed

* All four PNGs were rendered and inspected at 100 % after downscaling a copy to 1890 px
  wide (= 6.3 in at 300 dpi): no clipped text, no overlapping boxes, readable arrow heads.
* Print-size check: smallest label is 7.6 pt (Figure 3); class-diagram member text 7.7 pt,
  class titles 8.4 pt; every figure is ≤ 9.5 in tall at 6.3 in wide.
* The class diagram and ER diagram were cross-checked against the built code and
  `schema.sql` (class names, DAO methods, table/column names and keys), including the
  extension classes `AlertHistoryService` / `StockAlert` and the `SUPPLIERS.avg_lead_days`
  and `SUPPLIERS.delivery_count` columns.
* The use-case diagram was cross-checked against the GUI tabs, controllers and service
  methods (`PurchaseOrderService.submit/cancel/receive`, `StockService.stockIn/out/adjust`,
  report and administration actions).

## Files

```
docs/design/
+-- generate_design_diagrams.py      generator for all four figures
+-- pillow_uml.py                    Pillow UML rendering toolkit (from the P1 figure toolchain)
+-- figure1_use_case_p2.png
+-- figure2_class_diagram_p2.png
+-- figure3_er_diagram_p2.png
+-- figure4_mvc_architecture_p2.png
```
