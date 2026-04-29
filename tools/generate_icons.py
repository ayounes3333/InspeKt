#!/usr/bin/env python3
"""
generate_icons.py — Produces the three desktop icon files required by the
Compose Desktop nativeDistributions block in composeApp/build.gradle.kts.

Output files (relative to repo root):
  composeApp/src/desktopMain/resources/icon.png   (1024×1024 RGBA master)
  composeApp/src/desktopMain/resources/icon.ico   (Windows multi-resolution ICO)
  composeApp/src/desktopMain/resources/icon.icns  (Apple ICNS)

System dependencies:
  - Python 3.8+
  - Pillow >= 9.0  (pip install Pillow)

Usage:
  python3 tools/generate_icons.py
"""

import math
import struct
import io
import os
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    raise SystemExit("Pillow is required: pip install Pillow")

# ---------------------------------------------------------------------------
# Paths
# ---------------------------------------------------------------------------
REPO_ROOT = Path(__file__).parent.parent
RESOURCES = REPO_ROOT / "composeApp" / "src" / "desktopMain" / "resources"
RESOURCES.mkdir(parents=True, exist_ok=True)

PNG_OUT = RESOURCES / "icon.png"
ICO_OUT = RESOURCES / "icon.ico"
ICNS_OUT = RESOURCES / "icon.icns"

MASTER_SIZE = 1024


# ---------------------------------------------------------------------------
# Drawing helpers
# ---------------------------------------------------------------------------

def lerp_color(c1, c2, t):
    """Linearly interpolate between two RGBA colours."""
    return tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))


def draw_rounded_rect(draw, bbox, radius, color):
    """Fill a rounded rectangle."""
    x0, y0, x1, y1 = bbox
    draw.rectangle([x0 + radius, y0, x1 - radius, y1], fill=color)
    draw.rectangle([x0, y0 + radius, x1, y1 - radius], fill=color)
    draw.ellipse([x0, y0, x0 + 2 * radius, y0 + 2 * radius], fill=color)
    draw.ellipse([x1 - 2 * radius, y0, x1, y0 + 2 * radius], fill=color)
    draw.ellipse([x0, y1 - 2 * radius, x0 + 2 * radius, y1], fill=color)
    draw.ellipse([x1 - 2 * radius, y1 - 2 * radius, x1, y1], fill=color)


def make_gradient_background(size, color_top, color_bottom, radius_frac=0.18):
    """Create a rounded-square gradient background image (RGBA)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pixels = img.load()
    radius = int(size * radius_frac)

    for y in range(size):
        t = y / (size - 1)
        row_color = lerp_color(color_top, color_bottom, t)
        for x in range(size):
            # Determine if inside rounded rect
            cx = max(radius, min(size - radius - 1, x))
            cy = max(radius, min(size - radius - 1, y))
            # Corner check
            dx = abs(x - cx)
            dy = abs(y - cy)
            if x < radius and y < radius:
                dx, dy = radius - x, radius - y
                if math.hypot(dx, dy) > radius:
                    continue
            elif x > size - radius - 1 and y < radius:
                dx, dy = x - (size - radius - 1), radius - y
                if math.hypot(dx, dy) > radius:
                    continue
            elif x < radius and y > size - radius - 1:
                dx, dy = radius - x, y - (size - radius - 1)
                if math.hypot(dx, dy) > radius:
                    continue
            elif x > size - radius - 1 and y > size - radius - 1:
                dx, dy = x - (size - radius - 1), y - (size - radius - 1)
                if math.hypot(dx, dy) > radius:
                    continue
            pixels[x, y] = tuple(row_color)

    return img


def draw_magnifier(draw, size, color, stroke_frac=0.06):
    """Draw a magnifying-glass glyph on a Draw context."""
    stroke = max(2, int(size * stroke_frac))
    cx, cy = size / 2, size * 0.44
    # lens circle
    lens_r = size * 0.28
    bb = (cx - lens_r, cy - lens_r, cx + lens_r, cy + lens_r)
    draw.ellipse(bb, outline=color, width=stroke)

    # handle
    angle = math.radians(135)
    hx0 = cx + lens_r * math.cos(angle)
    hy0 = cy + lens_r * math.sin(angle)
    handle_len = size * 0.22
    hx1 = hx0 + handle_len * math.cos(angle)
    hy1 = hy0 + handle_len * math.sin(angle)
    draw.line([(hx0, hy0), (hx1, hy1)], fill=color, width=stroke)

    # curly-brace / API motif inside the lens: small "{ }" text
    # We draw it manually as two arcs to avoid font dependency
    inner_r = lens_r * 0.42
    brace_stroke = max(1, stroke // 2)
    brace_color = color

    # Left brace arc segments
    lx = cx - inner_r * 0.55
    # top arc
    draw.arc(
        [lx - inner_r * 0.28, cy - inner_r * 0.9,
         lx + inner_r * 0.28, cy - inner_r * 0.1],
        start=200, end=360, fill=brace_color, width=brace_stroke,
    )
    # bottom arc
    draw.arc(
        [lx - inner_r * 0.28, cy + inner_r * 0.1,
         lx + inner_r * 0.28, cy + inner_r * 0.9],
        start=0, end=160, fill=brace_color, width=brace_stroke,
    )

    # Right brace arc segments (mirrored)
    rx = cx + inner_r * 0.55
    draw.arc(
        [rx - inner_r * 0.28, cy - inner_r * 0.9,
         rx + inner_r * 0.28, cy - inner_r * 0.1],
        start=180, end=340, fill=brace_color, width=brace_stroke,
    )
    draw.arc(
        [rx - inner_r * 0.28, cy + inner_r * 0.1,
         rx + inner_r * 0.28, cy + inner_r * 0.9],
        start=20, end=180, fill=brace_color, width=brace_stroke,
    )

    # Centre dot
    dot_r = brace_stroke
    draw.ellipse(
        [cx - dot_r, cy - dot_r, cx + dot_r, cy + dot_r],
        fill=brace_color,
    )


def render_master(size=MASTER_SIZE):
    """Render the master icon at `size` × `size`."""
    # Gradient: indigo → deep purple
    color_top = (79, 70, 229, 255)     # indigo-600
    color_bottom = (109, 40, 217, 255)  # violet-700
    white = (255, 255, 255, 255)

    img = make_gradient_background(size, color_top, color_bottom)
    draw = ImageDraw.Draw(img)
    draw_magnifier(draw, size, white)
    return img


# ---------------------------------------------------------------------------
# ICO writer
# ---------------------------------------------------------------------------
ICO_SIZES = [16, 32, 48, 64, 128, 256]


def build_ico(master: Image.Image) -> bytes:
    """Build a valid multi-resolution Windows ICO from the master image."""
    frames = []
    for sz in ICO_SIZES:
        frame = master.resize((sz, sz), Image.LANCZOS).convert("RGBA")
        buf = io.BytesIO()
        frame.save(buf, format="PNG")
        frames.append(buf.getvalue())

    n = len(frames)
    # ICONDIR header: 6 bytes
    # ICONDIRENTRY per image: 16 bytes
    data_offset = 6 + 16 * n
    header = struct.pack("<HHH", 0, 1, n)  # reserved=0, type=1 (ICO), count

    entries = b""
    image_data = b""
    for i, raw in enumerate(frames):
        sz = ICO_SIZES[i]
        w = sz if sz < 256 else 0   # 256 stored as 0 per spec
        h = sz if sz < 256 else 0
        size_bytes = len(raw)
        offset = data_offset + len(image_data)
        entries += struct.pack("<BBBBHHII", w, h, 0, 0, 1, 32, size_bytes, offset)
        image_data += raw

    return header + entries + image_data


# ---------------------------------------------------------------------------
# ICNS writer
# ---------------------------------------------------------------------------
# ICNS OSType codes and corresponding pixel sizes (1x only; Pillow's ICNS
# writer handles @2x internally).  We target the minimal required set.
ICNS_TYPES = [
    (b"is32", 16),
    (b"il32", 32),
    (b"ih32", 48),
    (b"iT32", 128),
    (b"ic08", 256),
    (b"ic09", 512),
    (b"ic10", 1024),
]


def build_icns(master: Image.Image) -> bytes:
    """
    Build a minimal Apple ICNS file using PNG-compressed chunks.

    Apple accepts PNG-compressed icon data for all modern OSTypes (ic04 and
    above), so we embed PNG data directly inside the ICNS container.
    """
    chunks = b""
    for ostype, sz in ICNS_TYPES:
        frame = master.resize((sz, sz), Image.LANCZOS).convert("RGBA")
        buf = io.BytesIO()
        frame.save(buf, format="PNG")
        png_bytes = buf.getvalue()
        # Each chunk: 4-byte OSType + 4-byte length (including the 8-byte header)
        chunk_len = 8 + len(png_bytes)
        chunks += ostype + struct.pack(">I", chunk_len) + png_bytes

    # ICNS file header: magic + total file length
    total_len = 8 + len(chunks)
    return b"icns" + struct.pack(">I", total_len) + chunks


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    print(f"Rendering master icon at {MASTER_SIZE}×{MASTER_SIZE}…")
    master = render_master(MASTER_SIZE)

    # PNG (use 512×512 for the runtime resource; jpackage uses it for Linux)
    png_size = 1024
    png_img = master.resize((png_size, png_size), Image.LANCZOS)
    png_img.save(PNG_OUT, format="PNG")
    print(f"  Wrote {PNG_OUT}  ({PNG_OUT.stat().st_size} bytes)")

    # ICO
    ico_bytes = build_ico(master)
    ICO_OUT.write_bytes(ico_bytes)
    print(f"  Wrote {ICO_OUT}  ({ICO_OUT.stat().st_size} bytes)")

    # ICNS
    icns_bytes = build_icns(master)
    ICNS_OUT.write_bytes(icns_bytes)
    print(f"  Wrote {ICNS_OUT}  ({ICNS_OUT.stat().st_size} bytes)")

    print("Done.")


if __name__ == "__main__":
    main()
