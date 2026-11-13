#!/usr/bin/env python3
"""
Alex's theme: two and a half minutes for the one who is not afraid of him.

Herobrine's track is ten minutes of drone, wind and distance, and it plays when he is
close. This is the other side of the same world: warm rather than cold, in a major key,
led by a struck bell that is allowed to ring out, over a pad that moves slowly enough to
feel like breathing. It is wistful rather than frightening - the progression keeps
touching the sixth, which is where the sadness sits in something otherwise warm.

It is written from nothing, like the rest of the mod's audio. It does not use, quote or
reconstruct anybody else's melody.

The shape is forty bars at sixty-four beats a minute, which is exactly a hundred and
fifty seconds. Both ends are silent, so it can loop without a seam.
"""

import numpy as np
from scipy.signal import lfilter
import soundfile as sf

SR = 44100
DURATION = 150.0
BPM = 64.0
BEAT = 60.0 / BPM          # 0.9375 s
BAR = BEAT * 4             # 3.75 s
BARS = 40
BLOCK = BEAT * 8           # one chord lasts two bars

OUT = "src/main/resources/assets/herobrine/sounds/music_alex.ogg"


def midi_to_hz(m):
    return 440.0 * (2.0 ** ((m - 69) / 12.0))


class Reverb:
    """
    Comb and allpass sections run as IIRs, with the filter state carried from one block
    to the next so the tail is continuous across the joins. The same network the haunting
    track uses, tuned shorter and darker: a smaller room, because this is somewhere safe.
    """

    COMB = (1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
    ALLPASS = (225, 556, 441, 341)

    def __init__(self, room=0.88, damp_hz=2800.0, sr=SR):
        self.delays = [max(1, int(d * sr / 44100.0)) for d in self.COMB]
        self.fb = room
        self.zi = [np.zeros(d) for d in self.delays]
        self.ap_delays = [max(1, int(d * sr / 44100.0)) for d in self.ALLPASS]
        self.ap_zi = [np.zeros(d) for d in self.ap_delays]
        self.ap_g = 0.5
        self.damp_a = np.exp(-2 * np.pi * damp_hz / sr)
        self.lp_state = 0.0

    def process(self, x):
        x = np.asarray(x, dtype=np.float64)
        wet = np.zeros(len(x))

        for d, zi in zip(self.delays, self.zi):
            a = np.zeros(d + 1)
            a[0] = 1.0
            a[d] = -self.fb
            y, zf = lfilter(np.array([1.0]), a, x, zi=zi)
            zi[:] = zf
            wet += y

        wet /= len(self.delays)

        wet, zf = lfilter(np.array([1.0 - self.damp_a]), np.array([1.0, -self.damp_a]),
                          wet, zi=np.array([self.lp_state]))
        self.lp_state = zf[0]

        for d, zi in zip(self.ap_delays, self.ap_zi):
            g = self.ap_g
            b = np.zeros(d + 1)
            b[0] = -g
            b[d] = 1.0
            a = np.zeros(d + 1)
            a[0] = 1.0
            a[d] = -g
            wet, zf = lfilter(b, a, wet, zi=zi)
            zi[:] = zf

        return wet


# ---- The piece ------------------------------------------------------------
#
# Twenty chord blocks of eight beats. F major, and the sixth is what makes it ache:
# F, Dm, Bb, C is the whole vocabulary, and the tune keeps landing on it.

TRIADS = {
    "F":  (53, 57, 60),
    "Dm": (50, 53, 57),
    "Bb": (46, 50, 53),
    "C":  (48, 52, 55),
}
SUBS = {"F": 41, "Dm": 38, "Bb": 34, "C": 36}

PROGRESSION = [
    "F", "F", "Dm", "Dm", "Bb", "Bb", "C", "C",
    "F", "Dm", "Bb", "C",
    "F", "Dm", "Bb", "C",
    "F", "Dm", "Bb", "F",
]

# The tune. (beat, midi, beats, velocity) - sparse on purpose: what is left out is
# most of why it sounds the way it does.
MELODY = [
    # Alone with the bells.
    (0, 69, 6, 0.55), (8, 72, 6, 0.50), (16, 69, 4, 0.50),
    (22, 65, 6, 0.45), (32, 72, 4, 0.60), (38, 69, 4, 0.55),
    (44, 65, 6, 0.50), (52, 67, 4, 0.50), (58, 69, 5, 0.50),
    # The tune proper.
    (64, 72, 3, 0.65), (68, 69, 3, 0.60), (72, 65, 5, 0.60),
    (79, 67, 2, 0.55), (82, 69, 5, 0.60), (89, 72, 3, 0.60),
    (93, 74, 5, 0.62), (100, 72, 5, 0.60), (107, 69, 4, 0.55),
    (113, 67, 3, 0.55), (117, 65, 6, 0.60),
    # Thinning out again.
    (128, 72, 4, 0.50), (134, 69, 4, 0.48), (140, 65, 5, 0.45),
    (146, 60, 6, 0.38),
]

# A struck bell: near-harmonic partials with a touch of stretch, the high ones dying
# first, which is what separates it from a plain sine.
BELL_PARTIALS = (1.0, 2.01, 3.02, 4.03, 5.05, 6.07)
BELL_AMPS = (1.0, 0.55, 0.32, 0.18, 0.10, 0.05)
BELL_DECAY = (4.0, 3.4, 2.8, 2.2, 1.7, 1.3)


def envelope(n, attack, decay):
    """Fast strike, exponential fall, and a short fade at the very end of the tail."""
    t = np.arange(n) / SR
    env = np.exp(-t / decay)
    a = max(1, int(attack * SR))
    env[:a] *= np.linspace(0.0, 1.0, a)
    return env


def add(buf, start, samples):
    i = int(start * SR)
    j = min(len(buf), i + len(samples))
    if i >= len(buf):
        return
    buf[i:j] += samples[: j - i]


def bell(freq, beats, velocity):
    n = int(beats * BEAT * SR) + int(4.0 * SR)
    out = np.zeros(n)
    t = np.arange(n) / SR

    for partial, amp, decay in zip(BELL_PARTIALS, BELL_AMPS, BELL_DECAY):
        # A few cents of detune between the two sides keep it from sounding like an organ.
        for cents in (-4.0, 4.0):
            f = freq * partial * (2.0 ** (cents / 1200.0))
            out += (amp / 2.0) * np.sin(2 * np.pi * f * t + partial) * envelope(n, 0.004, decay)

    # A soft thump at the attack: the felt, not just the string.
    out += 0.06 * np.sin(2 * np.pi * freq * 0.5 * t) * envelope(n, 0.006, 0.12)

    return out * velocity * 0.34 / max(np.abs(out).max(), 1e-9)


def pad_voice(freq, seconds, velocity):
    """Two sines a couple of cents apart with a slow swell, so it never sits still."""
    n = int(seconds * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)

    for cents in (-3.0, 3.0):
        f = freq * (2.0 ** (cents / 1200.0))
        out += 0.5 * np.sin(2 * np.pi * f * t)
        out += 0.10 * np.sin(2 * np.pi * f * 2.0 * t + 1.1)
        out += 0.03 * np.sin(2 * np.pi * f * 3.0 * t + 2.3)

    swell = np.ones(n)
    a = min(n, int(1.1 * SR))
    r = min(n, int(1.4 * SR))
    swell[:a] = np.linspace(0.0, 1.0, a)
    swell[-r:] *= np.linspace(1.0, 0.0, r)

    return out * swell * velocity * 0.16


def sub_voice(freq, seconds, velocity):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    out = np.sin(2 * np.pi * freq * t) + 0.18 * np.sin(2 * np.pi * freq * 2 * t)
    swell = np.ones(n)
    a = min(n, int(0.8 * SR))
    r = min(n, int(1.0 * SR))
    swell[:a] = np.linspace(0.0, 1.0, a)
    swell[-r:] *= np.linspace(1.0, 0.0, r)
    return out * swell * velocity * 0.10


def pad_gain(block_index):
    """Bells alone at the start, the warmth under it from the fifth block, gone at the end."""
    if block_index < 4:
        return 0.0
    if block_index < 6:
        return (block_index - 4) / 2.0
    if block_index >= 18:
        return max(0.0, (20 - block_index) / 2.0)
    return 1.0


def render_dry():
    n = int(DURATION * SR)
    bells = np.zeros(n)
    pad = np.zeros(n)
    sub = np.zeros(n)

    for beat, midi, beats, velocity in MELODY:
        add(bells, beat * BEAT, bell(midi_to_hz(midi), beats, velocity))

    for index, chord in enumerate(PROGRESSION):
        gain = pad_gain(index)

        if gain <= 0.0:
            continue

        start = index * BLOCK
        seconds = BLOCK + 1.6

        for midi in TRIADS[chord]:
            add(pad, start, pad_voice(midi_to_hz(midi), seconds, gain))

        add(sub, start, sub_voice(midi_to_hz(SUBS[chord]), seconds, gain))

    # The bell leads. The pad and the bass are there to hold it up, not to cover it: an
    # earlier balance measured 68% of the energy below 500 Hz and nothing at all above
    # 2 kHz, which is mud rather than warmth.
    return bells * 1.00 + pad * 0.34 + sub * 0.26


BLOCK_SAMPLES = SR * 5

# Silent at both ends, so the loop has no seam.
FADE_IN = 1.5
FADE_OUT = 6.0


def main():
    print("rendering the dry mix...", flush=True)
    dry = render_dry()

    # Written a block at a time rather than handed to sf.write in one piece: the whole
    # file is 6.6 million samples, and the encoder is happier in chunks.
    print("reverb and master", flush=True)
    verb = Reverb()

    with sf.SoundFile(OUT, "w", samplerate=SR, channels=1,
                      format="OGG", subtype="VORBIS") as f:
        for start in range(0, len(dry), BLOCK_SAMPLES):
            segment = dry[start:start + BLOCK_SAMPLES].astype(np.float64)
            wet = verb.process(segment)
            mixed = segment * 0.80 + wet * 0.30
            # Gentle saturation: rounds the peaks without audible distortion.
            mixed = np.tanh(mixed * 1.04)
            f.write(mixed.astype(np.float32))

    del dry

    # Normalising needs the whole file, so it is a second pass over what was just written.
    print("normalising", flush=True)
    with sf.SoundFile(OUT, "r") as f:
        buf = f.read(dtype="float32")

    buf = buf - np.mean(buf)
    buf = buf / (np.max(np.abs(buf)) + 1e-9) * 0.89
    buf = np.tanh(buf * 1.04).astype(np.float32)

    fi, fo = int(FADE_IN * SR), int(FADE_OUT * SR)
    buf[:fi] *= (np.linspace(0.0, 1.0, fi) ** 1.5).astype(np.float32)
    buf[-fo:] *= (np.linspace(1.0, 0.0, fo) ** 1.5).astype(np.float32)

    with sf.SoundFile(OUT, "w", samplerate=SR, channels=1,
                      format="OGG", subtype="VORBIS") as f:
        for start in range(0, len(buf), BLOCK_SAMPLES):
            f.write(buf[start:start + BLOCK_SAMPLES])

    print("wrote %s" % OUT)
    print("  duration %.2f s" % (len(buf) / SR))
    print("  peak     %.4f" % np.max(np.abs(buf)))
    print("  rms      %.4f" % np.sqrt(np.mean(buf ** 2)))
    print("  clipped  %d samples" % int((np.abs(buf) > 0.999).sum()))
    print("  head rms %.8f" % np.sqrt(np.mean(buf[:SR] ** 2)))
    print("  tail rms %.8f" % np.sqrt(np.mean(buf[-SR:] ** 2)))


if __name__ == "__main__":
    main()
