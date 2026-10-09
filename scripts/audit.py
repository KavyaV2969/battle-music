"""Audit installed species/form definitions and the canonical default assignments."""
from pathlib import Path
import json, re, sys, zipfile
from catalog import ROOT
sys.path.insert(0,str(ROOT/'.tools'))

def audit(profile=None):
    from jsonschema import Draft202012Validator
    profile=Path(profile or ROOT.parent)
    catalog=json.loads((ROOT/'catalog/music.json').read_text())
    assigned={s for a in catalog['assignments'] for s in a['species']}
    identifiers={'battlemusic:'+t['id'] for t in catalog['tracks']}
    schema=json.loads((ROOT/'schemas/rule.schema.json').read_text())
    rule_files=list((ROOT/'datapack').rglob('*.json'))
    for path in rule_files:
        rule=json.loads(path.read_text());Draft202012Validator(schema).validate(rule)
        if rule['sound'] is not None and rule['sound'] not in identifiers:raise AssertionError(f'{path}: unknown sound')
    examples=list((ROOT/'examples').glob('*.json'))
    for path in examples:Draft202012Validator(schema).validate(json.loads(path.read_text()))
    rows=[];missing=[];known=set();trainers=set();definition_count=0
    archives=sorted((profile/'mods').glob('*.jar'))+sorted((profile/'datapacks').glob('*.zip'))+sorted((profile/'saves/New World/datapacks').glob('*.zip'))
    for path in archives:
        with zipfile.ZipFile(path) as z:
            for name in z.namelist():
                if re.match(r'data/[^/]+/trainers/[^/]+\.json$',name):trainers.add(name.split('/')[1]+':'+Path(name).stem)
                if not re.match(r'data/[^/]+/species/.+\.json$',name):continue
                obj=json.loads(z.read(name));definition_count+=1
                ident=name.split('/')[1]+':'+Path(name).stem;known.add(ident)
                for form in [None]+obj.get('forms',[]):
                    labels=set(obj.get('labels',[]))|set((form or {}).get('labels',[]))
                    if not labels.intersection({'legendary','mythical','paradox','ultra_beast'}):continue
                    row={'species':ident,'form':(form or {}).get('name','Normal'),'source':path.name,'covered':ident in assigned}
                    rows.append(row)
                    if not row['covered']:missing.append(row)
    unknown=sorted(assigned-known)
    trainer_assignments={trainer for path in rule_files for trainer in json.loads(path.read_text()).get('match',{}).get('trainers',[])}
    unknown_trainers=sorted(trainer_assignments-trainers)
    report=dict(success=not missing and not unknown and not unknown_trainers,sound_events=len(identifiers),rules=len(rule_files),validated_examples=len(examples),verified_trainer_assignments=len(trainer_assignments),unknown_trainers=unknown_trainers,species_definitions=definition_count,special_definitions=len(rows),unique_special_species=len({r['species'] for r in rows}),uncovered=missing,unknown_assignments=unknown,coverage=rows)
    (ROOT/'reports').mkdir(exist_ok=True)
    (ROOT/'reports/coverage.json').write_text(json.dumps(report,indent=2)+'\n')
    (ROOT/'reports/coverage.md').write_text('# Installed music coverage\n\n'+f'{len(rows)} special species/form definitions audited across {len(archives)} installed archives.\n\n'+f'{len(identifiers)} sound events; {len(rule_files)} default rules; {len(missing)} uncovered definitions.\n\n'+('\n'.join('- '+str(r) for r in missing) if missing else 'Every audited legendary, mythical, paradox and Ultra Beast definition has an explicit assignment.')+'\n\nUnknown species assignments: '+str(unknown)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='coverage'},indent=2))
    if not report['success']:raise SystemExit(1)
    return report
if __name__=='__main__':audit()
