"""Canonical explicit assignments. Filenames are never interpreted at runtime."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MUSIC = Path.home() / 'Desktop/MusicMod'
REJ = Path.home() / 'Documents/Rejuvenation Battle Theme Pack V13.5/rejuvenation v14 music port/Audio/BGM'

GROUPS = [
 ('gen1', 'All_Gen1_Legendaries', 'articuno zapdos moltres mewtwo mew'),
 ('gen6', 'All_Gen6_Legendaries', 'xerneas yveltal zygarde diancie hoopa volcanion'),
 ('regi', 'All_Regi', 'regirock regice registeel regigigas regieleki regidrago'),
 ('weather_trio', 'All_Weather_Trio', 'kyogre groudon rayquaza'),
 ('arceus', 'Arceus', 'arceus'), ('calyrex', 'Calyrex', 'calyrex'),
 ('cosmog_family', 'Cosmog_Cosmoem_Lunala_Solgaleo', 'cosmog cosmoem lunala solgaleo'),
 ('deoxys', 'Deoxys', 'deoxys'), ('dialga_palkia', 'Dialga_Palkia', 'dialga palkia'),
 ('entei', 'Entei', 'entei'), ('eternatus', 'Eternatus', 'eternatus'),
 ('galar_dogs', 'Galar_Dogs', 'zacian zamazenta'),
 ('galar_horses', 'Galarian_Horses', 'glastrier spectrier'),
 ('giratina', 'Giratina', 'giratina'), ('ho_oh', 'Ho-oh', 'hooh'),
 ('lake_guardians', 'Lake_Guardian_Trio', 'uxie mesprit azelf'),
 ('loyal_three', 'Loyal_Three', 'okidogi munkidori fezandipiti'), ('lugia', 'Lugia', 'lugia'),
 ('miraidon_koraidon', 'Miraidon_Koraidon', 'miraidon koraidon'),
 ('necrozma', 'Necrozma', 'necrozma'), ('ogerpon', 'Ogerpon', 'ogerpon'),
 ('paradox_future_trio', 'Paradox_Future_Gen5_Legendaries', 'ironleaves ironcrown ironboulder'),
 ('paradox_past_trio', 'Paradox_Past_Gen3_Legendaries', 'walkingwake ragingbolt gougingfire'),
 ('paradox', 'Paradox', 'greattusk screamtail brutebonnet fluttermane slitherwing sandyshocks roaringmoon irontreads ironbundle ironhands ironjugulis ironmoth ironthorns ironvaliant'),
 ('pecharunt', 'Pecharunt', 'pecharunt'), ('raikou', 'Raikou', 'raikou'),
 ('remaining_gen3', 'Remaining_Gen3_Legendaries', 'latias latios jirachi'),
 ('remaining_gen4', 'Remaining_Gen4_Legendaries', 'heatran cresselia darkrai phione manaphy shaymin'),
 ('remaining_gen5', 'Remaining_Gen5_Legendaries', 'cobalion terrakion virizion tornadus thundurus landorus keldeo meloetta genesect enamorus'),
 ('suicune', 'Suicune', 'suicune'), ('tao_trio', 'Tao_Trio', 'reshiram zekrom kyurem'),
 ('tapu', 'Tapu_Quartet', 'tapukoko tapulele tapubulu tapufini'),
 ('terapagos', 'Terapagos', 'terapagos'),
 ('treasures_of_ruin', 'Treasures_Of_Ruin', 'wochien chienpao tinglu chiyu'),
 ('ultra_beasts', 'Ultra_Beasts', 'nihilego buzzwole pheromosa xurkitree celesteela kartana guzzlord poipole naganadel stakataka blacephalon'),
 ('victini', 'Undertale Finale Dual Mix', 'victini'),
]

def catalog():
    tracks, assignments = [], []
    for gen in range(1, 10):
        tracks.append(dict(id=f'wild.gen{gen}', source=str(MUSIC / f'Gen{gen}.mp3'), imported=f'Gen{gen}.mp3', title=f'Generation {gen} Wild Battle'))
    for ident, filename, species in GROUPS:
        imported = 'Victini_Undertale.mp3' if ident == 'victini' else filename + '.mp3'
        tracks.append(dict(id='legendary.'+ident, source=str(MUSIC / 'Legendary' / (filename+'.mp3')), imported=imported, title=filename.replace('_',' ')))
        assignments.append(dict(id=ident, species=['cobblemon:'+s for s in species.split()], sound='battlemusic:legendary.'+ident))
    for ident, source, imported, species in [
        ('celebi','Battle- Aquatic.mp3','Celebi.mp3','celebi'),
        ('gen7_specials','Battle- Cyanide.mp3','Gen7_Specials.mp3','typenull silvally magearna marshadow zeraora meltan melmetal'),
        ('gen8_specials','Battle- Popstar.mp3','Gen8_Specials.mp3','kubfu urshifu zarude')]:
        tracks.append(dict(id='legendary.'+ident, source=str(REJ / source), imported=imported, title=imported.removesuffix('.mp3').replace('_',' ')))
        assignments.append(dict(id=ident, species=['cobblemon:'+s for s in species.split()], sound='battlemusic:legendary.'+ident))
    tracks += [dict(id='legendary.galarian_birds', source=str(MUSIC / 'Legendary/Galarian_Legendary_Birds.mp3'), imported='Galarian_Legendary_Birds.mp3', title='Galarian Legendary Birds'),
               dict(id='legendary.victini_alternative', source=str(MUSIC / 'Legendary/Victini.mp3'), imported='Victini_Alternative.mp3', title='Victini Alternative')]
    assignments.append(dict(id='galarian_birds', species=['cobblemon:'+s for s in ('articuno','zapdos','moltres')], aspects=['galarian'], sound='battlemusic:legendary.galarian_birds', priority=300))
    for ident, resource in [('gym','gym_battle_johto'),('elite_four','hgss/trainer_kanto'),('champion','champion_battle')]:
        tracks.append(dict(id='trainer.kanto.'+ident, resource='cobblemon:battle/'+resource, title='Kanto '+ident.replace('_',' ').title()))
    return dict(schema_version=1, tracks=tracks, assignments=assignments)

def generate():
    data=catalog(); (ROOT/'catalog').mkdir(exist_ok=True)
    (ROOT/'catalog/music.json').write_text(json.dumps(data,indent=2)+'\n')
    rules=ROOT/'datapack/data/battlemusic/battlemusic/rules'; rules.mkdir(parents=True,exist_ok=True)
    # This directory is exclusively generated from this module, never hand-edited.
    for old in rules.rglob('*.json'): old.unlink()
    def write(name,kind,match,sound,priority):
        path=rules/(name+'.json'); path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(dict(schema_version=1,kind=kind,priority=priority,match=match,sound=sound,transition_ticks=30),indent=2)+'\n')
    for gen in range(1,10): write(f'generation/gen{gen}','base',dict(battle_type='wild',labels=[f'gen{gen}']),f'battlemusic:wild.gen{gen}',100)
    for assignment in data['assignments']:
        match={key:assignment[key] for key in ('species','aspects') if key in assignment}
        for kind in ('base','entrance'):
            write(f'{kind}/{assignment["id"]}',kind,dict(battle_type='wild',**match) if kind=='base' else match,assignment['sound'],assignment.get('priority',200))
    gyms='brock misty ltsurge erika koga sabrina blaine giovanni'.split()
    for trainer in gyms: write('trainer/kanto_'+trainer,'base',dict(battle_type='trainer',trainers=['rctmod:kanto_'+trainer]),'battlemusic:trainer.kanto.gym',300)
    for trainer in 'lorelei bruno agatha lance'.split(): write('trainer/kanto_league_'+trainer,'base',dict(battle_type='trainer',trainers=['rctmod:kanto_league_'+trainer]),'battlemusic:trainer.kanto.elite_four',300)
    write('trainer/kanto_champion_blue','base',dict(battle_type='trainer',trainers=['rctmod:kanto_champion_blue']),'battlemusic:trainer.kanto.champion',300)
    (ROOT/'datapack/pack.mcmeta').write_text(json.dumps({'pack':{'pack_format':48,'description':'Battle Music: generation, special Pokemon and Kanto league rules'}},indent=2)+'\n')
    print(f'Generated {len(data["tracks"])} sound events and {len(list(rules.rglob("*.json")))} rules')

if __name__=='__main__': generate()
