"""Where this build updates from.

Every Python path behind ``hermes update`` and the startup update check reads these values, so
this file is the one place to change. Bringing work in from another project is a separate manual
step: see ``scripts/sync-from-source.sh``.
"""

from urllib.parse import urlparse

UPDATE_REPO = "Catalin-1993/hermes-agentn-android"
UPDATE_BRANCH = "agenti-pe-telefon"

UPDATE_REPO_HTTPS_URL = f"https://github.com/{UPDATE_REPO}.git"
UPDATE_REPO_SSH_URL = f"git@github.com:{UPDATE_REPO}.git"
UPDATE_REPO_CANONICAL = f"github.com/{UPDATE_REPO}".lower()
UPDATE_ORIGIN_REF = f"origin/{UPDATE_BRANCH}"
UPDATE_ARCHIVE_URL = f"https://github.com/{UPDATE_REPO}/archive/refs/heads/{UPDATE_BRANCH}.zip"
UPDATE_RELEASE_URL_BASE = f"https://github.com/{UPDATE_REPO}/releases/tag"
UPDATE_COMPARE_API_BASE = f"https://api.github.com/repos/{UPDATE_REPO}/compare"
INSTALL_SH_URL = f"https://raw.githubusercontent.com/{UPDATE_REPO}/{UPDATE_BRANCH}/scripts/install.sh"
INSTALL_PS1_URL = f"https://raw.githubusercontent.com/{UPDATE_REPO}/{UPDATE_BRANCH}/scripts/install.ps1"

# Checkouts cloned before the move still point here. `hermes update` repoints them at UPDATE_REPO.
FORMER_UPDATE_REPOS = frozenset({
    "github.com/nousresearch/hermes-agent",
    "github.com/adybag14-cyber/hermes-agent",
})


def canonical_github_remote(url: str | None) -> str:
    """Return ``host/owner/repo`` for common GitHub remote URL forms."""
    if not url:
        return ""
    value = url.strip()
    for ssh_prefix in ("git@github.com:", "ssh://git@github.com/"):
        if value.startswith(ssh_prefix):
            value = "github.com/" + value[len(ssh_prefix):]
            break
    else:
        parsed = urlparse(value)
        if parsed.netloc and parsed.path:
            value = f"{parsed.netloc}{parsed.path}"
    return value.strip().rstrip("/").removesuffix(".git").lower()


def is_former_update_repo(url: str | None) -> bool:
    """True for a remote this build no longer updates from."""
    return canonical_github_remote(url) in FORMER_UPDATE_REPOS
