"""Inspect licensed dictionary/CEFR overlap before freezing the selection."""
import csv
import json
import re
import zipfile
from collections import Counter, defaultdict
from pathlib import Path

RAW = Path(__file__).resolve().parents[1] / 'raw'
POS = {'n':'noun', 'v':'verb', 'a':'adjective', 's':'adjective', 'r':'adverb'}

def load_levels():
    levels = {}
    for name, label in [('cefrj.csv', 'CEFR-J 1.5'), ('octanove.csv', 'Octanove 1.0')]:
        for r in csv.DictReader((RAW/name).read_text(encoding='utf-8-sig').splitlines()):
            for word in r['headword'].split('/'):
                if re.fullmatch('[a-z]+(?:[- ][a-z]+)*', word):
                    levels.setdefault((word, r['pos']), (r['CEFR'], label))
    return levels

def load_wordnet():
    entries = defaultdict(list)
    with zipfile.ZipFile(RAW/'wordnet.zip') as z:
        for pos in ['noun', 'verb', 'adj', 'adv']:
            for line in z.read(f'wordnet/data.{pos}').decode().splitlines():
                if not line or line[0] == ' ': continue
                meta, gloss = line.split('|', 1)
                parts = meta.split()
                count = int(parts[3], 16)
                words = [w.replace('_',' ') for w in parts[4:4+count*2:2]]
                examples = re.findall(r'"([^"]+)"', gloss)
                definition = gloss.split('; "')[0].strip()
                for word in words:
                    entries[(word, POS[parts[2]])].append(dict(offset=parts[0], pos=parts[2], definition=definition, examples=examples, synonyms=words))
    return entries

def load_dictionary():
    return [dict(json.loads(line), sourceFile=p.name, sourceLine=i) for p in sorted(RAW.glob('*.jsonl'))
            for i,line in enumerate(p.read_text(encoding='utf-8').splitlines(),1) if line.strip()]

def candidates():
    levels, wn = load_levels(), load_wordnet()
    rows = []
    for r in load_dictionary():
        key = (r['headword'], POS.get(r['pos']))
        if key not in levels or levels[key][0] not in ['B1','B2','C1','C2']: continue
        if not r.get('senses_vi') or not r.get('pron'): continue
        if r.get('extra',{}).get('llm_confidence') == 'low': continue
        examples = [(s,e) for s in wn.get(key,[]) for e in s['examples'] if re.search(r'\b'+re.escape(key[0])+r'\b',e,re.I)]
        rows.append(dict(r, level=levels[key][0], levelSource=levels[key][1], wordnet=wn.get(key,[]), wnExamples=examples))
    return rows

if __name__ == '__main__':
    rows = candidates()
    print('Candidates',len(rows),Counter(r['level'] for r in rows))
    print('With exact examples',Counter(r['level'] for r in rows if r['wnExamples']))
    for r in rows[:12]:
        print(r['headword'],r['level'],r.get('senses_en'),r['senses_vi'],r['wnExamples'][:1])
