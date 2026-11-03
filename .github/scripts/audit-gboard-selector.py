"""Metadata-only selector audit; requires androguard. Never copies the host APK.
Usage: python audit-gboard-selector.py /path/to/Gboard.apk
Checks the same six semantic roles and the shipped signature fallback families.
"""
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

apk = sys.argv[1]
classes = {}
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if not re.fullmatch(r"classes(?:[2-9]|[1-9][0-9]+)?\.dex", name):
            continue
        for cls in DEX(archive.read(name)).get_classes():
            methods = []
            for method in cls.get_methods():
                methods.append(dict(owner=cls.get_name(), name=method.get_name(),
                    desc=method.get_descriptor().replace(" ", ""),
                    public=bool(method.get_access_flags() & 1),
                    static=bool(method.get_access_flags() & 8),
                    text="\n".join(i.get_output() for i in method.get_instructions())
                         if method.get_code() else ""))
            classes[cls.get_name()] = dict(super=cls.get_superclassname(), methods=methods)
all_methods = [m for c in classes.values() for m in c["methods"]]
voice = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;"
selector = "Lcom/google/android/libraries/inputmethod/preferencewidgets/CustomSelectorWithWidgetPreference;"
vc = classes[voice]

def unique(items):
    items = list(items)
    assert len(items) == 1, [(m["owner"], m["name"], m["desc"]) for m in items]
    return items[0]

def calls(method, target):
    return target["owner"] + "->" + target["name"] + target["desc"] in method["text"].replace(" ", "")

layout = unique(m for m in vc["methods"] if m["public"] and not m["static"] and m["desc"] == "()I")
create = unique(m for m in vc["methods"] if m["public"] and not m["static"] and m["desc"] == "(Landroid/os/Bundle;)V")
write = unique(m for m in vc["methods"] if m["static"] and re.fullmatch(
    r"\(ZL[^;]+;" + re.escape(selector + selector) + r"\)V", m["desc"]))
setup = unique(m for m in vc["methods"] if m["public"] and not m["static"] and m["desc"] == "()V"
    and vc["super"] + "->" + m["name"] + "()V" in m["text"]
    and any("setupUnifiedLayout" in x["text"] and calls(m, x) for x in vc["methods"]))
reads = []
for method in all_methods:
    if method["desc"] == "(Landroid/content/Context;)Z" and "pref_key_switch_to_muse" in method["text"]:
        reads.extend(m for m in classes[method["owner"]]["methods"] if m["static"]
            and m["name"] != method["name"] and m["desc"] == method["desc"] and calls(method, m))
read = unique(reads)
defaults = unique(m for m in all_methods if m["public"] and not m["static"] and m["desc"] == "()V"
    and all(x in m["text"] for x in ("com/google/android/apps/inputmethod/libs/agenticdictation/extension/AgenticDictationExtension",
                                   "onCreateExtension", "pref_key_switch_to_muse")))
roles = dict(zip(("layout", "setup", "create", "write", "read", "defaults"),
                 (layout, setup, create, write, read, defaults)))
source = (Path(__file__).resolve().parents[2] / "app/src/main/java/dev/kaede/pixelboard/RamblerBindings.java").read_text()
profiles = re.findall(r'new Profile\("([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)", "([^"]+)"\)', source)
matching_profiles = []
for version, ln, wn, wa, ro, do, dn in profiles:
    expected = dict(layout=(voice, ln, "()I"), setup=(voice, "ac", "()V"),
        create=(voice, "f", "(Landroid/os/Bundle;)V"),
        write=(voice, wn, "(ZL" + wa + ";" + selector + selector + ")V"),
        read=("L" + ro + ";", "a", "(Landroid/content/Context;)Z"), defaults=("L" + do + ";", dn, "()V"))
    if all((m["owner"], m["name"], m["desc"]) == expected[role] for role, m in roles.items()):
        matching_profiles.append(version)
assert len(matching_profiles) == 1, matching_profiles
with open(apk, "rb") as stream:
    digest = hashlib.file_digest(stream, "sha256").hexdigest()
print(json.dumps(dict(sha256=digest, signature_profiles=matching_profiles,
    semantic_roles={role: {k: m[k] for k in ("owner", "name", "desc")} for role, m in roles.items()}), indent=2))
