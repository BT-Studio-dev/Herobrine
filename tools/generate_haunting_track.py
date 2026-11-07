"""
Regenerates assets/herobrine/sounds/music_haunt.ogg, the ten minute haunting
track. Run it from the repository root:

    pip install numpy soundfile
    python3 tools/generate_haunting_track.py

Everything is synthesised - there is no sampled material in here, so the track
is the mod's own in the same way the rest of the sound set is. Adjust the
tables below (BELL_TIMES, MELODY, PARTIALS) and re-run to reshape the piece.

Note the cooldown in HauntingEvents.MUSIC_COOLDOWN_TICKS must stay longer than
DUR, or a second play starts on top of the first.

Generates the haunting track: ten minutes of drone, wind, distant bells and
something moving under the floor, to replace the old fifty second cue.

Everything is synthesised with numpy - no sampled material, so the track is the
mod's own in the same way the rest of the sound set is.

Work is done in blocks rather than over the whole ten minutes at once: a
full-length float64 buffer is a couple of hundred megabytes, and several of them
at once is enough to take the process down.
"""
import numpy as np
import soundfile as sf

SR = 44100
DUR = 600.0
N = int(SR * DUR)
BLOCK = 15
BN = int(BLOCK * SR)
PAD = 4096

out = np.zeros(N, dtype=np.float32)
print("target: %.1f million samples, %d blocks" % (N / 1e6, N // BN + 1), flush=True)


def box(x, w):
    c = np.empty(len(x) + 1, dtype=np.float64)
    c[0] = 0.0
    np.cumsum(x, out=c[1:])
    return (c[w:] - c[:-w]) / w


def band(lo_hz, hi_hz, n, seed):
    r = np.random.default_rng(seed)
    x = r.standard_normal(n + 2 * PAD)
    w_hi = max(2, int(SR / hi_hz))
    w_lo = max(w_hi + 2, int(SR / lo_hz))
    narrow = box(x, w_hi)
    wide = box(x, w_lo)
    k = min(len(narrow), len(wide))
    y = narrow[:k] - wide[len(wide) - k:]
    y = y[:n].astype(np.float32)
    return y / (np.max(np.abs(y)) + 1e-9)


def slow_env(tt, period_s, phase=0.0, floor=0.55):
    return floor + (1.0 - floor) * 0.5 * (1.0 + np.sin(2 * np.pi * tt / period_s + phase))


def arc(tt, attack_s, release_s, bulge=0.0):
    """Shape over GLOBAL time, so it is correct when called per block."""
    e = np.ones(len(tt), dtype=np.float64)
    if attack_s:
        m = tt < attack_s
        e[m] = (tt[m] / attack_s) ** 2
    if release_s:
        rem = DUR - tt
        m = rem < release_s
        e[m] *= (np.clip(rem[m], 0, None) / release_s) ** 2
    if bulge:
        e *= 1.0 - bulge + bulge * np.sin(np.pi * np.clip(tt / DUR, 0, 1)) ** 0.7
    return e


def add(idx, sig, level):
    ln = min(len(sig), N - idx)
    if ln > 0:
        out[idx:idx + ln] += (sig[:ln] * level).astype(np.float32)


PARTIALS = [
    (55.000, 0.50, 47.0), (55.130, 0.34, 61.0), (82.410, 0.36, 53.0),
    (82.560, 0.22, 71.0), (110.00, 0.20, 43.0), (110.24, 0.13, 67.0),
    (130.81, 0.13, 37.0), (164.81, 0.11, 59.0), (220.00, 0.05, 83.0),
]


def render_drone(n, tt):
    env = arc(tt, 26.0, 50.0, bulge=0.30) * slow_env(tt, 96.0, floor=0.72)
    block = np.zeros(n, dtype=np.float64)
    for freq, amp, lfo in PARTIALS:
        block += amp * np.sin(2 * np.pi * freq * tt) * env * slow_env(tt, lfo, phase=freq, floor=0.45)
    return block


def render_wind(b0, n, tt):
    lo = band(90.0, 420.0, n, seed=7 + b0)
    hi = band(500.0, 2600.0, n, seed=11000 + b0)
    e_lo = arc(tt, 18.0, 40.0, bulge=0.55) * slow_env(tt, 74.0, phase=1.1, floor=0.20)
    e_hi = arc(tt, 30.0, 45.0, bulge=0.65) * slow_env(tt, 53.0, phase=2.3, floor=0.05)
    return lo * e_lo * 0.34 + hi * e_hi * 0.075


print("rendering drone and wind", flush=True)
for b0 in range(0, N, BN):
    n = min(BN, N - b0)
    tt = (b0 + np.arange(n, dtype=np.float64)) / SR
    add(b0, render_drone(n, tt), 1.0)
    add(b0, render_wind(b0, n, tt), 1.0)
    if (b0 // BN) % 10 == 0:
        print("  %.0fs" % (b0 / SR), flush=True)
print("  peak %.3f" % np.max(np.abs(out)), flush=True)

BELL_RATIOS = [0.5, 1.0, 1.183, 1.506, 2.0, 2.514, 2.662, 3.011, 4.166]
BELL_AMPS = [0.55, 1.00, 0.60, 0.42, 0.36, 0.24, 0.20, 0.14, 0.07]


def strike(start_s, f0, level, decay_s):
    i0 = int(start_s * SR)
    if i0 >= N:
        return
    ln = min(int((decay_s + 0.4) * SR), N - i0)
    tt = np.arange(ln, dtype=np.float64) / SR
    sig = np.zeros(ln, dtype=np.float64)
    for ratio, amp in zip(BELL_RATIOS, BELL_AMPS):
        tau = decay_s / (1.0 + 0.55 * (ratio - 0.5))
        sig += amp * np.sin(2 * np.pi * f0 * ratio * tt) * np.exp(-tt / tau)
    atk = int(0.004 * SR)
    sig[:atk] *= np.linspace(0.0, 1.0, atk)
    sig /= np.max(np.abs(sig)) + 1e-9
    add(i0, sig, level)
    for delay, mul in ((0.83, 0.34), (1.61, 0.17)):
        add(i0 + int(delay * SR), sig * mul, level)


A3, C4, D4, E4, F4, G4 = 220.0, 261.63, 293.66, 329.63, 349.23, 392.00
BELL_TIMES = [
    (18.0, A3, 0.30, 7.0), (46.0, E4, 0.26, 6.5), (77.0, A3, 0.30, 7.0),
    (104.0, C4, 0.24, 6.0), (133.0, G4, 0.22, 5.5), (162.0, A3, 0.30, 7.0),
    (188.0, D4, 0.24, 6.0), (214.0, E4, 0.26, 6.0), (241.0, A3, 0.32, 7.5),
    (266.0, F4, 0.22, 5.5), (291.0, C4, 0.25, 6.0), (314.0, G4, 0.24, 5.5),
    (338.0, A3, 0.34, 7.5), (360.0, E4, 0.28, 6.5), (381.0, D4, 0.25, 6.0),
    (400.0, A3, 0.36, 8.0), (418.0, C4, 0.26, 6.0), (434.0, G4, 0.25, 5.5),
    (450.0, A3, 0.38, 8.0), (464.0, F4, 0.24, 5.5), (478.0, E4, 0.28, 6.5),
    (491.0, A3, 0.36, 7.5), (503.0, D4, 0.24, 5.5), (515.0, G4, 0.24, 5.5),
    (529.0, A3, 0.32, 7.0), (546.0, C4, 0.22, 5.5), (566.0, A3, 0.26, 6.5),
    (588.0, E4, 0.18, 5.0),
]
print("bells", flush=True)
for start, f0, level, decay in BELL_TIMES:
    strike(start, f0, level, decay)
print("  peak %.3f" % np.max(np.abs(out)), flush=True)

print("rumble and swells", flush=True)
for start, length, level in ((300, 9, 0.5), (352, 7, 0.6), (398, 11, 0.7),
                             (444, 8, 0.75), (487, 12, 0.8), (528, 9, 0.6)):
    i0 = int(start * SR)
    ln = min(int(length * SR), N - i0)
    shape = np.sin(np.pi * np.linspace(0, 1, ln)) ** 1.6 * level
    add(i0, band(28.0, 150.0, ln, seed=23 + start) * shape, 0.55)

for f0, start, length, level in ((34.0, 322, 26, 0.30), (30.0, 410, 30, 0.34),
                                 (38.0, 470, 24, 0.30), (28.0, 534, 28, 0.26)):
    i0 = int(start * SR)
    ln = min(int(length * SR), N - i0)
    tt = np.arange(ln, dtype=np.float64) / SR
    swell = np.sin(np.pi * np.clip(tt / length, 0, 1)) ** 2
    wobble = 1.0 + 0.06 * np.sin(2 * np.pi * 0.11 * tt)
    add(i0, np.sin(2 * np.pi * f0 * wobble * tt) * swell, level)
print("  peak %.3f" % np.max(np.abs(out)), flush=True)

MELODY = [
    (208, 3.5, A3, 0.14), (216, 3.0, C4, 0.12), (224, 4.0, E4, 0.13),
    (233, 3.0, D4, 0.12), (240, 4.5, C4, 0.13), (250, 3.0, A3, 0.12),
    (258, 4.0, G4 / 2, 0.11), (267, 3.5, A3, 0.13), (276, 3.0, E4, 0.12),
    (284, 4.0, F4, 0.12), (293, 3.0, E4, 0.11), (301, 4.5, D4, 0.12),
    (311, 3.0, C4, 0.11), (320, 4.0, A3, 0.12), (330, 3.5, E4, 0.11),
    (340, 3.0, D4, 0.10), (349, 4.0, C4, 0.10), (359, 5.0, A3, 0.11),
    (371, 3.0, G4 / 2, 0.10), (380, 3.5, A3, 0.10), (390, 4.0, C4, 0.09),
    (401, 3.0, D4, 0.09), (411, 4.5, E4, 0.09), (423, 3.5, D4, 0.08),
    (434, 4.0, C4, 0.08), (446, 5.0, A3, 0.09), (459, 3.5, E4, 0.08),
    (470, 4.0, A3, 0.08), (483, 3.0, C4, 0.07), (494, 4.5, A3, 0.07),
    (508, 4.0, G4 / 2, 0.07), (522, 5.0, A3, 0.07), (540, 4.0, C4, 0.06),
    (556, 5.0, A3, 0.06), (574, 6.0, A3, 0.05),
]
print("melody", flush=True)
for start, length, f0, level in MELODY:
    i0 = int(start * SR)
    ln = min(int(length * SR), N - i0)
    if ln <= 0:
        continue
    tt = np.arange(ln, dtype=np.float64) / SR
    note = np.zeros(ln, dtype=np.float64)
    for mult, amp in ((1.0, 1.0), (1.5, 0.22), (2.0, 0.12), (3.0, 0.05)):
        vib = 1.0 + 0.0035 * np.sin(2 * np.pi * (4.3 + mult) * tt)
        note += amp * np.sin(2 * np.pi * f0 * mult * vib * tt)
    atk = int(0.35 * SR)
    note[:atk] *= np.linspace(0.0, 1.0, atk)
    note *= np.exp(-tt / (length * 0.42))
    note /= np.max(np.abs(note)) + 1e-9
    add(i0, note, level)
print("  peak %.3f" % np.max(np.abs(out)), flush=True)

print("mastering", flush=True)
buf = out.astype(np.float32)
for b0 in range(0, N, BN):
    n = min(BN, N - b0)
    seg = buf[b0:b0 + n].astype(np.float64)
    smoothed = box(seg, 3)
    seg -= 0.10 * (seg - smoothed[:n] if len(smoothed) >= n else np.pad(smoothed, (0, n - len(smoothed))))
    buf[b0:b0 + n] = seg.astype(np.float32)

peak = float(np.max(np.abs(buf)))
print("  pre-normalise peak %.3f" % peak, flush=True)
buf = buf / (peak + 1e-9) * 0.92
buf = (np.tanh(buf * 1.06) * 0.94).astype(np.float32)
buf -= np.mean(buf)

fi, fo = int(4.0 * SR), int(9.0 * SR)
buf[:fi] *= (np.linspace(0.0, 1.0, fi) ** 1.5).astype(np.float32)
buf[-fo:] *= (np.linspace(1.0, 0.0, fo) ** 1.5).astype(np.float32)
print("final peak %.3f  rms %.4f" % (np.max(np.abs(buf)), np.sqrt(np.mean(buf ** 2))), flush=True)

dst = "src/main/resources/assets/herobrine/sounds/music_haunt.ogg"
# Feed the encoder a block at a time: handing it all 26 million samples in one
# call takes the process down.
with sf.SoundFile(dst, "w", samplerate=SR, channels=1,
                  format="OGG", subtype="VORBIS") as f:
    for b0 in range(0, N, BN):
        f.write(buf[b0:b0 + BN])
        if (b0 // BN) % 10 == 0:
            print("  wrote %.0fs" % (b0 / SR), flush=True)
print("wrote", dst, flush=True)
