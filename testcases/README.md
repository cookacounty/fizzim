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
| `exhaustive_generation.fzm` | Generated stress diagram with 50+ transitions, multi-tier forks, state groups, transition actions, internals, outputs, inputs, and parameters. |
| `tools/generate_exhaustive_generation_case.js` | Source-of-truth generator for the exhaustive stress diagram and its equivalence testbench. |
| `tools/fuzz_backend_compare.js` | Creates randomized non-human-readable variants of the generic diagram and compares direct Perl generation against Java-launched generation. |
| `tb_generic_state_action_equiv.sv` | Drives the generated golden RTL and feature RTL with identical inputs, then forces every encoded state and checks every input combination for one transition step. |
| `tb_exhaustive_generation_equiv.sv` | Forces every exhaustive stress state through every input combination and compares the Fizzim 2.0 RTL against the generated Fizzim 1.0-compatible RTL. |
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

For the generated stress-test intent and regeneration rules, see
`docs/GENERATION_STRESS_TESTS.md`.

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

The same project build can be run without opening the GUI:

```bash
java -jar fizzim.jar --build-project testcases/generic_project.fzp
```

The command-line build uses the same HDL backend preferences as the GUI Build
All action, including any configured comparison backend. Successful builds
record the HDL output path, Fizzim release, and build number in each diagram so
the GUI can reopen the project with the HDL status in sync.

Generated HDL includes a deterministic header with the Fizzim version and
source diagram name. Source checksums can be enabled from the HDL generation
settings, but are disabled by default so layout-only diagram edits do not
create extra Verilog diffs.

Generated filenames default to `.v`, but the HDL generation settings can use
`.sv` or a custom extension. Explicit extensions in custom output filenames or
per-diagram project HDL paths are preserved.

Projects are saved as `.fzp` files. Fizzim enforces that extension, auto-saves
project membership changes, shows recent projects under
`File > Open Recent Project`, and reopens the last project or diagram at startup
when the remembered path still exists.
