"""Compile and test the independent planning guard; does not build or install a game mod."""
from pathlib import Path
import subprocess
import os
import shutil

root = Path(__file__).resolve().parents[1]
classes = root / 'build-new/classes'
classes.mkdir(parents=True, exist_ok=True)
sources = sorted((root / 'src').rglob('*.java'))
cp = os.environ.get('ZOMBIE_BUDDY_JAR', '/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/ZombieBuddy.jar') + os.pathsep + '/home/j0schi/SteamLibrary/steamapps/common/ProjectZomboid/projectzomboid/projectzomboid.jar'
subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), *map(str, sources)], check=True)
resources = root / 'src/main/resources'
if resources.is_dir(): shutil.copytree(resources, classes, dirs_exist_ok=True)
subprocess.run(['java', '-ea', '-cp', str(classes)+os.pathsep+cp, 'personal.worldgen.WorldGenTest'], check=True)
