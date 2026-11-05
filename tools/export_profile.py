"""Export protected cells of enabled workshop maps into a PersonalWorldGen profile."""
import argparse
import re
from pathlib import Path


def version_layer(mod: Path) -> Path:
    versions = [p for p in mod.iterdir()
                if p.is_dir() and re.fullmatch(r'42(?:\.\d+)*', p.name)
                and tuple(map(int, p.name.split('.'))) <= (42, 21, 0)]
    return max(versions, key=lambda p: tuple(map(int, p.name.split('.')))) if versions else mod / 'common'


def mod_id(mod: Path) -> str:
    for info in (version_layer(mod) / 'mod.info', mod / 'common/mod.info'):
        if info.is_file():
            match = re.search(r'^id=(.*?)\s*$', info.read_text(errors='replace'), re.M)
            if match:
                return match.group(1).strip()
    return ''


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--zomboid', type=Path, default=Path.home() / 'Zomboid')
    parser.add_argument('--workshop', type=Path,
                        default=Path.home() / 'SteamLibrary/steamapps/workshop/content/108600')
    parser.add_argument('--out', type=Path, default=Path(__file__).resolve().parents[1] / 'profile.tsv')
    args = parser.parse_args()

    config = args.zomboid / 'mods/default.txt'
    enabled = {m.strip().rstrip(',') for m in re.findall(r'^\s*mod\s*=\s*(.+?)\s*,?\s*$',
                                                        config.read_text(encoding='utf-8'), re.M)}
    cells = set()
    for item in sorted(args.workshop.iterdir()):
        if not item.is_dir():
            continue
        for mod in (item / 'mods').glob('*'):
            if not mod.is_dir() or mod_id(mod) not in enabled:
                continue
            layer = version_layer(mod)
            for base in (mod / 'common', layer):
                for info in (base / 'media/maps').glob('*/map.info'):
                    for header in info.parent.glob('*.lotheader'):
                        match = re.match(r'^(-?\d+)_(-?\d+)\.lotheader$', header.name)
                        if match:
                            cells.add((int(match.group(1)), int(match.group(2))))
    game_version = (args.zomboid / 'version.txt').read_text(encoding='utf-8').split()[0]
    lines = ['# PersonalWorldGen map profile', f'version\t{game_version}']
    lines += [f'cell\t{x}\t{y}' for x, y in sorted(cells)]
    args.out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(f'{args.out}: {len(cells)} cells, game {game_version}')


if __name__ == '__main__':
    main()
