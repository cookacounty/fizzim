# Generation Stress Tests

Fizzim keeps two generated stress tests in `testcases` so backend changes can
be checked against diagrams that exercise newer modeling features.

## Exhaustive Generation Case

`testcases/tools/generate_exhaustive_generation_case.js` is the source of truth
for `testcases/exhaustive_generation.fzm`. Re-run it whenever the stress
requirements change:

```sh
node testcases/tools/generate_exhaustive_generation_case.js
```

The generator intentionally creates a large, grid-spaced diagram rather than a
hand-polished example. It covers:

- at least 50 transitions,
- multiple tiers of forks,
- forks with one to four incoming and outgoing transitions,
- forks both inside and outside state groups,
- transitions into states inside and outside state groups,
- transition actions on paths entering and leaving forks,
- equations and actions that reference inputs, outputs, internals, and
  parameters,
- at least five inputs, outputs, internals, and parameters,
- `regdp`, state-encoded (`reg` in the backend XML), and `comb`
  outputs/internals.

The normal backend flow expands this Fizzim 2.0 diagram into a generated
Fizzim 1.0-compatible diagram at
`testcases/generated/exhaustive_generation_fizzim1.fzm`. It then generates RTL
from both diagrams and runs a simulation that forces every state through every
input combination, comparing generated next-state and output behavior.
It also records transition source/destination pair coverage and requires at
least 90% of the expected pairs to be observed. The expected-pair set is an
intentional over-approximation of structurally possible routes, so the test
prints any uncovered-pair mask for debugging without requiring unreachable or
priority-masked routes to hit 100%.

The Fizzim 1.0 compatibility expansion preserves transition-action priority by
turning each action into a state assignment guarded by the selected transition
condition. That guard includes the transition equation, higher-priority
exclusion terms, and the resulting destination state.

The Fizzim 2.0 backend applies the same priority rule directly when emitting
transition actions: lower-priority transition actions are masked by all
higher-priority transition equations from the same source. This prevents an
action on a low-priority always-true transition from firing when a higher route
was actually selected.

State-group output/internal assignments are also flattened into child states
before HDL generation. A child state receives the parent group assignment unless
that child has a locally edited value for the same signal. Inherited global
defaults on the child do not block the parent group value. The Fizzim
1.0-compatible generated diagram uses the same rule so equivalence tests compare
the intended semantics.

Fork placement relative to a state group is visual only for generation. A fork
drawn inside a state-group box is not listed as a state-group child in the
saved diagram, because forks are not states and do not inherit state-group
outputs. The GUI should still move enclosed forks when a state group is dragged
so the diagram remains visually coherent.

State-encoded `reg` outputs use literal state-bit values in this test. The
backend consumes those values while assigning state encodings, so parameterized
values are reserved for `regdp` and `comb` outputs/internals.

The generated transition priorities start at `1` instead of `0`. The backend
accepts numeric priorities, but older warning paths treat zero like an unset
field when a state has multiple exits.

Use the full test flow for regression:

```sh
./make.cmd test
```

On Linux, the same backend script can be run directly:

```sh
bash testcases/run_backend_flow.sh
```

The generated `.fzm`, generated RTL, simulator output, and Yosys scripts are
test artifacts. If a requirement changes, update the generator and regenerate
instead of manually editing the generated diagram.
