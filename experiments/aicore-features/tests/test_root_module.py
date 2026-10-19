"""Host checks for the optional feature-only package, including installer refusal."""
from pathlib import Path
import os
import subprocess
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'root-module'


class RootModuleTest(unittest.TestCase):
    def test_payload_is_feature_only(self):
        payload = sorted(str(p.relative_to(ROOT / 'system'))
                         for p in (ROOT / 'system').rglob('*') if p.is_file())
        self.assertEqual(payload, ['product/etc/sysconfig/pixelboard_aicore_sm8850.xml'])
        xml = ET.parse(ROOT / 'system' / payload[0]).getroot()
        self.assertEqual(xml.tag, 'config')
        self.assertEqual([e.attrib for e in xml], [
            {'name': 'com.google.android.feature.AICORE_QC'},
            {'name': 'com.google.android.feature.AICORE_QC_SM8850'}])
        for forbidden in ('system.prop', 'sepolicy.rule', 'post-fs-data.sh', 'service.sh'):
            self.assertFalse((ROOT / forbidden).exists())

    def install(self, soc='SM8850', sdk='37', boot='true'):
        shim = '''
getprop() {
  case "$1" in
    ro.soc.model) printf '%s' "$TEST_SOC" ;;
    ro.build.version.sdk) printf '%s' "$TEST_SDK" ;;
  esac
}
abort() { echo "$1"; exit 1; }
ui_print() { echo "$1"; }
set_perm_recursive() { :; }
set_perm() { :; }
. "$TEST_SCRIPT"
'''
        return subprocess.run(['sh', '-c', shim], text=True, capture_output=True,
                              env={**os.environ, 'TEST_SOC': soc, 'TEST_SDK': sdk,
                                   'BOOTMODE': boot, 'KSU': 'true', 'MODPATH': str(ROOT),
                                   'TEST_SCRIPT': str(ROOT / 'customize.sh')})

    def test_supported_device(self):
        self.assertEqual(self.install().returncode, 0)

    def test_wrong_soc_or_missing_identity_refused(self):
        for soc in ('SM8635', 'SM8750', ''):
            with self.subTest(soc=soc):
                self.assertNotEqual(self.install(soc=soc).returncode, 0)

    def test_old_invalid_android_and_recovery_refused(self):
        for sdk in ('33', '', 'invalid'):
            with self.subTest(sdk=sdk):
                self.assertNotEqual(self.install(sdk=sdk).returncode, 0)
        self.assertNotEqual(self.install(boot='false').returncode, 0)

    def test_shell_syntax(self):
        for name in ('customize.sh', 'action.sh'):
            subprocess.run(['sh', '-n', str(ROOT / name)], check=True)


if __name__ == '__main__':
    unittest.main()
