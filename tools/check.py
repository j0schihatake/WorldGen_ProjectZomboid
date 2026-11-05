"""Compile and test the independent planning guard; does not build or install a game mod."""
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
classes = root / 'build/classes'
classes.mkdir(parents=True, exist_ok=True)
sources = sorted((root / 'src').rglob('*.java'))
subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-d', str(classes), *map(str, sources)], check=True)
subprocess.run(['java', '-ea', '-cp', str(classes), 'personal.worldgen.ProtectedCellsTest'], check=True)
