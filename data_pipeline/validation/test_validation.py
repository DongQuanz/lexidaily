import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('validator', ROOT / 'data_pipeline/scripts/validate_seed.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

class DatasetValidationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / 'seed'
        shutil.copytree(ROOT / 'data_pipeline/normalized', self.root)
    def tearDown(self): self.temp.cleanup()
    def mutate(self, path, callback):
        file = self.root / path
        rows = json.loads(file.read_text(encoding='utf-8'))
        callback(rows)
        file.write_text(json.dumps(rows, ensure_ascii=False), encoding='utf-8')
    def test_valid_seed(self): self.assertEqual((3000, 3000), module.validate(self.root))
    def test_duplicate_identity_rejected(self):
        self.mutate('vocabulary/lexemes.json', lambda rows: rows.append(rows[0]))
        with self.assertRaises(AssertionError): module.validate(self.root)
    def test_invalid_source_reference_rejected(self):
        self.mutate('sources/occurrences.json', lambda rows: rows[0].update(sourcePackId='missing'))
        with self.assertRaises(AssertionError): module.validate(self.root)
    def test_invalid_level_rejected(self):
        self.mutate('vocabulary/senses.json', lambda rows: rows[0].update(cefrLevel='band 7'))
        with self.assertRaises(AssertionError): module.validate(self.root)
    def test_copyright_context_field_rejected(self):
        self.mutate('sources/occurrences.json', lambda rows: rows[0].update(passage='unwanted full passage'))
        with self.assertRaises(AssertionError): module.validate(self.root)
    def test_future_volume_supported_but_invalid_volume_rejected(self):
        self.mutate('sources/demo.json', lambda rows: rows[0].update(series='CAMBRIDGE_IELTS', volume=29))
        self.assertEqual((3000, 3000), module.validate(self.root))
        self.mutate('sources/demo.json', lambda rows: rows[0].update(volume=0))
        with self.assertRaises(AssertionError): module.validate(self.root)

    def test_examples_must_support_literal_cloze(self):
        self.mutate('vocabulary/senses.json',lambda rows: rows[0].update(exampleEn='This sentence has no target word.'))
        with self.assertRaises(AssertionError): module.validate(self.root)

    def test_duplicate_pack_membership_rejected(self):
        self.mutate('packs/memberships.json',lambda rows:rows.append(rows[0]))
        with self.assertRaises(AssertionError): module.validate(self.root)

    def test_source_licensing_is_required(self):
        (self.root/'licenses/ATTRIBUTION.txt').unlink()
        with self.assertRaises(AssertionError): module.validate(self.root)

    def test_legacy_identities_preserved(self):
        source = ROOT/'data_pipeline/curated/legacy_demo.psv'
        expected = {f'{r[1]}_{r[2]}_01' for line in source.read_text(encoding='utf-8').splitlines()
                    if line and not line.startswith('#') for r in [line.split('|')]}
        actual = {r['id'] for r in json.loads((self.root/'vocabulary/senses.json').read_text(encoding='utf-8'))}
        self.assertEqual(70,len(expected))
        self.assertTrue(expected <= actual)

    def test_export_matches_normalized_data(self):
        exported = [json.loads(line) for line in (ROOT/'data_pipeline/exports/ielts_3000.jsonl').read_text(encoding='utf-8').splitlines()]
        senses = {r['id']:r for r in json.loads((self.root/'vocabulary/senses.json').read_text(encoding='utf-8'))}
        self.assertEqual(3000,len(exported))
        for r in exported:
            self.assertEqual(senses[r['senseId']]['meaningVi'],r['meaningVi'])
            self.assertEqual(senses[r['senseId']]['exampleEn'],r['exampleEn'])

    def test_curated_snapshot_survives_git_line_normalization(self):
        spec = importlib.util.spec_from_file_location('seed_builder', ROOT/'data_pipeline/scripts/build_seed.py')
        builder = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(builder)
        checkout = Path(self.temp.name)/'checkout-curated'
        shutil.copytree(ROOT/'data_pipeline/curated',checkout)
        # Git stores these text files with LF, even when the author uses Windows.
        for path in checkout.rglob('*'):
            if path.is_file():
                path.write_bytes(path.read_text(encoding='utf-8').encode('utf-8'))
        with patch.object(builder,'CURATED',checkout):
            self.assertEqual(3000,len(builder.load_entries()))

if __name__ == '__main__': unittest.main()
