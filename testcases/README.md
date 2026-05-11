# Public Backend Testcase

This folder contains the public regression testcase used to compare Fizzim 2.0
feature diagrams against a generated Fizzim 1.0-compatible golden diagram.

Detailed documentation now lives in the wiki:

https://github.com/cookacounty/fizzim/wiki/Backend-and-Regression-Testing

## Key Files

| File | Purpose |
| --- | --- |
| `generic_state_machine.fzm` | Fizzim 2.0 source diagram using forks, state groups, state-group default entry, and transition actions. |
| `generic_project.fzp` | Minimal project file listing the generic diagrams for testing `File > Project > Build All` and project-level `Lint All`. |
| `generic_state_machine_lint_issues.fzm` | Copy of the generic diagram with intentional lint issues for GUI validation demos. |
| `generate_fizzim1_compat.js` | Generates a Fizzim 1.0-compatible golden diagram from the source diagram. |
| `tools/generate_fork_stategroup_stress.js` | Generates an intentionally non-human-readable stress diagram, independent reference RTL, and testbench for multi-level forks mixed with state groups. |
| `tools/fuzz_backend_compare.js` | Creates randomized non-human-readable variants of the generic diagram and compares direct Perl generation against Java-launched generation. |
| `tb_generic_state_action_equiv.sv` | Drives the generated golden RTL and feature RTL with identical inputs. |
| `tb_fork_stategroup_stress_equiv.sv` | Drives the generated fork/state-group stress RTL against independent handwritten reference RTL. |
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

## Project Files

Fizzim project files use the `.fzp` extension and are intentionally simple text
files. Each nonblank, non-comment line names one `.fzm` diagram. Relative paths
are resolved from the project file location.

Example:

```text
# Fizzim 2.0 project
generic_state_machine.fzm
generic_state_machine_lint_issues.fzm
```

Use `File > Project > Build All` to generate HDL for every diagram in the
current project using the configured HDL generation settings. Use
`File > Project > Lint All` or the Project pane `Lint All` button to run lint
across every diagram in the project.

Projects are saved as `.fzp` files. Fizzim enforces that extension, auto-saves
project membership changes, shows recent projects under
`File > Open Recent Project`, and reopens the last project or diagram at startup
when the remembered path still exists.
