from pathlib import Path
from hashlib import sha256
import os
import shutil
import subprocess
import tempfile
import unittest
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1] / 'root-module'


class SystemAppInstallerTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        self.module = self.work / 'module'
        shutil.copytree(ROOT, self.module)
        self.source = self.work / 'installed'
        self.source.mkdir()
        self.files = ['base.apk', 'split_config.arm64_v8a.apk', 'split_config.en.apk']
        pins = []
        for name in self.files:
            content = ('fixture APK: ' + name).encode()
            (self.source / name).write_bytes(content)
            if 'arm64' in name:
                with ZipFile(self.source / name, 'w') as archive:
                    archive.writestr('lib/arm64-v8a/libQnnHtpV81Skel.so', b'fixture-v81')
                    archive.writestr('lib/arm64-v8a/libgoogle3.so', b'fixture-core')
                content = (self.source / name).read_bytes()
            pins.append(sha256(content).hexdigest() + ' ' + name)
        (self.module / 'apk-sha256.txt').write_text('\n'.join(pins) + '\n')
        self.listing = self.work / 'paths'
        self.write_listing()

    def write_listing(self):
        self.listing.write_text(''.join('package:' + str(self.source / name) + '\n'
                                       for name in self.files))

    def install(self, soc='SM8850', sdk='37', boot='true'):
        shim = '''
getprop() {
 case "$1" in
  ro.soc.model) printf '%s' "$TEST_SOC" ;;
  ro.build.version.sdk) printf '%s' "$TEST_SDK" ;;
 esac
}
pm() { cat "$TEST_PATHS"; }
abort() { echo "$1"; exit 1; }
ui_print() { echo "$1"; }
set_perm_recursive() { :; }
set_perm() { :; }
. "$MODPATH/customize.sh"
'''
        return subprocess.run(['sh', '-c', shim], capture_output=True, text=True,
                              env={**os.environ, 'MODPATH': str(self.module),
                                   'TMPDIR': str(self.work), 'TEST_PATHS': str(self.listing),
                                   'TEST_SOC': soc, 'TEST_SDK': sdk, 'BOOTMODE': boot})

    def test_complete_snapshot_and_originals_preserved(self):
        originals = {name: (self.source / name).read_bytes() for name in self.files}
        result = self.install()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        dest = self.module / 'system/product/app/PixelBoardAiCore'
        self.assertEqual(sorted(p.name for p in dest.glob('*.apk')), sorted(self.files))
        self.assertEqual((dest / 'lib/arm64/libQnnHtpV81Skel.so').read_bytes(), b'fixture-v81')
        for name, content in originals.items():
            self.assertEqual((dest / name).read_bytes(), content)
            self.assertEqual((self.source / name).read_bytes(), content)
        self.assertFalse((self.module / 'system/product/priv-app').exists())

    def test_rejects_changed_apk(self):
        (self.source / 'base.apk').write_bytes(b'wrong version')
        self.assertNotEqual(self.install().returncode, 0)

    def test_requires_arm64_split(self):
        self.files.remove('split_config.arm64_v8a.apk')
        self.write_listing()
        self.assertNotEqual(self.install().returncode, 0)

    def test_rejects_unknown_split(self):
        self.files.append('split_config.unknown.apk')
        (self.source / self.files[-1]).write_bytes(b'unknown')
        self.write_listing()
        self.assertNotEqual(self.install().returncode, 0)

    def test_rejects_duplicate_base(self):
        self.files.append('base.apk')
        self.write_listing()
        self.assertNotEqual(self.install().returncode, 0)

    def test_rejects_bad_device_and_boot_mode(self):
        for args in ({'soc': 'SM8635'}, {'sdk': '33'}, {'sdk': 'bad'}, {'boot': 'false'}):
            with self.subTest(args=args):
                self.assertNotEqual(self.install(**args).returncode, 0)

    def test_shell_syntax_and_no_persistent_policy_changes(self):
        for name in ('customize.sh', 'action.sh'):
            subprocess.run(['sh', '-n', str(ROOT / name)], check=True)
        for name in ('system.prop', 'sepolicy.rule', 'service.sh', 'post-fs-data.sh'):
            self.assertFalse((ROOT / name).exists())


if __name__ == '__main__':
    unittest.main()
