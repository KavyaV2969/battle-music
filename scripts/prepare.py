"""Prepare exact installed intermediary dependencies; nothing is redistributed."""
from pathlib import Path
import argparse, json, shutil, zipfile

ROOT = Path(__file__).resolve().parents[1]
def prepare(profile=None):
    profile = Path(profile or ROOT.parent).resolve()
    meta = profile.parent.parent / 'meta'
    out = ROOT / 'build/deps'
    out.mkdir(parents=True, exist_ok=True)
    inputs = [profile / 'mods/Cobblemon-fabric-1.7.3+1.21.1.jar',
              profile / '.fabric/remappedJars/minecraft-1.21.1-0.18.4/client-intermediary.jar']
    version = json.loads((meta / 'versions/1.21.1-0.18.4/1.21.1-0.18.4.json').read_text())
    for lib in version['libraries']:
        parts = lib['name'].split(':')
        if len(parts) != 3 or parts[0] in ('org.lwjgl', 'ca.weblite') or 'native' in parts[1]: continue
        group, name, ver = parts
        path = meta / 'libraries' / group.replace('.', '/') / name / ver / f'{name}-{ver}.jar'
        if path.exists(): inputs.append(path)
    inputs += list((profile / 'mods').glob('rctapi-fabric-*.jar'))
    names = set()
    for path in inputs:
        if not path.is_file(): raise FileNotFoundError(path)
        shutil.copyfile(path, out / path.name); names.add(path.name)
    for pattern in ('fabric-api-*.jar', 'fabric-language-kotlin-*.jar'):
        path = next((profile / 'mods').glob(pattern))
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                if name.startswith('META-INF/jars/') and name.endswith('.jar'):
                    target = Path(name).name
                    (out / target).write_bytes(archive.read(name)); names.add(target)
    for old in out.glob('*.jar'):
        if old.name not in names: old.unlink()
    print(f'Prepared {len(names)} compile dependencies from {profile}')
    return profile

if __name__ == '__main__':
    ap = argparse.ArgumentParser(); ap.add_argument('--profile'); args = ap.parse_args()
    prepare(args.profile)
