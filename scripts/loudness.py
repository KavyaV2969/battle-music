"""Measured two-pass normalization of private music; originals are preserved."""
import json, math, re, subprocess

PROFILE = dict(version=1, integrated_lufs=-16.0, true_peak_dbtp=-2.0, loudness_range_lu=20.0,
               leading_silence_threshold_db=-60.0)

def leading_silence(ffmpeg, path):
    run=subprocess.run([ffmpeg,'-nostdin','-hide_banner','-i',str(path),'-t','15',
        '-af','silencedetect=noise=-60dB:d=0.02','-f','null','NUL'],capture_output=True,text=True,check=True)
    events=re.findall(r'silence_(start|end): ([0-9.]+)',run.stderr)
    if events and events[0][0]=='start' and float(events[0][1])<.001:
        for event,value in events[1:]:
            if event=='end': return max(0.0,float(value)-.005)
        raise ValueError(f'No audible intro found in first 15 seconds: {path}')
    return 0.0

def measure(ffmpeg,path,trim=0.0):
    prefix=f'atrim=start={trim:.6f},asetpts=PTS-STARTPTS,' if trim else ''
    run=subprocess.run([ffmpeg,'-nostdin','-hide_banner','-i',str(path),'-vn','-af',
        prefix+'loudnorm=I=-16:TP=-2:LRA=20:print_format=json','-f','null','NUL'],capture_output=True,text=True,check=True)
    match=re.search(r'\{\s*"input_i".*?\}',run.stderr,re.S)
    if not match:raise ValueError(f'No loudness measurement: {path}')
    result=json.loads(match.group())
    if not all(math.isfinite(float(result[k])) for k in ('input_i','input_tp','input_lra','input_thresh','target_offset')):
        raise ValueError(f'Non-finite loudness: {path}')
    return result

def normalization_filter(measured,trim):
    return (f'atrim=start={trim:.6f},asetpts=PTS-STARTPTS,'
        'loudnorm=I=-16:TP=-2:LRA=20:linear=true:'
        f'measured_I={measured["input_i"]}:measured_TP={measured["input_tp"]}:'
        f'measured_LRA={measured["input_lra"]}:measured_thresh={measured["input_thresh"]}:'
        f'offset={measured["target_offset"]}')
