"""Freeze a bounded licensed lexical selection. Run fetch_sources.py first.

This is the network-independent selection stage; build_seed.py only reads its
checked-in result. English/Vietnamese pairs must be aligned upstream, or have
exactly one gloss and one translation. No positional pairing of polysemy.
"""
import hashlib
import csv
import json
import re
from collections import Counter
from pathlib import Path
from audit_candidates import RAW, POS, load_dictionary, load_levels, load_wordnet

ROOT = Path(__file__).resolve().parents[2]
CURATED = ROOT / 'data_pipeline/curated'
DICT_COMMIT = '4d6e92e8bcf8e3e762410c2b0a9f98fea8e62e5b'
CEFR_COMMIT = 'd4e45b75b38f27b30dfc5c44d8c571aec7e7092f'
STOP = set('a an the to of in on and or for with by from as that is be being something someone one any person thing state act make having have relating related'.split())
EXCLUDE = set('fuck fucking shit bullshit piss suck cunt bastard whore lust horny goblin gnome ogre yelp yummy mama mamma mustache moustache fax facsimile daisy gook'.split())
# Exclusions are curricular relevance and register decisions, not CEFR ratings.
EXCLUDE |= set('aborigine blackboard ladle loin loincloth boar stallion eunuch pious piety chaste chastity wig wigwam wench chivalry swain chivalrous thou thee thy thine hath hast henceforth lest yonder lest beget begat woe woe betwixt betoken ye yea yelp whoa yippee alas ah aha hooray hurray yummy'.split())
EXCLUDE |= set('ad-lib armory armoury astringent behest blitz blubber brimstone buccaneer buffoonery cay cloister cosset confection contraption crony divination exorcism exorcist flightiness flit frolic glint glitz gout happenstance headmistress hotelier hull impresario incise indolence ingrate ingratiate innuendo laryngitis leaven maggot mangle mien moor munificence ordain phantom philistine plutocrat posturing predilection prurience pyre rapture remonstrance revile revulsion romp romper shamble singe slither snobbery soliloquize solipsism squirm'.split())

def tokens(s):
    return set(re.findall('[a-z]+', s.lower())) - STOP

def overlap(a, b):
    a, b = tokens(a), tokens(b)
    return len(a & b) / max(1, len(a | b))

def editorial_words():
    words = set()
    for filename in ['legacy_demo.psv', 'ielts_editorial.psv']:
        for line in (CURATED / filename).read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'): words.add(line.split('|')[1])
    return words

def classify(word, definition, inventory, taxonomy):
    # Exact lemma is stronger than dictionary-domain keywords. Retain the
    # general topic rather than assigning an unsupported specialist subject.
    exact = [t for t, terms in taxonomy.items() if word in terms]
    if exact: return exact[:2]
    words = tokens(definition + ' ' + inventory)
    ranked = sorted(((len(words & terms), t) for t, terms in taxonomy.items() if t not in ['general','academic']), reverse=True)
    return [ranked[0][1]] if ranked and ranked[0][0] >= 1 else ['general']

def main():
    levels, wn = load_levels(), load_wordnet()
    taxonomy = {t:set(terms.split()) for t,terms in json.loads((CURATED/'topics.json').read_text()).items()}
    original = editorial_words()
    original_pos = {}
    for filename in ['legacy_demo.psv','ielts_editorial.psv']:
        for line in (CURATED/filename).read_text(encoding='utf-8').splitlines():
            if line.strip() and not line.startswith('#'):
                fields = line.split('|')
                original_pos.setdefault(fields[1],fields[2])
    variant_groups = {}
    for r in csv.DictReader((RAW/'cefrj.csv').read_text(encoding='utf-8-sig').splitlines()):
        variants = [v for v in r['headword'].split('/') if re.fullmatch('[a-z]+(?:[- ][a-z]+)*',v)]
        if len(variants) > 1:
            for v in variants: variant_groups[v] = variants
    all_dictionary = load_dictionary()
    known_words = {r['headword'] for r in all_dictionary}
    for word in sorted(known_words):
        variant = re.sub(r'isation\b','ization',word)
        variant = re.sub(r'ise\b','ize',variant)
        variant = re.sub(r'isement\b','izement',variant)
        if variant != word and variant in known_words:
            group = list(dict.fromkeys(variant_groups.get(word,[word])+variant_groups.get(variant,[variant])))
            for v in group: variant_groups[v] = group
    by_word = {}
    for r in all_dictionary:
        if r.get('pron'): by_word.setdefault((r['headword'],POS.get(r['pos'])),r)
    # This is kept separately so editorial definitions do not inherit lexical
    # provenance from a dictionary merely because an IPA is reused.
    ipa = {}
    for word in sorted(original):
        variants = [word, word.replace('isation','ization'), word.replace('ise','ize'), word.replace('fibre','fiber'), word.replace('fertiliser','fertilizer'), word.replace('artefact','artifact')]
        found = next((by_word[(v,original_pos[word])] for v in variants if (v,original_pos[word]) in by_word),None)
        if found: ipa[word] = dict(ipaAm=found['pron'], entryId=found['id'], sourceFile=found['sourceFile'], sourceLine=found['sourceLine'])
    (CURATED/'editorial_pronunciations.json').write_text(json.dumps(ipa,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
    choices = []
    for r in all_dictionary:
        word, pos = r['headword'], POS.get(r['pos'])
        if word in original or word in EXCLUDE or not re.fullmatch('[a-z]+(?:[- ][a-z]+)*',word): continue
        if (word,pos) not in levels or levels[(word,pos)][0] not in ['B1','B2','C1','C2']: continue
        if r.get('extra',{}).get('llm_confidence') in ['low','medium']: continue
        aligned = r.get('senses_en') and len(r['senses_en']) == len(r.get('senses_vi',[]))
        single = len(r.get('gloss_en',[])) == len(r.get('senses_vi',[])) == 1
        if not aligned and not single: continue
        en = r['senses_en'] if aligned else r['gloss_en']
        # Pick a source-aligned sense with a usable WordNet example if present.
        best = None
        for i, definition in enumerate(en):
            for syn in wn.get((word,pos),[]):
                match = overlap(definition,syn['definition'])
                if match < .55: continue
                for example in syn['examples']:
                    if not re.search(r'\b'+re.escape(word)+r'\b',example,re.I): continue
                    if len(example.split()) < 6 or len(example) > 220: continue
                    # Fragments and imperative usage snippets are not promoted
                    # to sentence exercises. Examples remain explicitly sourced.
                    if not re.search(r'\b(is|are|was|were|be|been|being|has|have|had|can|could|will|would|should|may|might|must|do|does|did|he|she|they|we|I|you|it|his|her|their|our)\b',example,re.I): continue
                    if any(t in tokens(example) for t in EXCLUDE): continue
                    rank = match - i*.03
                    if best is None or rank > best[0]: best = (rank,i,syn,example)
        sense_index = best[1] if best else 0
        definition, vi = en[sense_index].strip(), r['senses_vi'][sense_index].strip()
        if len(definition) < 5 or len(vi) < 2 or '\ufffd' in definition+vi: continue
        topics = classify(word,definition,'',taxonomy)
        level,level_source = levels[(word,pos)]
        if topics == ['general'] and level in ['B2','C1','C2'] and not any(t in definition.lower() for t in ['informal','slang','colloquial']):
            topics = ['academic']
        example = best[3] if best else ''
        if example: example = example[0].upper()+example[1:]+('' if example[-1] in '.!?' else '.')
        entry = dict(lemma=word,partOfSpeech=pos,cefrLevel=level,meaningVi=vi,
                     definitionEn=definition[0].upper()+definition[1:]+('' if definition[-1] in '.!?' else '.'),
                     ipaAm=r.get('pron',''),exampleEn=example,topics=topics,
                     dictionaryEntryId=r['id'],dictionaryFile=r['sourceFile'],dictionaryLine=r['sourceLine'],
                     dictionarySenseIndex=sense_index,alignment='upstream_parallel_senses' if aligned else 'single_gloss_single_translation',
                     levelSource=level_source,upstreamOrigin=r['source'],upstreamConfidence=r.get('extra',{}).get('llm_confidence','not supplied'),
                     wordnetLocator=f"{best[2]['pos']}:{best[2]['offset']}" if best else '',
                     reviewStatus='automated_import_not_expert_reviewed',
                     spellingVariants=[v for v in variant_groups.get(word,[]) if v != word])
        # Frequency is only a selection input. No unlicensed frequency values
        # or invented corpus ranks are emitted in application metadata.
        priority = (1.2 if topics != ['general'] else 0) + min(r.get('freq',0),5)*.2 + (.3 if example else 0) + (.1 if r.get('pron') else 0) + (.3 if level in ['B2','C1'] else 0)
        choices.append((priority,entry))
    choices.sort(key=lambda x:(-x[0],x[1]['lemma'],x[1]['partOfSpeech']))
    selected, seen = [],set(original)
    seen_groups = {v for word in original for v in variant_groups.get(word,[word])}
    for _,entry in choices:
        if entry['lemma'] in seen or entry['lemma'] in seen_groups: continue
        selected.append(entry); seen.add(entry['lemma'])
        seen_groups.update(variant_groups.get(entry['lemma'],[entry['lemma']]))
        if len(seen) == 3000: break
    if len(seen) != 3000: raise ValueError(f'Only {len(seen)} suitable words; add editorial content rather than padding.')
    selected.sort(key=lambda r:r['lemma'])
    target = CURATED/'dictionary_selection.jsonl'
    target.write_text(''.join(json.dumps(r,ensure_ascii=False,sort_keys=True)+'\n' for r in selected),encoding='utf-8',newline='\n')
    sources = dict(dictionaryCommit=DICT_COMMIT,cefrCommit=CEFR_COMMIT,
                   dictionaryUrl=f'https://github.com/thichhoc-org/thichhoc-dict/tree/{DICT_COMMIT}',
                   cefrUrl=f'https://github.com/openlanguageprofiles/olp-en-cefrj/tree/{CEFR_COMMIT}',
                   wordnetUrl='https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/wordnet.zip',
                   wordnetSha256=hashlib.sha256((RAW/'wordnet.zip').read_bytes()).hexdigest(),
                   selectionSha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                   dictionaryFiles={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(RAW.glob('*.jsonl'))},
                   cefrFiles={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in [RAW/'cefrj.csv',RAW/'octanove.csv']})
    (CURATED/'source_lock.json').write_text(json.dumps(sources,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(f'Frozen {len(selected)} imported + {len(original)} editorial = {len(seen)} unique entries.')
    print('Imported CEFR:',dict(Counter(r['cefrLevel'] for r in selected)))
    print('Imported examples:',sum(bool(r['exampleEn']) for r in selected))

if __name__ == '__main__': main()
