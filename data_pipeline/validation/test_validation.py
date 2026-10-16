import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest

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
    def test_valid_seed(self): self.assertEqual((70, 70), module.validate(self.root))
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
        self.assertEqual((70, 70), module.validate(self.root))
        self.mutate('sources/demo.json', lambda rows: rows[0].update(volume=0))
        with self.assertRaises(AssertionError): module.validate(self.root)

if __name__ == '__main__': unittest.main()
