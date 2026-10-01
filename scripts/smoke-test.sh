#!/bin/sh
set -eu

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir="$project_dir/build/smoke-test"

mkdir -p "$classes_dir"
javac --release 8 -encoding UTF-8 -d "$classes_dir" \
  "$project_dir/app/src/main/java/com/desudesu/simpleftp/FtpServer.java" \
  "$project_dir/tests/com/desudesu/simpleftp/FtpServerSmokeTest.java"
java -cp "$classes_dir" com.desudesu.simpleftp.FtpServerSmokeTest
