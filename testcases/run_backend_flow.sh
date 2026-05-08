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
STATE_ACTION_FZM="$SCRIPT_DIR/generic_state_action.fzm"
STATE_ACTION_FEATURE_FZM="$GENERATED_DIR/generic_state_action_transition_actions.fzm"
STATE_ACTION_SV="$GENERATED_DIR/generic_state_action.sv"
STATE_ACTION_FEATURE_SV="$GENERATED_DIR/generic_state_action_transition_actions.sv"
STATE_ACTION_TESTBENCH="$SCRIPT_DIR/tb_generic_state_action_transition_actions.sv"
GOLDEN_SV_YOSYS="testcases/generated/generic_ctrl_fsm.sv"
FEATURE_SV_YOSYS="testcases/generated/generic_ctrl_fsm_features.sv"
STATE_ACTION_SV_YOSYS="testcases/generated/generic_state_action.sv"
STATE_ACTION_FEATURE_SV_YOSYS="testcases/generated/generic_state_action_transition_actions.sv"

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
fork_fail_lines="$(grep -n 'if ((op_done || op_fail) && (op_fail))' "$FEATURE_SV" | cut -d: -f1)"
fork_retry_lines="$(grep -n 'if ((op_done || op_fail) && (!op_fail && op_done && retry_ok))' "$FEATURE_SV" | cut -d: -f1)"
fork_done_lines="$(grep -n 'if ((op_done || op_fail) && (!op_fail && op_done && !retry_ok))' "$FEATURE_SV" | cut -d: -f1)"
abort_lines="$(grep -n 'else if (abort)' "$FEATURE_SV" | cut -d: -f1)"
for occurrence in 1 2; do
  fail_line="$(printf '%s\n' "$fork_fail_lines" | sed -n "${occurrence}p")"
  retry_line="$(printf '%s\n' "$fork_retry_lines" | sed -n "${occurrence}p")"
  done_line="$(printf '%s\n' "$fork_done_lines" | sed -n "${occurrence}p")"
  abort_line="$(printf '%s\n' "$abort_lines" | sed -n "${occurrence}p")"
  if [[ -z "$fail_line" || -z "$retry_line" || -z "$done_line" || -z "$abort_line" ]]; then
    echo "Feature RTL priority check could not find expected fork/abort transitions" >&2
    exit 1
  fi
  if (( fail_line >= retry_line || retry_line >= done_line || done_line >= abort_line )); then
    echo "Feature RTL did not keep fork branch priority ahead of direct abort transition" >&2
    exit 1
  fi
done

echo "Generating transition-actions example RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$STATE_ACTION_FZM" > "$STATE_ACTION_SV"
"$PERL_BIN" -0pe '
my $action = q{
      <rdy>
            <status>
            GLOBAL_FIXED
            </status>
         <value>
         1
            <status>
            LOCAL
            </status>
         </value>
         <vis>
         1
            <status>
            GLOBAL_VAR
            </status>
         </vis>
         <type>
         output
            <status>
            GLOBAL_VAR
            </status>
         </type>
         <comment>
         transition action
            <status>
            LOCAL
            </status>
         </comment>
         <color>
         -16777216
            <status>
            GLOBAL_VAR
            </status>
         </color>
         <useratts>
         
            <status>
            GLOBAL_VAR
            </status>
         </useratts>
         <resetval>
         
            <status>
            GLOBAL_VAR
            </status>
         </resetval>
         <x2Obj>
         0
         </x2Obj>
         <y2Obj>
         0
         </y2Obj>
         <page>
         1
         </page>
      </rdy>
};
s/generic_state_action/generic_state_action_transition_actions/g;
s/state==S1 && nextstate==S2/0/g;
s/(<transition>\s*<attributes>\s*<name>.*?<value>\s*trans59\s*<status>.*?)(\s*<priority>)/$1\n$action$2/s;
' "$STATE_ACTION_FZM" > "$STATE_ACTION_FEATURE_FZM"
"$PERL_BIN" "$BACKEND" -noaddversion "$STATE_ACTION_FEATURE_FZM" > "$STATE_ACTION_FEATURE_SV"
grep -q '// datapath transition actions' "$STATE_ACTION_FEATURE_SV"
grep -q 'rdy <= 1;' "$STATE_ACTION_FEATURE_SV"

echo "Compiling and simulating generic equivalence"
if [[ "$SIM" == "xrun" ]] && command -v xrun >/dev/null 2>&1; then
  xrun -64bit -sv -clean -access +rwc -top tb_generic_ctrl_equiv \
    "$GOLDEN_SV" "$FEATURE_SV" "$TESTBENCH"
  xrun -64bit -sv -clean -access +rwc -top tb_generic_state_action_transition_actions \
    "$STATE_ACTION_SV" "$STATE_ACTION_FEATURE_SV" "$STATE_ACTION_TESTBENCH"
elif command -v iverilog >/dev/null 2>&1 && command -v vvp >/dev/null 2>&1; then
  iverilog -g2012 -Wall -o "$GENERATED_DIR/generic_ctrl_equiv.vvp" \
    "$GOLDEN_SV" "$FEATURE_SV" "$TESTBENCH"
  vvp "$GENERATED_DIR/generic_ctrl_equiv.vvp"
  iverilog -g2012 -Wall -o "$GENERATED_DIR/generic_state_action_transition_actions.vvp" \
    "$STATE_ACTION_SV" "$STATE_ACTION_FEATURE_SV" "$STATE_ACTION_TESTBENCH"
  vvp "$GENERATED_DIR/generic_state_action_transition_actions.vvp"
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
  cat > "$GENERATED_DIR/yosys_check_state_action.ys" <<YOSYS
read_verilog -sv "$STATE_ACTION_SV_YOSYS"
hierarchy -check -top generic_state_action
proc
opt
check
YOSYS
  cat > "$GENERATED_DIR/yosys_check_state_action_transition_actions.ys" <<YOSYS
read_verilog -sv "$STATE_ACTION_FEATURE_SV_YOSYS"
hierarchy -check -top generic_state_action_transition_actions
proc
opt
check
YOSYS
  yosys -q -s "$GENERATED_DIR/yosys_check_golden.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_features.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_state_action.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_state_action_transition_actions.ys"
fi

echo "PASS generic backend flow"
