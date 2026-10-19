"""Preserve the published metadata of resolved runtime modules with the release."""
import pathlib
import re
import urllib.request

report = pathlib.Path("dependencies.txt").read_text()
coordinates = set()
for line in report.splitlines():
    match = re.search(r"(?:[+\\]---) ([\w.\-]+):([\w.\-]+):([^\s:]+)(?: -> ([^\s]+))?", line)
    if match:
        group, artifact, version, selected = match.groups()
        if selected and ":" not in selected:
            version = selected
        coordinates.add((group, artifact, version))
coordinates.add(("io.github.libxposed", "api", "102.0.0"))
out = pathlib.Path("dist/notices/poms")
out.mkdir(parents=True, exist_ok=True)
for group, artifact, version in sorted(coordinates):
    path = f"{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}.pom"
    errors = []
    for base in ("https://repo.maven.apache.org/maven2/", "https://dl.google.com/dl/android/maven2/"):
        try:
            with urllib.request.urlopen(base + path, timeout=30) as response:
                content = response.read()
            (out / f"{group}.{artifact}-{version}.pom").write_bytes(content)
            break
        except Exception as error:
            errors.append(str(error))
    else:
        raise RuntimeError(f"Cannot archive POM for {group}:{artifact}:{version}: {errors}")
print(f"Archived {len(coordinates)} dependency POMs.")
