"""CI reports may summarize failures, but must not publish private test output."""
import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('java_report', Path(__file__).resolve().parents[2] / 'ci' / 'java_report.py')
reporter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reporter)


class ReportTests(unittest.TestCase):
    def test_only_counts_and_identifiers_leave_the_runner(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'maven.log').write_text('PRIVATE_PASSWORD CODE MAIL DUMP\n[INFO] BUILD FAILURE', encoding='utf-8')
            (root / 'TEST-example.xml').write_text('''<testsuite tests="2" failures="1" errors="0" skipped="0">
              <properties><property name="password" value="PRIVATE_PASSWORD"/></properties>
              <testcase classname="example.Safe" name="rejectsLeak"><failure>PRIVATE_PASSWORD CODE MAIL</failure></testcase>
              <system-out>PRIVATE_PASSWORD DUMP</system-out></testsuite>''', encoding='utf-8')
            output = reporter.report(root / 'maven.log', root)
            self.assertIn('tests=2, failures=1', output)
            self.assertIn('example.Safe.rejectsLeak', output)
            self.assertNotIn('PRIVATE_PASSWORD', output)
            self.assertNotIn('CODE MAIL', output)
            self.assertNotIn('DUMP', output)
