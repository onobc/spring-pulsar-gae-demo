#!/usr/bin/env bash

cd "$(dirname "$0")"
CUR_DIR=$(pwd)

echo -e "env_variables:\n  SPRING_PROFILES_ACTIVE: 'streamnative'" > .cloudenv-gae.yaml
cat .cloudenv.yaml | sed -u 's/^/  /' >> .cloudenv-gae.yaml
