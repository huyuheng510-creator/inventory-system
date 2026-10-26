# Test Evidence - StockFlow Inventory Management System (Project 2)

**Project:** COMP603/ENSE600 Project 2 - Java 17 Swing GUI inventory management system with embedded
Apache Derby, a JDBC DAO layer and JUnit 4 tests
**Date of recorded runs:** 2026-10-23 (Asia/Shanghai)
**Build tool:** Apache Maven 3.9.14
**Compiler target:** `--release 17` (runtime used for the recorded runs: Oracle JDK 21.0.10)
**Dependencies:** Apache Derby 10.16.1.1 (embedded), JUnit 4.13.2, Hamcrest 1.3
**Platform:** Windows 11, PowerShell 7

---

## 1. How to reproduce

```
cd inventory-system
mvn clean verify
```

`mvn clean verify` compiles the project, runs all JUnit 4 tests and packages the self-contained
runnable jar. The Project 1 console scenarios are superseded by the suite below.

The one-command wrapper does the same and checks the jar:

```
powershell -ExecutionPolicy Bypass -File scripts\verify.ps1
```

GUI checks were recorded by launching the built jar / main window and capturing the real desktop
(see `docs\screenshots\`).

---

## 2. Result summary

| ID | Scenario | Command / method | Expected | Observed | Result |
|---|---|---|---|---|---|
| T1 | Build and package | `mvn clean package` | `BUILD SUCCESS`, runnable jar with Derby included | 103 sources compiled with `release 17`; shaded jar `target\inventory-management-system-2.0.0.jar` (~3.8 MB) with `Main-Class: com.cjlu.inventory.App`, Derby engine and `META-INF/services/java.sql.Driver` -> `org.apache.derby.iapi.jdbc.AutoloadedDriver` | **PASS** |
| T2 | JUnit 4 suite | `mvn test` | all tests pass | `Tests run: 35, Failures: 0, Errors: 0, Skipped: 0` (8 test classes) | **PASS** |
| T3 | Fresh database creation and seeding | launch application from an empty working directory | database + schema created, demo data seeded automatically | `data\inventorydb` created; seed verified: 2 users, 5 categories, 4 suppliers, 10 products, 18 transactions, 2 purchase orders | **PASS** |
| T4 | Purchase-order workflow end-to-end | service-level smoke run + `PurchaseOrderServiceTest` | draft -> submit -> partial receive -> full receive; stock and audit log updated atomically | seeded order `PO-20261023-001` received: `status=RECEIVED`, `receivedQty=42`, product quantities and `STOCK_IN` transactions updated; partial state `PARTIALLY_RECEIVED` and validation rules covered by tests | **PASS** |
| T5 | GUI startup and navigation | launch jar, sign in as `admin`, switch every tab | login dialog, main window, all role tabs and data load without exception | login window titled `StockFlow - Sign In`; main window `StockFlow Inventory Management System - Project 2` (1280x800) with Dashboard/Products/Stock/Purchase Orders/Reports/Administration; all tabs captured in `docs\screenshots\` | **PASS** |
| T6 | Role-based UI | sign in as `staff` | administration and admin-only actions hidden | 5 tabs for staff (no Administration); admin-only actions additionally guarded by service checks | **PASS** |

---

## 3. Detailed evidence

### 3.1 T1 - Build and package

```
[INFO] Compiling 103 source files with javac [debug release 17] to target\classes
[INFO] Building jar: ...\target\inventory-management-system-2.0.0.jar
[INFO] Including org.apache.derby:derby:jar:10.16.1.1 in the shaded jar.
[INFO] Including org.apache.derby:derbyshared:jar:10.16.1.1 in the shaded jar.
[INFO] Replacing original artifact with shaded artifact.
[INFO] BUILD SUCCESS
```

Jar inspection:

```
entries: 1851
Main-Class: com.cjlu.inventory.App
META-INF/services/java.sql.Driver -> org.apache.derby.iapi.jdbc.AutoloadedDriver
org/apache/derby/iapi/jdbc/AutoloadedDriver.class present
```

### 3.2 T2 - JUnit 4 test suite

```
-------------------------------------------------------
 T E S T S
-------------------------------------------------------
Running com.cjlu.inventory.dao.ProductDaoTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.dao.PurchaseOrderDaoTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.dao.TransactionDaoTest
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.service.AuthServiceTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.service.ProductCatalogServiceTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.service.PurchaseOrderServiceTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.service.ReportServiceTest
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
Running com.cjlu.inventory.service.StockServiceTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
-------------------------------------------------------
Results:
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test classes (all under `src/test/java/com/cjlu/inventory/`):

| Class | Tests | What is verified |
|---|---|---|
| `dao.ProductDaoTest` | 5 | insert / update / find by id / find by SKU (case-insensitive) / exists / search / category filter / low-stock query / delete |
| `dao.TransactionDaoTest` | 3 | persistence and history order; date range `from <= occurredAt < to`; `findRecent` ordering and limit |
| `dao.PurchaseOrderDaoTest` | 4 | header + line round-trip; status updates; sequential order numbers; status filter; line deletion per order |
| `service.AuthServiceTest` | 5 | login success / wrong password / unknown user; session state; SHA-256 hashes not plain text; admin-only checks |
| `service.StockServiceTest` | 6 | stock-in / stock-out; insufficient stock rejected without writing; signed adjustment (admin only); observer notifications for `IN_STOCK -> LOW_STOCK` |
| `service.PurchaseOrderServiceTest` | 6 | draft validation; admin submit / cancel; staff authorisation failures; partial receive -> `PARTIALLY_RECEIVED`; full receive -> `RECEIVED` with stock and transaction assertions; received orders cannot be received again |
| `service.ProductCatalogServiceTest` | 4 | product creation and category filter; duplicate SKU; delete protection for products with history; referenced category/supplier cannot be deleted |
| `service.ReportServiceTest` | 2 | valuation rows/total; movement aggregation inside a fixed date range |

Every test class gets a **unique embedded Derby in-memory database**
(`jdbc:derby:memory:test_<Class>_<nanoTime>;create=true`), created through
`SchemaInitializer.ensureSchema(...)` and shut down in `@After`. Tests are independent and repeatable.

### 3.3 T3 - Fresh database and demo seed

Launching the application from an empty working directory produced:

```
users=2 categories=5 suppliers=4 products=10 transactions=18 orders=2 lines=3
```

The database directory `data\inventorydb` and the `data\exports` directory are created
automatically; no SQL script, service or manual setup is required.

### 3.4 T4 - Purchase-order workflow

Service-level run against the seeded database (recorded during development):

```
logged in as admin admin=true
PO PO-20261023-002 status=DRAFT   lines=1 value=210.00  open=true
PO PO-20261023-001 status=ORDERED lines=2 value=2673.00 open=true
stockIn 5 -> ELE-001 qty=47
received PO-20261023-001 status=RECEIVED receivedQty=42 products=10/10
```

`PurchaseOrderServiceTest` additionally asserts the partial-receipt path, the atomic combination of
`products.quantity` + `stock_transactions` + `purchase_order_lines.received_quantity`, the
`received_at` timestamp, and the authorisation rules (staff cannot submit/cancel).

### 3.5 T5 / T6 - GUI checks

* Login window (title `StockFlow - Sign In`, 470x540) shows the demo-account hint and inline red
  error for invalid credentials; successful sign-in opens the main window.
* Main window title `StockFlow Inventory Management System - Project 2`, default size 1280x800,
  minimum 1100x700, centred; header shows the signed-in user and role.
* Administrator sees six tabs: Dashboard, Products, Stock, Purchase Orders, Reports, Administration.
  Staff sees five tabs (no Administration) and no product/stock master-data actions.
* All tabs load seeded data without exceptions; every dialog (product, stock movement, adjustment,
  purchase draft, receive, change password, users, categories, suppliers) was constructed and
  exercised during GUI development smoke runs (34/34 construction checks and 26/26 behaviour
  checks reported by the GUI implementation).
* Captured screenshots: `docs\screenshots\login.png`, `dashboard.png`, `products.png`, `stock.png`,
  `purchase-orders.png`, `reports.png`, `administration.png`.

---

## 4. Known limitations of this evidence

* The GUI screenshots were captured with a temporary Robot-based harness on the real Windows
  desktop; they document layout and data, not animation or interaction timing.
* CSV export dialogs use `JFileChooser`; the export service methods are covered by tests and the GUI
  code path was reviewed/compiled, but choosing a folder in the native dialog was not automated.
* Login failures are intentionally shown as an inline red message in the dialog (not as a modal
  error box), which is the designed behaviour recorded in the screenshot.
* Login success state, low-stock alert observer messages and dashboard refresh after stock changes
  were verified by the GUI smoke runs; they are not part of the headless JUnit suite by design
  (Swing is excluded from CI tests with `-Djava.awt.headless=true`).
