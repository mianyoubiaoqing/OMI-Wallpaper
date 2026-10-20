"""Audit only Git-managed files, never local build caches or user diagnostics."""
import pathlib, re, subprocess, sys

root = pathlib.Path(__file__).resolve().parents[1]
names = subprocess.check_output(["git", "ls-files", "-z"], cwd=root).decode().split("\0")
errors = []
patterns = {
    "absolute workstation path": re.compile(r"(?<![A-Za-z0-9])[A-Za-z]:[\\/](?!/)|/(?:Users|home)/[^/\s]+"),
    "messaging account identifier": re.compile(r"wxid_[A-Za-z0-9]+"),
    "GitHub credential": re.compile(r"gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,}"),
    "private key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
}
blocked = {".apk", ".aab", ".png", ".jpg", ".jpeg", ".webp", ".mp4", ".jks", ".keystore", ".pem", ".key", ".p12", ".log"}
for name in filter(None, names):
    path = root / name
    # This single preview is rendered by the MIT-licensed procedural demo.
    # Do not expand this allowlist to user screenshots or third-party artwork.
    if name == "docs/demo.png":
        if not path.read_bytes().startswith(b"\x89PNG\r\n\x1a\n") or path.stat().st_size > 1000000:
            errors.append(name + ": invalid procedural demo preview")
        continue
    if path.suffix.lower() in blocked or any(part in name.split("/") for part in [".codex-remote-attachments", "reference", ".android", ".tools"]):
        errors.append(name + ": unexpected private/media/build artifact")
    if name == "local.properties" or name.startswith("app/src/main/assets/wallpaper/"):
        errors.append(name + ": private local configuration or texture pack")
    if path.suffix == ".jar":
        if name != "gradle/wrapper/gradle-wrapper.jar":
            errors.append(name + ": unexpected binary")
        continue
    text = path.read_text(encoding="utf-8-sig")
    for label, pattern in patterns.items():
        if pattern.search(text):
            errors.append(name + ": " + label)
if errors:
    print("\n".join(errors))
    sys.exit(1)
print("Public-source audit passed (" + str(len(list(filter(None, names)))) + " Git-managed files)")
