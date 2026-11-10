#!/usr/bin/env python3
"""Compile WorldGen planning code into the B42 mod jar. No world writes."""
from __future__ import annotations
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLASSES = ROOT / "build-new" / "mod-classes"
OUT = ROOT / "42" / "media" / "java" / "client" / "PersonalWorldGen.jar"


def main() -> None:
    shutil.rmtree(CLASSES, ignore_errors=True)
    CLASSES.mkdir(parents=True, exist_ok=True)
    sources = sorted((ROOT / "src" / "main" / "java").rglob("*.java"))
    cp = os.environ.get("ZOMBIE_BUDDY_JAR",
        "/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/ZombieBuddy.jar")
    subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-cp", cp,
                    "-d", str(CLASSES), *map(str, sources)], check=True)
    resources = ROOT / "src" / "main" / "resources"
    if resources.is_dir():
        shutil.copytree(resources, CLASSES, dirs_exist_ok=True)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["jar", "--create", "--file", str(OUT), "--date=2020-01-01T00:00:00Z", "-C", str(CLASSES), "."], check=True)
    print(f"Built: {OUT}")


if __name__ == "__main__":
    main()
