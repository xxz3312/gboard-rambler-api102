"""Optional local audit: python audit-gboard-retention.py /path/to/Gboard.apk.

Requires androguard. Outputs metadata only; never uploads or copies the APK.
"""
import hashlib
import json
import sys
import zipfile

from loguru import logger
logger.remove()
from androguard.core.dex import DEX

apk = sys.argv[1]
anchors = (
    "getUnpinnedItemTimeLimitInMilliSeconds",
    "Failed to parse retention hours",
    "com/google/android/apps/inputmethod/libs/clipboard/ClipboardContentProviderUtils",
)
matches = []
with zipfile.ZipFile(apk) as archive:
    for name in archive.namelist():
        if not name.endswith(".dex"):
            continue
        dex = DEX(archive.read(name))
        for cls in dex.get_classes():
            for method in cls.get_methods():
                if not method.get_code() or not method.get_access_flags() & 8:
                    continue
                if method.get_descriptor() != "(Landroid/content/Context;)J":
                    continue
                instructions = list(method.get_instructions())
                text = "\n".join(i.get_output() for i in instructions)
                if not all(anchor in text for anchor in anchors):
                    continue
                if any(clock in text for clock in ("currentTimeMillis", "Instant;->", "SystemClock;->")):
                    continue
                if not any(
                    duration + "->ofHours(J)" in text and duration + "->toMillis()J" in text
                    for duration in ("Lj$/time/Duration;", "Ljava/time/Duration;")
                ):
                    continue
                matches.append({"dex": name, "owner": cls.get_name(), "name": method.get_name(),
                                "descriptor": method.get_descriptor()})
with open(apk, "rb") as stream:
    digest = hashlib.file_digest(stream, "sha256").hexdigest()
print(json.dumps({"apk_sha256": digest, "match_count": len(matches), "matches": matches}, indent=2))
if len(matches) != 1:
    sys.exit("No unique native duration getter; do not pick an arbitrary method")
