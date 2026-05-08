#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATED_DIR="$SCRIPT_DIR/generated"

SOURCE_FZM="$SCRIPT_DIR/generic_state_machine.fzm"
COMPAT_FZM="$GENERATED_DIR/generic_state_machine_fizzim1.fzm"
FEATURE_SV="$GENERATED_DIR/generic_state_action.sv"
COMPAT_SV="$GENERATED_DIR/generic_state_action_fizzim.sv"
TESTBENCH="$SCRIPT_DIR/tb_generic_state_action_equiv.sv"
LEGACY_BACKEND="$SCRIPT_DIR/tb/legacy/fizzim_5_20.pl"

BACKEND="${BACKEND:-$REPO_ROOT/fizzim.pl}"
if [[ -z "${FIZZIM1_BACKEND:-}" && -f "$LEGACY_BACKEND" ]]; then
  FIZZIM1_BACKEND="$LEGACY_BACKEND"
else
  FIZZIM1_BACKEND="${FIZZIM1_BACKEND:-$BACKEND}"
fi
PERL_BIN="${PERL_BIN:-${FIZZIM_PERL:-perl}}"
SIM="${SIM:-xrun}"

add_oss_to_path() {
  local suite="${OSS_CAD_SUITE:-}"
  if [[ -z "$suite" && -d "/c/Users/MEA10713/Downloads/oss-cad-suite-windows-x64-20260506/oss-cad-suite" ]]; then
    suite="/c/Users/MEA10713/Downloads/oss-cad-suite-windows-x64-20260506/oss-cad-suite"
  fi
  if [[ -n "$suite" && ! -d "$suite" ]] && command -v cygpath >/dev/null 2>&1; then
    suite="$(cygpath -u "$suite" 2>/dev/null || printf '%s' "$suite")"
  fi
  if [[ -n "$suite" && -d "$suite/bin" ]]; then
    export YOSYSHQ_ROOT="$suite"
    export SSL_CERT_FILE="$suite/etc/cacert.pem"
    export PYTHON_EXECUTABLE="$suite/lib/python3.exe"
    export QT_PLUGIN_PATH="$suite/lib/qt5/plugins"
    export QT_LOGGING_RULES="*=false"
    export GTK_EXE_PREFIX="$suite"
    export GTK_DATA_PREFIX="$suite"
    export GDK_PIXBUF_MODULEDIR="$suite/lib/gdk-pixbuf-2.0/2.10.0/loaders"
    export GDK_PIXBUF_MODULE_FILE="$suite/lib/gdk-pixbuf-2.0/2.10.0/loaders.cache"
    export OPENFPGALOADER_SOJ_DIR="$suite/share/openFPGALoader"
    export PATH="$suite/bin:$suite/lib:$PATH"
  fi
}

run_compat_generator() {
  if command -v node >/dev/null 2>&1; then
    node "$SCRIPT_DIR/generate_fizzim1_compat.js" "$SOURCE_FZM" "$COMPAT_FZM"
  else
    echo "Node.js is required to generate the Fizzim 1.0-compatible golden diagram." >&2
    exit 1
  fi
}

mkdir -p "$GENERATED_DIR"
add_oss_to_path

echo "Generating Fizzim 1.0-compatible golden diagram"
run_compat_generator

if grep -q '<fork>\|<stategroup>\|<entryState>' "$COMPAT_FZM"; then
  echo "Compatibility diagram still contains Fizzim 2.0-only objects" >&2
  exit 1
fi

echo "Generating Fizzim 2.0 feature RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$SOURCE_FZM" > "$FEATURE_SV"

echo "Generating Fizzim 1.0-compatible golden RTL"
"$PERL_BIN" "$FIZZIM1_BACKEND" -noaddversion "$COMPAT_FZM" > "$COMPAT_SV"

grep -q 'module generic_state_action ' "$FEATURE_SV"
grep -q 'module generic_state_action_fizzim ' "$COMPAT_SV"
grep -q '// datapath transition actions' "$FEATURE_SV"
if grep -q '// datapath transition actions' "$COMPAT_SV"; then
  echo "Compatibility RTL still uses transition actions" >&2
  exit 1
fi
grep -q 'SG_ACTIVE\.S_GREEN' "$FEATURE_SV"
if grep -q 'SG_ACTIVE\.S_GREEN\|fork' "$COMPAT_SV"; then
  echo "Compatibility RTL leaked state-group or fork names" >&2
  exit 1
fi

echo "Compiling and simulating generic FSM equivalence"
if [[ "$SIM" == "xrun" ]] && command -v xrun >/dev/null 2>&1; then
  xrun -64bit -sv -clean -access +rwc -top tb_generic_state_action_equiv \
    "$COMPAT_SV" "$FEATURE_SV" "$TESTBENCH"
elif command -v iverilog >/dev/null 2>&1 && command -v vvp >/dev/null 2>&1; then
  iverilog -g2012 -Wall -o "$GENERATED_DIR/generic_state_action_equiv.vvp" \
    "$COMPAT_SV" "$FEATURE_SV" "$TESTBENCH"
  vvp "$GENERATED_DIR/generic_state_action_equiv.vvp"
else
  echo "Could not find xrun or iverilog/vvp for simulation." >&2
  exit 1
fi

if command -v yosys >/dev/null 2>&1; then
  echo "Running Yosys syntax/synthesis checks"
  cat > "$GENERATED_DIR/yosys_check_generic_state_action.ys" <<YOSYS
read_verilog -sv "testcases/generated/generic_state_action.sv"
hierarchy -check -top generic_state_action
proc
opt
check
YOSYS
  cat > "$GENERATED_DIR/yosys_check_generic_state_action_fizzim.ys" <<YOSYS
read_verilog -sv "testcases/generated/generic_state_action_fizzim.sv"
hierarchy -check -top generic_state_action_fizzim
proc
opt
check
YOSYS
  yosys -q -s "$GENERATED_DIR/yosys_check_generic_state_action.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_generic_state_action_fizzim.ys"
fi

echo "PASS generic Fizzim 1.0 compatibility backend flow"
