# Testbench Area

This folder holds pinned reference material used by the public regression flow.

Detailed documentation now lives in the wiki:

https://github.com/cookacounty/fizzim/wiki/Backend-and-Regression-Testing

Legacy reference documentation is summarized here:

https://github.com/cookacounty/fizzim/wiki/Legacy-Documentation

## Contents

- `legacy/fizzim_5_20.pl` - pinned legacy Fizzim backend used by regression tests.
- `legacy/FizzimGui_5_20.java` - legacy GUI source kept for reference only.
- `legacy/fizzim_tutorial_20160423.pdf` - original tutorial/full documentation PDF.
- `run_public_reference_flow.sh` - builds visual FSM diagrams from public HDL
  projects and checks the generated RTL where direct comparison is practical.

## Public Reference FSMs

The public-reference flow keeps each external project in its own subfolder:

- `wicker-systemverilog-fsm`: BSD-3-Clause Moore FSM examples. The flow
  directly compares generated Fizzim RTL against the original landing-gear and
  ticket-machine Verilog by forcing every state and input combination.
- `amclain-verilog-uart`: MIT UART transmitter/receiver control-state
  reconstructions. The full UART datapath remains in the source project; this
  fixture is for visual FSM review and generated-RTL syntax checking.
- `timrudy-uart-verilog`: GPL-3.0 UART control-state reconstructions. The GPL
  source checkout is intentionally ignored by Git; keep this as a local/manual
  review fixture unless you explicitly want GPL source included.

The downloaded `source/` folders are ignored. Re-clone the public repositories
there before running comparisons that need original source files.

Run the regression from the repository root:

```bash
make test
```

Windows fallback:

```powershell
.\make.cmd test
```
