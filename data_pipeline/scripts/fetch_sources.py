"""Fetch pinned upstream inputs for refreezing; normal builds work offline."""
import concurrent.futures
import json
import urllib.request
from collections import Counter
from pathlib import Path

RAW = Path(__file__).resolve().parents[1] / 'raw'
DICT_COMMIT = '4d6e92e8bcf8e3e762410c2b0a9f98fea8e62e5b'
CEFR_COMMIT = 'd4e45b75b38f27b30dfc5c44d8c571aec7e7092f'

def download(item):
    name, url = item
    target = RAW / name
    RAW.mkdir(parents=True,exist_ok=True)
    if not target.exists() or not target.stat().st_size:
        with urllib.request.urlopen(url, timeout=60) as response:
            temporary = target.with_suffix(target.suffix+'.part')
            temporary.write_bytes(response.read())
            temporary.replace(target)
    return target

if __name__ == '__main__':
    tree = json.load(urllib.request.urlopen(f'https://api.github.com/repos/thichhoc-org/thichhoc-dict/git/trees/{DICT_COMMIT}?recursive=1'))
    files = [(Path(x['path']).name, f"https://raw.githubusercontent.com/thichhoc-org/thichhoc-dict/{DICT_COMMIT}/{x['path']}")
             for x in tree['tree'] if x['path'].startswith('dict-en-vi/data/entries/') and x['path'].endswith('.jsonl')]
    files += [(name, f'https://raw.githubusercontent.com/openlanguageprofiles/olp-en-cefrj/{CEFR_COMMIT}/{upstream}') for name, upstream in
              [('cefrj.csv', 'cefrj-vocabulary-profile-1.5.csv'), ('octanove.csv', 'octanove-vocabulary-profile-c1c2-1.0.csv'), ('cefr-readme.md', 'README.md')]]
    files += [('thichhoc-LICENSE-DATA.txt', f'https://raw.githubusercontent.com/thichhoc-org/thichhoc-dict/{DICT_COMMIT}/dict-en-vi/LICENSE-DATA')]
    files += [('wordnet.zip','https://raw.githubusercontent.com/nltk/nltk_data/gh-pages/packages/corpora/wordnet.zip')]
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        for i, path in enumerate(pool.map(download, files), 1):
            if i % 20 == 0: print(f'Downloaded {i}/{len(files)}', flush=True)
    import hashlib
    lock_path = RAW.parent/'curated/source_lock.json'
    if lock_path.exists():
        lock = json.loads(lock_path.read_text(encoding='utf-8'))
        checksums = dict(lock['dictionaryFiles'],**lock['cefrFiles'],**{'wordnet.zip':lock['wordnetSha256']})
        for name,expected in checksums.items():
            if hashlib.sha256((RAW/name).read_bytes()).hexdigest() != expected:
                raise ValueError(f'Upstream checksum changed: {name}; inspect it before updating the lock.')
    print(f'Fetched and checked {len(files)} source files. No application data changed.')
