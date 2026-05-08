# Public Backend Testcases

This folder contains a shippable testcase for the fork and state-group features.
It uses two versions of the same state machine:

| File | Purpose |
| --- | --- |
| `generic_ctrl_fsm.fzm` | Golden reference written the old way, with every transition and output repeated explicitly. |
| `generic_ctrl_fsm_features.fzm` | Equivalent machine using fork dots and state groups to remove duplication. |

The scripts regenerate Verilog from both `.fzm` files, compile the generated
RTL, and run a testbench that drives both machines with identical inputs.

## What The Machine Covers

The generic controller is intentionally small enough to read in the GUI:

- six real states: `S_IDLE`, `S_LOAD`, `S_RUN_A`, `S_RUN_B`, `S_DONE`, and `S_ERROR`
- one state group, `G_RUN`, around `S_RUN_A` and `S_RUN_B`
- two fork points:
  - `FORK_START` routes `start` to either load or run based on `mode`
  - `FORK_RESULT` routes shared run completion logic to retry, done, or error
- shared group outputs: `G_RUN` owns `busy=1` and `op_enable=1`
- a shared group transition through a fork: `G_RUN -> FORK_RESULT`
- fork branch priority handling: the shared `G_RUN -> FORK_RESULT` priority
  stays ahead of the direct `G_RUN -> S_ERROR` abort transition, even though
  the fork's outgoing branch priorities vary
- debug `statename` strings for grouped states, for example `G_RUN.S_RUN_A`

The feature version should generate logic equivalent to the golden version.
Forks and state groups are diagram-only/modeling conveniences; they should not
become real encoded states in generated Verilog.

## Windows Git Bash

From the repo root:

```bash
make test
```

or directly:

```bash
bash testcases/run_backend_flow.sh
```

The script looks for:

- `perl` on `PATH`, or `FIZZIM_PERL`, or the bundled SmartGit Perl path used in this workspace
- OSS CAD Suite at `OSS_CAD_SUITE`, or the local download path used in this workspace

You can override paths:

```bash
OSS_CAD_SUITE=/c/path/to/oss-cad-suite PERL_BIN=/c/path/to/perl.exe make test
```

## Linux

From the repo root:

```bash
make test
```

By default the Linux script uses `xrun` for simulation. If `xrun` is not found
but `iverilog` and `vvp` are available, it falls back to Icarus Verilog.

Useful overrides:

```bash
PERL_BIN=/path/to/perl SIM=xrun bash testcases/run_backend_flow.sh
SIM=iverilog bash testcases/run_backend_flow.sh
```

## Generated Files

Generated RTL and simulator outputs go under:

```text
testcases/generated/
```

That folder is ignored by Git. The source `.fzm` files, testbench, and scripts
are the files meant to be reviewed and shipped.
