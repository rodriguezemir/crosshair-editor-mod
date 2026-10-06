import struct
import unittest
import zlib
from pathlib import Path

import generate_crosshair_blur as blur


ASSET = (
    Path(__file__).resolve().parents[1]
    / "src/main/resources/assets/crosshaireditor/textures/gui/sprites/hud/crosshair_blurred.png"
)


def decode_png(data):
    """Decode this generator's small, non-interlaced RGBA PNG and validate CRCs."""
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    offset = 8
    chunks = []
    while offset < len(data):
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4 : offset + 8]
        payload = data[offset + 8 : offset + 8 + length]
        crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        assert zlib.crc32(kind + payload) & 0xFFFFFFFF == crc
        chunks.append((kind, payload))
        offset += length + 12
    assert offset == len(data)
    header = chunks[0][1]
    width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", header)
    assert (depth, color, compression, filtering, interlace) == (8, 6, 0, 0, 0)
    compressed = b"".join(payload for kind, payload in chunks if kind == b"IDAT")
    raw = zlib.decompress(compressed)
    assert len(raw) == height * (width * 4 + 1)
    rows = []
    pos = 0
    for _ in range(height):
        method = raw[pos]
        row = list(raw[pos + 1 : pos + 1 + width * 4])
        pos += width * 4 + 1
        assert method == 0
        rows.append(row)
    return width, height, rows


class CrosshairBlurTests(unittest.TestCase):
    def test_kernel_is_normalized_and_symmetric(self):
        kernel = blur.gaussian_kernel()
        self.assertEqual(len(kernel), 7)
        self.assertAlmostEqual(sum(kernel), 1.0, places=14)
        self.assertEqual(kernel, kernel[::-1])

    def test_alpha_is_soft_symmetric_and_falls_off(self):
        alpha = blur.alpha_plane()
        self.assertEqual((len(alpha[0]), len(alpha)), (15, 15))
        for y in range(15):
            for x in range(15):
                self.assertAlmostEqual(alpha[y][x], alpha[y][14 - x], places=14)
                self.assertAlmostEqual(alpha[y][x], alpha[14 - y][x], places=14)
                self.assertAlmostEqual(alpha[y][x], alpha[x][y], places=14)
        self.assertLess(alpha[7][7], 1.0)
        self.assertGreater(alpha[7][7], alpha[7][8])
        self.assertGreater(alpha[7][8], alpha[7][9])
        self.assertEqual([alpha[0][0], alpha[0][14], alpha[14][0], alpha[14][14]], [0.0] * 4)
        self.assertAlmostEqual(sum(map(sum, alpha)), 17.0, delta=15 * 15 / 255)

    def test_checked_in_png_matches_math_and_deterministic_encoding(self):
        data = ASSET.read_bytes()
        width, height, rgba = decode_png(data)
        self.assertEqual((width, height), (15, 15))
        expected = blur.alpha_plane()
        for y in range(height):
            for x in range(width):
                self.assertEqual(rgba[y][x * 4 : x * 4 + 3], [255, 255, 255])
                self.assertAlmostEqual(rgba[y][x * 4 + 3] / 255, expected[y][x], delta=1 / 255)
        self.assertEqual(data, blur.png_bytes())


if __name__ == "__main__":
    unittest.main()
