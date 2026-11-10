"""
Regenerates assets/herobrine/sounds/music_haunt.ogg, the ten minute haunting track.

    pip install numpy scipy soundfile
    python3 tools/generate_haunting_track.py

Everything is synthesised - there is no sampled material in here, so the track is
the mod's own in the same way the rest of the sound set is.

The cooldown in HauntingEvents.MUSIC_COOLDOWN_TICKS must stay longer than DUR, or
a second playthrough starts on top of the first.

Two constraints shape the code more than the composition does:

  * A full-length float64 buffer is a couple of hundred megabytes, and several of
    them at once takes the process down. Rendering happens in blocks, and the dry
    mix lives in a memory-mapped file rather than in RAM.
  * The reverb is an IIR filter, so it cannot simply be applied block by block -
    its delay lines carry state from one block into the next. It is built from
    comb and allpass sections that scipy can run as IIRs, and each section's
    filter state is carried forward across every block, which is what makes the
    tail continuous instead of restarting every fifteen seconds.

Shape of the piece over its ten minutes:

    0:00  the drone surfaces, wind only
    1:20  the first bell, a long way off
    3:30  rumbles underneath, the melody starts to speak
    5:30  the low end arrives and the melody opens up
    8:00  it thins back to the drone and the wind
"""
import os
import tempfile

import numpy as np
import soundfile as sf
from scipy.signal import lfilter

SR = 44100
DUR = 600.0
N = int(SR * DUR)
BLOCK = 15
BN = int(BLOCK * SR)
FADE_IN = 4.0
FADE_OUT = 9.0

A1 = 55.00          # the root the whole piece is built on
E2 = 82.41          # the fifth
A2, C4, D4, E4, F4, G4 = 110.0, 261.63, 293.66, 329.63, 349.23, 392.00


# ---------------------------------------------------------------- utilities

def slow(tt, period, phase=0.0, floor=0.5):
    """A slow breath between floor and 1."""
    return floor + (1.0 - floor) * 0.5 * (1.0 + np.sin(2 * np.pi * tt / period + phase))


def arc(tt, attack, release, bulge=0.0):
    """Shape over GLOBAL time, so it stays correct when called per block."""
    e = np.ones(len(tt), dtype=np.float64)
    if attack:
        m = tt < attack
        e[m] = (tt[m] / attack) ** 2
    if release:
        rem = DUR - tt
        m = rem < release
        e[m] *= (np.clip(rem[m], 0.0, None) / release) ** 2
    if bulge:
        e *= 1.0 - bulge + bulge * np.sin(np.pi * np.clip(tt / DUR, 0.0, 1.0)) ** 0.7
    return e


def biquad_band(lo_hz, hi_hz):
    """RBJ bandpass coefficients, for wind that has a shape rather than a box."""
    f0 = np.sqrt(lo_hz * hi_hz)
    q = f0 / max(1.0, hi_hz - lo_hz)
    w0 = 2 * np.pi * f0 / SR
    alpha = np.sin(w0) / (2 * q)
    b = np.array([alpha, 0.0, -alpha])
    a = np.array([1 + alpha, -2 * np.cos(w0), 1 - alpha])
    return b / a[0], a / a[0]


def noise(n, seed):
    return np.random.default_rng(seed).standard_normal(n)


# ------------------------------------------------------------------- reverb

class Reverb:
    """
    Comb and allpass sections run as IIRs, with the filter state carried from one
    block to the next so the tail is continuous across the joins.

    Each comb is y[n] = x[n] + fb * y[n - D], which lfilter can run at C speed.
    The damping is a one-pole lowpass on the summed combs rather than inside each
    loop: slightly different from a textbook Freeverb, but it darkens the tail
    the same way and it stays vectorised.
    """

    COMB = (1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    ALLPASS = (225, 556, 441, 341)

    def __init__(self, room=0.94, damp_hz=2200.0, sr=SR):
        s = sr / 44100.0
        self.delays = [max(1, int(d * s)) for d in self.COMB]
        self.fb = room
        self.zi = [np.zeros(d, dtype=np.float64) for d in self.delays]
        self.ap_delays = [max(1, int(d * s)) for d in self.ALLPASS]
        self.ap_zi = [np.zeros(d, dtype=np.float64) for d in self.ap_delays]
        self.ap_g = 0.5
        # one-pole lowpass on the wet path
        self.damp_a = np.exp(-2 * np.pi * damp_hz / sr)
        self.lp_state = 0.0
        self.out_zi = np.zeros(max(self.delays), dtype=np.float64)

    def process(self, x):
        x = np.asarray(x, dtype=np.float64)
        wet = np.zeros(len(x), dtype=np.float64)

        for d, zi in zip(self.delays, self.zi):
            a = np.zeros(d + 1, dtype=np.float64)
            a[0] = 1.0
            a[d] = -self.fb
            y, zf = lfilter(np.array([1.0]), a, x, zi=zi)
            zi[:] = zf
            wet += y

        wet /= len(self.delays)

        # one-pole damping, state carried across blocks
        wet = lfilter(np.array([1.0 - self.damp_a]), np.array([1.0, -self.damp_a]),
                      wet, zi=np.array([self.lp_state]))[0]
        self.lp_state = wet[-1] if len(wet) else self.lp_state

        for d, zi in zip(self.ap_delays, self.ap_zi):
            g = self.ap_g
            b = np.zeros(d + 1, dtype=np.float64)
            b[0] = -g
            b[d] = 1.0
            a = np.zeros(d + 1, dtype=np.float64)
            a[0] = 1.0
            a[d] = -g
            wet, zf = lfilter(b, a, wet, zi=zi)
            zi[:] = zf

        return wet


# ------------------------------------------------------------------- dry mix

def render_dry(path):
    """Render every element into a memory-mapped buffer, block by block."""
    dry = np.memmap(path, dtype=np.float32, mode="w+", shape=(N,))

    def add(idx, sig, level):
        ln = min(len(sig), N - idx)
        if ln > 0:
            dry[idx:idx + ln] += (sig[:ln] * level).astype(np.float32)

    # Harmonic series, not bare sines: this is what turns a thin tone into a pad.
    # (harmonic, amplitude, detune in cents, amplitude-breath period)
    DRONE = [
        (1, 1.00, 47.0), (2, 0.46, 61.0), (3, 0.28, 53.0), (4, 0.20, 71.0),
        (5, 0.13, 43.0), (6, 0.10, 67.0), (8, 0.06, 37.0), (10, 0.04, 83.0),
    ]
    FIFTH = [(1, 0.42, 59.0), (2, 0.24, 73.0), (3, 0.14, 41.0), (4, 0.09, 89.0)]

    def voices(root, table):
        out = []
        for harmonic, amp, period in table:
            for cents in (-7.0, 7.0):
                out.append((root * harmonic * 2 ** (cents / 1200.0),
                            amp * 0.5, period, cents))
        return out

    DRONE_VOICES = voices(A1, DRONE) + [(f * 0.75, a * 0.75, p, c)
                                        for f, a, p, c in voices(E2, FIFTH)]

    print("drone and wind", flush=True)
    b_lo, a_lo = biquad_band(70.0, 380.0)
    b_hi, a_hi = biquad_band(700.0, 3200.0)
    z_lo = np.zeros(max(len(a_lo), len(b_lo)) - 1)
    z_hi = np.zeros(max(len(a_hi), len(b_hi)) - 1)

    for b0 in range(0, N, BN):
        n = min(BN, N - b0)
        tt = (b0 + np.arange(n, dtype=np.float64)) / SR
        body = arc(tt, 30.0, 60.0, bulge=0.34) * slow(tt, 111.0, floor=0.66)
        block = np.zeros(n, dtype=np.float64)

        # a slow drift over the whole stack, so it never sits perfectly still
        drift = 1.0 + 0.0009 * np.sin(2 * np.pi * tt / 137.0)

        for freq, amp, period, cents in DRONE_VOICES:
            block += (amp * np.sin(2 * np.pi * freq * drift * tt)
                      * slow(tt, period, phase=cents, floor=0.38))
        block *= body
        add(b0, block, 0.30)

        # wind: two bands of shaped noise, each breathing on its own clock
        lo, z_lo = lfilter(b_lo, a_lo, noise(n, 7 + b0), zi=z_lo)
        hi, z_hi = lfilter(b_hi, a_hi, noise(n, 11000 + b0), zi=z_hi)
        e_lo = arc(tt, 22.0, 45.0, bulge=0.6) * slow(tt, 79.0, phase=1.1, floor=0.16)
        e_hi = arc(tt, 34.0, 50.0, bulge=0.7) * slow(tt, 57.0, phase=2.3, floor=0.03)
        add(b0, lo * e_lo * 0.22 + hi * e_hi * 0.05, 1.0)

        if (b0 // BN) % 10 == 0:
            print("  %.0fs" % (b0 / SR), flush=True)

    # Bells: fewer than before and further apart. Twenty-eight strikes across
    # ten minutes read as a clock; fourteen read as something happening.
    BELL_RATIOS = [0.5, 1.0, 1.183, 1.506, 2.0, 2.514, 2.662, 3.011, 4.166, 5.433]
    BELL_AMPS = [0.42, 1.00, 0.58, 0.40, 0.33, 0.22, 0.18, 0.13, 0.07, 0.04]
    BELL_TIMES = [
        (46.0, A2, 0.26, 8.0), (104.0, E4, 0.22, 7.0), (168.0, A2, 0.28, 8.5),
        (222.0, C4, 0.20, 6.5), (279.0, A2, 0.30, 8.5), (334.0, G4, 0.22, 6.5),
        (381.0, E4, 0.24, 7.0), (430.0, A2, 0.32, 9.0), (477.0, D4, 0.22, 6.5),
        (516.0, A2, 0.30, 8.5), (548.0, C4, 0.20, 6.0), (572.0, E4, 0.18, 5.5),
    ]

    print("bells", flush=True)
    for start, f0, level, decay in BELL_TIMES:
        i0 = int(start * SR)
        if i0 >= N:
            continue
        ln = min(int((decay + 1.0) * SR), N - i0)
        tt = np.arange(ln, dtype=np.float64) / SR
        sig = np.zeros(ln, dtype=np.float64)
        for ratio, amp in zip(BELL_RATIOS, BELL_AMPS):
            tau = decay / (1.0 + 0.5 * (ratio - 0.5))
            sig += amp * np.sin(2 * np.pi * f0 * ratio * tt) * np.exp(-tt / tau)
        atk = int(0.004 * SR)
        sig[:atk] *= np.linspace(0.0, 1.0, atk)
        sig /= np.max(np.abs(sig)) + 1e-9
        add(i0, sig, level)
        # two echoes, quieter and later than the strike
        for delay, mul in ((0.83, 0.30), (1.61, 0.15)):
            add(i0 + int(delay * SR), sig * mul, level)

    print("rumble and swells", flush=True)
    b_sub, a_sub = biquad_band(24.0, 140.0)
    for start, length, level in ((300, 10, 0.55), (396, 12, 0.7), (470, 14, 0.8), (524, 9, 0.6)):
        i0 = int(start * SR)
        ln = min(int(length * SR), N - i0)
        shape = np.sin(np.pi * np.linspace(0, 1, ln)) ** 1.7 * level
        z = np.zeros(max(len(a_sub), len(b_sub)) - 1)
        rum, _ = lfilter(b_sub, a_sub, noise(ln, 23 + start), zi=z)
        add(i0, rum * shape, 0.5)

    for f0, start, length, level in ((34.0, 326, 28, 0.34), (30.0, 414, 32, 0.38),
                                     (38.0, 474, 26, 0.32), (28.0, 538, 30, 0.28)):
        i0 = int(start * SR)
        ln = min(int(length * SR), N - i0)
        tt = np.arange(ln, dtype=np.float64) / SR
        swell = np.sin(np.pi * np.clip(tt / length, 0, 1)) ** 2
        wobble = 1.0 + 0.05 * np.sin(2 * np.pi * 0.09 * tt)
        add(i0, np.sin(2 * np.pi * f0 * wobble * tt) * swell, level)

    # Melody: half as many notes as before, and each one given room to breathe.
    MELODY = [
        (196, 6.0, A2 * 2, 0.115), (214, 5.0, C4, 0.10), (232, 7.0, E4, 0.105),
        (252, 5.0, D4, 0.095), (270, 8.0, C4, 0.10), (292, 6.0, A2 * 2, 0.095),
        (312, 7.0, E4, 0.09), (334, 5.0, F4, 0.085), (352, 8.0, E4, 0.09),
        (374, 6.0, D4, 0.085), (394, 7.0, C4, 0.08), (416, 9.0, A2 * 2, 0.085),
        (440, 6.0, E4, 0.08), (462, 8.0, F4, 0.075), (486, 7.0, E4, 0.075),
        (510, 9.0, D4, 0.07), (536, 10.0, C4, 0.065), (562, 12.0, A2 * 2, 0.06),
    ]

    print("melody", flush=True)
    for start, length, f0, level in MELODY:
        i0 = int(start * SR)
        ln = min(int(length * SR), N - i0)
        if ln <= 0:
            continue
        tt = np.arange(ln, dtype=np.float64) / SR
        note = np.zeros(ln, dtype=np.float64)
        # a harmonic timbre rather than a bare sine, so it reads as an instrument
        for mult, amp in ((1.0, 1.0), (2.0, 0.30), (3.0, 0.16), (4.0, 0.08), (5.0, 0.04)):
            vib = 1.0 + 0.0032 * np.sin(2 * np.pi * (4.1 + 0.4 * mult) * tt)
            note += amp * np.sin(2 * np.pi * f0 * mult * vib * tt)
        atk = int(0.5 * SR)
        note[:atk] *= np.linspace(0.0, 1.0, atk)
        note *= np.exp(-tt / (length * 0.40))
        note /= np.max(np.abs(note)) + 1e-9
        add(i0, note, level)

    dry.flush()
    return dry


# -------------------------------------------------------------------- master

def main():
    tmp = os.path.join(tempfile.gettempdir(), "herobrine_dry.f32")
    print("target: %.1f million samples" % (N / 1e6), flush=True)
    dry = render_dry(tmp)

    print("reverb and master", flush=True)
    verb = Reverb(room=0.845, damp_hz=2600.0)
    dst = "src/main/resources/assets/herobrine/sounds/music_haunt.ogg"

    wet_level, dry_level = 0.55, 0.72

    with sf.SoundFile(dst, "w", samplerate=SR, channels=1,
                      format="OGG", subtype="VORBIS") as f:
        for b0 in range(0, N, BN):
            n = min(BN, N - b0)
            seg = dry[b0:b0 + n].astype(np.float64)
            wet = verb.process(seg)
            mixed = seg * dry_level + wet * wet_level
            # gentle saturation: rounds the peaks without audible distortion
            mixed = np.tanh(mixed * 1.02)
            f.write(mixed.astype(np.float32))
            if (b0 // BN) % 10 == 0:
                print("  wrote %.0fs" % (b0 / SR), flush=True)

    # Normalise, then fade the ends so the ten minutes can loop or be restarted
    # without a click. This needs the whole file, so it is a second pass.
    print("normalising", flush=True)
    with sf.SoundFile(dst, "r") as f:
        buf = f.read(dtype="float32")

    buf = buf - np.mean(buf)
    peak = float(np.max(np.abs(buf)))
    buf = buf / (peak + 1e-9) * 0.89
    buf = np.tanh(buf * 1.04).astype(np.float32)

    fi, fo = int(FADE_IN * SR), int(FADE_OUT * SR)
    buf[:fi] *= (np.linspace(0.0, 1.0, fi) ** 1.5).astype(np.float32)
    buf[-fo:] *= (np.linspace(1.0, 0.0, fo) ** 1.5).astype(np.float32)

    with sf.SoundFile(dst, "w", samplerate=SR, channels=1,
                      format="OGG", subtype="VORBIS") as f:
        for b0 in range(0, len(buf), BN):
            f.write(buf[b0:b0 + BN])

    print("final peak %.3f  rms %.4f  %.1fs"
          % (np.max(np.abs(buf)), np.sqrt(np.mean(buf ** 2)), len(buf) / SR), flush=True)

    dry._mmap.close()
    os.unlink(tmp)
    print("wrote", dst, flush=True)


if __name__ == "__main__":
    main()
