#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
GENERATED_DIR="$SCRIPT_DIR/generated"
BACKEND="$REPO_ROOT/fizzim.pl"
GOLDEN_FZM="$SCRIPT_DIR/generic_ctrl_fsm.fzm"
FEATURE_FZM="$SCRIPT_DIR/generic_ctrl_fsm_features.fzm"
GOLDEN_SV="$GENERATED_DIR/generic_ctrl_fsm.sv"
FEATURE_SV="$GENERATED_DIR/generic_ctrl_fsm_features.sv"
TESTBENCH="$SCRIPT_DIR/tb_generic_ctrl_equiv.sv"
GOLDEN_SV_YOSYS="testcases/generated/generic_ctrl_fsm.sv"
FEATURE_SV_YOSYS="testcases/generated/generic_ctrl_fsm_features.sv"

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

mkdir -p "$GENERATED_DIR"
add_oss_to_path

echo "Generating golden RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$GOLDEN_FZM" > "$GOLDEN_SV"

echo "Generating feature RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$FEATURE_FZM" > "$FEATURE_SV"

grep -q 'reg \[2047:0\] statename' "$FEATURE_SV"
grep -q 'G_RUN\.S_RUN_A' "$FEATURE_SV"
run_result_fork_count="$(grep -c 'if ((op_done || op_fail) && (op_fail))' "$FEATURE_SV")"
retry_fork_count="$(grep -c 'if ((op_done || op_fail) && (!op_fail && op_done && retry_ok))' "$FEATURE_SV")"
done_fork_count="$(grep -c 'if ((op_done || op_fail) && (!op_fail && op_done && !retry_ok))' "$FEATURE_SV")"
if [[ "$run_result_fork_count" -lt 2 || "$retry_fork_count" -lt 2 || "$done_fork_count" -lt 2 ]]; then
  echo "Feature RTL did not expand the G_RUN state-group transition through FORK_RESULT for both child states" >&2
  exit 1
fi
if grep -q 'FORK_' "$FEATURE_SV"; then
  echo "Feature RTL leaked fork pseudo-state names" >&2
  exit 1
fi

echo "Compiling and simulating generic equivalence"
if [[ "$SIM" == "xrun" ]] && command -v xrun >/dev/null 2>&1; then
  xrun -64bit -sv -clean -access +rwc -top tb_generic_ctrl_equiv \
    "$GOLDEN_SV" "$FEATURE_SV" "$TESTBENCH"
elif command -v iverilog >/dev/null 2>&1 && command -v vvp >/dev/null 2>&1; then
  iverilog -g2012 -Wall -o "$GENERATED_DIR/generic_ctrl_equiv.vvp" \
    "$GOLDEN_SV" "$FEATURE_SV" "$TESTBENCH"
  vvp "$GENERATED_DIR/generic_ctrl_equiv.vvp"
else
  echo "Could not find xrun or iverilog/vvp for simulation." >&2
  exit 1
fi

if command -v yosys >/dev/null 2>&1; then
  echo "Running Yosys syntax/synthesis checks"
  cat > "$GENERATED_DIR/yosys_check_golden.ys" <<YOSYS
read_verilog -sv "$GOLDEN_SV_YOSYS"
hierarchy -check -top generic_ctrl_fsm
proc
opt
check
YOSYS
  cat > "$GENERATED_DIR/yosys_check_features.ys" <<YOSYS
read_verilog -sv "$FEATURE_SV_YOSYS"
hierarchy -check -top generic_ctrl_fsm_features
proc
opt
check
YOSYS
  yosys -q -s "$GENERATED_DIR/yosys_check_golden.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_features.ys"
fi

echo "PASS generic backend flow"
