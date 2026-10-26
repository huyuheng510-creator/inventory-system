#!/usr/bin/env python3
"""
generate_design_diagrams.py -- renders the four Project 2 design figures for the
COMP603/ENSE600 report (StockFlow Inventory Management System):

    figure1_use_case_p2.png          UML use-case diagram (Project 2 scope)
    figure2_class_diagram_p2.png     final class diagram (MVC + service + DAO + model)
    figure3_er_diagram_p2.png        entity-relationship diagram of the Derby schema
    figure4_mvc_architecture_p2.png  MVC / layered architecture diagram

All figures are opaque white RGB PNGs laid out for the report's 6.3 in content
width, and use the same Pillow-only UML toolkit as the Project 1 figures
(pillow_uml.py, kept from the Project 1 diagram toolchain so the report keeps
one visual language). Every label is measured before placement and every canvas
is drawn at 2x and resampled with LANCZOS.

Usage
-----
    python generate_design_diagrams.py                 # render all four figures
    python generate_design_diagrams.py --only fig4     # render a single figure
"""
from __future__ import annotations

import argparse
from pathlib import Path

from pillow_uml import (
    ClassBox,
    Edge,
    NoteBox,
    Renderer,
    draw_actor_named_above,
    draw_band,
    draw_edge_heads_labels,
    draw_edges,
    draw_note,
    draw_notation_legend,
    draw_usecase,
    ellipse_anchor,
    wrap_text,
    LINE,
    WHITE,
)

HERE = Path(__file__).resolve().parent

# palette additions for the ER / architecture figures
TINT_PANEL = (242, 246, 252)
TINT_PANEL_ALT = (250, 246, 238)
TINT_PANEL_GREEN = (240, 247, 241)
TINT_PANEL_GREY = (238, 240, 246)
TINT_CHIP = (255, 255, 255)
TINT_DB = (222, 235, 226)
EDGE_GREY = (60, 60, 66)


# ---------------------------------------------------------------------------
# shared helpers
# ---------------------------------------------------------------------------
def save_fig(r: Renderer, out_dir: Path, name: str) -> Path:
    path = out_dir / name
    r.save(path)
    print("wrote %s (%dx%d)" % (path, r.w, r.h))
    return path


def arrow_down(r: Renderer, x: float, y1: float, y2: float, label: str,
               size: float, dashed: bool = False, label_dx: float = 18.0):
    r.line([(x, y1), (x, y2)], width=2.2, dash=(12, 8) if dashed else None)
    r.open_arrow((x, y2), (x, y1), size=18.0)
    if label:
        r.text(x + label_dx, (y1 + y2) / 2.0, label, size, anchor="lm",
               fill=EDGE_GREY)


def arrow_up(r: Renderer, x: float, y1: float, y2: float, label: str,
             size: float, label_dx: float = 18.0):
    r.line([(x, y1), (x, y2)], width=2.2, dash=(12, 8))
    r.open_arrow((x, y2), (x, y1), size=18.0)
    if label:
        r.text(x - label_dx, (y1 + y2) / 2.0, label, size, anchor="rm",
               fill=EDGE_GREY)


def draw_chips(r: Renderer, x: float, y: float, max_w: float, items,
               size: float = 27.0, gap: float = 12.0, row_gap: float = 12.0):
    cx = x
    cy = y
    chip_h = r.lh(size) + 14.0
    for item in items:
        w = r.tw(item, size) + 26.0
        if cx + w > x + max_w:
            cx = x
            cy += chip_h + row_gap
        r.rrect((cx, cy, cx + w, cy + chip_h), 9, fill=TINT_CHIP,
                outline=(176, 184, 198), width=1.6)
        r.text(cx + 13.0, cy + 7.0, item, size, anchor="la", fill=(30, 30, 36))
        cx += w + gap
    return cy + chip_h


def draw_layer_panel(r: Renderer, x: float, y: float, w: float, h: float,
                     title: str, subtitle: str, items, tint,
                     body_size: float = 28.0):
    r.rrect((x, y, x + w, y + h), 14, fill=tint, outline=(176, 184, 198),
            width=2.2)
    r.text(x + 20.0, y + 12.0, title, 32.0, bold=True, anchor="la")
    r.text(x + 20.0, y + 54.0, subtitle, body_size, anchor="la",
           fill=(92, 92, 100))
    draw_chips(r, x + 20.0, y + 86.0, w - 40.0, items, size=body_size)


def draw_cylinder(r: Renderer, x: float, y: float, w: float, h: float,
                  lines, size: float = 28.0):
    r.rect((x, y + 22, x + w, y + h - 22), fill=TINT_DB, outline=LINE, width=2.0)
    r.ellipse((x, y, x + w, y + 44), fill=TINT_DB, outline=LINE, width=2.0)
    r.ellipse((x, y + h - 44, x + w, y + h), fill=TINT_DB, outline=LINE,
              width=2.0)
    for i, line in enumerate(lines):
        r.text(x + w / 2.0, y + h / 2.0 - (len(lines) - 1) * 17.0 + i * 34.0,
               line, size, anchor="mm")


# ===========================================================================
# FIGURE 1 -- use-case diagram (Project 2 scope)
# ===========================================================================
def figure_use_case(out_dir: Path):
    W, H = 1800, 2120
    r = Renderer(W, H)
    F, F_TITLE, F_UC, F_ACTOR, F_LABEL = 31.0, 35.0, 31.0, 32.0, 31.0
    BX0, BY0, BX1, BY1 = 250.0, 270.0, 1550.0, 1900.0
    ADMIN_X, STAFF_X = 150.0, 1650.0

    r.rect((BX0, BY0, BX1, BY1), fill=WHITE, outline=LINE, width=3.0)
    r.text((BX0 + BX1) / 2.0, BY0 - 54.0,
           "StockFlow Inventory Management System (Swing GUI + Derby)",
           F_TITLE, bold=True, anchor="ma")

    a_admin = draw_actor_named_above(r, ADMIN_X, 215.0, "Administrator", F_ACTOR)
    a_staff = draw_actor_named_above(r, STAFF_X, 215.0, "Warehouse Staff", F_ACTOR)

    gen_y = 40.0
    edges = [Edge(
        [a_admin["head_right"], (ADMIN_X + 122, a_admin["head_right"][1]),
         (ADMIN_X + 122, gen_y), (STAFF_X - 122, gen_y),
         (STAFF_X - 122, a_staff["head_left"][1]), a_staff["head_left"]],
        kind="gen", head_size=20.0, label="«generalisation»",
        label_pos=((ADMIN_X + STAFF_X) / 2.0, gen_y))]

    UC_W, UC_H = 340.0, 96.0
    COL1, COL2, COL3 = 470.0, 850.0, 1350.0

    admin_rows = {"UC-03": 560.0, "UC-04": 700.0, "UC-05": 840.0,
                  "UC-09": 980.0, "UC-14": 1120.0, "UC-17": 1260.0}
    staff_rows = {"UC-01": 420.0, "UC-02": 548.0, "UC-15": 676.0,
                  "UC-06": 804.0, "UC-16": 932.0, "UC-18": 1060.0,
                  "UC-07": 1188.0, "UC-08": 1316.0, "UC-10": 1444.0,
                  "UC-11": 1572.0, "UC-12": 1700.0, "UC-13": 1828.0}
    update_y, check_y = 1000.0, 1240.0

    labels = {
        "UC-01": "Login", "UC-02": "Change Password", "UC-03": "Manage Products",
        "UC-04": "Manage Categories", "UC-05": "Manage Suppliers",
        "UC-06": "Search / Filter",
        "UC-07": "Record Stock-In", "UC-08": "Record Stock-Out",
        "UC-09": "Adjust Stock", "UC-10": "Stock History",
        "UC-11": "Low-Stock Alerts", "UC-12": "Generate Reports",
        "UC-13": "Export Report", "UC-14": "Manage Users",
        "UC-15": "View Dashboard", "UC-16": "New Purchase Order",
        "UC-17": "Submit / Cancel PO",
        "UC-18": "Receive Delivery",
        "UPD": "Update Stock Level", "CHK": "Check Stock Availability",
    }
    pos = {}
    for code, y in admin_rows.items():
        pos[code] = (COL1, y)
    for code, y in staff_rows.items():
        pos[code] = (COL3, y)
    pos["UPD"] = (COL2, update_y)
    pos["CHK"] = (COL2, check_y)

    # actor associations
    for code in admin_rows:
        edges.append(Edge([a_admin["hand_right"], (COL1 - UC_W / 2.0, admin_rows[code])],
                          kind="assoc"))
    for code in staff_rows:
        edges.append(Edge([a_staff["hand_left"], (COL3 + UC_W / 2.0, staff_rows[code])],
                          kind="assoc"))

    def uc_edge(src, dst, kind, label_pos=None):
        sx, sy = pos[src]
        dx, dy = pos[dst]
        start = ellipse_anchor(sx, sy, UC_W / 2.0, UC_H / 2.0, dx, dy)
        end = ellipse_anchor(dx, dy, UC_W / 2.0, UC_H / 2.0, sx, sy)
        return Edge([start, end], kind=kind, label_pos=label_pos)

    edges.append(uc_edge("UC-07", "UPD", "include", (1100, 1094)))
    edges.append(uc_edge("UC-08", "CHK", "include", (1100, 1278)))
    edges.append(uc_edge("UC-08", "UPD", "include", (1000, 1150)))
    edges.append(uc_edge("UC-09", "UPD", "include", (720, 925)))
    edges.append(uc_edge("UC-18", "UPD", "include", (1100, 1026)))
    # low-stock alert extends the stock-level update (routed through the gap)
    x11, y11 = pos["UC-11"]
    edges.append(Edge([(x11 - UC_W / 2.0, y11), (1168, y11), (1168, 1042),
                       ellipse_anchor(COL2, update_y, UC_W / 2.0, UC_H / 2.0, 1168, update_y)],
                      kind="extend", label_pos=(1128, 1360)))
    # export extends report generation (adjacent rows)
    x13, y13 = pos["UC-13"]
    x12, y12 = pos["UC-12"]
    edges.append(Edge([(x13, y13 - UC_H / 2.0), (x12, y12 + UC_H / 2.0)],
                      kind="extend", label_pos=(1210, 1764)))

    draw_edges(r, edges, F_LABEL)
    for code, (cx, cy) in pos.items():
        lines = [code]
        if code not in ("UPD", "CHK"):
            lines = [code] + wrap_text(r, labels[code], F_UC, UC_W - 48)
        else:
            lines = wrap_text(r, labels[code], F_UC, UC_W - 48)
        draw_usecase(r, cx, cy, UC_W, UC_H, lines, F_UC)
    draw_edge_heads_labels(r, edges, F_LABEL)

    draw_note(r, 540, 1420, 560,
              "Receive Delivery reuses Record Stock-In and Update Stock "
              "Level; Submit / Cancel PO is administrator-only.",
              size=F, title="Project 2 additions")
    draw_note(r, 540, 1650, 560,
              "UC-15 to UC-18 are new in Project 2; UC-01 to UC-14 extend the "
              "Project 1 use-case model. Included use cases have no separate "
              "UC number.", size=F, title="Numbering note")

    # legend
    def legend_row(items, y):
        sample_w, gap = 90.0, 54.0
        widths = [sample_w + 14 + r.tw(lbl, F) for lbl, _ in items]
        total = sum(widths) + gap * (len(items) - 1)
        x = (W - total) / 2.0
        for (lbl, kind), wid in zip(items, widths):
            p1, p2 = (x, y), (x + sample_w, y)
            if kind == "gen":
                r.line([p1, p2], width=2.0)
                r.hollow_triangle(p2, p1, size=20.0)
            elif kind in ("include", "extend"):
                r.line([p1, p2], width=2.0, dash=(12, 8))
                r.open_arrow(p2, p1, size=16.0)
            else:
                r.line([p1, p2], width=2.0)
            r.text(x + sample_w + 14, y + r.lh(F) / 2.0, lbl, F, anchor="lm")
            x += wid + gap

    legend_row([("association (actor - use case)", "assoc"),
                ("generalisation (hollow triangle)", "gen")], 1955.0)
    legend_row([("«include» (dashed arrow)", "include"),
                ("«extend» (dashed arrow)", "extend")], 2010.0)
    return save_fig(r, out_dir, "figure1_use_case_p2.png")


# ===========================================================================
# FIGURE 2 -- final class diagram (MVC + service + DAO + model)
# ===========================================================================
def figure_class_diagram(out_dir: Path):
    F_BODY, F_TITLE, F_LABEL = 34.0, 37.0, 34.0
    GAP, TOP = 8.0, 76.0
    meas = Renderer(2400, 200)

    def cb(name, kind="class", attrs=(), methods=(), stereo=None):
        return ClassBox(name, kind=kind, attrs=attrs, methods=methods,
                        title_size=F_TITLE, attr_size=F_BODY, pitch_ratio=0.95,
                        max_title_w=400.0, stereo=stereo)

    # ---------------- ui / mvc layer --------------------------------------
    u_ac = cb("ApplicationContext",
              attrs=["- databaseManager", "- 7 DAOs", "- 6 services"],
              methods=["+ createDefault()"])
    u_frame = cb("MainFrame", attrs=["- context"],
                 methods=["+ buildTabs(): void"])
    u_login = cb("LoginDialog", attrs=["- loginController"],
                 methods=["+ isAuthenticated()"])
    u_ctrl = cb("MainController", attrs=["- authService", "- orderService"],
                methods=["+ signOut(): void", "+ changePassword()"])
    u_prod = cb("ProductController", attrs=["- productService", "- catalogService"],
                methods=["+ saveProduct()", "+ deleteProduct()"])
    u_po = cb("PurchaseOrderController", attrs=["- orderService"],
              methods=["+ createDraft()", "+ receiveOrder()"])
    u_obs = cb("GuiStockObserver", methods=["+ onStockLevelChanged()"])
    ui_boxes = [u_ac, u_frame, u_login, u_ctrl, u_prod, u_po, u_obs]

    # ---------------- service layer ---------------------------------------
    s_auth = cb("AuthService", attrs=["- userDao", "- currentUser"],
                methods=["+ login(): Optional<User>", "+ requireAdmin(): void"])
    s_prod = cb("ProductService", attrs=["- productDao", "- authService"],
                methods=["+ createProduct()", "+ importFromCsv()"])
    s_cat = cb("CatalogService", attrs=["- catDao", "- supDao"],
               methods=["+ createCategory()", "+ deleteSupplier()"])
    s_stock = cb("StockService",
                 attrs=["- productDao", "- txDao", "- observers"],
                 methods=["+ stockIn()", "+ stockOut()", "+ adjustStock()"])
    s_report = cb("ReportService",
                  attrs=["- productDao", "- txDao"],
                  methods=["+ inventoryValuation()", "+ reorderSuggestions()"])
    s_po = cb("PurchaseOrderService",
              attrs=["- orderDao", "- productDao", "- stockService"],
              methods=["+ createDraft()", "+ submit()", "+ receive()"])
    s_txf = cb("TransactionFactory", methods=["+ stockIn(...)", "+ stockOut(...)"])
    s_policy = cb("ReorderPolicy", "interface",
                  methods=["+ getName(): String", "+ suggestQuantity()"])
    s_thr = cb("ThresholdReorderPolicy", methods=["+ suggestQuantity()"])
    s_dem = cb("DemandBasedReorderPolicy", methods=["+ suggestQuantity()"])
    s_obs = cb("StockLevelObserver", "interface",
               methods=["+ onStockLevelChanged()"])
    s_hist = cb("AlertHistoryService", attrs=["- alerts"],
                methods=["+ getAlerts()", "+ clear()"])
    service_boxes = [s_auth, s_prod, s_cat, s_stock, s_report, s_po, s_txf,
                     s_policy, s_thr, s_dem, s_obs, s_hist]

    # ---------------- DAO / database layer --------------------------------
    d_dao = cb("Dao<T>", "interface",
               methods=["+ findAll()", "+ findById()", "+ save()", "+ deleteById()"])
    d_prod = cb("ProductDao", "interface",
                methods=["+ findBySku()", "+ search()", "+ findLowStock()"])
    d_jdbc = cb("JdbcProductDao", methods=["+ findById()", "+ save(entity)"])
    d_tx = cb("TransactionDao", "interface",
              methods=["+ findByProductId()", "+ findBetween()"])
    d_po = cb("PurchaseOrderDao", "interface",
              methods=["+ findByStatus()", "+ nextOrderNumber()"])
    d_conn = cb("ConnectionProvider", "interface",
                methods=["+ withConnection(work)"])
    d_txr = cb("TransactionRunner", "interface",
               methods=["+ runInTransaction(work)"])
    d_db = cb("DatabaseManager", stereo="«singleton»",
              attrs=["- jdbcUrl: String"],
              methods=["+ withConnection()", "+ runInTransaction()", "+ shutdown()"])
    d_schema = cb("SchemaInitializer", methods=["+ ensureSchema()"])
    dao_boxes = [d_dao, d_prod, d_jdbc, d_tx, d_po, d_conn, d_txr, d_db, d_schema]

    # ---------------- model layer -----------------------------------------
    m_ident = cb("Identifiable", "interface", methods=["+ getId(): String"])
    m_abs = cb("AbstractEntity", "abstract",
               attrs=["- id: String", "- createdAt"],
               methods=["+ getId(): String", "+ toDisplayString()"])
    m_user = cb("User", attrs=["- username", "- passwordHash", "- role"],
                methods=["+ isAdmin(): boolean"])
    m_cat = cb("Category", attrs=["- name", "- description"],
               methods=["+ toDisplayString()"])
    m_prod = cb("Product", attrs=["- sku", "- unitPrice", "- quantity",
                                 "- reorderLevel"],
                methods=["+ increaseStock()", "+ decreaseStock()",
                         "+ getStockStatus()"])
    m_sup = cb("Supplier", attrs=["- name", "- contactName", "- email",
                                 "- avgLeadDays", "- deliveryCount"],
               methods=["+ recordDelivery()", "+ leadTimeSummary()"])
    m_tx = cb("StockTransaction", attrs=["- productId", "- type", "- quantity"],
              methods=["+ getSignedQuantity()", "+ getTotalValue()"])
    m_po = cb("PurchaseOrder", attrs=["- orderNumber", "- supplierId", "- status"],
              methods=["+ getTotalValue()"])
    m_poline = cb("PurchaseOrderLine", attrs=["- orderId", "- productId",
                                              "- quantity", "- receivedQuantity"],
                  methods=["+ getRemainingQuantity()"])
    m_postatus = cb("PurchaseOrderStatus", "enum",
                    attrs=["DRAFT", "ORDERED", "PARTIALLY_RECEIVED",
                           "RECEIVED", "CANCELLED"])
    m_alert = cb("StockAlert", attrs=["- productSku", "- status", "- occurredAt"],
                 methods=["+ getProductSku()"])
    model_boxes = [m_ident, m_abs, m_user, m_cat, m_prod, m_sup, m_tx,
                   m_po, m_poline, m_postatus, m_alert]

    # ---------------- column widths ---------------------------------------
    cols = {"ui": ui_boxes, "service": service_boxes,
            "dao": dao_boxes, "model": model_boxes}
    for boxes in cols.values():
        for b in boxes:
            b.measure(meas)
    col_w = {k: max(b.w for b in v) for k, v in cols.items()}
    for k, boxes in cols.items():
        for b in boxes:
            b.w = col_w[k]
    print("figure2 column widths:", {k: round(v) for k, v in col_w.items()})

    pad = {"ui": (18, 28), "service": (18, 18), "dao": (44, 38), "model": (66, 40)}
    band_gap, margin = 12.0, 9.0
    order = ["ui", "service", "dao", "model"]
    band_w = {k: col_w[k] + pad[k][0] + pad[k][1] for k in order}
    W = int(round(2 * margin + sum(band_w.values()) + band_gap * (len(order) - 1)))
    body_pt = F_BODY * 6.3 * 72.0 / W
    title_pt = F_TITLE * 6.3 * 72.0 / W
    print("figure2 width = %d px -> member %.2f pt, title %.2f pt at 6.3 in"
          % (W, body_pt, title_pt))
    assert W <= 2060, "figure too wide for >=7.5 pt member text: %d px" % W

    ui_note = NoteBox(
        "Further controllers: Dashboard, Stock, Report, Admin. Views (panels, "
        "dialogs, TableModels), getters/setters and constructors are omitted. "
        "UserRole, TransactionType and the derived StockStatus are omitted. "
        "In the model layer only the representative attributes and operations "
        "are shown.", col_w["ui"], size=F_BODY)
    dao_note = NoteBox(
        "JdbcTransactionDao, JdbcPurchaseOrderDao, JdbcUserDao, "
        "JdbcCategoryDao and JdbcSupplierDao follow the same pattern and map "
        "rows to the domain objects.", col_w["dao"], size=F_BODY)
    for n in (ui_note, dao_note):
        n.measure(meas)

    # ---------------- placement -------------------------------------------
    x = margin
    band_x, box_x = {}, {}
    for k in order:
        band_x[k] = (x, x + band_w[k])
        box_x[k] = x + pad[k][0]
        x += band_w[k] + band_gap

    def stack(items, x0, extra=None):
        y = TOP
        for i, it in enumerate(items):
            it.x = x0
            it.y = y
            y += it.h + GAP + (extra.get(i, 0.0) if extra else 0.0)
        return y - GAP

    ui_col = ui_boxes + [ui_note]
    service_col = service_boxes[:]
    dao_col = dao_boxes + [dao_note]
    model_col = model_boxes[:]

    ui_bottom = stack(ui_col, box_x["ui"])
    svc_bottom = stack(service_col, box_x["service"])
    dao_bottom = stack(dao_col, box_x["dao"])
    model_bottom = stack(model_col, box_x["model"])

    legend_top = ui_bottom + 60.0
    legend_h = F_BODY * 1.25 + 5 * F_BODY * 1.2 + F_BODY * 1.1
    H = int(round(max(svc_bottom, dao_bottom, model_bottom,
                      legend_top + legend_h) + 26.0))
    print("figure2 height = %d px -> %.2f in printed at 6.3 in width"
          % (H, H * 6.3 / W))
    assert H <= 1.5 * W, "figure too tall for one page: %d px" % H
    r = Renderer(W, H)

    # ---------------- edges ------------------------------------------------
    edges = []

    def cx(b):
        return b.x + b.w / 2.0

    def my(b):
        return b.y + b.h / 2.0

    # ---- model: identity, generalisation and associations ----------------
    edges.append(Edge([(cx(m_abs), m_abs.y), (cx(m_ident), m_ident.y + m_ident.h)],
                      kind="real", head_size=16.0))
    model_base = box_x["model"]
    children = [m_user, m_cat, m_prod, m_sup, m_tx, m_po, m_poline]
    for i, src in enumerate(children):
        lane = model_base - 12.0 - i * 8.0
        sy = my(src)
        ty = m_abs.y + m_abs.h * (0.88 - 0.12 * i)
        edges.append(Edge([(src.x, sy), (lane, sy), (lane, ty), (m_abs.x, ty)],
                          kind="gen", head_size=16.0))

    edges.append(Edge([(cx(m_cat), m_cat.y + m_cat.h), (cx(m_prod), m_prod.y)],
                      kind="assoc", mult_a="0..1", mult_b="1",
                      mult_a_pos=(cx(m_cat) + 38.0, m_cat.y + m_cat.h + 16.0),
                      mult_b_pos=(cx(m_cat) + 38.0, m_prod.y - 16.0)))
    edges.append(Edge([(cx(m_prod), m_prod.y + m_prod.h), (cx(m_sup), m_sup.y)],
                      kind="assoc", mult_a="1", mult_b="0..1",
                      mult_a_pos=(cx(m_prod) + 38.0, m_prod.y + m_prod.h + 16.0),
                      mult_b_pos=(cx(m_prod) + 38.0, m_sup.y - 16.0)))
    edges.append(Edge([(cx(m_poline), m_poline.y), (cx(m_po), m_po.y + m_po.h)],
                      kind="assoc"))
    model_right = box_x["model"] + col_w["model"]
    lane_r1 = model_right + 10.0
    sy = m_tx.y + 24.0
    ty = m_prod.y + m_prod.h - 24.0
    edges.append(Edge([(m_tx.x + m_tx.w, sy), (lane_r1, sy), (lane_r1, ty),
                       (m_prod.x + m_prod.w, ty)], kind="assoc",
                      mult_a="*", mult_b="1",
                      mult_a_pos=(m_tx.x + m_tx.w + 16.0, sy - 20.0),
                      mult_b_pos=(m_prod.x + m_prod.w + 16.0, ty - 20.0)))
    lane_r2 = model_right + 22.0
    sy = m_poline.y + 20.0
    ty = m_prod.y + m_prod.h - 56.0
    edges.append(Edge([(m_poline.x + m_poline.w, sy), (lane_r2, sy), (lane_r2, ty),
                       (m_prod.x + m_prod.w, ty)], kind="assoc"))
    lane_r3 = model_right + 34.0
    sy = m_po.y + 24.0
    ty = m_sup.y + m_sup.h - 20.0
    edges.append(Edge([(m_po.x + m_po.w, sy), (lane_r3, sy), (lane_r3, ty),
                       (m_sup.x + m_sup.w, ty)], kind="assoc"))

    # ---- dao / db -------------------------------------------------------
    edges.append(Edge([(cx(d_prod), d_prod.y), (cx(d_dao), d_dao.y + d_dao.h)],
                      kind="gen", head_size=16.0))
    edges.append(Edge([(cx(d_jdbc), d_jdbc.y), (cx(d_prod), d_prod.y + d_prod.h)],
                      kind="real", head_size=16.0))
    dao_base = box_x["dao"]
    for i, src in enumerate([d_tx, d_po]):
        lane = dao_base - 10.0 - i * 8.0
        sy = my(src)
        ty = d_dao.y + d_dao.h * (0.72 - 0.18 * i)
        edges.append(Edge([(src.x, sy), (lane, sy), (lane, ty), (d_dao.x, ty)],
                          kind="gen", head_size=16.0))
    dao_right = box_x["dao"] + col_w["dao"]
    for i, target in enumerate([d_conn, d_txr]):
        lane = dao_right + 10.0 + i * 12.0
        sy = d_db.y + d_db.h * (0.40 + 0.2 * i)
        ty = target.y + target.h * 0.55
        edges.append(Edge([(d_db.x + d_db.w, sy), (lane, sy), (lane, ty),
                           (target.x + target.w, ty)], kind="real", head_size=16.0))

    # ---- service --------------------------------------------------------
    edges.append(Edge([(cx(s_thr), s_thr.y), (cx(s_policy), s_policy.y + s_policy.h)],
                      kind="real", head_size=16.0))
    lane_sr = box_x["service"] + col_w["service"] + 8.0
    ty = s_policy.y + s_policy.h * 0.7
    edges.append(Edge([(s_dem.x + s_dem.w, my(s_dem)), (lane_sr, my(s_dem)),
                       (lane_sr, ty), (s_policy.x + s_policy.w, ty)],
                      kind="real", head_size=16.0))
    lane_sl = box_x["service"] - 8.0
    sy = s_stock.y + s_stock.h * 0.4
    ty = s_obs.y + s_obs.h * 0.5
    edges.append(Edge([(s_stock.x, sy), (lane_sl, sy), (lane_sl, ty),
                       (s_obs.x, ty)], kind="assoc",
                      mult_a="1", mult_b="0..*",
                      mult_a_pos=(s_stock.x - 14.0, sy - 20.0),
                      mult_b_pos=(s_obs.x - 52.0, ty)))
    lane_sl2 = box_x["service"] - 16.0
    sy = s_report.y + s_report.h * 0.5
    ty = s_policy.y + s_policy.h * 0.3
    edges.append(Edge([(s_report.x, sy), (lane_sl2, sy), (lane_sl2, ty),
                       (s_policy.x, ty)], kind="assoc", mult_a="1", mult_b="1"))

    # AlertHistoryService implements StockLevelObserver and is notified by StockService.
    edges.append(Edge([(cx(s_hist), s_hist.y), (cx(s_obs), s_obs.y + s_obs.h)],
                      kind="real", head_size=16.0))
    lane_sl3 = box_x["service"] - 24.0
    sy = s_stock.y + s_stock.h * 0.7
    ty = s_hist.y + s_hist.h * 0.5
    edges.append(Edge([(s_stock.x, sy), (lane_sl3, sy), (lane_sl3, ty),
                       (s_hist.x, ty)], kind="assoc",
                      mult_a="1", mult_b="0..*"))
    # The alert history stores immutable StockAlert snapshots.
    lane_sr2 = box_x["service"] + col_w["service"] + 16.0
    sy = s_hist.y + s_hist.h * 0.5
    ty = m_alert.y + m_alert.h * 0.5
    edges.append(Edge([(s_hist.x + s_hist.w, sy), (lane_sr2, sy),
                       (lane_sr2, ty), (m_alert.x, ty)], kind="assoc",
                      mult_a="1", mult_b="*"))

    # ---- service -> dao interfaces --------------------------------------
    svc_dao_pairs = [(s_auth, d_dao), (s_prod, d_prod), (s_cat, d_dao),
                     (s_stock, d_tx), (s_report, d_tx), (s_po, d_po)]
    for svc, dao in svc_dao_pairs:
        target_y = dao.y + dao.h * (0.28 + 0.18 * (svc_dao_pairs.index((svc, dao)) % 3))
        edges.append(Edge([(svc.x + svc.w, my(svc)), (dao.x, target_y)],
                          kind="assoc"))

    # ---- ui / mvc -------------------------------------------------------
    edges.append(Edge([(cx(u_ac), u_ac.y + u_ac.h), (cx(u_frame), u_frame.y)],
                      kind="dep", head_size=16.0))
    ui_right = box_x["ui"] + col_w["ui"]
    for i, ctrl in enumerate([u_ctrl, u_prod, u_po]):
        lane = ui_right + 8.0 + i * 8.0
        sy = u_frame.y + u_frame.h * 0.55
        ty = ctrl.y + ctrl.h * 0.5
        edges.append(Edge([(u_frame.x + u_frame.w, sy), (lane, sy), (lane, ty),
                           (ctrl.x + ctrl.w, ty)], kind="dep", head_size=16.0))
    edges.append(Edge([(u_login.x + u_login.w, my(u_login)),
                       (u_ctrl.x + u_ctrl.w, u_ctrl.y + u_ctrl.h * 0.25)],
                      kind="dep", head_size=16.0))
    edges.append(Edge([(u_obs.x + u_obs.w, my(u_obs)), (s_obs.x, my(s_obs))],
                      kind="real", head_size=16.0))
    for ui_box, svc_box in [(u_ctrl, s_auth), (u_prod, s_prod), (u_po, s_po)]:
        edges.append(Edge([(ui_box.x + ui_box.w, my(ui_box)),
                           (svc_box.x, svc_box.y + svc_box.h * 0.35)],
                          kind="dep", head_size=16.0))

    # ---- dao -> model ---------------------------------------------------
    edges.append(Edge([(d_jdbc.x + d_jdbc.w, my(d_jdbc)),
                       (m_prod.x, m_prod.y + m_prod.h * 0.72)],
                      kind="dep", head_size=16.0))

    # ---------------- draw -------------------------------------------------
    for k in order:
        draw_band(r, band_x[k][0], band_x[k][1], 12.0, H - 12.0,
                  "«package» " + k, size=F_BODY)
    for e in edges:
        e.draw_body(r, F_LABEL)
    for b in ui_boxes + service_boxes + dao_boxes + model_boxes:
        b.draw(r)
    ui_note.draw(r)
    dao_note.draw(r)
    for e in edges:
        e.draw_head(r, F_LABEL)
    for e in edges:
        e.draw_labels(r, F_LABEL)

    draw_notation_legend(r, band_x["ui"][0] + 14.0, legend_top, col_w["ui"], F_BODY)
    return save_fig(r, out_dir, "figure2_class_diagram_p2.png")


# ===========================================================================
# FIGURE 3 -- entity-relationship diagram (Derby schema)
# ===========================================================================
def draw_entity(r: Renderer, x: float, y: float, w: float, name: str,
                rows, title_size: float = 36.0, body_size: float = 32.0):
    """table box: shaded title + PK/FK/UQ attribute rows; returns bounding box"""
    pad = 14.0
    key_w = 56.0
    title_h = r.lh(title_size, bold=True) + 18.0
    row_h = r.lh(body_size) * 1.02 + 6.0
    h = title_h + len(rows) * row_h + 10.0
    r.rect((x, y, x + w, y + h), fill=WHITE, outline=LINE, width=2.2)
    r.rect((x, y, x + w, y + title_h), fill=(226, 235, 247), outline=None, width=0)
    r.line([(x, y + title_h), (x + w, y + title_h)], width=2.0)
    r.text(x + w / 2.0, y + 8.0, name, title_size, bold=True, anchor="ma")
    yy = y + title_h + 5.0
    for key, field, datatype in rows:
        colour = (140, 24, 24) if key == "PK" else (24, 84, 140) if key == "FK" \
            else (90, 90, 96) if key == "UQ" else (70, 70, 76)
        if key:
            r.text(x + pad, yy, key, body_size, bold=True, anchor="la", fill=colour)
        r.text(x + pad + key_w, yy, field, body_size, anchor="la")
        r.text(x + w - pad, yy, datatype, body_size, anchor="ra", fill=(86, 86, 92))
        yy += row_h
    return (x, y, x + w, y + h)


def crows_foot(r: Renderer, tip, towards, size: float = 30.0,
               spread: float = 14.0, optional: bool = False):
    """crow's-foot at *tip*; the line arrives from *towards*."""
    ux, uy = Renderer._unit(towards[0], towards[1], tip[0], tip[1])
    bx, by = tip[0] - ux * size, tip[1] - uy * size
    px, py = -uy, ux
    for off in (-spread, 0.0, spread):
        r.line([(bx + px * off, by + py * off), tip], width=2.0)
    if optional:
        cx, cy = tip[0] - ux * (size + 14.0), tip[1] - uy * (size + 14.0)
        rad = 8.0
        r.ellipse((cx - rad, cy - rad, cx + rad, cy + rad),
                  fill=WHITE, outline=LINE, width=2.0)


def relation(r: Renderer, pts, one_label="1", many_label="0..*",
             many_optional: bool = False, label_positions=()):
    """draw an ER relationship polyline with crow's foot and cardinality labels"""
    r.line(pts, width=2.2)
    crows_foot(r, pts[-1], pts[-2], optional=many_optional)
    ax, ay = pts[0]
    bx, by = pts[1]
    ux, uy = Renderer._unit(ax, ay, bx, by)
    px, py = -uy, ux
    cx, cy = ax + ux * 26.0, ay + uy * 26.0
    r.line([(cx - px * 12.0, cy - py * 12.0), (cx + px * 12.0, cy + py * 12.0)],
           width=2.0)
    for label, pos in label_positions:
        r.halo_text(pos[0], pos[1], label, 32.0, anchor="mm", bg=WHITE)


def figure_er(out_dir: Path):
    W, H = 1900, 1660
    r = Renderer(W, H)

    draw_entity(r, 20, 90, 520, "CATEGORIES", [
        ("PK", "id", "VARCHAR(36)"),
        ("UQ", "name", "VARCHAR(40)"),
        ("", "description", "VARCHAR(120)"),
    ])
    draw_entity(r, 620, 90, 600, "PRODUCTS", [
        ("PK", "id", "VARCHAR(36)"),
        ("UQ", "sku", "VARCHAR(20)"),
        ("", "name", "VARCHAR(60)"),
        ("FK", "category_id", "VARCHAR(36)"),
        ("FK", "supplier_id", "VARCHAR(36)"),
        ("", "unit_price", "DECIMAL(12,2)"),
        ("", "quantity", "INTEGER"),
        ("", "reorder_level", "INTEGER"),
        ("", "unit", "VARCHAR(20)"),
    ])
    draw_entity(r, 1280, 90, 520, "SUPPLIERS", [
        ("PK", "id", "VARCHAR(36)"),
        ("UQ", "name", "VARCHAR(60)"),
        ("", "contact_name", "VARCHAR(40)"),
        ("", "phone", "VARCHAR(20)"),
        ("", "email", "VARCHAR(80)"),
        ("", "avg_lead_days", "DOUBLE"),
        ("", "delivery_count", "INTEGER"),
    ])
    draw_entity(r, 80, 700, 620, "STOCK_TRANSACTIONS", [
        ("PK", "id", "VARCHAR(36)"),
        ("FK", "product_id", "VARCHAR(36)"),
        ("", "type", "VARCHAR(20)"),
        ("", "quantity", "INTEGER"),
        ("", "unit_price", "DECIMAL(12,2)"),
        ("", "operator_username", "VARCHAR(20)"),
        ("", "note", "VARCHAR(120)"),
        ("", "occurred_at", "TIMESTAMP"),
    ])
    draw_entity(r, 1240, 700, 620, "PURCHASE_ORDERS", [
        ("PK", "id", "VARCHAR(36)"),
        ("UQ", "order_number", "VARCHAR(24)"),
        ("FK", "supplier_id", "VARCHAR(36)"),
        ("", "status", "VARCHAR(24)"),
        ("", "created_by", "VARCHAR(20)"),
        ("", "created_on", "TIMESTAMP"),
        ("", "expected_date", "DATE"),
        ("", "received_at", "TIMESTAMP"),
        ("", "note", "VARCHAR(120)"),
    ])
    draw_entity(r, 1240, 1220, 620, "PURCHASE_ORDER_LINES", [
        ("PK", "id", "VARCHAR(36)"),
        ("FK", "order_id", "VARCHAR(36)"),
        ("FK", "product_id", "VARCHAR(36)"),
        ("", "quantity", "INTEGER"),
        ("", "unit_cost", "DECIMAL(12,2)"),
        ("", "received_quantity", "INTEGER"),
    ])
    draw_entity(r, 60, 1180, 560, "USERS", [
        ("PK", "id", "VARCHAR(36)"),
        ("UQ", "username", "VARCHAR(20)"),
        ("", "password_hash", "VARCHAR(64)"),
        ("", "display_name", "VARCHAR(60)"),
        ("", "role", "VARCHAR(16)"),
    ])

    # ---- relationships ---------------------------------------------------
    relation(r, [(540, 200), (620, 200)],
             label_positions=[("1", (560, 170)), ("0..*", (576, 234))])
    relation(r, [(1280, 200), (1220, 200)],
             label_positions=[("1", (1258, 170)), ("0..*", (1242, 234))])
    relation(r, [(740, 546), (480, 700)],
             label_positions=[("1", (706, 586)), ("0..*", (516, 676))])
    relation(r, [(1140, 546), (1140, 620), (1220, 620), (1220, 1120),
                 (1290, 1220)],
             label_positions=[("1", (1158, 572)), ("0..*", (1308, 1196))])
    relation(r, [(1520, 374), (1520, 700)],
             label_positions=[("1", (1540, 414)), ("0..*", (1540, 662))])
    relation(r, [(1620, 1156), (1620, 1220)],
             label_positions=[("1", (1640, 1178)), ("0..*", (1640, 1208))])

    # ---- notes and caption ----------------------------------------------
    draw_note(r, 720, 1180, 500,
              "PK = primary key, FK = foreign key, UQ = unique constraint. "
              "Crow's foot = many end; a circle would mark an optional parent. "
              "PRODUCTS.category_id and supplier_id may be NULL.",
              size=32.0, title="Notation")
    draw_note(r, 1280, 390, 500,
              "operator_username and created_by store the signed-in username "
              "as an audit value, not a foreign key.", size=32.0, title="USERS")
    r.text(W / 2.0, 1592.0,
           "StockFlow Project 2 - Apache Derby embedded schema (created by "
           "SchemaInitializer on first start)",
           size=32.0, anchor="mm", fill=(70, 70, 76))
    return save_fig(r, out_dir, "figure3_er_diagram_p2.png")


# ===========================================================================
# FIGURE 4 -- MVC / layered architecture
# ===========================================================================
def figure_mvc(out_dir: Path):
    W, H = 1650, 1620
    r = Renderer(W, H)
    F = 28.0
    view_items = [
        "LoginDialog", "MainFrame", "DashboardPanel", "ProductPanel",
        "StockPanel", "PurchaseOrderPanel", "ReportsPanel",
        "AdministrationPanel", "ProductDialog", "PurchaseOrderDialog",
        "ReceiveDialog", "TableModels",
    ]
    controller_items = [
        "LoginController", "MainController", "DashboardController",
        "ProductController", "StockController", "PurchaseOrderController",
        "ReportController", "AdminController", "GuiStockObserver",
    ]
    model_items = [
        "AuthService", "ProductService", "CatalogService", "StockService",
        "ReportService", "PurchaseOrderService", "TransactionFactory",
        "ReorderPolicy (+2 strategies)", "StockLevelObserver", "User",
        "Product", "Category", "Supplier", "StockTransaction",
        "PurchaseOrder", "PurchaseOrderLine",
    ]
    data_items = [
        "Dao<T> + 6 DAO interfaces", "Jdbc*Dao implementations",
        "DatabaseManager «Singleton»", "SchemaInitializer",
        "TransactionRunner (unit of work)", "ConnectionProvider",
    ]

    draw_layer_panel(r, 60, 50, 1530, 260, "VIEW - Swing GUI",
                     "javax.swing components, TableModels and dialogs; no "
                     "business rules, no SQL", view_items, TINT_PANEL, F)
    draw_layer_panel(r, 60, 370, 1530, 240, "CONTROLLER - ui.controller",
                     "translates user events into service calls and refreshes "
                     "the views", controller_items, TINT_PANEL_ALT, F)
    draw_layer_panel(r, 60, 670, 1530, 280, "MODEL - service + domain",
                     "business rules, validation, authorisation, design patterns",
                     model_items, TINT_PANEL_GREEN, F)
    draw_layer_panel(r, 60, 1010, 1530, 200, "DATA ACCESS - dao + db",
                     "DAO interfaces, JDBC implementations and the embedded "
                     "database manager", data_items, TINT_PANEL_GREY, F)

    arrow_down(r, 190, 312, 368, "user events -> controller actions", F)
    arrow_up(r, 1400, 368, 312, "view refresh + observer alerts", F)
    arrow_down(r, 190, 612, 668, "service calls (business rules)", F)
    arrow_up(r, 1400, 668, 612, "results + low-stock notifications", F)
    arrow_down(r, 190, 952, 1008, "DAO interface calls (CRUD)", F)
    arrow_up(r, 1400, 1008, 952, "domain objects", F)

    draw_cylinder(r, 560, 1262, 530, 138,
                  ["Embedded Apache Derby database",
                   "data/inventorydb - no server, no manual setup"])
    arrow_down(r, 825, 1212, 1260, "JDBC - in-process", F)

    note = ("Layers: VIEW -> CONTROLLER -> MODEL -> DATA ACCESS -> Derby. "
            "Patterns: MVC, DAO, Singleton (DatabaseManager), Factory "
            "(TransactionFactory), Observer (StockLevelObserver), Strategy "
            "(ReorderPolicy), Unit of Work (TransactionRunner).")
    draw_note(r, 60, 1440, 1530, note, size=28.0)
    return save_fig(r, out_dir, "figure4_mvc_architecture_p2.png")


# ---------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Render the Project 2 design figures")
    ap.add_argument("--only", choices=["fig1", "fig2", "fig3", "fig4"],
                    default=None, help="render only one figure")
    ap.add_argument("--outdir", default=str(HERE))
    args = ap.parse_args()
    out_dir = Path(args.outdir)
    out_dir.mkdir(parents=True, exist_ok=True)
    if args.only in (None, "fig1"):
        figure_use_case(out_dir)
    if args.only in (None, "fig2"):
        figure_class_diagram(out_dir)
    if args.only in (None, "fig3"):
        figure_er(out_dir)
    if args.only in (None, "fig4"):
        figure_mvc(out_dir)


if __name__ == "__main__":
    main()
