"""Compress the images inside a StoryBoy .gbk package.

Re-encodes every PNG as progressive JPEG (quality 80), resizes artwork to the
documented standards (poster 1024x1536, banner 1536x1024, story images capped
at 1280px on the long side), and rewrites every image reference in story.json
(metadata artwork, node image/images, and inventory/evidence item images).

Usage:
    python scripts/compress_gamebook.py path/to/book.gbk [-o output.gbk] [--version 0.4.0]

Without -o the package is compressed in place. Requires Pillow.
"""

import argparse
import io
import json
import os
import zipfile

from PIL import Image

MAX_STORY_DIM = 1280
POSTER_SIZE = (1024, 1536)
BANNER_SIZE = (1536, 1024)
QUALITY = 80


def compress_image(data: bytes, name: str) -> bytes:
    img = Image.open(io.BytesIO(data))
    if img.mode in ("RGBA", "LA", "P"):
        img = img.convert("RGB")
    if name.startswith("poster"):
        img.thumbnail(POSTER_SIZE, Image.LANCZOS)
    elif name.startswith("banner"):
        img.thumbnail(BANNER_SIZE, Image.LANCZOS)
    else:
        img.thumbnail((MAX_STORY_DIM, MAX_STORY_DIM), Image.LANCZOS)
    out = io.BytesIO()
    img.save(out, "JPEG", quality=QUALITY, optimize=True, progressive=True)
    return out.getvalue()


def rename_refs(story: dict, mapping: dict) -> None:
    def fix(path):
        return mapping.get(path, path)

    meta = story.get("metadata", {})
    for key in ("cover_image", "title_image"):
        if meta.get(key):
            meta[key] = fix(meta[key])

    for catalog_key in ("inventory", "evidence"):
        for item in story.get(catalog_key) or []:
            if isinstance(item, dict) and item.get("image"):
                item["image"] = fix(item["image"])

    for node in story.get("nodes", []):
        if node.get("image"):
            node["image"] = fix(node["image"])
        images = node.get("images")
        if isinstance(images, list):
            for i, image in enumerate(images):
                if isinstance(image, str):
                    images[i] = fix(image)
                elif isinstance(image, dict) and image.get("path"):
                    image["path"] = fix(image["path"])
        for catalog_key in ("inventory", "evidence", "items"):
            value = node.get(catalog_key)
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, dict) and item.get("image"):
                        item["image"] = fix(item["image"])


def verify_references(z: zipfile.ZipFile) -> list:
    names = set(z.namelist())
    story = json.loads(z.read("story.json").decode("utf-8"))
    missing = []

    def check(path):
        if path and path not in names:
            missing.append(path)

    meta = story.get("metadata", {})
    check(meta.get("cover_image"))
    check(meta.get("title_image"))
    for node in story.get("nodes", []):
        check(node.get("image"))
        images = node.get("images")
        if isinstance(images, list):
            for image in images:
                check(image if isinstance(image, str) else image.get("path"))
    for catalog_key in ("inventory", "evidence"):
        for item in story.get(catalog_key) or []:
            if isinstance(item, dict):
                check(item.get("image"))
    return missing


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", help="Path to the .gbk file")
    parser.add_argument("-o", "--output", help="Output path (default: in place)")
    parser.add_argument("--version", help="New metadata.version for the package")
    args = parser.parse_args()

    out_path = args.output or args.package
    before = os.path.getsize(args.package)

    with zipfile.ZipFile(args.package) as z:
        entries = {
            info.filename: z.read(info.filename)
            for info in z.infolist()
            if not info.filename.endswith("/")
        }

    mapping = {}
    new_entries = {}
    for name, data in entries.items():
        if name.lower().endswith(".png"):
            new_name = name[:-4] + ".jpg"
            new_entries[new_name] = compress_image(data, os.path.basename(name))
            mapping[name] = new_name
        else:
            new_entries[name] = data

    story = json.loads(new_entries["story.json"].decode("utf-8"))
    rename_refs(story, mapping)
    if args.version:
        story["metadata"]["version"] = args.version
    new_entries["story.json"] = json.dumps(story, ensure_ascii=False, indent=2).encode("utf-8")

    with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as z:
        for name, data in new_entries.items():
            z.writestr(name, data)

    with zipfile.ZipFile(out_path) as z:
        missing = verify_references(z)
    if missing:
        raise SystemExit(f"Package references missing images: {missing}")

    after = os.path.getsize(out_path)
    print(f"{out_path}: {before / 1024 / 1024:.1f} MB -> {after / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()
