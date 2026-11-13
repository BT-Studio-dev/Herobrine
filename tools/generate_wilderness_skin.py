#!/usr/bin/env python3
"""Builds the Wilderness Bound skin for the Skin Library.

Minecraft 26.3 is the Wilderness Bound drop, so this variant is Herobrine
taken over by the wild: the tan becomes pale grey bark, the hair becomes dark
moss, the cyan shirt becomes moss green and the blue trousers become peat. A
scattering of moss speckles is added so he reads as something that has been
standing in a forest for a very long time.

It is built *from* the library's existing 'default' skin rather than from
nothing, for the same reason the Alex skin was built from Steve's: every pixel
of the layout, the shading and the proportions is already right, and only the
palette needs to change. That also means the new skin cannot drift out of step
with the shape the renderer expects.

The eyes follow the library's convention: the base texture leaves them
transparent and 'skin_emissive.png' fills those four pixels in, which is how
the glow is produced. Do not paint the eyes into the base skin.

Run it and it writes the two textures in place:

    python3 tools/generate_wilderness_skin.py

It also writes a preview next to it so the result can be looked at.
"""

import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKINS = os.path.join(
    ROOT, "src/main/resources/assets/lunareclipse.watching/textures/item/skins"
)
SOURCE = os.path.join(SKINS, "default/skin.png")
TARGET_DIR = os.path.join(SKINS, "wilderness_bound")
TARGET = os.path.join(TARGET_DIR, "skin.png")
EMISSIVE = os.path.join(TARGET_DIR, "skin_emissive.png")
PREVIEW = os.path.join(os.path.dirname(ROOT), "wilderness_bound_preview.png")

# Steve's palette -> the wild. Skin becomes pale grey-green bark.
SKIN = {
    (0xAA, 0x72, 0x59): (0x8C, 0x91, 0x79),
    (0x9B, 0x63, 0x49): (0x7E, 0x84, 0x70),
    (0x90, 0x59, 0x3F): (0x73, 0x7A, 0x66),
    (0x81, 0x53, 0x39): (0x66, 0x6C, 0x5A),
    (0x76, 0x4B, 0x33): (0x59, 0x5F, 0x4E),
    (0x8F, 0x5E, 0x3E): (0x72, 0x78, 0x68),
    (0x94, 0x60, 0x3E): (0x76, 0x7C, 0x6A),
    (0xB3, 0x79, 0x5E): (0x9A, 0xA0, 0x8A),
    (0xB7, 0x83, 0x6B): (0xA6, 0xAC, 0x98),
    # Mouth and chin shading; kept dark so the face still reads as a face.
    (0x6A, 0x40, 0x30): (0x46, 0x3F, 0x33),
    (0x77, 0x42, 0x35): (0x4E, 0x46, 0x37),
    (0x49, 0x25, 0x10): (0x3A, 0x35, 0x29),
    (0x42, 0x1D, 0x0A): (0x33, 0x2F, 0x24),
}

# Brown hair -> dark moss.
HAIR = {
    (0x2B, 0x1E, 0x0D): (0x1E, 0x2A, 0x18),
    (0x33, 0x24, 0x11): (0x26, 0x36, 0x1F),
    (0x24, 0x18, 0x08): (0x16, 0x21, 0x0F),
    (0x3F, 0x2A, 0x15): (0x2E, 0x40, 0x26),
    (0x26, 0x1A, 0x0A): (0x14, 0x20, 0x0E),
    (0x34, 0x25, 0x12): (0x25, 0x35, 0x1C),
}

# His cyan shirt -> moss green.
SHIRT = {
    (0x00, 0xAF, 0xAF): (0x6B, 0x8B, 0x42),
    (0x00, 0xA4, 0xA4): (0x60, 0x7E, 0x3A),
    (0x04, 0x95, 0x95): (0x55, 0x70, 0x32),
    (0x05, 0x88, 0x88): (0x4A, 0x62, 0x2B),
    (0x00, 0x7F, 0x7F): (0x3F, 0x54, 0x24),
    (0x03, 0x7A, 0x7A): (0x37, 0x4A, 0x1F),
    (0x00, 0x68, 0x68): (0x2C, 0x3D, 0x18),
    (0x0A, 0xBC, 0xBC): (0x7A, 0x9A, 0x4E),
    (0x00, 0xCC, 0xCC): (0x8A, 0xAB, 0x5B),
}

# His blue trousers -> peat, and his grey shoes -> dark leaf litter.
TROUSERS = {
    (0x41, 0x35, 0x9B): (0x45, 0x36, 0x23),
    (0x3A, 0x31, 0x89): (0x3B, 0x2E, 0x1E),
    (0x46, 0x3A, 0xA5): (0x4E, 0x3D, 0x2A),
    (0x4A, 0x4A, 0x4A): (0x2F, 0x2B, 0x23),
    (0x37, 0x37, 0x37): (0x26, 0x23, 0x19),
    (0x3F, 0x3F, 0x3F): (0x2A, 0x27, 0x20),
    (0x28, 0x28, 0x28): (0x1C, 0x1A, 0x15),
    (0x55, 0x55, 0x55): (0x35, 0x31, 0x29),
}

PALETTE = {}
for group in (SKIN, HAIR, SHIRT, TROUSERS):
    PALETTE.update(group)

# The four eye pixels. The base skin leaves them transparent and the emissive
# layer fills them in.
EYES = [(9, 12), (10, 12), (13, 12), (14, 12)]
EYE_GLOW = (0xEA, 0xFF, 0xE0, 0xFF)

# Moss speckles. Seeded so the file is byte for byte reproducible.
SPECKLES = [
    (0x7A, 0x9A, 0x4E),
    (0x6B, 0x8B, 0x42),
    (0x87, 0xA8, 0x57),
    (0x55, 0x6B, 0x33),
]
SPECKLE_REGIONS = [
    (20, 20, 28, 32),    # body front
    (32, 20, 40, 32),    # body back
    (16, 20, 20, 32),    # body right
    (28, 20, 32, 32),    # body left
    (20, 36, 28, 48),    # jacket front
    (44, 20, 48, 32),    # right arm
    (44, 36, 48, 48),    # right sleeve
    (36, 52, 40, 64),    # left arm
    (52, 52, 56, 64),    # left sleeve
    (4, 20, 8, 32),      # right leg
    (4, 36, 8, 48),      # right trouser leg
    (20, 52, 24, 64),    # left leg
    (4, 52, 8, 64),      # left trouser leg
    (8, 0, 16, 8),       # top of the head
    (0, 8, 8, 16),       # side of the head
    (16, 8, 24, 16),     # other side
    (24, 8, 32, 16),     # back of the head
    (40, 0, 48, 8),      # hat top
]


def recolour(source):
    """Map every opaque pixel through the palette, leaving anything unknown be."""
    image = source.copy()
    pixels = image.load()
    unknown = set()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue
            replacement = PALETTE.get((r, g, b))
            if replacement is None:
                unknown.add((r, g, b))
                continue
            pixels[x, y] = replacement + (a,)
    if unknown:
        names = ", ".join("#%02X%02X%02X" % c for c in sorted(unknown))
        raise SystemExit("Unmapped colours in the source skin: " + names)
    return image


def add_moss(image):
    """Scatter moss over the body, limbs and back of the head."""
    pixels = image.load()
    rng = random.Random(20261107)
    placed = 0
    for _ in range(260):
        x0, y0, x1, y1 = rng.choice(SPECKLE_REGIONS)
        x, y = rng.randrange(x0, x1), rng.randrange(y0, y1)
        if pixels[x, y][3] == 0:
            continue
        pixels[x, y] = rng.choice(SPECKLES) + (255,)
        placed += 1
    return placed


def build_preview(skin, emissive):
    """Lay the front faces out as a standing figure, hat layers composited on."""
    scale = 14

    def face(x0, y0, w, h, overlay=None):
        part = skin.crop((x0, y0, x0 + w, y0 + h)).convert("RGBA")
        if overlay is not None:
            part.alpha_composite(skin.crop(
                (overlay[0], overlay[1], overlay[0] + w, overlay[1] + h)
            ).convert("RGBA"))
        return part

    head = face(8, 8, 8, 8, (40, 8))
    body = face(20, 20, 8, 12, (20, 36))
    right_arm = face(44, 20, 4, 12, (44, 36))
    left_arm = face(36, 52, 4, 12, (52, 52))
    right_leg = face(4, 20, 4, 12, (4, 36))
    left_leg = face(20, 52, 4, 12, (4, 52))

    figure = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    figure.alpha_composite(head, (4, 0))
    figure.alpha_composite(body, (4, 8))
    figure.alpha_composite(right_arm, (0, 8))
    figure.alpha_composite(left_arm, (12, 8))
    figure.alpha_composite(right_leg, (4, 20))
    figure.alpha_composite(left_leg, (8, 20))

    # The glow sits on the eyes only; paint it over the assembled face.
    glow = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    for x, y in EYES:
        glow.putpixel((x - 8 + 4, y - 8), EYE_GLOW)
    figure.alpha_composite(glow)

    figure = figure.resize((16 * scale, 32 * scale), Image.NEAREST)

    flat = skin.resize((64 * scale // 2, 64 * scale // 2), Image.NEAREST)

    canvas = Image.new("RGBA", (flat.width + figure.width + 48, flat.height + 32),
                       (24, 26, 22, 255))
    canvas.alpha_composite(flat, (16, 16))
    canvas.alpha_composite(figure, (flat.width + 32, 16))
    return canvas


def main():
    source = Image.open(SOURCE).convert("RGBA")
    if source.size != (64, 64):
        raise SystemExit("Expected a 64x64 source skin, got %s" % (source.size,))

    skin = recolour(source)
    placed = add_moss(skin)
    for x, y in EYES:
        skin.putpixel((x, y), (0, 0, 0, 0))

    emissive = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x, y in EYES:
        emissive.putpixel((x, y), EYE_GLOW)

    os.makedirs(TARGET_DIR, exist_ok=True)
    skin.save(TARGET)
    emissive.save(EMISSIVE)

    build_preview(skin, emissive).save(PREVIEW)

    print("Wrote %s" % os.path.relpath(TARGET, ROOT))
    print("Wrote %s" % os.path.relpath(EMISSIVE, ROOT))
    print("Wrote %s" % os.path.relpath(PREVIEW, ROOT))
    print("Moss speckles placed: %d" % placed)


if __name__ == "__main__":
    main()
