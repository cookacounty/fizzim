# Public Backend Testcase

This folder contains the public regression testcase used to compare Fizzim 2.0
feature diagrams against a generated Fizzim 1.0-compatible golden diagram.

Detailed documentation now lives in the wiki:

https://github.com/cookacounty/fizzim/wiki/Backend-and-Regression-Testing

## Key Files

| File | Purpose |
| --- | --- |
| `generic_state_machine.fzm` | Fizzim 2.0 source diagram using forks, state groups, state-group default entry, and transition actions. |
| `generic_state_machine_lint_issues.fzm` | Copy of the generic diagram with intentional lint issues for GUI validation demos. |
| `generate_fizzim1_compat.js` | Generates a Fizzim 1.0-compatible golden diagram from the source diagram. |
| `tools/fuzz_backend_compare.js` | Creates randomized non-human-readable variants of the generic diagram and compares direct Perl generation against Java-launched generation. |
| `tb_generic_state_action_equiv.sv` | Drives the generated golden RTL and feature RTL with identical inputs. |
| `generated/` | Generated diagrams, RTL, simulator outputs, and Yosys scripts. Ignored by Git. |

## Run

From the repo root:

```bash
make test
```

To run randomized backend comparison fuzzing:

```bash
make test-fuzz
```

or directly:

```bash
bash testcases/run_backend_flow.sh
```

For the lint showcase, see:

https://github.com/cookacounty/fizzim/wiki/Validation-and-Lint
