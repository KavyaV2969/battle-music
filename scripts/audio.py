"""Import private originals, encode Vorbis, and build the client resource pack."""
from pathlib import Path
import concurrent.futures, hashlib, json, shutil, subprocess, sys
from catalog import ROOT
from loudness import PROFILE,leading_silence,measure,normalization_filter
sys.path.insert(0,str(ROOT/'.tools'))

def audio():
    import imageio_ffmpeg
    from mutagen import File
    from mutagen.oggvorbis import OggVorbis
    from build import zip_tree,VERSION
    data=json.loads((ROOT/'catalog/music.json').read_text())
    source=ROOT/'audio-src';source.mkdir(exist_ok=True)
    pack=ROOT/'build/audio-pack';assets=pack/'assets/battlemusic';assets.mkdir(parents=True,exist_ok=True)
    receipt=ROOT/'build/audio-receipt.json'
    previous=json.loads(receipt.read_text()) if receipt.exists() else {}
    ffmpeg=imageio_ffmpeg.get_ffmpeg_exe()
    def encode(track):
        ident=track['id'];original=Path(track['source']);imported=source/track['imported']
        if not original.is_file():raise FileNotFoundError(original)
        digest=hashlib.sha256(original.read_bytes()).hexdigest()
        if not imported.exists() or hashlib.sha256(imported.read_bytes()).hexdigest()!=digest:shutil.copy2(original,imported)
        output=assets/'sounds'/Path(ident.replace('.','/')).with_suffix('.ogg');output.parent.mkdir(parents=True,exist_ok=True)
        title=track['title'];tags=File(imported,easy=True);artist=(tags.get('artist') or [''])[0] if tags else ''
        cached=previous.get(ident,{})
        if not output.exists() or cached.get('source_sha256')!=digest or cached.get('normalization')!=PROFILE:
            trim=leading_silence(ffmpeg,imported);before=measure(ffmpeg,imported,trim)
            subprocess.run([ffmpeg,'-nostdin','-hide_banner','-loglevel','error','-y','-i',str(imported),'-map_metadata','0','-vn','-af',normalization_filter(before,trim),'-ac','2','-ar','44100','-c:a','libvorbis','-q:a','5','-threads','1','-metadata','title='+title,'-metadata','artist='+artist,str(output)],check=True)
            after=measure(ffmpeg,output)
            cached=dict(normalization=PROFILE,leading_silence_removed_seconds=round(trim,6),
                input_lufs=float(before['input_i']),output_lufs=float(after['input_i']),
                output_true_peak_dbtp=float(after['input_tp']))
        if abs(cached['output_lufs']-PROFILE['integrated_lufs'])>.75 or cached['output_true_peak_dbtp']>-1.0:
            raise ValueError(f'Normalized loudness outside tolerance: {ident}: {cached}')
        decoded=OggVorbis(output)
        assert decoded.info.channels==2 and decoded.info.sample_rate==44100 and decoded.info.length>0
        return ident,dict(source_sha256=digest,ogg_sha256=hashlib.sha256(output.read_bytes()).hexdigest(),seconds=round(decoded.info.length,3),channels=decoded.info.channels,title=title,artist=artist,
            **{k:cached[k] for k in ('normalization','leading_silence_removed_seconds','input_lufs','output_lufs','output_true_peak_dbtp')})
    tracks=[t for t in data['tracks'] if 'source' in t]
    result={}
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for future in concurrent.futures.as_completed([pool.submit(encode,t) for t in tracks]):
            ident,row=future.result();result[ident]=row;print('Converted',ident,flush=True)
    sounds={t['id']:{'sounds':[{'name':t.get('resource','battlemusic:'+t['id'].replace('.','/')),'stream':True}]} for t in data['tracks']}
    (assets/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
    # Music Notification 3 reads this resource stack, keyed by the resolved Ogg resource.
    notification=pack/'assets/musicnotification';notification.mkdir(parents=True,exist_ok=True)
    metadata={'battlemusic:'+ident.replace('.','/'):{'title':row['title'],'author':row['artist'],
        'album':'Private Battle Music','customId':'battlemusic:'+ident} for ident,row in result.items()}
    (notification/'musics.json').write_text(json.dumps(metadata,indent=2)+'\n')
    (pack/'pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':34,'description':'Private Battle Music audio: generation and special encounter themes'}},indent=2)+'\n')
    receipt.write_text(json.dumps(result,indent=2,sort_keys=True)+'\n')
    loudness_report=dict(success=True,target=PROFILE,tracks={k:{f:v for f,v in row.items() if f in ('input_lufs','output_lufs','output_true_peak_dbtp','leading_silence_removed_seconds')} for k,row in sorted(result.items())})
    (ROOT/'reports/loudness.json').write_text(json.dumps(loudness_report,indent=2)+'\n')
    (ROOT/'catalog/imports.json').write_text(json.dumps({'schema_version':1,'encoding':{'codec':'Vorbis','quality':5,'sample_rate':44100,'channels':2},'tracks':result},indent=2,sort_keys=True)+'\n')
    (ROOT/'dist').mkdir(exist_ok=True);zip_tree(ROOT/'dist'/f'battlemusic-audio-{VERSION}.zip',[pack])
    entries=['# Sound Physics Remastered 1.5.1: 0 bypasses physics; it does not mute playback.']
    entries += ['battlemusic\\:'+t['id']+'=0' for t in data['tracks']]
    entries += [r'cobblemon\:battle.pvw.default=0',r'cobblemon\:battle.pvn.default=0']
    (ROOT/'compat').mkdir(exist_ok=True)
    (ROOT/'compat/sound_rates.properties').write_text('\n'.join(entries)+'\n')
    print('Audio pack ready:',len(result),'Vorbis tracks,',len(sounds),'events',flush=True)
if __name__=='__main__':audio()
