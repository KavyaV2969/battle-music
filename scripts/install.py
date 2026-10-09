"""Install locally built artifacts, preserving backups and unrelated settings."""
from pathlib import Path
import argparse, datetime, hashlib, json, re, shutil, subprocess
from catalog import ROOT
from build import VERSION

def install(profile, world, music_volume=None):
    if music_volume is not None and not 0<=music_volume<=1:raise SystemExit('--music-volume must be between 0 and 1.')
    profile=Path(profile).resolve()
    world_dir=(profile/'saves'/world).resolve()
    if not world_dir.is_relative_to(profile/'saves') or not (world_dir/'level.dat').is_file():
        raise SystemExit('Choose an existing world inside the target profile saves directory.')
    # Avoid launcher argument output: inspect only whether a Minecraft JVM is running.
    check=subprocess.run(['powershell','-NoProfile','-Command',
        "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { $_.CommandLine -match 'minecraft|fabricmc|KnotClient|KnotServer' }).Count"],capture_output=True,text=True,check=True)
    if int(check.stdout.strip() or '0'):
        raise SystemExit('Close Minecraft before installation; running games can overwrite settings or hold JAR files open.')
    receipt=json.loads((ROOT/'reports/live-client.json').read_text())
    jar=ROOT/f'dist/battlemusic-{VERSION}.jar'
    if not receipt.get('success') or receipt.get('jar_sha256')!=hashlib.sha256(jar.read_bytes()).hexdigest():
        raise SystemExit('Run successful isolated live verification for this exact JAR before installing.')
    backup=profile/'battle-music-backups'/datetime.datetime.now().strftime('%Y%m%d-%H%M%S')
    changed=[]
    def write(dest, data):
        relative=dest.relative_to(profile)
        if dest.exists():
            saved=backup/relative;saved.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(dest,saved)
        dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data);changed.append(str(relative))
    for source,dest in [(jar,profile/'mods'/jar.name),
        (ROOT/f'dist/battlemusic-audio-{VERSION}.zip',profile/f'resourcepacks/battlemusic-audio-{VERSION}.zip'),
        (ROOT/f'dist/battlemusic-defaults-{VERSION}.zip',world_dir/f'datapacks/battlemusic-defaults-{VERSION}.zip')]:
        write(dest,source.read_bytes())
    options_path=profile/'options.txt';options=options_path.read_text(encoding='utf-8')
    line=re.search(r'^resourcePacks:(.*)$',options,re.M)
    if not line:raise SystemExit('options.txt has no resourcePacks entry; artifacts installed, enable audio pack manually.')
    packs=json.loads(line.group(1));event=f'file/battlemusic-audio-{VERSION}.zip'
    if event not in packs:packs.append(event)
    options=options[:line.start(1)]+json.dumps(packs,separators=(',',':'))+options[line.end(1):]
    if music_volume is not None:options=re.sub(r'^soundCategory_music:.*$',f'soundCategory_music:{music_volume}',options,flags=re.M)
    write(options_path,options.encode('utf-8'))
    physics=profile/'config/sound_physics_remastered/sound_rates.properties'
    text=physics.read_text() if physics.exists() else ''
    for entry in (ROOT/'compat/sound_rates.properties').read_text().splitlines():
        if not entry or entry.startswith('#'):continue
        key=entry.split('=')[0];pattern=r'^'+re.escape(key)+r'=.*$'
        text=re.sub(pattern,lambda _:entry,text,flags=re.M) if re.search(pattern,text,re.M) else text+'\n'+entry
    write(physics,(text+'\n').encode('utf-8'))
    backup.mkdir(parents=True,exist_ok=True)
    record={'profile':str(profile),'world':world,'changed':changed,'backup':str(backup),'jar_sha256':receipt['jar_sha256'],'music_volume':music_volume}
    (backup/'installation.json').write_text(json.dumps(record,indent=2)+'\n')
    (ROOT/'reports/installation.json').write_text(json.dumps(record,indent=2)+'\n')
    print(json.dumps(record,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--profile',type=Path,default=ROOT.parent);parser.add_argument('--world',required=True);parser.add_argument('--music-volume',type=float)
    args=parser.parse_args();install(args.profile,args.world,args.music_volume)
