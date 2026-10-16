"""Strict referential validation for normalized content before Android import."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
POS = {'noun', 'verb', 'adjective', 'adverb', 'preposition', 'conjunction', 'pronoun', 'determiner', 'interjection'}
CEFR = {'A1', 'A2', 'B1', 'B2', 'C1', 'C2'}
SKILLS = {'Reading', 'Listening', 'Writing', 'Speaking'}
SOURCE_TYPES = {'OPEN_WORDLIST', 'CEFR_REFERENCE', 'IELTS_BOOK', 'IELTS_AUTHENTIC_TEST', 'IELTS_OFFICIAL_SAMPLE', 'USER_IMPORT', 'CUSTOM'}

def validate(root):
    def read(path): return json.loads((root / path).read_text(encoding='utf-8'))
    def indexed(rows, label):
        ids = [r['id'] for r in rows]
        assert all(ids) and len(ids) == len(set(ids)), f'Duplicate or empty {label} ID'
        return {r['id']: r for r in rows}
    lexemes = indexed(read('vocabulary/lexemes.json'), 'lexeme')
    senses = indexed(read('vocabulary/senses.json'), 'sense')
    topics = indexed(read('vocabulary/topics.json'), 'topic')
    sources = indexed(sum((json.loads(p.read_text(encoding='utf-8')) for p in sorted((root/'sources').glob('*.json')) if p.name != 'occurrences.json'), []), 'source')
    packs = indexed(sum((read(f'packs/{s}.json') for s in ('general', 'academic', 'ielts_topics')), []), 'pack')
    sense_keys = set()
    words_with_senses = set()
    for s in senses.values():
        assert s['lexemeId'] in lexemes and s['partOfSpeech'] in POS
        assert s.get('cefrLevel') in CEFR and s['definitionEn'].strip() and s['meaningVi'].strip()
        assert isinstance(s['exampleEn'], str) and isinstance(s['exampleVi'], str)
        if s['exampleEn']:
            assert re.search(r'\b'+re.escape(lexemes[s['lexemeId']]['lemma'])+r'\b',s['exampleEn'],re.I), f"Example missing target: {s['id']}"
        assert all('|' not in s[f] and '\ufffd' not in s[f] for f in ['meaningVi','definitionEn','exampleEn','exampleVi'])
        key = (s['lexemeId'], s['partOfSpeech'], s['definitionEn'].strip().lower())
        assert key not in sense_keys, 'Duplicate sense'
        sense_keys.add(key)
        words_with_senses.add(s['lexemeId'])
    lemmas = [w['lemma'].strip().casefold() for w in lexemes.values()]
    assert len(lemmas) == len(set(lemmas)), 'Duplicate normalised headword'
    for w in lexemes.values():
        assert w['lemma'].strip() and w['id'] in words_with_senses and '|' not in w['id']
        assert w['lexicalDataSource'] in sources
        assert w['cefrSource'] and w['frequencySource']
        for field in ('ieltsRelevanceScore', 'academicScore', 'productivePriority', 'difficultyScore'):
            assert 0 <= w[field] <= 1
    for source in sources.values():
        assert source['sourceType'] in SOURCE_TYPES and source['licenseType']
        assert source.get('volume') is None or isinstance(source['volume'], int) and source['volume'] > 0
    for o in indexed(read('sources/occurrences.json'), 'occurrence').values():
        assert o['lexemeId'] in lexemes and o['sourcePackId'] in sources
        assert o.get('senseId') is None or o['senseId'] in senses and senses[o['senseId']]['lexemeId'] == o['lexemeId']
        assert o.get('skill') is None or o['skill'] in SKILLS
        assert o.get('testNumber') is None or isinstance(o['testNumber'], int) and o['testNumber'] > 0
        assert o.get('occurrenceCount', 1) > 0
        assert not any(key in o for key in ('passage', 'transcript', 'audio', 'answerKey', 'context'))
    topic_links = read('vocabulary/topic_links.json')
    assert len(topic_links) == len({(r['lexemeId'],r['topicId']) for r in topic_links}), 'Duplicate topic membership'
    assert set(lexemes) == {r['lexemeId'] for r in topic_links}, 'Word without a topic'
    for link in topic_links:
        assert link['lexemeId'] in lexemes and link['topicId'] in topics
    pronunciations = read('vocabulary/pronunciations.json')
    assert len(pronunciations) == len({p['senseId'] for p in pronunciations}), 'Duplicate pronunciation'
    for p in pronunciations:
        assert p['senseId'] in senses and (p.get('ipaBr','').strip() or p.get('ipaAm','').strip())
    for c in indexed(read('vocabulary/collocations.json'), 'collocation').values():
        assert c['senseId'] in senses and c['text'].strip() and '|' not in c['text']
    for f in read('vocabulary/word_families.json'): assert f['rootLexemeId'] in lexemes and f['relatedLexemeId'] in lexemes
    for p in read('vocabulary/paraphrases.json'): assert p['senseId'] in senses and p['text'].strip()
    memberships = read('packs/memberships.json')
    assert len(memberships) == len({(r['packId'],r['lexemeId']) for r in memberships}), 'Duplicate pack membership'
    for link in memberships: assert link['packId'] in packs and link['lexemeId'] in lexemes
    manifest = read('manifest.json')
    assert manifest['lexemes'] == len(lexemes) and manifest.get('senses',len(senses)) == len(senses)
    assert isinstance(manifest['version'],int) and manifest['version'] > 0
    if manifest['version'] >= 2:
        assert set(lexemes) == {r['lexemeId'] for r in memberships if r['packId']=='ielts_core'}, 'Incomplete IELTS core pack'
        report = read('quality_report.json')
        assert report['lexemes'] == len(lexemes) and report['senses'] == len(senses)
        assert report['entriesWithExamples'] == sum(bool(s['exampleEn']) for s in senses.values())
        assert report['entriesWithPronunciation'] == len(pronunciations)
        assert report['entriesWithCollocations'] == len({r['senseId'] for r in read('vocabulary/collocations.json')})
        for notice in ['ATTRIBUTION.txt','CC-BY-SA-4.0.txt','THICHHOC-LICENSE-DATA.txt','CEFR-PROFILES-NOTICE.txt','WORDNET-3.0-LICENSE.txt','CMUDICT-LICENSE.txt']:
            assert (root/'licenses'/notice).is_file(), f'Missing license notice: {notice}'
    return len(lexemes), len(senses)

if __name__ == '__main__':
    for target in (ROOT / 'data_pipeline/normalized', ROOT / 'app/src/main/assets'):
        words, senses = validate(target)
        print(f'PASS: {target.name}: {words} lexemes / {senses} senses; all references valid.')
    normalized,assets = ROOT/'data_pipeline/normalized',ROOT/'app/src/main/assets'
    for path in sorted(normalized.rglob('*')):
        if path.is_file():
            assert path.read_bytes() == (assets/path.relative_to(normalized)).read_bytes(), f'Asset mirror differs: {path.name}'
    print('PASS: normalized data and bundled assets are identical.')
