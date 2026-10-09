"""Dedicated Fabric environment initialization, without opening a port or accepting an EULA."""
import hashlib,json,os,shutil,subprocess,urllib.request,zipfile
from catalog import ROOT
from build import JAVA,VERSION
from live import META,MC,allowed

def smoke():
    game=ROOT/'integration/server';game.mkdir(parents=True,exist_ok=True)
    version=json.loads((META/f'versions/{MC}/{MC}.json').read_text())
    spec=version['downloads']['server'];bundle=ROOT/'build/server-bundle.jar'
    if not bundle.exists():urllib.request.urlretrieve(spec['url'],bundle)
    if hashlib.sha1(bundle.read_bytes()).hexdigest()!=spec['sha1']:raise RuntimeError('Server download hash mismatch')
    server=ROOT/'build/minecraft-server.jar'
    with zipfile.ZipFile(bundle) as archive:
        name=next(n for n in archive.namelist() if n.startswith('META-INF/versions/') and n.endswith('.jar'))
        server.write_bytes(archive.read(name))
    mods=game/'mods';mods.mkdir(exist_ok=True)
    for pattern in ('Cobblemon-fabric-*.jar','fabric-api-*.jar','fabric-language-kotlin-*.jar'):
        source=next((ROOT.parent/'mods').glob(pattern));shutil.copyfile(source,mods/source.name)
    jar=ROOT/f'dist/battlemusic-{VERSION}.jar';shutil.copyfile(jar,mods/jar.name)
    cp=[]
    for lib in version['libraries']:
        if not allowed(lib):continue
        parts=lib['name'].split(':')[:3]
        artifact=lib.get('downloads',{}).get('artifact',{}).get('path')
        if artifact is None:artifact=f'{parts[0].replace(".","/")}/{parts[1]}/{parts[2]}/{parts[1]}-{parts[2]}.jar'
        cp.append(str(META/'libraries'/artifact))
    cp.append(str(server))
    loadlog=game/'classes.log'
    args=[str(JAVA/'java.exe'),'-Xmx3G','-XX:ActiveProcessorCount=4','-Xlog:class+load:file=classes.log','-cp',os.pathsep.join(cp),'net.fabricmc.loader.impl.launch.knot.KnotServer','--initSettings','nogui']
    result=subprocess.run(args,cwd=game,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180,creationflags=subprocess.CREATE_NO_WINDOW)
    (ROOT/'reports/dedicated-server.log').write_bytes(result.stdout)
    output=result.stdout.decode('utf-8',errors='replace');classes=loadlog.read_text(errors='replace')
    client_loaded='dev.battlemusic.client.' in classes
    report={'success':result.returncode==0 and 'Battle Music 0.1.0 ready' in output and not client_loaded,
        'mode':'Fabric dedicated server --initSettings; no world started, port opened, or EULA accepted',
        'client_classes_loaded':client_loaded,'exit_code':result.returncode,'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
    (ROOT/'reports/dedicated-server.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
    return report['success']

if __name__=='__main__':raise SystemExit(0 if smoke() else 1)
