#!/bin/bash

# This script automatically merges any branches created by Dependabot PRs onto a single branch, for testing
BRANCHES=$(git branch -a --list '*/dependabot*')
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT_BRANCH" == "main" ]]; then
  echo 'You are on main, aborting script!';
  exit 1;
fi

for BRANCH in $BRANCHES
do
	git merge -s recursive -X theirs --no-edit $BRANCH
done
