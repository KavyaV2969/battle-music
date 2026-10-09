"""Launch an owned isolated client using cached Minecraft/Fabric libraries and anonymous identity."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,time,sys
from catalog import ROOT
from build import JAVA,VERSION

PROFILE=ROOT.parent
META=PROFILE.parent.parent/'meta'
GAME=ROOT/'integration/game'
MC='1.21.1-0.18.4'

def copy_file(source,dest):
    dest.parent.mkdir(parents=True,exist_ok=True)
    if not dest.exists() or dest.stat().st_size!=source.stat().st_size or dest.stat().st_mtime_ns!=source.stat().st_mtime_ns:shutil.copy2(source,dest)

def setup():
    GAME.mkdir(parents=True,exist_ok=True)
    # Copy immutable mod/pack files and configuration. Only this game's copies are ever edited.
    for folder in ('mods','resourcepacks','shaderpacks','config','defaultconfigs','datapacks'):
        source=PROFILE/folder
        for path in source.rglob('*'):
            if path.is_file():copy_file(path,GAME/folder/path.relative_to(source))
    for old in (GAME/'mods').glob('battlemusic*.jar'):old.unlink()
    # Exercise the shipped default, independent of preferences from earlier isolated runs.
    settings=GAME/'config/battlemusic-client.json'
    if settings.exists():settings.unlink()
    copy_file(ROOT/f'dist/battlemusic-{VERSION}.jar',GAME/f'mods/battlemusic-{VERSION}.jar')
    copy_file(ROOT/'integration/battlemusic-verification.jar',GAME/'mods/battlemusic-verification.jar')
    copy_file(ROOT/f'dist/battlemusic-audio-{VERSION}.zip',GAME/f'resourcepacks/battlemusic-audio-{VERSION}.zip')
    copy_file(ROOT/f'dist/battlemusic-defaults-{VERSION}.zip',GAME/f'datapacks/battlemusic-defaults-{VERSION}.zip')
    world=GAME/'saves/BattleMusicVerification';world.mkdir(parents=True,exist_ok=True)
    if not (world/'level.dat').exists():copy_file(PROFILE/'saves/New World/level.dat',world/'level.dat')
    copy_file(ROOT/f'dist/battlemusic-defaults-{VERSION}.zip',world/f'datapacks/battlemusic-defaults-{VERSION}.zip')
    options=(PROFILE/'options.txt').read_text()
    for key,value in {'pauseOnLostFocus':'false','fullscreen':'false','enableVsync':'false','soundCategory_music':'0.65','renderDistance':'4','simulationDistance':'4','maxFps':'60'}.items():
        options=re.sub(r'^'+key+r':.*$',key+':'+value,options,flags=re.M)
    line=re.search(r'^resourcePacks:(.*)$',options,re.M)
    packs=json.loads(line.group(1));packs=[p for p in packs if not p.startswith('file/battlemusic-')]+[f'file/battlemusic-audio-{VERSION}.zip']
    options=options[:line.start(1)]+json.dumps(packs,separators=(',',':'))+options[line.end(1):]
    (GAME/'options.txt').write_text(options)
    iris=GAME/'config/iris.properties'
    if iris.exists():iris.write_text(iris.read_text().replace('enableShaders=true','enableShaders=false'))
    # Merge specific exclusions without replacing unrelated physics settings.
    physics=GAME/'config/sound_physics_remastered/sound_rates.properties'
    text=physics.read_text() if physics.exists() else ''
    for line in (ROOT/'compat/sound_rates.properties').read_text().splitlines():
        if not line or line.startswith('#'):continue
        key=line.split('=')[0]
        text=re.sub(r'^'+re.escape(key)+r'=.*$',line,text,flags=re.M) if re.search(r'^'+re.escape(key)+'=',text,re.M) else text+'\n'+line
    physics.parent.mkdir(parents=True,exist_ok=True);physics.write_text(text+'\n')

def allowed(lib):
    result=not bool(lib.get('rules'))
    for rule in lib.get('rules',[]):
        platform=rule.get('os',{})
        if platform.get('name','windows')!='windows' or platform.get('arch','x86_64') not in ('x86_64','amd64'):continue
        result=rule['action']=='allow'
    return result and lib.get('include_in_classpath',True)

def live():
    setup();version=json.loads((META/f'versions/{MC}/{MC}.json').read_text())
    cp=[]
    for lib in version['libraries']:
        if not allowed(lib):continue
        artifact=lib.get('downloads',{}).get('artifact',{}).get('path')
        if artifact is None:
            group,name,ver=lib['name'].split(':')[:3];artifact=f'{group.replace(".","/")}/{name}/{ver}/{name}-{ver}.jar'
        jar=META/'libraries'/artifact
        if not jar.is_file():raise FileNotFoundError(jar)
        cp.append(str(jar))
    cp.append(str(META/f'versions/{MC}/{MC}.jar'))
    native=META/f'natives/{MC}'
    args=[str(JAVA/'java.exe'),'-Xmx5G','-XX:ActiveProcessorCount=6','-Djava.library.path='+str(native),'-Dorg.lwjgl.system.SharedLibraryExtractPath='+str(native),'-Djna.tmpdir='+str(GAME/'natives'),'-Dminecraft.launcher.brand=BattleMusicVerification','-cp',os.pathsep.join(cp),version['mainClass'],'--username','MusicCheck','--version',MC,'--gameDir',str(GAME),'--assetsDir',str(META/'assets'),'--assetIndex',version['assetIndex']['id'],'--uuid','00000000000000000000000000000004','--accessToken','0','--userType','legacy','--versionType','release','--width','854','--height','480','--quickPlaySingleplayer','BattleMusicVerification']
    reportfile=GAME/'battlemusic-live.json'
    if reportfile.exists():reportfile.unlink()
    reports=ROOT/'reports';reports.mkdir(exist_ok=True)
    logfile=reports/'live-client.log'
    receipt={'success':False,'game_directory':str(GAME),'anonymous_identity':True,'source_region_and_playerdata_directories_copied':False,'source_level_dat_copied':True,'jar_sha256':hashlib.sha256((ROOT/f'dist/battlemusic-{VERSION}.jar').read_bytes()).hexdigest()}
    started=time.monotonic()
    with logfile.open('w',encoding='utf-8') as stream:
        proc=subprocess.Popen(args,cwd=GAME,stdout=stream,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
        (reports/'live-process.json').write_text(json.dumps({'pid':proc.pid,'game_directory':str(GAME)}))
        print('Owned isolated client PID',proc.pid,flush=True)
        try:
            while proc.poll() is None and time.monotonic()-started<600:
                if reportfile.exists():receipt.update(json.loads(reportfile.read_text()));break
                tail=logfile.read_text(encoding='utf-8',errors='replace')[-20000:]
                if any(s in tail for s in ('Mixin apply failed','Mixin transformation of','Failed to encode packet','Uncaught exception in thread "Cobblemon Showdown"')):receipt['error']='Runtime initialization/packet failure; see live-client.log';break
                time.sleep(3)
        finally:
            receipt['terminated_by_harness']=proc.poll() is None
            if proc.poll() is None:proc.terminate();proc.wait(timeout=30)
        receipt['exit_code']=proc.returncode;receipt['elapsed_seconds']=round(time.monotonic()-started,1)
    (reports/'live-client.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print(json.dumps(receipt,indent=2));return receipt['success']

if __name__=='__main__':sys.exit(0 if live() else 1)
