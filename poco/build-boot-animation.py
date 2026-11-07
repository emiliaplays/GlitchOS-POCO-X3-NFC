"""Render the POCO boot animation and a smaller animated preview."""

import io
import math
import os
import random
import subprocess
import wave
from array import array
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile

from PIL import Image, ImageDraw, ImageFont


WIDTH, HEIGHT, FPS = 1080, 2340, 30
FRAMES = 80
LOOP_FRAMES = 30
TAIL_FRAMES = 450
MINT = (139, 255, 224)
NAVY = (7, 19, 29)
OUT = Path(__file__).resolve().parent / "output" / "boot" / "glitch-v5"
CROP = (64, 895, 1016, 1455)
FONT_DIR = Path(os.environ["WINDIR"]) / "Fonts"
TITLE = ImageFont.truetype(str(FONT_DIR / "segoeuib.ttf"), 102)
DETAIL = ImageFont.truetype(str(FONT_DIR / "consola.ttf"), 27)


def blend(start, end, amount):
    return tuple(round(a + (b - a) * amount) for a, b in zip(start, end))


def spaced_text(draw, label, y, font, spacing, color):
    widths = [draw.textlength(letter, font=font) for letter in label]
    x = (WIDTH - sum(widths) - spacing * (len(label) - 1)) / 2
    for letter, width in zip(label, widths):
        draw.text((round(x), y), letter, font=font, fill=color, anchor="lt")
        x += width + spacing


def background():
    image = Image.new("RGB", (WIDTH, HEIGHT), NAVY)
    left, top, right, bottom = CROP
    width, height = right - left, bottom - top
    glow = Image.new("RGB", (96, 56))
    glow.putdata([
        blend(NAVY, (12, 35, 44), math.sin(math.pi * x / 95)**2 * math.sin(math.pi * y / 55)**2)
        for y in range(56) for x in range(96)
    ])
    image.paste(glow.resize((width, height), Image.Resampling.BILINEAR), (left, top))
    return image


def render(base, index):
    image = base.copy()
    draw = ImageDraw.Draw(image)
    progress = index / (FRAMES - 1)
    reveal = min(1, index / 29)
    fade = min(1, max(0, (index - 24) / 21))
    center = 1040
    # Draw the G as a continuous circuit trace.
    segments = [
        ((610, center - 81), (463, center - 81)),
        ((463, center - 81), (437, center - 55)),
        ((437, center - 55), (437, center + 55)),
        ((437, center + 55), (463, center + 81)),
        ((463, center + 81), (614, center + 81)),
        ((614, center + 81), (641, center + 54)),
        ((641, center + 54), (641, center + 5)),
        ((641, center + 5), (555, center + 5)),
    ]
    for step, (start, end) in enumerate(segments):
        draw.line((*start, *end), fill=(27, 66, 71), width=16)
        length = min(1, max(0, reveal * len(segments) - step))
        if length:
            point = tuple(round(a + (b - a) * length) for a, b in zip(start, end))
            draw.line((*start, *point), fill=MINT, width=16)
            draw.ellipse((point[0] - 8, point[1] - 8, point[0] + 8, point[1] + 8), fill=MINT)
    if fade:
        spaced_text(draw, "GLITCH UI", 1192, TITLE, 12, blend((12, 34, 43), MINT, fade))
        spaced_text(draw, "P O C O   E D I T I O N", 1324, DETAIL, 3, blend((11, 28, 36), (152, 194, 193), fade))
    draw.line((438, 1420, 642, 1420), fill=(27, 66, 71), width=4)
    draw.line((438, 1420, 438 + round(progress * 204), 1420), fill=MINT, width=4)
    if 8 <= index <= 19 or 29 <= index <= 39 or 48 <= index <= 51:
        glitch_slices(image, index)
    return image


def glitch_slices(image, index):
    rng = random.Random(index)
    strength = 1.0 if index < 40 else 0.42
    for _ in range(6):
        y = rng.randrange(958, 1295)
        height = rng.randrange(3, 13)
        shift = round(rng.choice((-1, 1)) * rng.randrange(13, 58) * strength)
        strip = image.crop((100, y, WIDTH - 100, y + height))
        image.paste(strip, (100 + shift, y))
    draw = ImageDraw.Draw(image)
    for _ in range(13):
        x, y = rng.randrange(156, 890), rng.randrange(925, 1330)
        color = rng.choice(((70, 164, 158), (85, 118, 172), (56, 95, 106)))
        draw.rectangle((x, y, x + rng.randrange(10, 66), y + rng.randrange(1, 4)), fill=color)


def make_sound(path):
    """Synthesize gated data bursts, a bass hit, and a resolving chime."""
    rate = 48000
    rng = random.Random(7)
    pcm = array("h")
    for i in range(round(rate * 2.2)):
        t = i / rate
        value = 0.0
        for onset, duration, start, end in (
            (0.23, 0.08, 820, 250), (0.34, 0.06, 1100, 500),
            (0.46, 0.13, 430, 1520), (0.99, 0.08, 1500, 380),
        ):
            burst = t - onset
            if 0 < burst < duration:
                envelope = math.sin(math.pi * burst / duration) ** 2
                phase = 2 * math.pi * (start * burst + (end - start) * burst**2 / (2 * duration))
                value += envelope * (0.075 * math.sin(phase) + 0.018 * rng.uniform(-1, 1))
        hit = t - 1.03
        if 0 < hit < 0.35:
            envelope = min(1, hit / 0.016) * math.exp(-12 * hit)
            value += 0.22 * envelope * math.sin(2 * math.pi * (74 * hit + 2 * (1 - math.exp(-8 * hit))))
        for onset, frequency, gain in ((1.09, 440, 0.12), (1.20, 659.25, 0.10), (1.39, 880, 0.07)):
            note = t - onset
            if 0 < note < 0.66:
                envelope = min(1, note / 0.025) * math.exp(-5.5 * note)
                envelope *= min(1, (0.66 - note) / 0.10)
                phase = 2 * math.pi * frequency * note
                value += gain * envelope * (math.sin(phase) + 0.15 * math.sin(2 * phase))
        pcm.append(round(value * 32767))
    with wave.open(str(path), "wb") as sound:
        sound.setparams((1, 2, rate, 0, "NONE", "not compressed"))
        sound.writeframes(pcm.tobytes())


def png(frame):
    buffer = io.BytesIO()
    frame.save(buffer, format="PNG")
    return buffer.getvalue()


def render_loop(final, index):
    frame = final.copy()
    if 8 <= index <= 12 or 23 <= index <= 26:
        glitch_slices(frame, 100 + index)
    return frame


def main():
    import imageio_ffmpeg

    OUT.mkdir(parents=True, exist_ok=True)
    base = background()
    previews = []
    wav = OUT / "GlitchUI-Startsound.wav"
    make_sound(wav)
    with ZipFile(OUT / "bootanimation.zip", "w", ZIP_STORED) as animation:
        animation.writestr("desc.txt", f"{WIDTH} {HEIGHT} {FPS}\nc 1 0 part0\np 1 0 part1\np 0 0 part2\n")
        for index in range(FRAMES):
            frame = render(base, index)
            animation.writestr(f"part0/{index:03d}.png", png(frame))
            previews.append(frame.resize((360, 780), Image.Resampling.LANCZOS))
        animation.write(wav, "part0/audio.wav")
        final = frame
        tail = []
        for index in range(LOOP_FRAMES):
            frame = render_loop(final, index)
            tail.append(png(frame))
            previews.append(frame.resize((360, 780), Image.Resampling.LANCZOS))
        # Store each tail frame once in playback order so MIUI never replays a multi-frame part.
        for index in range(TAIL_FRAMES):
            animation.writestr(f"part1/{index:03d}.png", tail[index % LOOP_FRAMES])
        animation.writestr("part2/000.png", tail[0])
    final.resize((360, 780), Image.Resampling.LANCZOS).save(OUT / "GlitchUI-Boot-Still.png")
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    subprocess.run(
        [ffmpeg, "-loglevel", "error", "-y", "-i", str(wav), "-ac", "2",
         "-b:a", "160k", str(OUT / "bootaudio.mp3")],
        check=True,
    )
    subprocess.run(
        [ffmpeg, "-loglevel", "error", "-y", "-f", "rawvideo", "-pixel_format", "rgb24",
         "-video_size", "360x780", "-framerate", str(FPS), "-i", "pipe:0",
         "-i", str(wav), "-c:v", "libx264", "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-movflags", "+faststart", str(OUT / "GlitchUI-Boot-Vorschau.mp4")],
        input=b"".join(im.tobytes() for im in previews),
        check=True,
    )


if __name__ == "__main__":
    main()
