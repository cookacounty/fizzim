# Public Backend Testcase

This folder now uses one source diagram:

| File | Purpose |
| --- | --- |
| `generic_state_machine.fzm` | Fizzim 2.0 source diagram using forks, state groups, state-group default entry, and transition actions. |
| `generate_fizzim1_compat.js` | Generates a Fizzim 1.0-compatible golden diagram from the source diagram. |
| `tb_generic_state_action_equiv.sv` | Drives the generated golden RTL and feature RTL with identical inputs. |

The generated golden diagram is written to:

```text
testcases/generated/generic_state_machine_fizzim1.fzm
```

It is intentionally generated, not hand-maintained. The single source of truth
is `generic_state_machine.fzm`.

## What The Flow Proves

The test compares:

- feature RTL generated directly from the Fizzim 2.0 diagram
- golden RTL generated from the Fizzim 1.0-compatible diagram

The compatibility generator expands Fizzim 2.0 modeling conveniences into
plain old Fizzim constructs:

- state-group outputs are copied to child states when the child leaves the
  output blank
- state-group exit transitions are copied to every child state
- transitions into a state group are redirected to the selected default entry
  child state
- forks are expanded into direct transitions with combined equations
- transition actions are converted into equivalent source-state regdp
  assignments

The compatible diagram removes `<fork>`, `<stategroup>`, and `<entryState>` so
it can be used as the old-style golden reference.

## Priority Rules Under Test

- Lower numeric transition priorities are emitted first.
- An equation of `1` is treated as a fallback and is emitted after conditional
  branches at the same priority.
- Fork branch ordering is kept inside the incoming transition's priority slot.
- State-group exit transitions are generated ahead of child-authored
  transitions from the same state.
- A group target resolves to the default entry state, never the group object.

## Running

From the repo root:

```bash
make test
```

or directly:

```bash
bash testcases/run_backend_flow.sh
```

The script uses:

- `perl` on `PATH`, or `PERL_BIN`, or `FIZZIM_PERL`
- `node` to generate the Fizzim 1.0-compatible diagram
- `xrun` when available, otherwise `iverilog`/`vvp`
- `yosys` when available for extra syntax/synthesis checks

To compare against an actual old backend, point `FIZZIM1_BACKEND` at it:

```bash
FIZZIM1_BACKEND=/path/to/old/fizzim.pl make test
```

Generated diagrams, RTL, simulator outputs, and Yosys scripts live under
`testcases/generated/`, which is ignored by Git.
