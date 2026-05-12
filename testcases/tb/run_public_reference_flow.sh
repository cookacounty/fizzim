#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
GENERATE_JS="$SCRIPT_DIR/generate_public_reference_fsms.js"

PERL_BIN="${PERL_BIN:-perl}"
if [[ "$PERL_BIN" == "perl" && -x "/c/Users/MEA10713/AppData/Local/Programs/Git/usr/bin/perl.exe" ]]; then
  PERL_BIN="/c/Users/MEA10713/AppData/Local/Programs/Git/usr/bin/perl.exe"
fi

add_oss_to_path() {
  local suite="${OSS_CAD_SUITE:-}"
  if [[ -z "$suite" && -d "/c/Users/MEA10713/Downloads/oss-cad-suite-windows-x64-20260506/oss-cad-suite" ]]; then
    suite="/c/Users/MEA10713/Downloads/oss-cad-suite-windows-x64-20260506/oss-cad-suite"
  fi
  if [[ -n "$suite" && -d "$suite/bin" ]]; then
    export PATH="$suite/bin:$suite/lib:$PATH"
  fi
}

generate_rtl() {
  local fzm="$1"
  local out="$2"
  mkdir -p "$(dirname "$out")"
  "$PERL_BIN" "$REPO_ROOT/fizzim.pl" -noaddversion "$fzm" > "$out"
}

compile_generated() {
  local rtl="$1"
  local top="$2"
  if command -v iverilog >/dev/null 2>&1; then
    iverilog -g2012 -Wall -s "$top" -o "${rtl%.v}.vvp" "$rtl"
  fi
  if command -v yosys >/dev/null 2>&1; then
    local ys="${rtl%.v}.ys"
    local yosys_rtl="$rtl"
    if command -v cygpath >/dev/null 2>&1; then
      yosys_rtl="$(cygpath -m "$rtl")"
    fi
    cat > "$ys" <<YOSYS
read_verilog -sv "$yosys_rtl"
hierarchy -check -top $top
proc
opt
check
YOSYS
    yosys -q -s "$ys"
  fi
}

run_wicker_equivalence() {
  local landing_source="$SCRIPT_DIR/wicker-systemverilog-fsm/source/landing-gear-controller/verilog/LandingGear.v"
  local ticket_source="$SCRIPT_DIR/wicker-systemverilog-fsm/source/ticket-machine/verilog/TicketMachine.v"
  if [[ ! -f "$landing_source" || ! -f "$ticket_source" ]]; then
    echo "Skipping Wicker source equivalence: source/ checkout is not present." >&2
    return
  fi
  if ! command -v iverilog >/dev/null 2>&1 || ! command -v vvp >/dev/null 2>&1; then
    echo "Skipping Wicker source equivalence: iverilog/vvp not found." >&2
    return
  fi

  iverilog -g2012 -Wall -s tb_landing_gear_reference_equiv \
    -o "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/landing_gear_reference_equiv.vvp" \
    "$landing_source" \
    "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/landing_gear_fsm.v" \
    "$SCRIPT_DIR/tb_landing_gear_reference_equiv.sv"
  vvp "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/landing_gear_reference_equiv.vvp"

  iverilog -g2012 -Wall -s tb_ticket_machine_reference_equiv \
    -o "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/ticket_machine_reference_equiv.vvp" \
    "$ticket_source" \
    "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/ticket_machine_fsm.v" \
    "$SCRIPT_DIR/tb_ticket_machine_reference_equiv.sv"
  vvp "$SCRIPT_DIR/wicker-systemverilog-fsm/generated/ticket_machine_reference_equiv.vvp"
}

cd "$REPO_ROOT"
add_oss_to_path

echo "Generating public reference Fizzim diagrams"
node "$GENERATE_JS"

declare -A CASES=(
  ["wicker-systemverilog-fsm/landing_gear_fsm"]="landing_gear_fsm"
  ["wicker-systemverilog-fsm/ticket_machine_fsm"]="ticket_machine_fsm"
  ["amclain-verilog-uart/amclain_uart_tx_fsm"]="amclain_uart_tx_fsm"
  ["amclain-verilog-uart/amclain_uart_rx_fsm"]="amclain_uart_rx_fsm"
  ["timrudy-uart-verilog/timrudy_uart8_tx_fsm"]="timrudy_uart8_tx_fsm"
  ["timrudy-uart-verilog/timrudy_uart8_rx_fsm"]="timrudy_uart8_rx_fsm"
)

for rel in "${!CASES[@]}"; do
  top="${CASES[$rel]}"
  echo "Generating $top"
  generate_rtl "$SCRIPT_DIR/$rel.fzm" "$SCRIPT_DIR/$(dirname "$rel")/generated/$top.v"
  compile_generated "$SCRIPT_DIR/$(dirname "$rel")/generated/$top.v" "$top"
done

run_wicker_equivalence

echo "PASS public reference FSM generation flow"
