#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec java -cp "$script_dir/fizzim.jar" FizzimJavaBackend "$@"
