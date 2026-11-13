#!/usr/bin/env python3
"""Validate and package the experimental Optimism shader pack.

Usage: GLSLANG=/path/to/glslang python3 shaders/build_optimism_test.py
Requires glslang (or glslangValidator) on PATH if GLSLANG is not set.
"""
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent
PACK = ROOT / 'optimism-26.3-test'
SHADERS = PACK / 'shaders'
ARCHIVE = ROOT / 'optimism-26.3-test.zip'
INCLUDE = re.compile(r'^\s*#include\s+"([^"]+)"\s*$', re.MULTILINE)


def expand(path, stack=()):
    path = path.resolve()
    if not path.is_relative_to(SHADERS.resolve()) or path in stack:
        raise ValueError(f'Unsafe or recursive shader include: {path}')
    text = path.read_text(encoding='utf-8')
    return INCLUDE.sub(lambda m: expand(path.parent / m.group(1), stack + (path,)), text)


def main():
    validator = os.environ.get('GLSLANG') or shutil.which('glslangValidator') or shutil.which('glslang')
    if not validator:
        raise SystemExit('Install glslang or set GLSLANG to its executable')
    programs = sorted({p.stem for p in SHADERS.glob('*.vsh')} |
                      {p.stem for p in SHADERS.glob('*.fsh')})
    if not programs:
        raise SystemExit('No shader programs found')
    with tempfile.TemporaryDirectory() as temp:
        for program in programs:
            paths = []
            for suffix, stage in (('.vsh', 'vert'), ('.fsh', 'frag')):
                src = SHADERS / (program + suffix)
                if not src.is_file():
                    raise SystemExit(f'Missing {src.name}')
                expanded = expand(src)
                if not expanded.lstrip().startswith('#version '):
                    raise SystemExit(f'Missing GLSL version in {src}')
                target = Path(temp) / f'{program}.{stage}'
                target.write_text(expanded, encoding='utf-8')
                paths.append(str(target))
            subprocess.run([validator, '-l', *paths], check=True)
    files = sorted(p for p in PACK.rglob('*') if p.is_file())
    with zipfile.ZipFile(ARCHIVE, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for file in files:
            info = zipfile.ZipInfo(file.relative_to(PACK).as_posix(), (2024, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            z.writestr(info, file.read_bytes())
    with zipfile.ZipFile(ARCHIVE) as z:
        assert z.testzip() is None
        assert set(z.namelist()) == {p.relative_to(PACK).as_posix() for p in files}
        for file in files:
            assert z.read(file.relative_to(PACK).as_posix()) == file.read_bytes()
    print(f'Validated {len(programs)} linked programs and built {ARCHIVE}')


if __name__ == '__main__':
    main()
