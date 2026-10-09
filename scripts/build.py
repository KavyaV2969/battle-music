"""Offline build against the installed intermediary ABI, using Java 21 directly."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, sys, zipfile
from prepare import ROOT, prepare

VERSION='0.1.0'
JAVA=Path(os.environ.get('JAVA_HOME',r'C:\Program Files\Java\jdk-21'))/'bin'
def zip_tree(target, roots):
    target.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for root in roots:
            for path in sorted(root.rglob('*')):
                if not path.is_file():continue
                info=zipfile.ZipInfo(path.relative_to(root).as_posix(),date_time=(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
                archive.writestr(info,path.read_bytes())

def compile_sources(folder, sources, cp):
    folder.mkdir(parents=True,exist_ok=True)
    args=['--release','21','-encoding','UTF-8','-proc:none','-classpath',cp,'-d',str(folder)]+[str(s) for s in sources]
    argfile=ROOT/'build/javac.args';argfile.write_text('\n'.join('"'+s.replace('\\','/').replace('"','\\"')+'"' for s in args),encoding='utf-8')
    subprocess.run([str(JAVA/'javac.exe'),'@'+str(argfile)],check=True)

def build(skip_audio=False):
    if not (ROOT/'build/deps/client-intermediary.jar').exists():prepare()
    cp=os.pathsep.join(str(p) for p in sorted((ROOT/'build/deps').glob('*.jar')))
    classes=ROOT/'build/classes';classes.mkdir(parents=True,exist_ok=True)
    # Only remove the owned compilation directory, so deleted sources cannot survive into an artifact.
    if classes.exists():shutil.rmtree(classes)
    compile_sources(classes,list((ROOT/'src/main/java').rglob('*.java'))+list((ROOT/'src/client/java').rglob('*.java')),cp)
    dist=ROOT/'dist';dist.mkdir(exist_ok=True)
    jar=dist/f'battlemusic-{VERSION}.jar';zip_tree(jar,[classes,ROOT/'src/main/resources'])
    tests=list((ROOT/'src/test/java').rglob('*.java'))
    if tests:
        testclasses=ROOT/'build/test-classes';compile_sources(testclasses,tests,str(classes)+os.pathsep+cp)
        subprocess.run([str(JAVA/'java.exe'),'-cp',os.pathsep.join([str(testclasses),str(classes),cp]),'dev.battlemusic.Verification',str(ROOT)],check=True)
    fixtures=list((ROOT/'src/integration/java').rglob('*.java'))
    if fixtures:
        fixtureclasses=ROOT/'build/fixture-classes';compile_sources(fixtureclasses,fixtures,str(classes)+os.pathsep+cp)
        zip_tree(ROOT/'integration/battlemusic-verification.jar',[fixtureclasses,ROOT/'src/integration/resources'])
    zip_tree(dist/f'battlemusic-defaults-{VERSION}.zip',[ROOT/'datapack'])
    if not skip_audio:
        from audio import audio
        audio()
    artifacts={p.name:dict(bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in dist.glob('*') if p.suffix in ('.jar','.zip')}
    (dist/'manifest.json').write_text(json.dumps(dict(version=VERSION,artifacts=artifacts),indent=2)+'\n')
    print('Build complete:',jar)

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--skip-audio',action='store_true');args=ap.parse_args();build(args.skip_audio)
