"""Print CI counts and failing test identifiers, never test bodies or raw Maven logs."""
from pathlib import Path
import os
import re
import sys
import xml.etree.ElementTree as ET


def report(log: Path, reports: Path) -> str:
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    failed = []
    for file in sorted(reports.glob('TEST-*.xml')):
        root = ET.parse(file).getroot()
        for key in totals:
            totals[key] += int(root.get(key, '0'))
        for test in root.findall('testcase'):
            if test.find('failure') is not None or test.find('error') is not None:
                name = test.get('classname', '') + '.' + test.get('name', '')
                failed.append(re.sub(r'[^a-zA-Z0-9_.$-]', '_', name)[:240])
    lines = ['Java verification: ' + ', '.join(f'{k}={v}' for k, v in totals.items())]
    lines += ['Failed test: ' + name for name in failed]
    text = log.read_text(encoding='utf-8', errors='replace')
    if '[INFO] BUILD SUCCESS' in text:
        lines.append('Maven clean verify: SUCCESS')
    else:
        lines.append('Maven clean verify: FAILED; raw output retained only in runner temporary storage.')
        if not totals['tests']:
            lines.append('Failure before test reports; check pinned actions, JDK/Wrapper and dependency availability.')
    return '\n'.join(lines) + '\n'


if __name__ == '__main__':
    output = report(Path(sys.argv[1]), Path(sys.argv[2]))
    print(output, end='')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as summary:
            summary.write(output)
