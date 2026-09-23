#!/usr/bin/env python3
"""Summarise a QueryFence report.

    python3 tools/queryfence-summary.py target/queryfence/report.json
    python3 tools/queryfence-summary.py target/queryfence/report.json --by table
    python3 tools/queryfence-summary.py target/queryfence/report.json --triage

Reads the JSON report a test run wrote and prints what is in it, grouped by rule, by table, by
violation code or by the code that produced the SQL. `--triage` prints one line per distinct
origin, which is the list you work through when adopting QueryFence on an existing project.

No dependencies: standard library only, Python 3.9+.
"""

from __future__ import annotations

import argparse
import collections
import json
import pathlib
import sys


def load(path: pathlib.Path) -> dict:
    try:
        return json.loads(path.read_text())
    except FileNotFoundError:
        sys.exit(f"No report at {path}. Run your tests first; QueryFence writes it at the end.")
    except json.JSONDecodeError as error:
        sys.exit(f"{path} is not valid JSON: {error}")


def findings(report: dict) -> list[dict]:
    out = []
    for group in report.get("policies", []):
        for finding in group.get("findings", []):
            out.append({**finding, "policy": group.get("policy"), "mode": group.get("mode")})
    return out


def origin_of(finding: dict) -> str:
    origin = finding.get("origin") or {}
    if not origin.get("class"):
        return "unknown origin"
    where = f"{origin['class']}#{origin['method']}"
    if origin.get("file") and origin.get("line", -1) >= 0:
        where += f" ({origin['file']}:{origin['line']})"
    return where


def table(rows: list[tuple[str, int]], header: str) -> None:
    if not rows:
        return
    width = max(len(name) for name, _ in rows)
    print(f"\n{header}")
    print("-" * (width + 8))
    for name, count in rows:
        print(f"{name.ljust(width)}  {count:>5}")


def counted(items: collections.Counter) -> list[tuple[str, int]]:
    return sorted(items.items(), key=lambda pair: (-pair[1], pair[0]))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=pathlib.Path, nargs="?",
                        default=pathlib.Path("target/queryfence/report.json"))
    parser.add_argument("--by", choices=["rule", "table", "code", "origin", "all"], default="all")
    parser.add_argument("--triage", action="store_true",
                        help="one line per distinct origin, to work through when adopting")
    args = parser.parse_args()

    report = load(args.report)
    found = findings(report)

    if report.get("disabled"):
        print("WARNING: QueryFence was disabled during this run")
        for reason in report.get("disabledReasons", []):
            print(f"  - {reason}")

    for group in report.get("policies", []):
        summary = group.get("summary", {})
        print(
            f"{group.get('policy')} [{group.get('mode')}]: "
            f"{summary.get('findings', 0)} findings, "
            f"{summary.get('statements', 0)} statements, "
            f"{summary.get('tests', 0)} tests"
        )

    if not found:
        print("\nNothing to report: every statement satisfied the policy.")
        return

    if args.by in ("rule", "all"):
        table(counted(collections.Counter(f.get("rule") or "parser" for f in found)),
              "By rule")
    if args.by in ("table", "all"):
        table(counted(collections.Counter(f.get("table") or "(not a table)" for f in found)),
              "By table")
    if args.by in ("code", "all"):
        table(counted(collections.Counter(f["code"] for f in found)), "By code")
    if args.by in ("origin", "all"):
        table(counted(collections.Counter(origin_of(f) for f in found))[:20],
              "By origin (top 20)")

    if args.triage:
        print("\nTriage list: one row per origin. Decide leak / false positive / suppression.")
        print("-" * 100)
        by_origin: dict[str, list[dict]] = collections.defaultdict(list)
        for finding in found:
            by_origin[origin_of(finding)].append(finding)
        for origin, group in sorted(by_origin.items(), key=lambda kv: -len(kv[1])):
            codes = ", ".join(sorted({f["code"] for f in group}))
            tables = ", ".join(sorted({f.get("table") or "-" for f in group}))
            print(f"\n{origin}")
            print(f"    {len(group)} finding(s)  codes: {codes}  tables: {tables}")
            print(f"    e.g. {group[0]['sql'][:160]}")


if __name__ == "__main__":
    main()
