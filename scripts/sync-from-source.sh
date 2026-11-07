#!/usr/bin/env bash
# Manually bring work in from another project. Nothing else in this repo syncs on its own.
#
#   scripts/sync-from-source.sh adybag14 [branch]            show what is new
#   scripts/sync-from-source.sh nousresearch [branch]        show what is new
#   scripts/sync-from-source.sh adybag14 [branch] --merge    merge it into the current branch
#
# It never pushes. Review the result, then push yourself.
set -euo pipefail

source_name="${1:-}"
case "$source_name" in
  adybag14) url="https://github.com/adybag14-cyber/hermes-agent.git" ;;
  nousresearch) url="https://github.com/NousResearch/hermes-agent.git" ;;
  *) echo "Usage: $0 adybag14|nousresearch [branch] [--merge]" >&2; exit 2 ;;
esac
shift

branch=""
merge=false
for arg in "$@"; do
  if [ "$arg" = "--merge" ]; then merge=true; else branch="$arg"; fi
done

git remote get-url "$source_name" >/dev/null 2>&1 || git remote add "$source_name" "$url"
# Pushing to a source project by accident should be impossible.
git remote set-url --push "$source_name" DISABLED

if [ -z "$branch" ]; then
  branch="$(git ls-remote --symref "$source_name" HEAD | sed -n 's|^ref: refs/heads/\(.*\)\tHEAD$|\1|p')"
fi
[ -n "$branch" ] || { echo "Could not find the default branch of $source_name." >&2; exit 1; }

git fetch "$source_name" "+refs/heads/$branch:refs/remotes/$source_name/$branch"
ref="$source_name/$branch"

new_commits="$(git rev-list --count "HEAD..$ref")"
if [ "$new_commits" = "0" ]; then
  echo "Your current branch already has everything from $ref."
  exit 0
fi

echo "$new_commits new commit(s) in $ref compared to your current branch (latest 50 shown):"
git log -n 50 --oneline --no-decorate "HEAD..$ref"

if ! "$merge"; then
  echo
  echo "Nothing was changed. Add --merge to bring this in."
  exit 0
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "Commit or stash your changes first; nothing was merged." >&2
  exit 1
fi
git merge --no-ff "$ref"
echo "Merged locally. Nothing was pushed."
