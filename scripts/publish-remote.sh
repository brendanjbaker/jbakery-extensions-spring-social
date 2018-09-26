#!/bin/bash

set -euxo pipefail

publish_id=$(openssl rand -hex 16)
path_script=$(readlink -f "$0")
path_root=$(dirname $(dirname "$path_script"))
project=jbakery-extensions-spring-social
branch_temporary=temp/$publish_id

function execute {
	# Check out temporary branch.

	pushd $path_root
	branch_original=$(git symbolic-ref --short HEAD)
	git checkout -b "$branch_temporary"
	popd

	# Build.

	pushd $path_root/$project
	export GPG_TTY=$(tty)
	export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
	mvn release:clean release:prepare release:clean
	popd

	# Cleanup.

	cleanup
}

function cleanup {
	echo "Original branch: $branch_original"
	echo "Temporary branch: $branch_temporary"
	pushd $path_root
	git checkout "$branch_original"
	git branch -D "$branch_temporary"
	popd
}

set +e
execute
set -e

cleanup || true
