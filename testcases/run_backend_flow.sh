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
STRESS_FZM="$SCRIPT_DIR/fork_stategroup_stress.fzm"
STRESS_REF_SV="$SCRIPT_DIR/fork_stategroup_stress_ref.sv"
STRESS_SV="$GENERATED_DIR/fork_stategroup_stress.sv"
STRESS_TESTBENCH="$SCRIPT_DIR/tb_fork_stategroup_stress_equiv.sv"
EXHAUSTIVE_FZM="$SCRIPT_DIR/exhaustive_generation.fzm"
EXHAUSTIVE_COMPAT_FZM="$GENERATED_DIR/exhaustive_generation_fizzim1.fzm"
EXHAUSTIVE_SV="$GENERATED_DIR/exhaustive_generation.sv"
EXHAUSTIVE_COMPAT_SV="$GENERATED_DIR/exhaustive_generation_fizzim.sv"
EXHAUSTIVE_TESTBENCH="$SCRIPT_DIR/tb_exhaustive_generation_equiv.sv"

BACKEND="${BACKEND:-$REPO_ROOT/fizzim.pl}"
if [[ -z "${FIZZIM1_BACKEND:-}" && -f "$LEGACY_BACKEND" ]]; then
  FIZZIM1_BACKEND="$LEGACY_BACKEND"
else
  FIZZIM1_BACKEND="${FIZZIM1_BACKEND:-$BACKEND}"
fi
PERL_BIN="${PERL_BIN:-${FIZZIM_PERL:-perl}}"
JAVA_BIN="${JAVA_BIN:-java}"
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

run_stress_generator() {
  if command -v node >/dev/null 2>&1; then
    node "$SCRIPT_DIR/tools/generate_fork_stategroup_stress.js"
  else
    echo "Node.js is required to generate the fork/state-group stress diagram." >&2
    exit 1
  fi
}

run_exhaustive_generator() {
  if command -v node >/dev/null 2>&1; then
    node "$SCRIPT_DIR/tools/generate_exhaustive_generation_case.js"
  else
    echo "Node.js is required to generate the exhaustive generation diagram." >&2
    exit 1
  fi
}

mkdir -p "$GENERATED_DIR"
add_oss_to_path

echo "Generating fork/state-group stress testcase"
run_stress_generator

echo "Generating exhaustive generation testcase"
run_exhaustive_generator

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

echo "Generating fork/state-group stress RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$STRESS_FZM" > "$STRESS_SV"
grep -q 'module fork_stategroup_stress ' "$STRESS_SV"
grep -q '// datapath transition actions' "$STRESS_SV"
grep -q 'SG_A\.S_A0' "$STRESS_SV"
grep -q 'SG_B\.S_B0' "$STRESS_SV"

echo "Compiling and simulating fork/state-group stress equivalence"
if [[ "$SIM" == "xrun" ]] && command -v xrun >/dev/null 2>&1; then
  xrun -64bit -sv -clean -access +rwc -top tb_fork_stategroup_stress_equiv \
    "$STRESS_REF_SV" "$STRESS_SV" "$STRESS_TESTBENCH"
elif command -v iverilog >/dev/null 2>&1 && command -v vvp >/dev/null 2>&1; then
  iverilog -g2012 -Wall -o "$GENERATED_DIR/fork_stategroup_stress_equiv.vvp" \
    "$STRESS_REF_SV" "$STRESS_SV" "$STRESS_TESTBENCH"
  vvp "$GENERATED_DIR/fork_stategroup_stress_equiv.vvp"
else
  echo "Could not find xrun or iverilog/vvp for stress simulation." >&2
  exit 1
fi

echo "Generating exhaustive Fizzim 1.0-compatible diagram"
node "$SCRIPT_DIR/generate_fizzim1_compat.js" "$EXHAUSTIVE_FZM" "$EXHAUSTIVE_COMPAT_FZM"
if grep -q '<fork>\|<stategroup>\|<entryState>' "$EXHAUSTIVE_COMPAT_FZM"; then
  echo "Exhaustive compatibility diagram still contains Fizzim 2.0-only objects" >&2
  exit 1
fi

echo "Generating exhaustive Fizzim 2.0 feature RTL"
"$PERL_BIN" "$BACKEND" -noaddversion "$EXHAUSTIVE_FZM" > "$EXHAUSTIVE_SV"
grep -q 'module exhaustive_generation ' "$EXHAUSTIVE_SV"
grep -q '// datapath transition actions' "$EXHAUSTIVE_SV"
grep -q 'SG_A\.S_A0' "$EXHAUSTIVE_SV"
grep -q 'SG_D\.S_D0' "$EXHAUSTIVE_SV"
if ! awk '
  /parameter P_ZERO = 0,/ { seen_zero = NR }
  /parameter P_ONE = 1,/ { seen_one = NR }
  /parameter P_TWO = 2,/ { seen_two = NR }
  /parameter P_LIMIT = 4,/ { seen_limit = NR }
  /parameter P_MASK = 4.hA/ { seen_mask = NR }
  END {
    exit !(seen_zero && seen_one && seen_two && seen_limit && seen_mask &&
           seen_zero < seen_one && seen_one < seen_two &&
           seen_two < seen_limit && seen_limit < seen_mask)
  }
' "$EXHAUSTIVE_SV"; then
  echo "Exhaustive feature RTL did not preserve GUI parameter order" >&2
  exit 1
fi
if [[ -f "$REPO_ROOT/fizzim.jar" ]] && command -v "$JAVA_BIN" >/dev/null 2>&1; then
  EXHAUSTIVE_JAVA_SV="$GENERATED_DIR/exhaustive_generation.java.sv"
  echo "Checking Java-launched backend matches Perl backend"
  FIZZIM_PERL="$PERL_BIN" "$JAVA_BIN" -cp "$REPO_ROOT/fizzim.jar" FizzimJavaBackend -noaddversion "$EXHAUSTIVE_FZM" > "$EXHAUSTIVE_JAVA_SV"
  if ! diff -u "$EXHAUSTIVE_SV" "$EXHAUSTIVE_JAVA_SV" >/dev/null; then
    echo "Java-launched backend output differs from direct Perl backend output" >&2
    diff -u "$EXHAUSTIVE_SV" "$EXHAUSTIVE_JAVA_SV" >&2 || true
    exit 1
  fi
fi

echo "Generating exhaustive Fizzim 1.0-compatible RTL"
"$PERL_BIN" "$FIZZIM1_BACKEND" -noaddversion "$EXHAUSTIVE_COMPAT_FZM" > "$EXHAUSTIVE_COMPAT_SV"
grep -q 'module exhaustive_generation_fizzim ' "$EXHAUSTIVE_COMPAT_SV"
if grep -q '// datapath transition actions\|SG_A\.S_A0\|fork' "$EXHAUSTIVE_COMPAT_SV"; then
  echo "Exhaustive compatibility RTL leaked transition actions, state-group names, or fork names" >&2
  exit 1
fi

echo "Compiling and simulating exhaustive generation equivalence"
if [[ "$SIM" == "xrun" ]] && command -v xrun >/dev/null 2>&1; then
  xrun -64bit -sv -clean -access +rwc -top tb_exhaustive_generation_equiv \
    "$EXHAUSTIVE_COMPAT_SV" "$EXHAUSTIVE_SV" "$EXHAUSTIVE_TESTBENCH"
elif command -v iverilog >/dev/null 2>&1 && command -v vvp >/dev/null 2>&1; then
  iverilog -g2012 -Wall -o "$GENERATED_DIR/exhaustive_generation_equiv.vvp" \
    "$EXHAUSTIVE_COMPAT_SV" "$EXHAUSTIVE_SV" "$EXHAUSTIVE_TESTBENCH"
  vvp "$GENERATED_DIR/exhaustive_generation_equiv.vvp"
else
  echo "Could not find xrun or iverilog/vvp for exhaustive generation simulation." >&2
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
  cat > "$GENERATED_DIR/yosys_check_fork_stategroup_stress.ys" <<YOSYS
read_verilog -sv "testcases/generated/fork_stategroup_stress.sv"
hierarchy -check -top fork_stategroup_stress
proc
opt
check
YOSYS
  yosys -q -s "$GENERATED_DIR/yosys_check_fork_stategroup_stress.ys"
  cat > "$GENERATED_DIR/yosys_check_exhaustive_generation.ys" <<YOSYS
read_verilog -sv "testcases/generated/exhaustive_generation.sv"
hierarchy -check -top exhaustive_generation
proc
opt
check
YOSYS
  cat > "$GENERATED_DIR/yosys_check_exhaustive_generation_fizzim.ys" <<YOSYS
read_verilog -sv "testcases/generated/exhaustive_generation_fizzim.sv"
hierarchy -check -top exhaustive_generation_fizzim
proc
opt
check
YOSYS
  yosys -q -s "$GENERATED_DIR/yosys_check_exhaustive_generation.ys"
  yosys -q -s "$GENERATED_DIR/yosys_check_exhaustive_generation_fizzim.ys"
fi

echo "PASS generic compatibility, fork/state-group stress, and exhaustive generation backend flow"
