"""Copy the four confirmed additions to the user's canonical Desktop library."""
from pathlib import Path
import hashlib, shutil

def copy_additions():
    library = Path.home() / 'Desktop/MusicMod/Legendary'
    rejuvenation = Path.home() / 'Documents/Rejuvenation Battle Theme Pack V13.5/rejuvenation v14 music port/Audio/BGM'
    sources = {
        'Celebi.mp3': rejuvenation / 'Battle- Aquatic.mp3',
        'Gen7_Specials.mp3': rejuvenation / 'Battle- Cyanide.mp3',
        'Gen8_Specials.mp3': rejuvenation / 'Battle- Popstar.mp3',
        'Victini_Undertale.mp3': library / 'Undertale Finale Dual Mix.mp3',
    }
    digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    for name, source in sources.items():
        if not source.is_file(): raise FileNotFoundError(source)
        target = library / name
        if target.exists() and digest(target) != digest(source):
            raise RuntimeError(f'Refusing to overwrite a different existing track: {target}')
    for name, source in sources.items():
        target = library / name
        if not target.exists(): shutil.copy2(source, target)
        assert digest(source) == digest(target)
        print(f'Copied and verified: {target}')

if __name__ == '__main__': copy_additions()
