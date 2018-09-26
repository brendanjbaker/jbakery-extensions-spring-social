#!/bin/bash

set -euxo pipefail

path_script=$(readlink -f "$0")
path_root=$(dirname $(dirname "$path_script"))
project=jbakery-extensions-spring-social

pushd $path_root/$project
mvn clean install
popd
