"""Build the version-2 IELTS seed offline from the checked-in curated inputs.

No external API, random generation, band-to-word mapping or corpus frequency
claims are involved. Existing demo IDs are retained for content upgrades.
"""
import hashlib
import json
import re
import zipfile
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CURATED = ROOT / 'data_pipeline/curated'
TARGETS = (ROOT / 'data_pipeline/normalized', ROOT / 'app/src/main/assets')
VERSION = 2

def read(name):
    return json.loads((CURATED/name).read_text(encoding='utf-8'))

def psv(name):
    for line_number, line in enumerate((CURATED/name).read_text(encoding='utf-8').splitlines(),1):
        if line.strip() and not line.startswith('#'):
            yield line_number, line.split('|')

def load_entries():
    rows = {}
    for line, fields in psv('legacy_demo.psv'):
        topic,word,pos,level,vi,en,example,ipa,collocations,synonym = fields
        rows[word] = dict(lemma=word,partOfSpeech=pos,cefrLevel=level,meaningVi=vi,definitionEn=en,
            exampleEn=example,ipaBr=ipa,ipaAm='',topics=[topic],collocations=collocations.split(';'),
            paraphrase=synonym,lexicalSource='lexidaily_demo',levelSource='LexiDaily editorial estimate (not EVP)',
            sourceLocator=f'curated/legacy_demo.psv:{line}',reviewStatus='editorial_original')
    ipa = read('editorial_pronunciations.json')
    for line, fields in psv('ielts_editorial.psv'):
        topic,word,pos,level,vi,en,example,collocations = fields
        if word in rows: continue  # Preserve the complete original 70-entry seed.
        rows[word] = dict(lemma=word,partOfSpeech=pos,cefrLevel=level,meaningVi=vi,definitionEn=en,
            exampleEn=example,ipaBr='',ipaAm=ipa.get(word,{}).get('ipaAm',''),topics=[topic],
            collocations=collocations.split(';'),paraphrase='',lexicalSource='lexidaily_ielts',
            levelSource='LexiDaily editorial estimate (not EVP)',sourceLocator=f'curated/ielts_editorial.psv:{line}',
            pronunciationProvenance=ipa.get(word),reviewStatus='editorial_original')
    locked = read('source_lock.json')
    snapshot = CURATED/'dictionary_selection.jsonl'
    assert hashlib.sha256(snapshot.read_bytes()).hexdigest() == locked['selectionSha256'], 'Selection checksum changed; intentionally refreeze the source selection.'
    for line in snapshot.read_text(encoding='utf-8').splitlines():
        r = json.loads(line)
        word = r['lemma']
        assert word not in rows, f'Duplicate headword {word}'
        rows[word] = dict(r,ipaBr='',collocations=[],paraphrase='',lexicalSource='thichhoc_dictionary',
            sourceLocator=f"dict-en-vi/data/entries/{r['dictionaryFile']}:{r['dictionaryLine']} entry={r['dictionaryEntryId']} sense={r['dictionarySenseIndex']}")
    assert len(rows) == 3000, f'Expected 3000 unique headwords, found {len(rows)}'
    return sorted(rows.values(),key=lambda r:r['lemma'])

def make_payload(entries):
    lexemes,senses,pronunciations,links,collocations,paraphrases,occurrences = [],[],[],[],[],[],[]
    flat = []
    usage_notes = read('usage_notes.json')
    topic_names = {t:t.title() for t in read('topics.json')}
    topic_names.update(general='General English',academic='Academic language')
    for r in entries:
        word,pos,level = r['lemma'],r['partOfSpeech'],r['cefrLevel']
        sid = f'{word}_{pos}_01'
        academic = {'B1':.35,'B2':.65,'C1':.85,'C2':.9}[level]
        productive = .85 if r['collocations'] else .65 if r['exampleEn'] else .5
        cefr_source = r['levelSource']
        if cefr_source in ('CEFR-J 1.5','Octanove 1.0'):
            cefr_source += '; headword/POS reference, not a verified sense-level or IELTS-band annotation'
        lexemes.append(dict(id=word,lemma=word,academicScore=academic,ieltsRelevanceScore=.7 if level=='B1' else .85,
            productivePriority=productive,difficultyScore={'B1':.3,'B2':.5,'C1':.7,'C2':.9}[level],
            lexicalDataSource=r['lexicalSource'],cefrSource=cefr_source,frequencySource='Not ranked'))
        senses.append(dict(id=sid,lexemeId=word,partOfSpeech=pos,definitionEn=r['definitionEn'],meaningVi=r['meaningVi'],
            cefrLevel=level,register='neutral',exampleEn=r['exampleEn'],exampleVi='',commonError=usage_notes.get(word,'')))
        if r.get('ipaBr') or r.get('ipaAm'):
            pronunciations.append(dict(senseId=sid,ipaBr=r.get('ipaBr',''),ipaAm=r.get('ipaAm','')))
        topics = list(r['topics'])
        if word in ['renewable','emission','sustainable']:
            topics += ['climate','energy']
        if r['topics']==['research']: topics += ['science']
        topics = list(dict.fromkeys(topics))
        links.extend(dict(lexemeId=word,topicId=t) for t in topics)
        collocations.extend(dict(id=f'{sid}_{i}',senseId=sid,text=c) for i,c in enumerate(r['collocations']))
        if r['paraphrase']:
            paraphrases.append(dict(senseId=sid,text=r['paraphrase'],relationType='NEAR_SYNONYM'))
        occurrences.append(dict(id=f"{r['lexicalSource']}:{word}",lexemeId=word,senseId=sid,
            sourcePackId=r['lexicalSource'],sourceLocator=r['sourceLocator'],occurrenceCount=1))
        skills = ['Reading']
        if r['exampleEn'] and level in ['B2','C1','C2']: skills.append('Writing')
        if r['collocations'] and level in ['B1','B2','C1']: skills.append('Speaking')
        if level in ['B1','B2'] and (r.get('ipaBr') or r.get('ipaAm')): skills.append('Listening')
        for skill in skills:
            occurrences.append(dict(id=f'curriculum:{word}:{skill}',lexemeId=word,senseId=sid,
                sourcePackId='lexidaily_ielts',skill=skill,topic=topics[0],occurrenceCount=1,
                sourceLocator='Editorial study recommendation based on level and available enrichment; not an observed IELTS test occurrence'))
        if r['lexicalSource']=='thichhoc_dictionary':
            level_source = 'cefrj_profile' if r['levelSource']=='CEFR-J 1.5' else 'octanove_profile'
            occurrences.append(dict(id=f'{level_source}:{word}',lexemeId=word,senseId=sid,
                sourcePackId=level_source,sourceLocator=f"{r['lemma']} / {pos}; headword-level reference only",occurrenceCount=1))
            if r['wordnetLocator']:
                occurrences.append(dict(id=f'wordnet:{word}',lexemeId=word,senseId=sid,
                    sourcePackId='wordnet_examples',sourceLocator=f"WordNet 3.0 synset {r['wordnetLocator']}; usage example, not an IELTS occurrence",occurrenceCount=1))
        if r.get('pronunciationProvenance'):
            ip = r['pronunciationProvenance']
            occurrences.append(dict(id=f'ipa:{word}',lexemeId=word,senseId=sid,sourcePackId='thichhoc_dictionary',
                sourceLocator=f"IPA only: {ip['sourceFile']}:{ip['sourceLine']} {ip['entryId']}",occurrenceCount=1))
        flat.append(dict(id=word,senseId=sid,lemma=word,partOfSpeech=pos,cefrLevel=level,
            meaningVi=r['meaningVi'],definitionEn=r['definitionEn'],exampleEn=r['exampleEn'],exampleVi='',
            ipaBr=r.get('ipaBr',''),ipaAm=r.get('ipaAm',''),topics=topics,collocations=r['collocations'],
            commonError=usage_notes.get(word,''),
            spellingVariants=r.get('spellingVariants',[]),lexicalDataSource=r['lexicalSource'],cefrSource=cefr_source,
            sourceLocator=r['sourceLocator'],reviewStatus=r['reviewStatus'],license='CC-BY-SA-4.0',
            recommendedSkills=skills,pronunciationProvenance=r.get('pronunciationProvenance'),
            exampleSource=('wordnet_examples' if r.get('wordnetLocator') else r['lexicalSource']) if r['exampleEn'] else None))
    families = [dict(rootLexemeId=root,relatedLexemeId=related,relation=relation) for root,related,relation in (
        ('analyse','analysis','NOUN_FORM'),('analyse','analytical','ADJECTIVE_FORM'),('analysis','analyse','VERB_FORM'),
        ('significant','significance','NOUN_FORM'),('significant','significantly','ADVERB_FORM'),('innovation','innovative','ADJECTIVE_FORM'),
        ('sustainable','sustainability','NOUN_FORM'),('mitigate','mitigation','NOUN_FORM'),('adapt','adaptation','NOUN_FORM'),
        ('automate','automation','NOUN_FORM'),('resign','resignation','NOUN_FORM'),('employ','employability','NOUN_FORM'))]
    sources = [
        dict(id='lexidaily_ielts',name='LexiDaily IELTS Editorial',sourceType='CUSTOM',licenseType='CC BY-SA 4.0',isBundled=True,priority=.7,
             description='Nội dung Anh–Việt biên soạn riêng. CEFR và độ liên quan IELTS là ước tính biên tập; không gán band cho từng từ. Ví dụ không lấy từ đề IELTS.'),
        dict(id='thichhoc_dictionary',name='Từ điển Anh–Việt thichhoc.com',sourceType='CUSTOM',licenseType='CC BY-SA 4.0; WordNet and CMUdict notices retained',isBundled=True,priority=.5,
             description='Dữ liệu từ điển: Từ điển Anh–Việt thichhoc.com (thichhoc-dict), giấy phép CC BY-SA 4.0. https://github.com/thichhoc-org/thichhoc-dict. Nguồn gốc: WordNet 3.1 (Princeton), CMUdict (CMU), Wiktionary. Đã chỉnh sửa dữ liệu. Bản chọn lọc tự động; nguồn có bản dịch do mô hình sinh, chưa được chuyên gia duyệt toàn bộ. IPA nguồn này là Anh–Mỹ, không phải bản ghi âm. Xem giấy phép và ghi công trong thư viện.'),
        dict(id='cefrj_profile',name='CEFR-J Wordlist 1.5',sourceType='CEFR_REFERENCE',licenseType='Free research/commercial use with attribution to Tono Laboratory, TUFS',isBundled=True,priority=.5,
             description='The CEFR-J Wordlist Version 1.5. Compiled by Yukio Tono, Tokyo University of Foreign Studies. Qua Open Language Profiles: https://github.com/openlanguageprofiles/olp-en-cefrj. Chỉ tham chiếu mức headword/từ loại; không phải EVP hay band IELTS.'),
        dict(id='octanove_profile',name='Octanove Vocabulary Profile C1/C2 1.0',sourceType='CEFR_REFERENCE',licenseType='CC BY-SA 4.0',isBundled=True,priority=.5,
             description='Octanove Labs, Octanove Vocabulary Profile C1/C2 version 1.0, qua Open Language Profiles. https://github.com/openlanguageprofiles/olp-en-cefrj. Nhãn tham chiếu headword/từ loại, không xác nhận band IELTS.'),
        dict(id='wordnet_examples',name='Princeton WordNet 3.0 usage examples',sourceType='CUSTOM',licenseType='WordNet License',isBundled=True,priority=.4,
             description='WordNet 3.0, Princeton University, qua nltk_data. Ví dụ được chọn theo nghĩa và có kiểm tra tự động; không phải câu từ đề IELTS. Giữ ghi công và giấy phép Princeton trong phần Giấy phép và ghi công.')]
    demo = [dict(id='lexidaily_demo',name='LexiDaily Original 70',sourceType='CUSTOM',licenseType='CC BY-SA 4.0',isBundled=True,priority=.5,
                 description='70 mục từ gốc, giữ nguyên ID để cập nhật nội dung mà không mất tiến độ. Định nghĩa, ví dụ và nhãn CEFR do biên tập, không phải dữ liệu đề IELTS.')]
    catalog = [dict(id=id,name=id.upper(),sourceType='OPEN_WORDLIST',licenseType='External dataset — not bundled',isBundled=False,
                    description='Vị trí nguồn tương thích; không tuyên bố thành viên hay đóng gói danh sách này.') for id in ['ngsl','nawl']]
    general = [dict(id='general',name='General English',description='Everyday and academic vocabulary for broader language use.')]
    academic = [dict(id='academic',name='Academic Core',description='B2–C2 learning selection; not the NAWL corpus or an official IELTS wordlist.')]
    packs = [dict(id='ielts_core',name='IELTS 3000 · 6.5–8.5',description='3000 mục từ cho người học hướng tới IELTS 6.5–8.5. Mục tiêu học tập cho cả bộ, không phải band của từng từ.')]
    packs += [dict(id=f'ielts_{t}',name=f'{name} · IELTS',description='Nhóm học theo chủ đề; liên kết nguồn nhập được gợi ý tự động, cần kiểm tra theo ngữ cảnh.') for t,name in topic_names.items()]
    packs += [dict(id=f'cefr_{level.lower()}',name=f'{level} · IELTS study',description='Tham chiếu CEFR theo nguồn hoặc ước tính biên tập; không quy đổi trực tiếp sang band IELTS.') for level in ['B1','B2','C1','C2']]
    pack_links = []
    for r in flat:
        ids = ['general','ielts_core',f"cefr_{r['cefrLevel'].lower()}"] + [f'ielts_{t}' for t in r['topics']]
        if r['cefrLevel'] != 'B1': ids += ['academic']
        pack_links += [dict(packId=p,lexemeId=r['id']) for p in ids]
    report = dict(version=VERSION,lexemes=len(entries),senses=len(senses),
        originalEditorialEntries=sum(r['lexicalSource']!='thichhoc_dictionary' for r in entries),
        importedEntries=sum(r['lexicalSource']=='thichhoc_dictionary' for r in entries),
        cefr=dict(sorted(Counter(r['cefrLevel'] for r in entries).items())),
        topicCoverage=dict(sorted(Counter(l['topicId'] for l in links).items())),
        entriesWithExamples=sum(bool(r['exampleEn']) for r in entries),
        entriesWithCollocations=sum(bool(r['collocations']) for r in entries),
        entriesWithPronunciation=len(pronunciations),collocations=len(collocations),
        expertReviewed=False,targetLearnerBand='6.5–8.5',perWordBandAssigned=False,
        notes=['CEFR source profiles annotate headword/POS, not the imported sense. Editorial estimates are marked separately.',
               'Examples, Vietnamese example translations, collocations, synonyms and word families are not complete for every entry. Missing enrichment is empty, never fabricated.',
               'Imported translations include upstream model-generated content. Automated validation and a bounded manual spot check do not constitute expert review of all 3000 entries.',
               'Topic labels on imported entries are heuristic. Source occurrences denote dictionary/profile membership, not observed IELTS test appearances.',
               'Skill-tagged CUSTOM records are editorial study recommendations, not evidence that a word occurred in an IELTS paper.',
               'Spelling variants listed in CEFR-J are deduplicated and not counted as separate study entries.'])
    payload = {}
    for name,data in [('lexemes',lexemes),('senses',senses),('pronunciations',pronunciations),('topic_links',links),
                      ('collocations',collocations),('paraphrases',paraphrases),('word_families',families),
                      ('topics',[dict(id=t,name=name) for t,name in topic_names.items()])]: payload[f'vocabulary/{name}.json'] = data
    payload.update({'sources/demo.json':demo,'sources/ielts.json':sources,'sources/ngsl.json':catalog[:1],'sources/nawl.json':catalog[1:],
                    'sources/occurrences.json':occurrences,'packs/general.json':general,'packs/academic.json':academic,
                    'packs/ielts_topics.json':packs,'packs/memberships.json':pack_links,
                    'manifest.json':dict(version=VERSION,lexemes=len(entries),senses=len(senses),
                                         description='IELTS 3000 bilingual study dataset, learner target 6.5–8.5; no per-word band claims',
                                         license='CC-BY-SA-4.0',qualityReport='quality_report.json'),
                    'quality_report.json':report})
    return payload,flat,report

def build():
    payload,flat,report = make_payload(load_entries())
    for root in TARGETS:
        for relative,data in payload.items():
            path = root/relative
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
        for source in (ROOT/'data_pipeline/licenses').glob('*.txt'):
            path = root/'licenses'/source.name
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(source.read_bytes())
    export = ROOT/'data_pipeline/exports/ielts_3000.jsonl'
    export.parent.mkdir(parents=True,exist_ok=True)
    export.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in flat),encoding='utf-8',newline='\n')
    guide = ('IELTS 3000 — dữ liệu từ vựng Anh–Việt\n\n'
        'Mở ielts_3000.jsonl bằng công cụ hỗ trợ JSON Lines và UTF-8. Mỗi dòng là một mục.\n'
        'Bộ gồm 3.000 mục từ/cụm từ, một từ loại và một nghĩa học chính cho mỗi mục.\n'
        'Mục tiêu người học: IELTS 6.5–8.5. Không gán band cho từng từ.\n'
        'Nhãn CEFR là tham chiếu headword/POS từ CEFR-J/Octanove hoặc ước tính biên tập.\n'
        f"Có {report['entriesWithExamples']} mục có ví dụ, {report['entriesWithCollocations']} mục có collocation và {report['entriesWithPronunciation']} mục có IPA.\n"
        'Trường nội dung bổ sung còn thiếu được để trống. Các mục nhập chưa được chuyên gia duyệt toàn bộ.\n'
        'recommendedSkills là gợi ý chương trình học, không phải bằng chứng xuất hiện trong đề thi.\n'
        'Xem quality_report.json để biết độ phủ, và licenses/ATTRIBUTION.txt để xem nguồn, giấy phép, thay đổi.\n'
        'Dữ liệu được cung cấp theo CC BY-SA 4.0; giữ ghi công khi phân phối lại.\n')
    bundle_items = {'ielts_3000.jsonl':export.read_bytes(),'README.vi.txt':guide.encode('utf-8'),
                    'manifest.json':(TARGETS[0]/'manifest.json').read_bytes(),
                    'quality_report.json':(TARGETS[0]/'quality_report.json').read_bytes()}
    bundle_items.update({f'licenses/{p.name}':p.read_bytes() for p in (ROOT/'data_pipeline/licenses').glob('*.txt')})
    with zipfile.ZipFile(export.with_suffix('.zip'),'w',compression=zipfile.ZIP_DEFLATED) as archive:
        for name,data in sorted(bundle_items.items()):
            info = zipfile.ZipInfo(name,date_time=(2026,10,16,0,0,0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info,data)
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__ == '__main__': build()
