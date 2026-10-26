#!/usr/bin/env python3
"""
generate_diagrams.py -- renders the three UML figures for the
COMP603/ENSE600 Project 1 report (StockFlow Inventory Management System).

Figures
-------
figure1_use_case.png     1600 x 1700   UML use-case diagram (spec 3.3)
figure2_class_iter1.png  1700 x 1400   class diagram, iteration 1 (spec 4)
figure3_class_iter2.png  1966 x 2812   class diagram, iteration 2 (spec 5)

Print readability (all figures are inserted 6.3 in wide in the report):
    printed_pt = font_px * (6.3 / canvas_width_px) * 72

    figure 1: member 27 px / 1600 px -> 7.66 pt
    figure 2: member 31 px / 1700 px -> 8.27 pt, titles 38 px -> 10.14 pt
    figure 3: member 34 px / 1966 px -> 7.84 pt, titles 40 px -> 9.23 pt
              (long classifier names wrap inside the title compartment, which
               keeps the four package bands narrow enough to reach 9 pt titles)

Print check: after rendering, downscale a copy to 1890 px wide (= 6.3 in at
300 dpi) and inspect it at 100 %; the script keeps every box and note sized
from measured text so nothing is clipped.

Design rules followed (spec 10):
  * PNG, opaque white RGB background, no transparency, no drop shadows;
  * Arial from C:\\Windows\\Fonts, 2x supersampled and resampled with LANCZOS
    so text and diagonal lines are antialiased;
  * every label measured with font.getlength(); boxes are sized from the
    measured text so nothing is clipped; no text below the print minimum;
  * notation: solid + hollow triangle = generalisation, dashed + hollow
    triangle = realisation, dashed + open arrow = dependency / <<include>> /
    <<extend>>, filled diamond = composition, plain line = association.

Only Pillow is required (matplotlib is not used).

Usage
-----
    python generate_diagrams.py               # render all three figures
    python generate_diagrams.py --only fig1   # render only figure 1
"""
from __future__ import annotations

import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent

# Supersampling factor: draw everything at 2x and resample down with LANCZOS so
# text and diagonal lines are antialiased on the final opaque RGB PNG.
SS = 2

# ---------------------------------------------------------------------------
# palette (all colours are opaque; prints well in greyscale too)
# ---------------------------------------------------------------------------
INK = (24, 24, 26)
LINE = (32, 32, 36)
MUTED = (90, 90, 96)
WHITE = (255, 255, 255)

TINT_INTERFACE = (221, 233, 247)
TINT_ABSTRACT = (232, 224, 245)
TINT_ENUM = (224, 240, 225)
TINT_CLASS = (238, 239, 243)
TINT_NOTE = (255, 250, 222)
TINT_BAND = (246, 247, 250)
TINT_USECASE = (255, 255, 255)
TINT_ACTOR_HEAD = (255, 255, 255)
TINT_WARN = (255, 241, 241)

FONT_REGULAR_CANDIDATES = [
    r"C:\Windows\Fonts\arial.ttf",
    r"C:\Windows\Fonts\segoeui.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/System/Library/Fonts/Supplemental/Arial.ttf",
]
FONT_BOLD_CANDIDATES = [
    r"C:\Windows\Fonts\arialbd.ttf",
    r"C:\Windows\Fonts\segoeuib.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
]


def _first_existing(paths):
    for p in paths:
        if Path(p).exists():
            return p
    return None


FONT_REGULAR = _first_existing(FONT_REGULAR_CANDIDATES)
FONT_BOLD = _first_existing(FONT_BOLD_CANDIDATES) or FONT_REGULAR
if FONT_REGULAR is None:
    raise SystemExit("No usable TrueType font found (expected Arial in C:\\Windows\\Fonts).")


# ---------------------------------------------------------------------------
# low level renderer: all public coordinates are in final (output) pixels and
# are multiplied by SS internally.
# ---------------------------------------------------------------------------
class Renderer:
    def __init__(self, width: int, height: int):
        self.w = width
        self.h = height
        self.img = Image.new("RGB", (width * SS, height * SS), WHITE)
        self.d = ImageDraw.Draw(self.img)
        self._fonts = {}

    # -- fonts / measure ---------------------------------------------------
    def font(self, size_px: float, bold: bool = False):
        key = (round(size_px, 1), bool(bold))
        f = self._fonts.get(key)
        if f is None:
            path = FONT_BOLD if bold else FONT_REGULAR
            f = ImageFont.truetype(path, max(6, int(round(size_px * SS))))
            self._fonts[key] = f
        return f

    def tw(self, text: str, size_px: float, bold: bool = False) -> float:
        """text width in final pixels"""
        return self.font(size_px, bold).getlength(text) / SS

    def lh(self, size_px: float, bold: bool = False) -> float:
        """line height (ascent+descent) in final pixels"""
        asc, desc = self.font(size_px, bold).getmetrics()
        return (asc + desc) / SS

    # -- primitives --------------------------------------------------------
    def _s(self, v):
        return v * SS

    def line(self, pts, width: float = 2.0, fill=LINE, dash=None):
        sp = [(self._s(x), self._s(y)) for x, y in pts]
        w = max(1, int(round(width * SS)))
        if dash:
            self._dashed(sp, dash, w, fill)
        else:
            self.d.line(sp, fill=fill, width=w, joint="curve")

    def _dashed(self, sp, pattern, width_scaled, fill):
        on, off = pattern
        on *= SS
        off *= SS
        for (x1, y1), (x2, y2) in zip(sp, sp[1:]):
            dist = math.hypot(x2 - x1, y2 - y1)
            if dist <= 0.001:
                continue
            ux, uy = (x2 - x1) / dist, (y2 - y1) / dist
            t = 0.0
            while t < dist:
                t2 = min(t + on, dist)
                self.d.line(
                    [(x1 + ux * t, y1 + uy * t), (x1 + ux * t2, y1 + uy * t2)],
                    fill=fill,
                    width=width_scaled,
                )
                t = t2 + off

    def rect(self, box, fill=None, outline=LINE, width: float = 2.0):
        x0, y0, x1, y1 = box
        self.d.rectangle(
            [self._s(x0), self._s(y0), self._s(x1), self._s(y1)],
            fill=fill,
            outline=outline,
            width=max(1, int(round(width * SS))),
        )

    def rrect(self, box, radius: float, fill=None, outline=LINE, width: float = 2.0):
        x0, y0, x1, y1 = box
        self.d.rounded_rectangle(
            [self._s(x0), self._s(y0), self._s(x1), self._s(y1)],
            radius=self._s(radius),
            fill=fill,
            outline=outline,
            width=max(1, int(round(width * SS))),
        )

    def ellipse(self, box, fill=None, outline=LINE, width: float = 2.0):
        x0, y0, x1, y1 = box
        self.d.ellipse(
            [self._s(x0), self._s(y0), self._s(x1), self._s(y1)],
            fill=fill,
            outline=outline,
            width=max(1, int(round(width * SS))),
        )

    def polygon(self, pts, fill=None, outline=LINE, width: float = 2.0):
        sp = [(self._s(x), self._s(y)) for x, y in pts]
        self.d.polygon(sp, fill=fill, outline=outline)
        if width:
            self.d.line(sp + [sp[0]], fill=outline, width=max(1, int(round(width * SS))))

    def text(self, x, y, s, size: float = 24, bold: bool = False, fill=INK, anchor="la"):
        if not s:
            return
        self.d.text(
            (self._s(x), self._s(y)),
            s,
            font=self.font(size, bold),
            fill=fill,
            anchor=anchor,
        )

    def text_bbox(self, x, y, s, size: float, bold: bool = False, anchor="la"):
        bb = self.d.textbbox(
            (self._s(x), self._s(y)), s, font=self.font(size, bold), anchor=anchor
        )
        return tuple(v / SS for v in bb)

    def halo_text(self, x, y, s, size: float, bold: bool = False, anchor="mm",
                  fill=INK, bg=WHITE, pad: float = 4.0):
        """text with an opaque background patch so it stays readable on a line"""
        if not s:
            return
        x0, y0, x1, y1 = self.text_bbox(x, y, s, size, bold, anchor)
        self.d.rectangle(
            [self._s(x0 - pad), self._s(y0 - pad), self._s(x1 + pad), self._s(y1 + pad)],
            fill=bg,
        )
        self.text(x, y, s, size, bold, fill, anchor)

    # -- arrow heads -------------------------------------------------------
    @staticmethod
    def _unit(ax, ay, bx, by):
        dx, dy = bx - ax, by - ay
        n = math.hypot(dx, dy)
        if n < 0.001:
            return 1.0, 0.0
        return dx / n, dy / n

    def hollow_triangle(self, tip, prev, size: float = 20.0, width_ratio: float = 0.52):
        """hollow (white filled) triangle for generalisation / realisation"""
        ux, uy = self._unit(prev[0], prev[1], tip[0], tip[1])
        bx, by = tip[0] - ux * size, tip[1] - uy * size
        px, py = -uy, ux
        half = size * width_ratio
        pts = [(tip[0], tip[1]), (bx + px * half, by + py * half),
               (bx - px * half, by - py * half)]
        self.polygon(pts, fill=WHITE, outline=LINE, width=2.0)

    def open_arrow(self, tip, prev, size: float = 16.0, angle: float = 27.0, width: float = 2.0):
        """open (v-shaped) arrow head for dependencies and <<include>>/<<extend>>"""
        ux, uy = self._unit(prev[0], prev[1], tip[0], tip[1])
        for sign in (1, -1):
            a = math.radians(angle) * sign
            ca, sa = math.cos(a), math.sin(a)
            vx = -(ux * ca - uy * sa)
            vy = -(ux * sa + uy * ca)
            self.line([(tip[0], tip[1]), (tip[0] + vx * size, tip[1] + vy * size)],
                      width=width, fill=LINE)

    def diamond(self, at, toward, filled: bool = True, length: float = 26.0,
                half: float = 8.0):
        ux, uy = self._unit(at[0], at[1], toward[0], toward[1])
        px, py = -uy, ux
        mid = (at[0] + ux * length / 2.0, at[1] + uy * length / 2.0)
        end = (at[0] + ux * length, at[1] + uy * length)
        pts = [
            (at[0], at[1]),
            (mid[0] + px * half, mid[1] + py * half),
            (end[0], end[1]),
            (mid[0] - px * half, mid[1] - py * half),
        ]
        self.polygon(pts, fill=LINE if filled else WHITE, outline=LINE, width=2.0)

    # -- composite helpers -------------------------------------------------
    def save(self, path: Path):
        out = self.img.resize((self.w, self.h), Image.LANCZOS)
        out.save(path, format="PNG")
        return path


# ---------------------------------------------------------------------------
# text utilities
# ---------------------------------------------------------------------------
def wrap_text(r: Renderer, text: str, size: float, max_w: float, bold: bool = False):
    """word-wrap text to max_w final pixels; honour explicit newlines"""
    out = []
    for para in text.split("\n"):
        words = para.split()
        if not words:
            out.append("")
            continue
        cur = words[0]
        for word in words[1:]:
            trial = cur + " " + word
            if r.tw(trial, size, bold) <= max_w:
                cur = trial
            else:
                out.append(cur)
                cur = word
        out.append(cur)
    return out


def draw_actor(r: Renderer, cx: float, feet_y: float, name: str | None = None,
               name_size: float = 27.0, line_w: float = 2.0):
    """UML stick figure; returns useful anchor points on the figure"""
    head_r = 15.0
    head_cy = feet_y - 78.0
    body_top = head_cy + head_r
    body_bot = feet_y - 32.0
    arm_y = body_top + 14.0
    r.ellipse((cx - head_r, head_cy - head_r, cx + head_r, head_cy + head_r),
              fill=WHITE, outline=LINE, width=line_w)
    r.line([(cx, body_top), (cx, body_bot)], width=line_w)
    r.line([(cx - 27, arm_y), (cx + 27, arm_y)], width=line_w)
    r.line([(cx, body_bot), (cx - 19, feet_y)], width=line_w)
    r.line([(cx, body_bot), (cx + 19, feet_y)], width=line_w)
    if name:
        r.text(cx, feet_y + 12, name, name_size, bold=True, anchor="ma")
    return {
        "head_top": (cx, head_cy - head_r),
        "head_left": (cx - head_r, head_cy),
        "head_right": (cx + head_r, head_cy),
        "body_left": (cx - 8, arm_y + 4),
        "body_right": (cx + 8, arm_y + 4),
        "hand_left": (cx - 27, arm_y),
        "hand_right": (cx + 27, arm_y),
    }


def draw_actor_named_above(r: Renderer, cx: float, feet_y: float, name: str,
                           name_size: float = 27.0, line_w: float = 2.0):
    """stick figure with the actor name placed above it (keeps lines clear)"""
    anchors = draw_actor(r, cx, feet_y, None, name_size, line_w)
    name_top = anchors["head_top"][1] - 16.0 - r.lh(name_size)
    r.text(cx, name_top, name, name_size, bold=True, anchor="ma")
    anchors["name_top"] = (cx, name_top)
    return anchors


def draw_usecase(r: Renderer, cx: float, cy: float, w: float, h: float,
                 lines, size: float = 27.0, fill=TINT_USECASE):
    r.ellipse((cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), fill=fill)
    pitch = r.lh(size) + 2.0
    n = len(lines)
    for i, line in enumerate(lines):
        y = cy + (i - (n - 1) / 2.0) * pitch
        r.text(cx, y, line, size, anchor="mm")


def ellipse_anchor(cx, cy, rx, ry, tx, ty):
    dx, dy = tx - cx, ty - cy
    if abs(dx) < 0.001 and abs(dy) < 0.001:
        return (cx + rx, cy)
    t = 1.0 / math.sqrt((dx / rx) ** 2 + (dy / ry) ** 2)
    return (cx + t * dx, cy + t * dy)


# ---------------------------------------------------------------------------
# edge drawing (used by all three figures)
# ---------------------------------------------------------------------------
class Edge:
    """A UML edge: polyline + notation kind + optional labels.

    kind: 'assoc'      plain solid line (association)
          'comp'       solid + filled diamond at start (composition)
          'aggr'       solid + hollow diamond at start (aggregation)
          'gen'        solid + hollow triangle at end (generalisation)
          'real'       dashed + hollow triangle at end (realisation)
          'dep'        dashed + open arrow at end (dependency)
          'include'    dashed + open arrow at end, <<include>> label
          'extend'     dashed + open arrow at end, <<extend>> label
    """

    def __init__(self, pts, kind="assoc", label=None, label_pos=None,
                 mult_a=None, mult_b=None, mult_a_pos=None, mult_b_pos=None,
                 body_width=2.0, head_size=None):
        self.pts = [(float(x), float(y)) for x, y in pts]
        self.kind = kind
        self.label = label
        self.label_pos = label_pos
        self.mult_a = mult_a
        self.mult_b = mult_b
        self.mult_a_pos = mult_a_pos
        self.mult_b_pos = mult_b_pos
        self.body_width = body_width
        self.head_size = head_size

    # -- geometry helpers --------------------------------------------------
    def _polyline_midpoint(self):
        pts = self.pts
        total = 0.0
        segs = []
        for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
            d = math.hypot(x2 - x1, y2 - y1)
            segs.append(d)
            total += d
        half = total / 2.0
        run = 0.0
        for (x1, y1), (x2, y2), d in zip(pts, pts[1:], segs):
            if run + d >= half and d > 0:
                t = (half - run) / d
                return (x1 + (x2 - x1) * t, y1 + (y2 - y1) * t)
            run += d
        return pts[len(pts) // 2]

    def _end_tangent(self, at_end: bool):
        pts = self.pts
        if len(pts) < 2:
            return (1.0, 0.0)
        if at_end:
            a, b = pts[-2], pts[-1]
        else:
            a, b = pts[1], pts[0]
        return Renderer._unit(a[0], a[1], b[0], b[1])

    # -- drawing -----------------------------------------------------------
    def draw_body(self, r: Renderer, size: float = 27.0):
        dashed = self.kind in ("real", "dep", "include", "extend")
        r.line(self.pts, width=self.body_width,
               dash=(14, 9) if dashed else None)

    def draw_head(self, r: Renderer, size: float = 27.0):
        if self.kind in ("gen", "real"):
            r.hollow_triangle(self.pts[-1], self.pts[-2],
                              size=self.head_size or 24.0)
        elif self.kind in ("dep", "include", "extend"):
            r.open_arrow(self.pts[-1], self.pts[-2], size=self.head_size or 18.0)
        elif self.kind == "comp":
            r.diamond(self.pts[0], self.pts[1], filled=True,
                      length=self.head_size or 28.0)
        elif self.kind == "aggr":
            r.diamond(self.pts[0], self.pts[1], filled=False,
                      length=self.head_size or 28.0)

    def draw_labels(self, r: Renderer, size: float = 27.0):
        # stereotype / edge label
        label = self.label
        if label is None and self.kind in ("include", "extend"):
            label = "«%s»" % self.kind
        if label:
            pos = self.label_pos or self._polyline_midpoint()
            r.halo_text(pos[0], pos[1], label, size, anchor="mm")
        # multiplicities
        if self.mult_a:
            pos = self.mult_a_pos or self._mult_pos(start=True)
            r.halo_text(pos[0], pos[1], self.mult_a, size, anchor="mm")
        if self.mult_b:
            pos = self.mult_b_pos or self._mult_pos(start=False)
            r.halo_text(pos[0], pos[1], self.mult_b, size, anchor="mm")

    def _mult_pos(self, start: bool):
        p = self.pts[0] if start else self.pts[-1]
        q = self.pts[1] if start else self.pts[-2]
        ux, uy = Renderer._unit(q[0], q[1], p[0], p[1])
        # move slightly back along the line and to the side
        back = 26.0
        side = 18.0
        px, py = -uy, ux
        return (p[0] + ux * back + px * side, p[1] + uy * back + py * side)


def draw_edges(r: Renderer, edges, size: float = 27.0):
    for e in edges:
        e.draw_body(r, size)
    return edges


def draw_edge_heads_labels(r: Renderer, edges, size: float = 27.0):
    for e in edges:
        e.draw_head(r, size)
    for e in edges:
        e.draw_labels(r, size)


# ---------------------------------------------------------------------------
# class box
# ---------------------------------------------------------------------------
class ClassBox:
    """UML classifier box with title compartment, attributes, operations."""

    def __init__(self, name, kind="class", attrs=(), methods=(),
                 stereo=None, w=None, title_size=30.0, attr_size=27.0,
                 tint=None, min_w=0.0, pitch_ratio=0.92, max_title_w=None):
        self.name = name
        self.kind = kind              # class | interface | abstract | enum | note
        self.attrs = list(attrs)
        self.methods = list(methods)
        self.stereo = stereo
        self.w = w
        self.title_size = title_size
        self.attr_size = attr_size
        self.tint = tint
        self.min_w = min_w
        self.pitch_ratio = pitch_ratio
        self.max_title_w = max_title_w
        self.name_lines = [name]
        self.x = self.y = 0.0
        self.h = 0.0

    def _split_title(self, r: Renderer, max_w: float):
        """wrap a long classifier name at upper-case word boundaries"""
        import re as _re
        parts = _re.findall(r"[A-Z][a-z0-9]*|[a-z0-9]+", self.name) or [self.name]
        lines, cur = [], ""
        for part in parts:
            trial = (cur + part) if cur else part
            if cur and r.tw(trial, self.title_size, bold=True) > max_w:
                lines.append(cur)
                cur = part
            else:
                cur = trial
        if cur:
            lines.append(cur)
        return lines or [self.name]

    # -- geometry ----------------------------------------------------------
    @property
    def stereo_text(self):
        if self.stereo is not None:
            return self.stereo
        return {
            "interface": "«interface»",
            "abstract": "«abstract»",
            "enum": "«enum»",
        }.get(self.kind)

    def measure(self, r: Renderer):
        pad_x = 14.0
        if self.max_title_w is not None and r.tw(self.name, self.title_size, bold=True) > self.max_title_w:
            self.name_lines = self._split_title(r, self.max_title_w)
        title_w = max(r.tw(ln, self.title_size, bold=True) for ln in self.name_lines)
        st = self.stereo_text
        st_w = r.tw(st, self.title_size - 2, bold=False) if st else 0.0
        mem_w = max([r.tw(m, self.attr_size) for m in self.attrs + self.methods] or [0.0])
        need = max(title_w, st_w, mem_w) + 2 * pad_x
        if self.w is None:
            self.w = max(need, self.min_w)
        else:
            self.w = max(self.w, need, self.min_w)
        lh_title = r.lh(self.title_size, bold=True)
        lh_mem = r.lh(self.attr_size) * self.pitch_ratio
        header_lines = len(self.name_lines) + (1 if st else 0)
        header_h = header_lines * lh_title + 16.0
        n_attr, n_meth = len(self.attrs), len(self.methods)
        body = (n_attr + n_meth) * lh_mem
        self.h = header_h + body + (18.0 if (n_attr or n_meth) else 0.0)
        if n_attr and n_meth:
            self.h += 8.0
        return self.w, self.h

    def draw(self, r: Renderer, title_size=None):
        x0, y0 = self.x, self.y
        x1, y1 = x0 + self.w, y0 + self.h
        tint = self.tint
        if tint is None:
            tint = {
                "interface": TINT_INTERFACE,
                "abstract": TINT_ABSTRACT,
                "enum": TINT_ENUM,
            }.get(self.kind, TINT_CLASS)
        r.rect((x0, y0, x1, y1), fill=WHITE, outline=LINE, width=2.0)
        ts = title_size or self.title_size
        lh_title = r.lh(ts, bold=True)
        st = self.stereo_text
        header_h = (len(self.name_lines) + (1 if st else 0)) * lh_title + 16.0
        r.rect((x0, y0, x1, y0 + header_h), fill=tint, outline=None, width=0)
        r.line([(x0, y0 + header_h), (x1, y0 + header_h)], width=2.0)
        ty = y0 + 8.0
        if st:
            r.text((x0 + x1) / 2.0, ty, st, ts - 2, anchor="ma", fill=(45, 45, 55))
            ty += lh_title
        for ln in self.name_lines:
            r.text((x0 + x1) / 2.0, ty, ln, ts, bold=True, anchor="ma")
            ty += lh_title
        lh_mem = r.lh(self.attr_size) * self.pitch_ratio
        y = y0 + header_h
        if self.attrs:
            y += 6.0
            for a in self.attrs:
                r.text(x0 + 14.0, y, a, self.attr_size, anchor="la")
                y += lh_mem
            y += 5.0
        if self.attrs and self.methods:
            r.line([(x0, y), (x1, y)], width=1.6)
        if self.methods:
            y += 5.0
            for m in self.methods:
                r.text(x0 + 14.0, y, m, self.attr_size, anchor="la")
                y += lh_mem
            y += 5.0

    # handy anchors
    def anchor(self, target, pad=0.0):
        """point on the box border in the direction of target"""
        cx, cy = self.x + self.w / 2.0, self.y + self.h / 2.0
        dx, dy = target[0] - cx, target[1] - cy
        if abs(dx) < 0.001 and abs(dy) < 0.001:
            return (self.x + self.w, cy)
        tx = (self.w / 2.0 + pad) / abs(dx) if dx else float("inf")
        ty = (self.h / 2.0 + pad) / abs(dy) if dy else float("inf")
        t = min(tx, ty)
        return (cx + dx * t, cy + dy * t)

    def side(self, side, t=0.5):
        if side == "left":
            return (self.x, self.y + self.h * t)
        if side == "right":
            return (self.x + self.w, self.y + self.h * t)
        if side == "top":
            return (self.x + self.w * t, self.y)
        return (self.x + self.w * t, self.y + self.h)


def draw_note(r: Renderer, x, y, w, text, size=27.0, max_w=None, title=None):
    """UML note (folded corner) with wrapped text; returns the drawn height"""
    pad = 14.0
    inner_w = w - 2 * pad
    lines = wrap_text(r, text, size, inner_w)
    if title:
        lines = [title] + lines
    lh = r.lh(size) + 2.0
    h = len(lines) * lh + 2 * pad + 6.0
    fold = 20.0
    # body with folded corner
    r.polygon([(x, y), (x + w - fold, y), (x + w, y + fold), (x + w, y + h), (x, y + h)],
              fill=TINT_NOTE, outline=LINE, width=2.0)
    r.line([(x + w - fold, y), (x + w - fold, y + fold), (x + w, y + fold)], width=1.6)
    ty = y + pad
    for i, line in enumerate(lines):
        if i == 0 and title:
            r.text(x + pad, ty, line, size, bold=True, anchor="la")
        else:
            r.text(x + pad, ty, line, size, anchor="la")
        ty += lh
    return h


# ---------------------------------------------------------------------------
# FIGURE 1 -- use case diagram (spec 3.3)
# ---------------------------------------------------------------------------
def figure1(out_dir: Path):
    W, H = 1600, 1700
    r = Renderer(W, H)

    F = 27.0          # minimum body font: 27 px / 1600 px * 6.3 in * 72 = 7.7 pt
    F_TITLE = 31.0
    F_UC = 27.0
    F_ACTOR = 28.0
    F_LABEL = 27.0

    # --- layout ----------------------------------------------------------
    BX0, BY0, BX1, BY1 = 220, 252, 1360, 1560     # system boundary
    BOUNDARY_LABEL = "StockFlow Inventory Management System (CUI)"

    # use cases: code -> (column cx, cy); three columns, ellipse 280 x 100
    UC_W, UC_H = 280.0, 100.0
    COL1, COL2, COL3 = 380.0, 790.0, 1200.0
    staff_rows = [330, 470, 610, 750, 890, 1030, 1170, 1310, 1450]
    staff_col = {
        "UC-01": staff_rows[0],   # Login
        "UC-02": staff_rows[1],   # Change Own Password
        "UC-06": staff_rows[2],   # Search / filter / sort
        "UC-07": staff_rows[3],   # Record Stock-In
        "UC-11": staff_rows[4],   # View Low-Stock Alerts
        "UC-08": staff_rows[5],   # Record Stock-Out
        "UC-10": staff_rows[6],   # View Transaction History
        "UC-12": staff_rows[7],   # Generate Reports
        "UC-13": staff_rows[8],   # Export Report
    }
    admin_col = {
        "UC-03": 330.0,
        "UC-04": 575.0,
        "UC-09": 820.0,           # aligned with "Update Stock Level"
        "UC-05": 1065.0,
        "UC-14": 1310.0,
    }
    update_y = 820.0
    check_y = staff_col["UC-08"]

    ucs = {
        "UC-01": ["UC-01", "Login"],
        "UC-02": ["UC-02", "Change Password"],
        "UC-03": ["UC-03", "Manage Products"],
        "UC-04": ["UC-04", "Manage Categories"],
        "UC-05": ["UC-05", "Manage Suppliers"],
        "UC-06": ["UC-06", "Search/Filter/Sort"],
        "UC-07": ["UC-07", "Record Stock-In"],
        "UC-08": ["UC-08", "Record Stock-Out"],
        "UC-09": ["UC-09", "Adjust Stock"],
        "UC-10": ["UC-10", "Transaction History"],
        "UC-11": ["UC-11", "Low-Stock Alerts"],
        "UC-12": ["UC-12", "Generate Reports"],
        "UC-13": ["UC-13", "Export Report"],
        "UC-14": ["UC-14", "Manage Users"],
        "UPD":   ["Update", "Stock Level"],
        "CHK":   ["Check Stock", "Availability"],
    }
    pos = {}
    for code, cy in staff_col.items():
        pos[code] = (COL3, cy)
    for code, cy in admin_col.items():
        pos[code] = (COL1, cy)
    pos["UPD"] = (COL2, update_y)
    pos["CHK"] = (COL2, check_y)

    # actors ---------------------------------------------------------------
    admin_feet = 195.0
    staff_feet = 195.0
    ADMIN_X, STAFF_X = 150.0, 1470.0

    # --- draw ------------------------------------------------------------
    # system boundary
    r.rect((BX0, BY0, BX1, BY1), fill=WHITE, outline=LINE, width=3.0)
    r.text((BX0 + BX1) / 2.0, BY0 - 54.0, BOUNDARY_LABEL, F_TITLE, bold=True, anchor="ma")

    # actors; their names sit above the stick figures so that no association
    # line can run through a name
    a_admin = draw_actor_named_above(r, ADMIN_X, admin_feet, "Administrator", F_ACTOR)
    a_staff = draw_actor_named_above(r, STAFF_X, staff_feet, "Warehouse Staff", F_ACTOR)

    # generalisation: Administrator (child) -> Warehouse Staff (parent).
    # routed around the outside: from the head side, up over the names, back
    # down into the parent actor's head side (hollow triangle at the parent).
    gen_y = 34.0
    gen = Edge(
        [a_admin["head_right"], (ADMIN_X + 122, a_admin["head_right"][1]),
         (ADMIN_X + 122, gen_y), (STAFF_X - 122, gen_y),
         (STAFF_X - 122, a_staff["head_left"][1]), a_staff["head_left"]],
        kind="gen", head_size=20.0,
        label="«generalisation»",
        label_pos=((ADMIN_X + STAFF_X) / 2.0, gen_y),
    )
    edges = [gen]

    # staff associations (the Administrator inherits these via generalisation)
    for code in staff_col:
        cx, cy = pos[code]
        start = a_staff["hand_left"]
        end = (cx + UC_W / 2.0, cy)        # rightmost point: line stays outside
        edges.append(Edge([start, end], kind="assoc"))

    # administrator associations to the admin-only use cases
    for code in admin_col:
        cx, cy = pos[code]
        start = a_admin["hand_right"]
        end = (cx - UC_W / 2.0, cy)        # leftmost point
        edges.append(Edge([start, end], kind="assoc"))

    # include / extend ----------------------------------------------------
    def uc_edge(src, dst, kind, label_pos=None):
        (sx, sy) = pos[src]
        (dx, dy) = pos[dst]
        start = ellipse_anchor(sx, sy, UC_W / 2.0, UC_H / 2.0, dx, dy)
        end = ellipse_anchor(dx, dy, UC_W / 2.0, UC_H / 2.0, sx, sy)
        return Edge([start, end], kind=kind, label_pos=label_pos)

    # UC-07 / UC-08 / UC-11 -> Update Stock Level
    edges.append(uc_edge("UC-07", "UPD", "include", label_pos=(1000, 782)))
    edges.append(uc_edge("UC-11", "UPD", "extend", label_pos=(1000, 858)))
    edges.append(uc_edge("UC-08", "UPD", "include", label_pos=(1000, 930)))
    # UC-08 -> Check Stock Availability
    edges.append(uc_edge("UC-08", "CHK", "include", label_pos=(1000, 1032)))
    # UC-09 -> Update Stock Level
    edges.append(uc_edge("UC-09", "UPD", "include", label_pos=(585, 792)))
    # UC-13 extends UC-12 (vertical, adjacent rows)
    x13, y13 = pos["UC-13"]
    x12, y12 = pos["UC-12"]
    edges.append(Edge([(x13, y13 - UC_H / 2.0), (x12, y12 + UC_H / 2.0)],
                      kind="extend", label_pos=(1115, 1380)))

    draw_edges(r, edges, F_LABEL)

    # use cases
    for code, (cx, cy) in pos.items():
        draw_usecase(r, cx, cy, UC_W, UC_H, ucs[code], F_UC)

    draw_edge_heads_labels(r, edges, F_LABEL)

    # note inside the boundary, in the free area under the include targets
    note_txt = ("Check Stock Availability and Update Stock Level are included "
                "use cases referenced in section 3.3; they have no separate "
                "UC-01-UC-14 identifier.")
    draw_note(r, 660, 1130, 380, note_txt, F, title="Notation note")

    # --- legend ----------------------------------------------------------
    legend_y1, legend_y2 = 1606, 1658
    samples = [
        ("association (actor - use case)", "assoc"),
        ("generalisation (hollow triangle)", "gen"),
    ]
    samples2 = [
        ("«include» (dashed arrow)", "include"),
        ("«extend» (dashed arrow)", "extend"),
    ]
    sample_w = 90.0
    gap = 54.0

    def draw_legend_row(row, y):
        widths = [sample_w + 14 + r.tw(lbl, F) for lbl, _ in row]
        total = sum(widths) + gap * (len(row) - 1)
        x = (W - total) / 2.0
        for (lbl, kind), wid in zip(row, widths):
            p1 = (x, y)
            p2 = (x + sample_w, y)
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

    draw_legend_row(samples, legend_y1)
    draw_legend_row(samples2, legend_y2)

    out = out_dir / "figure1_use_case.png"
    r.save(out)
    print("wrote %s (%dx%d)" % (out, W, H))
    return out


# ---------------------------------------------------------------------------
# note box
# ---------------------------------------------------------------------------
class NoteBox:
    def __init__(self, text, w, title=None, size=27.0):
        self.text = text
        self.w = w
        self.title = title
        self.size = size
        self.x = self.y = 0.0
        self.h = 0.0

    def measure(self, r: Renderer):
        pad = 14.0
        lines = wrap_text(r, self.text, self.size, self.w - 2 * pad)
        if self.title:
            lines = [self.title] + lines
        self.h = len(lines) * (r.lh(self.size) + 2) + 2 * pad + 6
        return self.w, self.h

    def draw(self, r: Renderer):
        draw_note(r, self.x, self.y, self.w, self.text, self.size, title=self.title)


def draw_band(r: Renderer, x0, x1, y0, y1, label, size=42.0):
    r.rrect((x0, y0, x1, y1), 14, fill=TINT_BAND, outline=(202, 206, 214), width=2.0)
    r.text(x0 + 16, y0 + 12, label, size, bold=True, anchor="la", fill=(40, 40, 48))


def draw_notation_legend(r: Renderer, x, y, w, size=40.0):
    """small UML notation legend; returns the lowest y used"""
    r.text(x, y, "Notation", size, bold=True, anchor="la")
    y += size * 1.25
    items = [
        ("real", "realisation"),
        ("gen", "generalisation"),
        ("assoc", "association"),
        ("comp", "composition"),
        ("dep", "dependency"),
    ]
    for kind, label in items:
        cy = y + size * 0.55
        p1 = (x + 4, cy)
        p2 = (x + 78, cy)
        if kind == "gen":
            r.line([p1, p2], width=2.4)
            r.hollow_triangle(p2, p1, size=22.0)
        elif kind == "real":
            r.line([p1, p2], width=2.4, dash=(12, 8))
            r.hollow_triangle(p2, p1, size=22.0)
        elif kind == "comp":
            r.line([p1, p2], width=2.4)
            r.diamond(p1, p2, filled=True, length=26.0, half=9.0)
        elif kind == "dep":
            r.line([p1, p2], width=2.4, dash=(12, 8))
            r.open_arrow(p2, p1, size=18.0)
        else:
            r.line([p1, p2], width=2.4)
        r.text(x + 96, cy, label, size, anchor="lm")
        y += size * 1.2
    r.text(x + 4, y, "«interface»  «abstract»", size, anchor="la")
    r.text(x + 4, y + size * 1.15, "«enum»", size, anchor="la")
    return y + size * 2.3


# ---------------------------------------------------------------------------
# FIGURE 3 -- final class diagram (spec 5 / verified code)
#
# Print-size targets (figure is printed 6.3 in wide):
#   printed_pt = font_px * (6.3 / canvas_w_px) * 72
#   member text 34 px on ~2050 px -> 7.5 pt; titles 38 px -> 8.4 pt
# ---------------------------------------------------------------------------
def figure3(out_dir: Path):
    F_BODY, F_TITLE, F_LABEL = 34.0, 40.0, 34.0
    GAP = 16.0
    TOP = 84.0
    meas = Renderer(2200, 200)

    def cb(name, kind="class", attrs=(), methods=()):
        return ClassBox(name, kind=kind, attrs=attrs, methods=methods,
                        title_size=F_TITLE, attr_size=F_BODY, pitch_ratio=0.94,
                        max_title_w=470.0)

    # ---------------- model layer (spec 5.2) ------------------------------
    m_ident = cb("Identifiable", "interface", methods=["+ getId(): String"])
    m_abs = cb("AbstractEntity", "abstract",
               attrs=["- id: String", "- createdAt"],
               methods=["+ getId(): String", "+ toDisplayString()"])
    m_user = cb("User",
                attrs=["- username: String", "- passwordHash: String", "- role: UserRole"],
                methods=["+ isAdmin(): boolean"])
    m_role = cb("UserRole", "enum", attrs=["ADMIN", "STAFF"])
    m_cat = cb("Category", attrs=["- name: String", "- description: String"],
               methods=["+ toDisplayString()"])
    m_prod = cb("Product",
                attrs=["- sku: String", "- unitPrice: BigDecimal", "- quantity: int",
                       "- reorderLevel: int", "- categoryId / supplierId"],
                methods=["+ increaseStock(int)", "+ decreaseStock(int)", "+ getStockStatus()"])
    m_sup = cb("Supplier",
               attrs=["- name: String", "- contactName: String", "- phone: String",
                      "- email: String"],
               methods=["+ toDisplayString()"])
    m_tx = cb("StockTransaction",
              attrs=["- productId: String", "- type: TransactionType", "- quantity: int",
                     "- occurredAt"],
              methods=["+ getSignedQuantity()", "+ getTotalValue()"])
    m_ttype = cb("TransactionType", "enum", attrs=["STOCK_IN", "STOCK_OUT", "ADJUSTMENT"])
    m_status = cb("StockStatus", "enum", attrs=["IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"])
    model_boxes = [m_ident, m_abs, m_user, m_role, m_cat, m_prod, m_sup,
                   m_tx, m_ttype, m_status]

    # ---------------- repository layer (spec 5.3) -------------------------
    r_repo = cb("Repository", "interface",
                methods=["+ findAll(): List<T>", "+ findById(id): Optional<T>",
                         "+ save(entity): void"])
    r_abs = cb("AbstractFileRepository", "abstract",
               attrs=["# file: Path", "# cache: Map"],
               methods=["+ load(): void", "+ save(entity): void", "# persist(): void"])
    r_user = cb("FileUserRepository", methods=["+ findByUsername()"])
    r_prod = cb("FileProductRepository",
                methods=["+ findBySku()", "+ search(keyword)", "+ findLowStock()"])
    r_tx = cb("FileTransactionRepository",
              methods=["+ findByProductId()", "+ findBetween()", "+ findRecent()"])
    repo_boxes = [r_repo, r_abs, r_user, r_prod, r_tx]

    # ---------------- service layer (spec 5.4 + verified deltas) ----------
    s_auth = cb("AuthService", attrs=["- userRepository", "- currentUser"],
                methods=["+ login(): Optional<User>", "+ requireAdmin(): void"])
    s_prod = cb("ProductService",
                attrs=["- productRepository", "- transactionRepository", "- authService"],
                methods=["+ createProduct()", "+ deleteProduct()"])
    s_cat = cb("CatalogService",
               attrs=["- categoryRepository", "- supplierRepository", "- productRepository",
                      "- authService"],
               methods=["+ createCategory()", "+ deleteCategory()"])
    s_stock = cb("StockService",
                 attrs=["- productRepository", "- transactionRepository", "- authService",
                        "- observers"],
                 methods=["+ stockIn()", "+ stockOut()", "+ adjustStock()"])
    s_report = cb("ReportService",
                  attrs=["- productRepository", "- transactionRepository", "- stockService",
                         "- dataDirectory: Path"],
                  methods=["+ inventoryValuation()", "+ reorderSuggestions(policy)"])
    s_txf = cb("TransactionFactory", methods=["+ stockIn(...)", "+ stockOut(...)",
                                              "+ adjustment(...)"])
    s_policy = cb("ReorderPolicy", "interface",
                  methods=["+ getName(): String", "+ suggestQuantity()"])
    s_thr = cb("ThresholdReorderPolicy", methods=["+ suggestQuantity()"])
    s_dem = cb("DemandBasedReorderPolicy", methods=["+ suggestQuantity()"])
    s_obs = cb("StockLevelObserver", "interface", methods=["+ onStockLevelChanged()"])
    s_con = cb("ConsoleLowStockObserver", methods=["+ onStockLevelChanged()"])
    service_boxes = [s_auth, s_prod, s_cat, s_stock, s_report, s_txf,
                     s_policy, s_thr, s_dem, s_obs, s_con]

    # ---------------- ui / app layer (spec 5.5, 5.6) ----------------------
    u_ac = cb("ApplicationContext",
              attrs=["- consoleIO: ConsoleIO", "- 5 repositories", "- 5 services",
                     "- loginMenu: LoginMenu"],
              methods=["+ createDefault()"])
    u_menu = cb("Menu", "interface", methods=["+ run(): void"])
    u_abs = cb("AbstractMenu", "abstract",
               attrs=["# io: ConsoleIO", "# running: boolean"],
               methods=["+ run(): void", "# displayMenu(): void", "# handleChoice()"])
    u_login = cb("LoginMenu", attrs=["- authService", "- mainMenu"])
    u_main = cb("MainMenu", attrs=["- authService", "- five sub-menus"])
    u_io = cb("ConsoleIO", attrs=["- scanner", "- out"],
              methods=["+ readLine(prompt)", "+ readInt(min, max)", "+ readDecimal(prompt)"])
    ui_boxes = [u_ac, u_menu, u_abs, u_login, u_main, u_io]

    # ---------------- column widths --------------------------------------
    cols = {"ui": ui_boxes, "service": service_boxes,
            "repository": repo_boxes, "model": model_boxes}
    for boxes in cols.values():
        for b in boxes:
            b.measure(meas)
    col_w = {k: max(b.w for b in v) for k, v in cols.items()}
    for k, boxes in cols.items():
        for b in boxes:
            b.w = col_w[k]

    pad = {"ui": (22, 20), "service": (20, 10), "repository": (20, 10), "model": (36, 26)}
    band_gap = 12.0
    margin = 9.0
    order = ["ui", "service", "repository", "model"]
    band_w = {k: col_w[k] + pad[k][0] + pad[k][1] for k in order}
    W = int(round(2 * margin + sum(band_w.values()) + band_gap * (len(order) - 1)))
    member_pt = F_BODY * 6.3 * 72.0 / W
    title_pt = F_TITLE * 6.3 * 72.0 / W
    print("figure3 width = %d px -> member %.2f pt, title %.2f pt at 6.3 in"
          % (W, member_pt, title_pt))
    assert W <= 2060, "figure too wide for >=7.5 pt member text: %d px" % W

    # ---------------- small notes (required by the figure spec) -----------
    ui_note = NoteBox(
        "Further menus: ProductMenu, CatalogMenu, StockMenu, ReportMenu, "
        "UserMenu. Getters/setters and constructors omitted.",
        col_w["ui"], size=F_BODY)
    repo_note = NoteBox(
        "FileCategoryRepository and FileSupplierRepository follow the same "
        "pattern.", col_w["repository"], size=F_BODY)
    for n in (ui_note, repo_note):
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

    ui_col = [u_ac, u_menu, u_abs, u_login, u_main, u_io, ui_note]
    service_col = service_boxes[:]
    repo_col = [r_repo, r_abs, r_user, r_prod, r_tx, repo_note]
    model_col = model_boxes[:]

    ui_bottom = stack(ui_col, box_x["ui"])
    svc_bottom = stack(service_col, box_x["service"])
    repo_bottom = stack(repo_col, box_x["repository"])
    # extra air between Category/Product and Product/Supplier for multiplicities
    model_bottom = stack(model_col, box_x["model"], extra={4: 44.0, 5: 44.0})

    legend_top = ui_bottom + 64.0
    legend_h = F_BODY * 1.25 + 5 * F_BODY * 1.2 + F_BODY * 1.1
    H = int(round(max(svc_bottom, model_bottom, repo_bottom,
                      legend_top + legend_h) + 22.0))
    print("figure3 height = %d px -> %.2f in printed at 6.3 in width"
          % (H, H * 6.3 / W))
    assert H <= 1.476 * W, "figure too tall for one A4 page: %d px" % H
    r = Renderer(W, H)

    # ---------------- edges ------------------------------------------------
    edges = []

    def cx(b):
        return b.x + b.w / 2.0

    def my(b):
        return b.y + b.h / 2.0

    # ---- model: identity / generalisation --------------------------------
    edges.append(Edge([(cx(m_abs), m_abs.y), (cx(m_ident), m_ident.y + m_ident.h)],
                      kind="real", head_size=16.0))
    edges.append(Edge([(cx(m_user), m_user.y), (cx(m_abs), m_abs.y + m_abs.h)],
                      kind="gen", head_size=16.0))
    gen_sources = [m_cat, m_prod, m_sup, m_tx]
    base = box_x["model"]
    targets = [m_abs.y + m_abs.h * (0.82 - 0.17 * i) for i in range(len(gen_sources))]
    for i, src in enumerate(gen_sources):
        lane = base - 8.0 - i * 8.0
        sy, ty = my(src), targets[i]
        edges.append(Edge([(src.x, sy), (lane, sy), (lane, ty), (m_abs.x, ty)],
                          kind="gen", head_size=16.0))

    # ---- model: associations --------------------------------------------
    edges.append(Edge([(cx(m_user), m_user.y + m_user.h), (cx(m_role), m_role.y)],
                      kind="assoc"))
    g1t = m_cat.y + m_cat.h
    edges.append(Edge([(cx(m_cat), g1t), (cx(m_prod), m_prod.y)],
                      kind="assoc", mult_a="0..1", mult_b="1",
                      mult_a_pos=(cx(m_cat) - 38.0, g1t + 16.0),
                      mult_b_pos=(cx(m_cat) + 34.0, m_prod.y - 16.0)))
    g2t = m_prod.y + m_prod.h
    edges.append(Edge([(cx(m_prod), g2t), (cx(m_sup), m_sup.y)],
                      kind="assoc", mult_a="1", mult_b="0..1",
                      mult_a_pos=(cx(m_prod) - 38.0, g2t + 16.0),
                      mult_b_pos=(cx(m_prod) + 34.0, m_sup.y - 16.0)))
    # StockTransaction -- Product (by productId), routed on the right side
    lane_r = m_tx.x + m_tx.w + 10.0
    sy = m_tx.y + 20.0
    ty = m_prod.y + m_prod.h - 22.0
    edges.append(Edge([(m_tx.x + m_tx.w, sy), (lane_r, sy), (lane_r, ty),
                       (m_prod.x + m_prod.w, ty)], kind="assoc",
                      mult_a="*", mult_b="1",
                      mult_a_pos=(m_tx.x + m_tx.w + 18.0, sy - 20.0),
                      mult_b_pos=(m_prod.x + m_prod.w + 18.0, ty - 20.0)))
    edges.append(Edge([(cx(m_tx), m_tx.y + m_tx.h), (cx(m_ttype), m_ttype.y)],
                      kind="assoc"))

    # ---- repository internal --------------------------------------------
    edges.append(Edge([(cx(r_abs), r_abs.y), (cx(r_repo), r_repo.y + r_repo.h)],
                      kind="real", head_size=16.0))
    edges.append(Edge([(cx(r_user), r_user.y), (cx(r_abs), r_abs.y + r_abs.h)],
                      kind="gen", head_size=16.0))
    repo_base = box_x["repository"]
    targets = [r_abs.y + r_abs.h * (0.72 - 0.16 * i) for i in range(2)]
    for i, src in enumerate([r_prod, r_tx]):
        lane = repo_base - 7.0 - i * 8.0
        sy, ty = my(src), targets[i]
        edges.append(Edge([(src.x, sy), (lane, sy), (lane, ty), (r_abs.x, ty)],
                          kind="gen", head_size=16.0))

    # ---- service internal -----------------------------------------------
    edges.append(Edge([(cx(s_thr), s_thr.y), (cx(s_policy), s_policy.y + s_policy.h)],
                      kind="real", head_size=16.0))
    lane_sr = box_x["service"] + col_w["service"] + 6.0
    ty = s_policy.y + s_policy.h * 0.7
    edges.append(Edge([(s_dem.x + s_dem.w, my(s_dem)), (lane_sr, my(s_dem)),
                       (lane_sr, ty), (s_policy.x + s_policy.w, ty)],
                      kind="real", head_size=16.0))
    lane1 = box_x["service"] - 7.0
    sy = s_stock.y + s_stock.h * 0.4
    ty = s_obs.y + s_obs.h * 0.5
    edges.append(Edge([(s_stock.x, sy), (lane1, sy), (lane1, ty), (s_obs.x, ty)],
                      kind="assoc", mult_a="1", mult_b="0..*",
                      mult_a_pos=(s_stock.x - 12.0, sy - 20.0),
                      mult_b_pos=(s_obs.x - 56.0, ty)))
    lane2 = box_x["service"] - 16.0
    sy = s_report.y + s_report.h * 0.5
    ty = s_policy.y + s_policy.h * 0.3
    edges.append(Edge([(s_report.x, sy), (lane2, sy), (lane2, ty), (s_policy.x, ty)],
                      kind="assoc", mult_a="1", mult_b="1",
                      mult_a_pos=(s_report.x - 12.0, sy - 20.0),
                      mult_b_pos=(s_policy.x - 12.0, ty - 20.0)))

    # ---- service -> repository interfaces (1 -- 1 associations) ---------
    svc_boxes = [s_auth, s_prod, s_cat, s_stock, s_report]
    repo_targets = [r_repo.y + 30.0 + i * 40.0 for i in range(5)]
    for i, svc in enumerate(svc_boxes):
        sy = my(svc)
        ty = repo_targets[i]
        edges.append(Edge([(svc.x + svc.w, sy), (r_repo.x, ty)], kind="assoc",
                          mult_a="1",
                          mult_a_pos=(svc.x + svc.w + 8.0, sy - 18.0)))

    # ---- ui internal -----------------------------------------------------
    edges.append(Edge([(cx(u_abs), u_abs.y), (cx(u_menu), u_menu.y + u_menu.h)],
                      kind="real", head_size=16.0))
    ui_base = box_x["ui"]
    lane_gen = ui_base - 8.0
    ty = u_abs.y + u_abs.h * 0.8
    edges.append(Edge([(u_main.x, my(u_main)), (lane_gen, my(u_main)),
                       (lane_gen, ty), (u_abs.x, ty)], kind="gen", head_size=16.0))
    lane_comp = ui_base - 17.0
    sy = u_abs.y + u_abs.h * 0.45
    ty = u_io.y + u_io.h * 0.5
    edges.append(Edge([(u_abs.x, sy), (lane_comp, sy), (lane_comp, ty), (u_io.x, ty)],
                      kind="comp", head_size=24.0))

    # ---- ui -> service dependencies --------------------------------------
    edges.append(Edge([(u_ac.x + u_ac.w, my(u_ac)),
                       (s_auth.x, s_auth.y + s_auth.h * 0.32)], kind="dep",
                      head_size=16.0))
    lane_ui = box_x["ui"] + col_w["ui"] + 8.0
    ty = s_auth.y + s_auth.h * 0.72
    edges.append(Edge([(u_login.x + u_login.w, my(u_login)), (lane_ui, my(u_login)),
                       (lane_ui, ty), (s_auth.x, ty)], kind="dep", head_size=16.0))

    # ---------------- draw -------------------------------------------------
    for k in order:
        draw_band(r, band_x[k][0], band_x[k][1], 12.0, H - 12.0,
                  "«package» " + k, size=F_BODY)
    for e in edges:
        e.draw_body(r, F_LABEL)
    for b in ui_boxes + service_boxes + repo_boxes + model_boxes:
        b.draw(r)
    ui_note.draw(r)
    repo_note.draw(r)
    for e in edges:
        e.draw_head(r, F_LABEL)
    for e in edges:
        e.draw_labels(r, F_LABEL)

    draw_notation_legend(r, band_x["ui"][0] + 14.0, legend_top, col_w["ui"], F_BODY)

    out = out_dir / "figure3_class_iter2.png"
    r.save(out)
    print("wrote %s (%dx%d)" % (out, W, H))
    return out


# ---------------------------------------------------------------------------
# FIGURE 2 -- class diagram, iteration 1 (spec 4)
#
# Print-size targets: member 31 px / title 38 px on 1700 px -> 8.3 / 10.1 pt
# ---------------------------------------------------------------------------
def figure2(out_dir: Path):
    W, H = 1700, 1400
    F, F_TITLE = 31.0, 38.0
    r = Renderer(W, H)

    def box(name, attrs=(), methods=(), stereo=None):
        b = ClassBox(name, attrs=attrs, methods=methods, stereo=stereo,
                     title_size=F_TITLE, attr_size=F, pitch_ratio=0.95)
        b.measure(r)
        return b

    ims = box("InventoryManagementSystem", stereo="«God class»",
              attrs=["- products: List<Product>", "- transactions: List<Transaction>",
                     "- users: List<User>", "- scanner: Scanner", "- loggedInUser: String"],
              methods=["+ main(String[]): void", "+ showMainMenu(): void", "+ login(): void",
                       "+ showProductMenu(): void", "+ addProduct(): void",
                       "+ editProduct(): void", "+ deleteProduct(): void",
                       "+ listProducts(): void", "+ recordStockIn(): void",
                       "+ recordStockOut(): void", "+ showLowStock(): void",
                       "+ generateReport(): void", "+ loadData(): void", "+ saveData(): void"])
    product = box("Product",
                  attrs=["+ id: String", "+ name: String", "+ category: String",
                         "+ supplier: String", "+ price: double", "+ quantity: int"])
    transaction = box("Transaction",
                      attrs=["+ id: String", "+ productId: String", "+ type: String",
                             "+ operator: String", "+ date: String", "+ quantity: int"])
    user = box("User",
               attrs=["+ username: String", "+ password: String", "+ role: String"])
    report_gen = box("ReportGenerator",
                     methods=["+ printValuation(products)", "+ printLowStock(products)",
                              "+ printHistory(transactions)"])
    login_screen = box("LoginScreen", methods=["+ authenticate(u, p)"])
    category = box("Category", attrs=["+ name: String", "+ description: String"])
    supplier = box("Supplier",
                   attrs=["+ name: String", "+ contactName: String", "+ phone: String",
                          "+ email: String"])

    # layout: data/report on the left, the God class in the centre, UI on the right
    ims.x, ims.y = 620.0, 60.0
    product.x, product.y = 90.0, 70.0
    report_gen.x, report_gen.y = 90.0, 400.0
    transaction.x, transaction.y = 90.0, 610.0
    user.x, user.y = 1300.0, 70.0
    login_screen.x, login_screen.y = 1300.0, 390.0
    category.x, category.y = 1300.0, 630.0
    supplier.x, supplier.y = 1300.0, 840.0

    smells = NoteBox("Design smells: God class holds UI + business rules + file I/O; no "
                     "interfaces or abstract classes; public fields, no validation; "
                     "Stringly-typed role and type; double money; opaque binary "
                     "inventory.dat; duplicated low-stock checks.",
                     1060.0, size=F, title="Iteration 1 deficiencies")
    smells.measure(r)
    smells.x, smells.y = 90.0, 980.0
    free_text = NoteBox("Product.category / supplier are plain Strings: there is no "
                        "association to Category or Supplier (design smell).",
                        380.0, size=F, title="Missing associations")
    free_text.measure(r)
    free_text.x, free_text.y = 1300.0, 1100.0

    edges = []
    edges.append(Edge([(ims.x, 200.0), (product.x + product.w, 200.0)], kind="comp",
                      mult_a="1", mult_b="1..*",
                      mult_a_pos=(ims.x - 34.0, 176.0),
                      mult_b_pos=(product.x + product.w + 40.0, 224.0)))
    edges.append(Edge([(ims.x, 730.0), (transaction.x + transaction.w, 730.0)], kind="comp",
                      mult_a="1", mult_b="1..*",
                      mult_a_pos=(ims.x - 34.0, 706.0),
                      mult_b_pos=(transaction.x + transaction.w + 40.0, 754.0)))
    edges.append(Edge([(ims.x + ims.w, 170.0), (user.x, 170.0)], kind="comp",
                      mult_a="1", mult_b="1..*",
                      mult_a_pos=(ims.x + ims.w + 22.0, 144.0),
                      mult_b_pos=(user.x - 42.0, 198.0)))
    edges.append(Edge([(ims.x, 470.0), (report_gen.x + report_gen.w, 470.0)],
                      kind="dep", head_size=18.0))
    edges.append(Edge([(ims.x + ims.w, 440.0), (login_screen.x, 440.0)],
                      kind="dep", head_size=18.0))
    edges.append(Edge([(report_gen.x + report_gen.w / 2.0, report_gen.y),
                       (product.x + product.w / 2.0, product.y + product.h)],
                      kind="dep", head_size=18.0))
    edges.append(Edge([(report_gen.x + report_gen.w / 2.0, report_gen.y + report_gen.h),
                       (transaction.x + transaction.w / 2.0, transaction.y)],
                      kind="dep", head_size=18.0))
    edges.append(Edge([(login_screen.x + login_screen.w / 2.0, login_screen.y),
                       (user.x + user.w / 2.0, user.y + user.h)],
                      kind="dep", head_size=18.0))

    for e in edges:
        e.draw_body(r, F)
    for b in (product, report_gen, transaction, ims, user, login_screen, category, supplier):
        b.draw(r)
    smells.draw(r)
    free_text.draw(r)
    for e in edges:
        e.draw_head(r, F)
    for e in edges:
        e.draw_labels(r, F)

    member_pt = F * 6.3 * 72.0 / W
    title_pt = F_TITLE * 6.3 * 72.0 / W
    print("figure2: member %.2f pt, title %.2f pt at 6.3 in (%dx%d)"
          % (member_pt, title_pt, W, H))
    out = out_dir / "figure2_class_iter1.png"
    r.save(out)
    print("wrote %s (%dx%d)" % (out, W, H))
    return out


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------
def main():
    ap = argparse.ArgumentParser(description="Render the COMP603 figures")
    ap.add_argument("--only", choices=["fig1", "fig2", "fig3"], default=None,
                    help="render only one figure")
    ap.add_argument("--outdir", default=str(HERE))
    args = ap.parse_args()
    out_dir = Path(args.outdir)
    out_dir.mkdir(parents=True, exist_ok=True)
    done = []
    if args.only in (None, "fig1"):
        done.append(figure1(out_dir))
    if args.only in (None, "fig2"):
        done.append(figure2(out_dir))
    if args.only in (None, "fig3"):
        done.append(figure3(out_dir))
    print("done: %d figure(s)" % len(done))


if __name__ == "__main__":
    main()
