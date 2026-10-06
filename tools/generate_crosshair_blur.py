"""Generate the fixed 15x15 Gaussian-softened crosshair sprite."""

import math
import struct
import zlib
from pathlib import Path


SIZE = 15
RADIUS = 3
SIGMA = 1.0
OUTPUT = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/crosshaireditor/textures/gui/sprites/hud/crosshair_blurred.png"
)


def gaussian_kernel():
    """Return the normalized, symmetric radius-3 Gaussian kernel."""
    weights = [math.exp(-(offset * offset) / (2 * SIGMA * SIGMA)) for offset in range(-RADIUS, RADIUS + 1)]
    total = sum(weights)
    return tuple(weight / total for weight in weights)


def alpha_plane():
    """Convolve the union of the original 9x9 one-pixel cross arms."""
    kernel = gaussian_kernel()
    result = []
    for y in range(-7, 8):
        row = []
        for x in range(-7, 8):
            alpha = 0.0
            for dy in range(-RADIUS, RADIUS + 1):
                source_y = y - dy
                if not -4 <= source_y <= 4:
                    continue
                for dx in range(-RADIUS, RADIUS + 1):
                    source_x = x - dx
                    if -4 <= source_x <= 4 and (source_x == 0 or source_y == 0):
                        alpha += kernel[dy + RADIUS] * kernel[dx + RADIUS]
            row.append(alpha)
        result.append(tuple(row))
    return tuple(result)


def _chunk(kind, payload):
    content = kind + payload
    return struct.pack(">I", len(payload)) + content + struct.pack(">I", zlib.crc32(content) & 0xFFFFFFFF)


def png_bytes():
    """Encode the alpha plane as deterministic white straight-alpha RGBA PNG."""
    raw = bytearray()
    for row in alpha_plane():
        raw.append(0)  # PNG filter: None
        for alpha in row:
            raw.extend((255, 255, 255, round(alpha * 255)))
    header = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + _chunk(b"IHDR", header)
        + _chunk(b"IDAT", zlib.compress(bytes(raw)))
        + _chunk(b"IEND", b"")
    )


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(png_bytes())


if __name__ == "__main__":
    main()
