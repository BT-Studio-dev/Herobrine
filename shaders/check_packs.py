#!/usr/bin/env python3
"""Check the supplied shader ZIPs without extracting or modifying them.

Run: python3 shaders/check_packs.py
SHA-1 values identify the exact files listed on Modrinth; they are not a
substitute for testing rendering or hardware compatibility.
"""
import hashlib
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parent
ORIGINALS = {
    'Cursed Fog - V1.0.10.zip': '1a46478d3b318a2e5887ae0187c3e72789c94729',
    "Sildur's Vibrant Shaders v2.02 Extreme.zip": '2362f4dd27178879c30374c8f33965999bc350c8',
    'optimism.zip': 'af84a3ba0b549b5958f1e270f11b5c6823166436',
    '§lE-LITE shaders 5.1.3.zip': '69b00c8662df174034e918f4ca52c35dd2e66e9d',
}


def check_zip(path):
    with zipfile.ZipFile(path) as archive:
        corrupt = archive.testzip()
        if corrupt:
            raise ValueError(f'{path.name}: corrupt entry {corrupt}')
        names = archive.namelist()
        if not any(n.startswith('shaders/') and n.endswith(('.fsh', '.vsh')) for n in names):
            raise ValueError(f'{path.name}: no shader programs under root shaders/ directory')
        return len(names)


def main():
    for name, expected in ORIGINALS.items():
        path = ROOT / name
        actual = hashlib.sha1(path.read_bytes()).hexdigest()
        if actual != expected:
            raise ValueError(f'{name}: expected SHA-1 {expected}, got {actual}')
        print(f'OK: {name} — {check_zip(path)} entries; matches recorded hash')

    pack = ROOT / 'optimism-26.3-test'
    archive = ROOT / 'optimism-26.3-test.zip'
    count = check_zip(archive)
    files = {p.relative_to(pack).as_posix(): p for p in pack.rglob('*') if p.is_file()}
    with zipfile.ZipFile(archive) as z:
        if set(z.namelist()) != set(files):
            raise ValueError('Optimism test ZIP contents differ from its source directory')
        for name, source in files.items():
            if z.read(name) != source.read_bytes():
                raise ValueError(f'Optimism test ZIP does not match source: {name}')
    print(f'OK: {archive.name} — {count} entries; matches source')
    print('Archive and source checks only; no in-game or GPU test was performed.')


if __name__ == '__main__':
    main()
