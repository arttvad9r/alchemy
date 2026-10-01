#!/usr/bin/env python3
"""Synthesize the procedural sound effects into assets/audio/procedural.

The bells are tuned to D minor pentatonic, the key of the background track, so a chime never clashes
with the music under it. Every effect is mono, so the game can pan it to where it happened on the
workspace. The output is deterministic: the same seed gives byte-identical sounds.

Usage: python3 tools/build_procedural_audio.py  (requires numpy, scipy and soundfile, only for development)
Then run tools/build_audio.py to copy the files into res/raw.
"""

from pathlib import Path

import numpy as np
import soundfile as sf
from scipy import signal

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "assets/audio/procedural"
SR = 48000
RNG = np.random.default_rng(1618)

# D minor pentatonic and its friends, in Hz.
D5, F5, G5, A5, C6, D6, E6, F6, A6, D7, F7, G7, A7, C8 = (
    587.33, 698.46, 783.99, 880.00, 1046.50, 1174.66, 1318.51, 1396.91, 1760.00, 2349.32, 2793.83, 3135.96, 3520.00, 4186.01,
)


def silence(seconds: float) -> np.ndarray:
    return np.zeros(int(seconds * SR))


def place(track: np.ndarray, sound: np.ndarray, at: float, gain: float = 1.0) -> None:
    start = int(at * SR)
    end = min(len(track), start + len(sound))
    track[start:end] += sound[: end - start] * gain


def bell(freq: float, decay: float, brightness: float = 1.0) -> np.ndarray:
    """A glassy celesta-like bell: slightly inharmonic partials, the upper ones dying first, doubled a few cents apart.

    It rings until it is 50 dB down and then fades, so it never ends in a click."""
    seconds = decay * 6
    t = np.arange(int(seconds * SR)) / SR
    partials = [(1.0, 1.0), (2.0, 0.42), (3.01, 0.16 * brightness), (4.17, 0.1 * brightness), (5.43, 0.05 * brightness)]
    out = np.zeros_like(t)
    for detune in (-0.0017, 0.0017):
        for ratio, amp in partials:
            f = freq * ratio * (1 + detune)
            if f > SR / 2.2:
                continue
            out += amp * np.sin(2 * np.pi * f * t + RNG.uniform(0, 2 * np.pi)) * np.exp(-t * ratio / decay)
    attack = np.minimum(1.0, t / 0.003)
    release = np.clip((seconds - t) / (seconds * 0.25), 0, 1) ** 2
    return out * attack * release / 2


def noise(seconds: float) -> np.ndarray:
    return RNG.standard_normal(int(seconds * SR))


def swept_band(source: np.ndarray, start_hz: float, end_hz: float, q: float = 1.2) -> np.ndarray:
    """Band-passes [source] with a centre gliding exponentially from start to end, block by block."""
    block = 256
    out = np.zeros_like(source)
    zi = None
    blocks = int(np.ceil(len(source) / block))
    for i in range(blocks):
        centre = start_hz * (end_hz / start_hz) ** (i / max(1, blocks - 1))
        low, high = centre / (1 + 1 / (2 * q)), centre * (1 + 1 / (2 * q))
        sos = signal.butter(2, [low, min(high, SR / 2.1)], btype="bandpass", fs=SR, output="sos")
        if zi is None:
            zi = signal.sosfilt_zi(sos) * 0
        chunk = source[i * block:(i + 1) * block]
        out[i * block:i * block + len(chunk)], zi = signal.sosfilt(sos, chunk, zi=zi)
    return out


def reverb(dry: np.ndarray, tail: float, wet: float, darkness_hz: float = 6000) -> np.ndarray:
    """A soft hall: convolution with decaying, darkened noise after a short pre-delay."""
    t = np.arange(int(tail * SR)) / SR
    ir = RNG.standard_normal(len(t)) * np.exp(-t * 6.9 / tail)
    ir = signal.sosfilt(signal.butter(2, darkness_hz, fs=SR, output="sos"), ir)
    ir = np.concatenate([np.zeros(int(0.018 * SR)), ir])
    ir /= np.sqrt(np.sum(ir ** 2))
    wet_signal = signal.fftconvolve(dry, ir)[: len(dry) + len(ir)]
    out = np.zeros(len(wet_signal))
    out[: len(dry)] += dry
    return out * (1 - wet) + wet_signal * wet


def finish(sound: np.ndarray, peak: float, fade: float = 0.05) -> np.ndarray:
    """Trims trailing silence, fades the end, removes DC and sets the peak level."""
    sound = sound - np.mean(sound)
    audible = np.nonzero(np.abs(sound) > np.max(np.abs(sound)) * 0.004)[0]
    sound = sound[: audible[-1] + 1] if len(audible) else sound
    n = min(len(sound), int(fade * SR))
    sound[-n:] *= np.linspace(1, 0, n) ** 2
    return sound * peak / np.max(np.abs(sound))


def pickup() -> np.ndarray:
    """Taking an element in hand: a soft rising 'fwip', heard often, so short and quiet."""
    seconds = 0.12
    t = np.arange(int(seconds * SR)) / SR
    glide = 520 * (1.6 ** np.minimum(1.0, t / 0.06))
    phase = 2 * np.pi * np.cumsum(glide) / SR
    body = (np.sin(phase) + 0.25 * np.sin(2 * phase)) * np.exp(-t / 0.035) * np.minimum(1.0, t / 0.004)
    air = swept_band(noise(seconds), 1800, 5200, q=1.5) * np.exp(-t / 0.03) * 0.6
    return finish(reverb(body + air, 0.35, 0.12), peak=0.22, fade=0.02)


def whoosh() -> np.ndarray:
    """Sweeping the workspace clean: an airy gust that rises and falls away."""
    seconds = 0.5
    t = np.arange(int(seconds * SR)) / SR
    gust = swept_band(noise(seconds), 380, 2600, q=0.9)
    envelope = np.where(t < 0.16, (t / 0.16) ** 2, np.exp(-(t - 0.16) / 0.09))
    shimmer = np.zeros_like(t)
    for f in (A6, D7):
        place(shimmer, bell(f, 0.25, 0.4), 0.1, 0.08)
    return finish(reverb(gust * envelope + shimmer, 0.6, 0.2), peak=0.45)


def hint() -> np.ndarray:
    """A hint revealed: a quick upward twinkle."""
    track = silence(3.0)
    for i, f in enumerate((D6, F6, A6, D7)):
        place(track, bell(f, 0.3, 0.7), i * 0.05, 1.0 - 0.12 * i)
    return finish(reverb(track, 0.9, 0.3), peak=0.42)


def achievement() -> np.ndarray:
    """An achievement earned: a bright open-fifth arpeggio over a shimmer, in the music's key."""
    track = silence(4.0)
    for i, f in enumerate((D5, A5, D6, A6)):
        place(track, bell(f, 0.55 - 0.05 * i), i * 0.085, 1.0 - 0.1 * i)
    place(track, bell(D6, 0.6, 0.6), 0.34, 0.45)
    place(track, bell(E6, 0.55, 0.5), 0.34, 0.27)
    t = np.arange(len(track)) / SR
    shimmer = swept_band(noise(len(track) / SR), 3000, 8000, q=2) * np.exp(-np.maximum(0, t - 0.3) / 0.3) * np.minimum(1, t / 0.3) * 0.12
    for _ in range(9):
        place(track, bell(RNG.choice((D7, F7, G7, A7, C8)), 0.12, 0.3), RNG.uniform(0.3, 1.1), RNG.uniform(0.08, 0.18))
    return finish(reverb(track + shimmer, 1.4, 0.32), peak=0.62)


def discover_grand() -> np.ndarray:
    """An epic or legendary element found: a deep swell, a strummed chord and a rain of sparkles."""
    track = silence(5.0)
    t = np.arange(len(track)) / SR
    # The swell: a sub drop under a rising band of air.
    drop = 2 * np.pi * np.cumsum(73.42 * (0.8 ** np.minimum(1, t / 0.5))) / SR
    boom = np.sin(drop) * np.exp(-t / 0.45) * np.minimum(1.0, t / 0.01)
    rise = swept_band(noise(len(track) / SR), 700, 7000, q=1.1) * np.minimum(1, t / 0.35) * np.exp(-np.maximum(0, t - 0.35) / 0.18)
    track += boom * 0.55 + rise * 0.22
    for i, f in enumerate((D5, A5, D6, E6, A6)):
        place(track, bell(f, 0.8 - 0.08 * i, 0.8), 0.14 + i * 0.045, 0.9 - 0.08 * i)
    for i in range(18):
        at = 0.25 + 1.3 * (i / 18) ** 1.4 + RNG.uniform(-0.03, 0.03)
        place(track, bell(RNG.choice((D7, F7, G7, A7, C8)), 0.14, 0.3), at, 0.2 * (1 - i / 22))
    return finish(reverb(track, 1.9, 0.36), peak=0.7, fade=0.15)


EFFECTS = {
    "pickup": pickup,
    "whoosh": whoosh,
    "hint": hint,
    "achievement": achievement,
    "discover_grand": discover_grand,
}


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name, make in EFFECTS.items():
        sound = make().astype(np.float32)
        sf.write(OUT / f"{name}.ogg", sound, SR, format="OGG", subtype="VORBIS")
        print(f"{name}: {len(sound) / SR:.2f} s, rms {np.sqrt(np.mean(sound ** 2)):.3f}")


if __name__ == "__main__":
    main()
