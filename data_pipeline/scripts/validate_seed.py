"""Strict referential validation for normalized content before Android import."""
import json
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
    sources = indexed(sum((read(f'sources/{s}.json') for s in ('demo', 'ngsl', 'nawl')), []), 'source')
    packs = indexed(sum((read(f'packs/{s}.json') for s in ('general', 'academic', 'ielts_topics')), []), 'pack')
    sense_keys = set()
    for s in senses.values():
        assert s['lexemeId'] in lexemes and s['partOfSpeech'] in POS
        assert s.get('cefrLevel') in CEFR and s['definitionEn'].strip() and s['meaningVi'].strip()
        key = (s['lexemeId'], s['partOfSpeech'], s['definitionEn'].strip().lower())
        assert key not in sense_keys, 'Duplicate sense'
        sense_keys.add(key)
    for w in lexemes.values():
        assert w['lemma'].strip() and any(s['lexemeId'] == w['id'] for s in senses.values())
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
    for link in read('vocabulary/topic_links.json'):
        assert link['lexemeId'] in lexemes and link['topicId'] in topics
    for p in read('vocabulary/pronunciations.json'): assert p['senseId'] in senses
    for c in read('vocabulary/collocations.json'): assert c['senseId'] in senses and c['text'].strip()
    for f in read('vocabulary/word_families.json'): assert f['rootLexemeId'] in lexemes and f['relatedLexemeId'] in lexemes
    for p in read('vocabulary/paraphrases.json'): assert p['senseId'] in senses and p['text'].strip()
    for link in read('packs/memberships.json'): assert link['packId'] in packs and link['lexemeId'] in lexemes
    assert read('manifest.json')['lexemes'] == len(lexemes)
    return len(lexemes), len(senses)

if __name__ == '__main__':
    for target in (ROOT / 'data_pipeline/normalized', ROOT / 'app/src/main/assets'):
        words, senses = validate(target)
        print(f'PASS: {target.name}: {words} lexemes / {senses} senses; all references valid.')
