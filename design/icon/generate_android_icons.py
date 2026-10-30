"""Generate Matchamisù Android launcher assets from the current master PNG.

Only whole-image high-quality resizing is performed. The master is never edited.
Run with Pillow and NumPy installed.
"""

from pathlib import Path
from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).with_name("matchamisu-icon-source.png")
RES = ROOT / "app" / "src" / "main" / "res"
DENSITY_SCALE = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def master_image() -> Image.Image:
    image = Image.open(SOURCE)
    if image.format != "PNG" or image.width != image.height:
        raise ValueError(f"Expected a square PNG master, got {image.format} {image.size}")
    return image.convert("RGBA")


def make_monochrome(source: Image.Image) -> Image.Image:
    """Use image luminance as alpha for Android themed-icon tinting."""
    luminance = source.convert("L")
    alpha = np.asarray(luminance, dtype=np.uint8)
    result = Image.new("RGBA", source.size, (255, 255, 255, 0))
    result.putalpha(Image.fromarray(alpha, "L"))
    return result


def main() -> None:
    source = master_image()
    mono = make_monochrome(source)
    for density, scale in DENSITY_SCALE.items():
        # Legacy launcher raster: whole master, standard density size.
        size = round(48 * scale)
        mipmap = RES / f"mipmap-{density}"
        for name, image in (("ic_launcher", source), ("ic_launcher_round", source),
                            ("ic_launcher_foreground", source), ("ic_launcher_background", source),
                            ("ic_launcher_monochrome", mono)):
            image.resize((size, size), Image.Resampling.LANCZOS).save(
                mipmap / f"{name}.webp", "WEBP", lossless=True
            )

        # Adaptive icon canvas is 108dp. Keep the complete image at full bleed;
        # Android's launcher mask is the only crop applied.
        adaptive_size = round(108 * scale)
        drawable = RES / f"drawable-{density}"
        for name, image in (("matchamisu_adaptive_foreground", source),
                            ("matchamisu_adaptive_background", Image.new("RGBA", source.size, (0, 0, 0, 0))),
                            ("matchamisu_adaptive_monochrome", mono)):
            image.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS).save(
                drawable / f"{name}.png"
            )


if __name__ == "__main__":
    main()
