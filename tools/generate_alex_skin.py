#!/usr/bin/env python3
"""Builds the Alex companion skin.

She is Steve's counterpart, so she is built *from* his skin rather than from nothing: the face
stays exactly as it is (the same white eyes with blue pupils, which is what tells either of them
apart from Herobrine in the dark), and three things change.

  1. The hair: brown becomes auburn, longer down the sides of the head, with a braid down the back.
  2. The clothes: his cyan and blue become moss and brown, so the pair of them read as two people
     who dressed themselves rather than one person in two skins.
  3. The arms: four pixels wide become three, which is the whole of what "slim" means in a skin
     file. The renderer uses the slim player model, so this is not cosmetic - a four pixel arm on
     the slim model samples a column that belongs to the next face over.

Run it and it writes straight over the texture in place:

    python3 tools/generate_alex_skin.py
"""

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SOURCE = os.path.join(ROOT, "src/main/resources/assets/herobrine/textures/entity/steve_friend.png")
TARGET = os.path.join(ROOT, "src/main/resources/assets/herobrine/textures/entity/alex_friend.png")

# Brown -> auburn. Keys are every hair colour in the Steve skin; no skin, mouth or eye colour
# appears here, so this can be applied to the head without touching the face.
HAIR = {
    (0x24, 0x18, 0x08): (0x33, 0x1A, 0x0F),
    (0x26, 0x1A, 0x0A): (0x2C, 0x16, 0x0C),
    (0x2B, 0x1E, 0x0D): (0x45, 0x24, 0x14),
    (0x33, 0x24, 0x11): (0x57, 0x2F, 0x1B),
    (0x34, 0x25, 0x12): (0x33, 0x1A, 0x0F),
    (0x3F, 0x2A, 0x15): (0x66, 0x38, 0x21),
}

# His cyan shirt and sleeves -> moss.
SHIRT = {
    (0x00, 0xAF, 0xAF): (0x7F, 0x91, 0x47),
    (0x0A, 0xBC, 0xBC): (0x8F, 0xA2, 0x53),
    (0x00, 0xA4, 0xA4): (0x72, 0x83, 0x3F),
    (0x04, 0x95, 0x95): (0x64, 0x73, 0x35),
    (0x05, 0x88, 0x88): (0x55, 0x62, 0x2C),
    (0x00, 0x7F, 0x7F): (0x4D, 0x58, 0x27),
}

# His blue trousers -> brown, and his grey shoes -> dark leather.
TROUSERS = {
    (0x46, 0x3A, 0xA5): (0x5A, 0x44, 0x30),
    (0x41, 0x35, 0x9B): (0x50, 0x3C, 0x2A),
    (0x3A, 0x31, 0x89): (0x45, 0x33, 0x22),
    (0x4A, 0x4A, 0x4A): (0x3A, 0x2C, 0x1F),
    (0x3F, 0x3F, 0x3F): (0x33, 0x27, 0x1C),
    (0x55, 0x55, 0x55): (0x45, 0x34, 0x25),
}

HAIR_DARK = (0x33, 0x1A, 0x0F)
HAIR_MID = (0x45, 0x24, 0x14)
HAIR_TIE = (0x2A, 0x2A, 0x30)

# The six faces of an arm, as (source box, target box) for the wide -> slim conversion. Boxes are
# (x, y, width, height). The two side faces are four wide in both models - they are the depth of
# the arm, not its width - so they only move. The other four lose their last column.
ARM_FACES = [
    # Right arm.
    ((40, 20, 4, 12), (40, 20, 4, 12)),   # outer
    ((44, 20, 4, 12), (44, 20, 3, 12)),   # front
    ((48, 20, 4, 12), (47, 20, 4, 12)),   # inner
    ((52, 20, 4, 12), (50, 20, 3, 12)),   # back
    ((44, 16, 4, 4), (44, 16, 3, 4)),     # top
    ((48, 16, 4, 4), (47, 16, 3, 4)),     # bottom
    # Right sleeve.
    ((40, 36, 4, 12), (40, 36, 4, 12)),
    ((44, 36, 4, 12), (44, 36, 3, 12)),
    ((48, 36, 4, 12), (47, 36, 4, 12)),
    ((52, 36, 4, 12), (50, 36, 3, 12)),
    ((44, 32, 4, 4), (44, 32, 3, 4)),
    ((48, 32, 4, 4), (47, 32, 3, 4)),
    # Left arm.
    ((32, 52, 4, 12), (32, 52, 4, 12)),
    ((36, 52, 4, 12), (36, 52, 3, 12)),
    ((40, 52, 4, 12), (39, 52, 4, 12)),
    ((44, 52, 4, 12), (42, 52, 3, 12)),
    ((36, 48, 4, 4), (36, 48, 3, 4)),
    ((40, 48, 4, 4), (39, 48, 3, 4)),
    # Left sleeve.
    ((48, 52, 4, 12), (48, 52, 4, 12)),
    ((52, 52, 4, 12), (52, 52, 3, 12)),
    ((56, 52, 4, 12), (55, 52, 4, 12)),
    ((60, 52, 4, 12), (58, 52, 3, 12)),
    ((52, 48, 4, 4), (52, 48, 3, 4)),
    ((56, 48, 4, 4), (55, 48, 3, 4)),
]


def recolour(source):
    """Hair, shirt and trousers, in one pass. The maps share no keys with any skin colour."""
    table = {**HAIR, **SHIRT, **TROUSERS}
    image = source.copy()
    pixels = image.load()

    for y in range(64):
        for x in range(64):
            r, g, b, a = pixels[x, y]

            if a == 0:
                continue

            replacement = table.get((r, g, b))

            if replacement is not None:
                pixels[x, y] = replacement + (a,)

    return image


def grow_hair(image):
    """Longer down the sides, and a braid down the back of the head."""
    pixels = image.load()

    # Sides of the head: hair all the way down to the jaw rather than stopping at the temple.
    for base in (0, 16):
        for y in range(8, 14):
            for x in range(base, base + 8):
                if pixels[x, y][3] == 0:
                    continue

                pixels[x, y] = (HAIR_DARK if x in (base, base + 7) else HAIR_MID) + (255,)

    # Back of the head: a braid two pixels wide, with a tie where it leaves the hair.
    for y in range(11, 16):
        for x in (27, 28):
            pixels[x, y] = (HAIR_DARK if x == 27 else HAIR_MID) + (255,)

    pixels[27, 10] = HAIR_TIE + (255,)
    pixels[28, 10] = HAIR_TIE + (255,)

    return image


def slim_arms(image):
    """Four pixel arms become three. Everything outside the arm boxes is copied untouched."""
    source = image.load()
    result = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    target = result.load()

    for y in range(64):
        for x in range(64):
            target[x, y] = source[x, y]

    for (sx, sy, sw, sh), (tx, ty, tw, th) in ARM_FACES:
        # Blank the target box first: some of it may hold part of a face that has moved.
        for dy in range(th):
            for dx in range(tw):
                target[tx + dx, ty + dy] = (0, 0, 0, 0)

        for dy in range(min(sh, th)):
            for dx in range(min(sw, tw)):
                target[tx + dx, ty + dy] = source[sx + dx, sy + dy]

    return result


def main():
    steve = Image.open(SOURCE).convert("RGBA")

    if steve.size != (64, 64):
        raise SystemExit("expected a 64x64 skin, got %s" % (steve.size,))

    alex = slim_arms(grow_hair(recolour(steve)))
    alex.save(TARGET)

    painted = sum(1 for pixel in alex.getdata() if pixel[3] != 0)
    print("wrote %s" % TARGET)
    print("  %d of 4096 pixels painted" % painted)


if __name__ == "__main__":
    main()
