"""Local metadata-only audit of the semantic Rambler UI graph. Requires androguard.
No APK or disassembled instructions are copied to this repository.
"""
import hashlib
import json
import re
import sys
import zipfile
from loguru import logger
logger.remove()
from androguard.core.dex import DEX
from androguard.core.apk import APK

apk = sys.argv[1]
classes, methods = {}, {}
with zipfile.ZipFile(apk) as z:
    for name in z.namelist():
        if not re.fullmatch(r"classes(?:[2-9]|[1-9][0-9]+)?\.dex", name):
            continue
        for cls in DEX(z.read(name)).get_classes():
            classes[cls.get_name()] = cls
            for method in cls.get_methods():
                desc = method.get_descriptor().replace(" ", "")
                key = cls.get_name(), method.get_name(), desc
                instructions = list(method.get_instructions()) if method.get_code() else []
                text = "\n".join(i.get_output() for i in instructions)
                calls = []
                for instruction in instructions:
                    if not instruction.get_name().startswith("invoke"):
                        continue
                    match = re.search(r"(L[^;]+;)->([^\s(]+)(\([^)]*\)\S+)", instruction.get_output())
                    if match:
                        calls.append((match[1], match[2], match[3].replace(" ", "")))
                methods[key] = {"key": key, "desc": desc, "text": text, "calls": calls,
                                "static": bool(method.get_access_flags() & 8)}

def unique(items):
    items = list(dict.fromkeys(items))
    if len(items) != 1:
        raise ValueError("Ambiguous or missing graph match: " + str(items))
    return items[0]

def owner(anchor):
    return unique(k[0] for k, v in methods.items() if k[1] == "<clinit>" and anchor in v["text"])

def signature(k, desc):
    return k[2] == desc

base = owner("com/google/android/apps/inputmethod/libs/agenticdictation/ui/impl/AbstractRamblerUiController")
header = owner("com/google/android/apps/inputmethod/libs/agenticdictation/ui/impl/RamblerUiVkController")
assert classes[header].get_superclassname() == base
family = [v for k, v in methods.items() if k[0] == base]
arg = "(ILjava/lang/Runnable;)"
factory = unique(v["key"] for v in family if v["desc"].startswith(arg)
                 and any(c[0] == base and c[2].startswith(arg) and c[2] != v["desc"] for c in v["calls"]))
key_type = factory[2].split(")", 1)[1]
builder = unique(v["key"] for v in family if v["desc"].startswith(arg)
                 and not v["desc"].endswith(key_type) and "ramble_close" in v["text"])
builder_type = builder[2].split(")", 1)[1]
label = unique(c for c in methods[builder]["calls"] if c[0] == builder_type and signature(c, "(I)V"))
build = unique(c for c in methods[factory]["calls"] if c[0] == builder_type and signature(c, "()" + key_type))
close = unique(v["key"] for v in family if v["desc"] == "(I)" + key_type and factory in v["calls"])
icon = unique(v["key"] for v in family if v["desc"] == "()I" and "pref_key_jetson_close_icon" in v["text"])
tear = unique(v["key"] for v in family if all(t in v["text"] for t in ("push_to_ramble", "ramble_close", "ramble_retry"))
              and v["desc"].endswith("Landroid/view/View;Landroid/view/View;)V"))
add = unique(c for v in family for c in v["calls"] if signature(c, "(I" + key_type + ")V") and methods[c]["static"])
remove = unique(c for c in methods[tear]["calls"] if signature(c, "(ILjava/lang/String;)V") and methods[c]["static"])
keys = [k for k, v in methods.items() if v["desc"] == "()" + key_type
        and classes[k[0]].get_superclassname() == base and factory in v["calls"] and close in v["calls"]]
header_key = unique(k for k in keys if k[0] == header)
widget_key = unique(k for k in keys if k[0] != header)
hide = unique(k for k, v in methods.items() if k[0] == header and v["desc"] == "(Landroid/view/ViewGroup;Z)V"
              and "getChildCount" in v["text"] and "->animate()" in v["text"])
android = APK(apk)
resources = android.get_android_resources()
labels = {}
for package, locales in resources.get_resolved_strings().items():
    for resource_id in locales.get("DEFAULT", {}):
        for config, value in resources.get_resolved_res_configs(resource_id):
            if config.get_qualifier() == "":
                if value in ("Minimize keyboard", "Back to keyboard"):
                    labels.setdefault(value, []).append(hex(resource_id))
                break
assert set(labels) == {"Minimize keyboard", "Back to keyboard"}
with open(apk, "rb") as stream:
    digest = hashlib.file_digest(stream, "sha256").hexdigest()
print(json.dumps({"version": android.get_androidversion_name(), "version_code": android.get_androidversion_code(),
                  "sha256": digest, "graph": dict(zip(
                      ["header", "widget", "factory", "builder", "label", "build", "close", "close_icon", "teardown", "hide", "add", "remove"],
                      [header_key, widget_key, factory, builder, label, build, close, icon, tear, hide, add, remove])),
                  "native_labels": labels}, indent=2))
