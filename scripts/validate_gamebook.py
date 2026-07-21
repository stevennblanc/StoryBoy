#!/usr/bin/env python3
"""Validate a StoryBoy gamebook before it ships.

Checks the things that are invisible by eye once a book passes a few dozen
nodes: dangling targets, nodes nothing links to, endings that were meant to be
choices, conditions that can never be met, and stats or images that do not
exist. Errors mean the book is broken; warnings mean it is probably a mistake.

Usage:
    python scripts/validate_gamebook.py web/store/the-sunken-vault.gbk
    python scripts/validate_gamebook.py path/to/story.json
    python scripts/validate_gamebook.py web/store/*.gbk

Exit code is 1 if any error was found, otherwise 0.
"""

from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

# Node fields that name another node.
TARGET_FIELDS = (
    "success_target", "failure_target", "win_target", "lose_target",
    "flee_target", "talk_target", "return_target", "return_to",
    "correct_target", "incorrect_target",
)

# Node types that end the book legitimately when they have no choices.
ENDING_TYPES = {"text", None}


class Report:
    def __init__(self, name: str) -> None:
        self.name = name
        self.errors: list[str] = []
        self.warnings: list[str] = []

    def error(self, message: str) -> None:
        self.errors.append(message)

    def warn(self, message: str) -> None:
        self.warnings.append(message)

    def print(self) -> None:
        print(f"\n{self.name}")
        for message in self.errors:
            print(f"  ERROR   {message}")
        for message in self.warnings:
            print(f"  warning {message}")
        if not self.errors and not self.warnings:
            print("  ok")


def load(path: Path) -> tuple[dict, set[str]]:
    """Return (story, asset paths). Asset paths are empty for a bare story.json."""
    if path.suffix == ".gbk":
        with zipfile.ZipFile(path) as archive:
            story = json.loads(archive.read("story.json").decode("utf-8"))
            assets = {name for name in archive.namelist()}
        return story, assets
    return json.loads(path.read_text(encoding="utf-8")), set()


def collect_choices(node: dict) -> list[dict]:
    choices = node.get("choices") or []
    locations = node.get("locations") or []
    return [c for c in choices if isinstance(c, dict)] + [
        c for c in locations if isinstance(c, dict)
    ]


def as_list(value) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        return [value]
    if isinstance(value, list):
        return [str(v) for v in value]
    return []


def item_ids(entries) -> set[str]:
    """Ids from a catalog or an inline grant, which may be strings or objects."""
    found = set()
    for entry in entries or []:
        if isinstance(entry, str):
            found.add(entry)
        elif isinstance(entry, dict) and entry.get("id"):
            found.add(entry["id"])
    return found


def validate(path: Path) -> Report:
    report = Report(str(path))
    try:
        story, assets = load(path)
    except Exception as exc:  # noqa: BLE001 - report, do not crash the batch
        report.error(f"could not read: {exc}")
        return report

    nodes = story.get("nodes") or []
    metadata = story.get("metadata") or {}

    # --- ids -------------------------------------------------------------
    by_id: dict[str, dict] = {}
    for node in nodes:
        node_id = node.get("id")
        if not node_id:
            report.error("a node has no id")
            continue
        if node_id in by_id:
            report.error(f"duplicate node id: {node_id}")
        by_id[node_id] = node

    start = metadata.get("start_node") or metadata.get("startNode")
    if not start:
        report.error("metadata has no start_node")
    elif start not in by_id:
        report.error(f"start_node points at a missing node: {start}")

    # --- what the book declares -----------------------------------------
    stat_ids = {s["id"] for s in story.get("stats") or [] if s.get("id")}
    map_ids = {m["id"] for m in story.get("map") or [] if m.get("id")}
    characters = story.get("characters") or []
    character_ids = {c["id"] for c in characters if c.get("id")}

    catalog_items = item_ids(story.get("inventory")) | item_ids(story.get("items"))
    catalog_equipment = item_ids(story.get("equipment")) | item_ids(story.get("gear"))
    catalog_evidence = item_ids(story.get("evidence"))

    # Ids that can also be granted inline by a node.
    granted_items, granted_equipment, granted_evidence = set(), set(), set()
    for node in nodes:
        granted_items |= item_ids(node.get("items") or node.get("inventory"))
        granted_equipment |= item_ids(node.get("equipment") or node.get("gear"))
        granted_evidence |= item_ids(node.get("evidence"))
    for character in characters:
        granted_equipment |= item_ids(character.get("equipment"))

    known_items = catalog_items | granted_items
    known_equipment = catalog_equipment | granted_equipment
    known_evidence = catalog_evidence | granted_evidence

    set_flags, read_flags = set(), set()
    for node in nodes:
        set_flags |= set(as_list(node.get("set_flags") or node.get("set_flag") or node.get("flags")))

    # --- per node --------------------------------------------------------
    def check_target(owner: str, target, field: str) -> None:
        if target is None:
            return
        if not isinstance(target, str) or not target:
            report.error(f"{owner}: {field} is not a node id ({target!r})")
        elif target not in by_id:
            report.error(f"{owner}: {field} points at a missing node '{target}'")

    def check_requirement(owner: str, requires: dict) -> None:
        if not isinstance(requires, dict):
            report.error(f"{owner}: requires is not an object")
            return
        for key, pool, label in (
            ("item", known_items, "item"),
            ("items", known_items, "item"),
            ("not_item", known_items, "item"),
            ("equipment", known_equipment, "equipment"),
            ("equipped", known_equipment, "equipment"),
            ("evidence", known_evidence, "evidence"),
        ):
            for value in as_list(requires.get(key)):
                if value not in pool:
                    report.error(f"{owner}: requires {label} '{value}' which no node grants")
        for key in ("character", "characters", "class", "not_character", "not_characters", "not_class"):
            for value in as_list(requires.get(key)):
                if value not in character_ids:
                    report.error(f"{owner}: requires unknown character '{value}'")
        for key in ("flag", "flags", "has_flag", "not_flag", "not_flags", "without_flag"):
            for value in as_list(requires.get(key)):
                read_flags.add(value)
                if value not in set_flags:
                    report.error(f"{owner}: requires flag '{value}' that no node ever sets")
        stats = requires.get("stat") or requires.get("stats")
        if isinstance(stats, dict):
            for stat_id in stats:
                if stat_id not in stat_ids:
                    report.error(f"{owner}: requires unknown stat '{stat_id}'")

    for node_id, node in by_id.items():
        node_type = node.get("type")

        for field in TARGET_FIELDS:
            if field in node:
                check_target(node_id, node[field], field)

        choices = collect_choices(node)
        for index, choice in enumerate(choices):
            check_target(f"{node_id} choice {index}", choice.get("target"), "target")
            if "requires" in choice:
                check_requirement(f"{node_id} choice {index}", choice["requires"])

        if node_type == "shop":
            check_target(node_id, node.get("return_target"), "return_target")
            currency = node.get("currency_stat")
            if currency and currency not in stat_ids:
                report.error(f"{node_id}: shop currency_stat '{currency}' is not a declared stat")
            for entry in node.get("items") or []:
                ref = entry.get("equipment") or entry.get("inventory")
                if ref and ref not in (known_equipment | known_items):
                    report.error(f"{node_id}: shop sells unknown item '{ref}'")

        if node_type in ("check", "combat", "battle"):
            for field, value in (
                ("stat_modifier", node.get("stat_modifier")),
                ("armor_stat", node.get("armor_stat")),
                ("health_stat", node.get("health_stat")),
                ("hit_stat", (node.get("player") or {}).get("hit_stat")),
                ("damage_stat", (node.get("player") or {}).get("damage_stat")),
            ):
                if value and value not in stat_ids:
                    report.error(f"{node_id}: {field} '{value}' is not a declared stat")

        for field in ("stat_changes", "adjust_stats", "gain_stats", "set_stats"):
            block = node.get(field)
            if isinstance(block, dict):
                for stat_id in block:
                    if stat_id not in stat_ids:
                        report.error(f"{node_id}: {field} touches unknown stat '{stat_id}'")

        for fragment in as_list(node.get("reveal_map") or node.get("reveals_map")):
            if fragment not in map_ids:
                report.error(f"{node_id}: reveals unknown map fragment '{fragment}'")

        # Usable items must move a stat that exists.
        for entry in (node.get("items") or []) + (node.get("inventory") or []):
            if isinstance(entry, dict):
                use = entry.get("use") or entry.get("use_effects") or entry.get("on_use")
                if isinstance(use, dict):
                    for stat_id in use:
                        if stat_id not in stat_ids:
                            report.error(
                                f"{node_id}: item '{entry.get('id')}' use affects unknown stat '{stat_id}'"
                            )

        # A node with nothing to do and no way out is a dead end unless it is
        # meant to be an ending.
        has_exit = bool(choices) or any(node.get(f) for f in TARGET_FIELDS)
        if not has_exit and node_type not in ENDING_TYPES:
            report.error(f"{node_id}: type '{node_type}' has no choices and no target - dead end")

        # Every choice being conditional means a player meeting none of them is
        # stranded with no way out. A choice with locked_text still renders, but
        # it renders disabled, so it is not an exit either.
        if choices and not any(f in node for f in TARGET_FIELDS):
            if all(c.get("requires") for c in choices):
                report.error(
                    f"{node_id}: every choice is conditional - a player meeting none of them is stranded"
                )

    # --- catalog-level use effects ---------------------------------------
    for entry in (story.get("inventory") or []) + (story.get("items") or []):
        if isinstance(entry, dict):
            use = entry.get("use") or entry.get("use_effects") or entry.get("on_use")
            if isinstance(use, dict):
                for stat_id in use:
                    if stat_id not in stat_ids:
                        report.error(f"item '{entry.get('id')}' use affects unknown stat '{stat_id}'")

    # --- reachability -----------------------------------------------------
    roots = [start] if start in by_id else []
    for character in characters:
        entry = character.get("start_node") or character.get("startNode")
        if entry:
            if entry in by_id:
                roots.append(entry)
            else:
                report.error(f"character '{character.get('id')}' starts at missing node '{entry}'")

    seen: set[str] = set()
    queue = list(roots)
    while queue:
        current = queue.pop()
        if current in seen:
            continue
        seen.add(current)
        node = by_id.get(current)
        if not node:
            continue
        for choice in collect_choices(node):
            target = choice.get("target")
            if isinstance(target, str) and target in by_id:
                queue.append(target)
        for field in TARGET_FIELDS:
            target = node.get(field)
            if isinstance(target, str) and target in by_id:
                queue.append(target)

    for node_id in by_id:
        if node_id not in seen:
            report.warn(f"{node_id}: nothing links here - unreachable")

    # --- flags and images -------------------------------------------------
    for flag in sorted(set_flags - read_flags):
        report.warn(f"flag '{flag}' is set but no choice ever reads it")

    if assets:
        referenced: set[str] = set()
        for node in nodes:
            for key in ("image", "poster", "banner"):
                if isinstance(node.get(key), str):
                    referenced.add(node[key])
            for image in node.get("images") or []:
                if isinstance(image, str):
                    referenced.add(image)
                elif isinstance(image, dict) and image.get("path"):
                    referenced.add(image["path"])
        for entry in (story.get("map") or []) + (story.get("inventory") or []) + (story.get("equipment") or []):
            if isinstance(entry, dict) and isinstance(entry.get("image"), str):
                referenced.add(entry["image"])
        for character in characters:
            if isinstance(character.get("image"), str):
                referenced.add(character["image"])
        for image in sorted(referenced):
            if image not in assets:
                report.error(f"missing image in package: {image}")

    return report


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    paths = [Path(arg) for arg in argv]
    reports = [validate(path) for path in paths]
    for report in reports:
        report.print()
    errors = sum(len(r.errors) for r in reports)
    warnings = sum(len(r.warnings) for r in reports)
    print(f"\n{len(reports)} book(s): {errors} error(s), {warnings} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
