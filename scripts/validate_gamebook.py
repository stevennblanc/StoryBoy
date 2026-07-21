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
import re
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


FLAG_KEYS = ("flag", "flags", "has_flag")
NOT_FLAG_KEYS = ("not_flag", "not_flags", "without_flag")
OTHER_PREDICATES = (
    "item", "items", "has_item", "not_item", "without_item", "missing_item",
    "equipment", "gear", "equipped", "evidence",
    "character", "characters", "class", "not_character", "not_characters", "not_class",
    "stat", "stats",
)


def flag_gap(choices: list[dict]):
    """Find a flag state where no choice is available.

    Returns None if the conditions are not purely flag-based (undecidable here),
    "" if every combination is covered, or a description of one that is not.
    """
    required, forbidden, flags = [], [], set()
    for choice in choices:
        requires = choice.get("requires") or {}
        if not isinstance(requires, dict):
            return None
        if any(key in requires for key in OTHER_PREDICATES):
            return None
        need = {v for key in FLAG_KEYS for v in as_list(requires.get(key))}
        block = {v for key in NOT_FLAG_KEYS for v in as_list(requires.get(key))}
        if not need and not block:
            return ""  # an unconditional choice: always a way out
        required.append(need)
        forbidden.append(block)
        flags |= need | block

    ordered = sorted(flags)
    for mask in range(1 << len(ordered)):
        state = {name for i, name in enumerate(ordered) if mask & (1 << i)}
        if any(need <= state and not (block & state)
               for need, block in zip(required, forbidden)):
            continue
        on = sorted(state)
        off = [f for f in ordered if f not in state]
        return f"set={on or 'nothing'} unset={off or 'nothing'}"
    return ""


def gold_in(text: str, currency_words: tuple) -> list[int]:
    """Figures the prose promises, e.g. "pocket 20 gold"."""
    if not text:
        return []
    pattern = r"(\d+)\s+(?:%s)\b" % "|".join(currency_words)
    return [int(m) for m in re.findall(pattern, text, re.I)]


def stat_delta(node: dict, stat_id: str) -> int:
    total = 0
    for key in ("stat_changes", "adjust_stats", "gain_stats"):
        block = node.get(key)
        if isinstance(block, dict) and isinstance(block.get(stat_id), int):
            total += block[stat_id]
    absolute = node.get("set_stats")
    if isinstance(absolute, dict) and isinstance(absolute.get(stat_id), int):
        total = absolute[stat_id]
    return total


def title_words(title: str) -> set:
    return {w for w in re.findall(r"[a-z]+", (title or "").lower()) if len(w) > 3}


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
        # stranded. When the conditions are all flag-based we can settle it
        # properly: enumerate the flag combinations and find any that no choice
        # covers. Otherwise fall back to warning that there is no fallback.
        if choices and not any(f in node for f in TARGET_FIELDS):
            if all(c.get("requires") for c in choices):
                uncovered = flag_gap(choices)
                if uncovered is None:
                    report.error(
                        f"{node_id}: every choice is conditional and they are not all flag-based - "
                        "a player meeting none of them is stranded"
                    )
                elif uncovered:
                    report.error(
                        f"{node_id}: no choice is available when {uncovered} - the player is stranded"
                    )

    # --- prose against rewards -------------------------------------------
    # A node that says "20 gold" and hands over none is the single easiest
    # mistake to make when rebalancing an economy, and it is invisible until a
    # player counts. Check what the prose promises against what is granted.
    # Currency means what shops charge in, plus a stat literally called gold -
    # not every stat that happens to have a label.
    currency_ids = {n["currency_stat"] for n in nodes
                    if n.get("type") == "shop" and n.get("currency_stat")}
    currency_ids |= {s["id"] for s in story.get("stats") or [] if s.get("id") == "gold"}
    currency = {"gold", "coin", "coins"}
    for stat in story.get("stats") or []:
        if stat.get("id") in currency_ids and stat.get("label"):
            currency.add(stat["label"].lower())
    currency_words = tuple(sorted(currency))
    currency_ids = sorted(currency_ids)

    catalog_titles = {}
    for entry in (story.get("inventory") or []) + (story.get("items") or []) \
            + (story.get("equipment") or []) + (story.get("gear") or []):
        if isinstance(entry, dict) and entry.get("id"):
            catalog_titles[entry["id"]] = entry.get("title", "")

    for node_id, node in by_id.items():
        text = node.get("text") or ""
        promised = gold_in(text, currency_words)
        if promised:
            actual = max((abs(stat_delta(node, sid)) for sid in currency_ids), default=0)
            if not any(p == actual for p in promised):
                report.error(
                    f"{node_id}: prose promises {promised[0]} but the node grants {actual}"
                )
        # A choice naming a price should match what its target actually charges.
        for choice in collect_choices(node):
            asked = gold_in(choice.get("text") or "", currency_words)
            target = by_id.get(choice.get("target"))
            if asked and target:
                charged = max((abs(stat_delta(target, sid)) for sid in currency_ids), default=0)
                if not any(a == charged for a in asked):
                    report.error(
                        f"{node_id}: a choice asks {asked[0]} but '{choice.get('target')}' "
                        f"changes it by {charged}"
                    )
        # Something handed over without a word of prose is a silent reward.
        for key in ("items", "inventory", "equipment", "gear"):
            for entry in (node.get(key) or []):
                item_id = entry if isinstance(entry, str) else (entry or {}).get("id")
                title = catalog_titles.get(item_id) if isinstance(entry, str) else (entry or {}).get("title")
                words = title_words(title or "")
                if words and not (words & title_words(text)):
                    report.warn(f"{node_id}: grants '{title}' but the prose never mentions it")

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
