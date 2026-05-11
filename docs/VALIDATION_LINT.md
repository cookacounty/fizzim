# Fizzim RTL/FSM Lint

Detailed documentation now lives in the GitHub wiki:

https://github.com/cookacounty/fizzim/wiki/Validation-and-Lint

This file is kept as a repo-local design note and may lag the wiki.

This document captures the intent behind the Fizzim 2.0 lint interface. Lint is
available from the main-window `Lint` button, `Tools > Validate / Lint Diagram`,
and project-level `Lint All`. The command runs structural checks that should
block generation, then adds a deeper ASIC/RTL review pass for legal diagrams
that may still generate risky, surprising, or hard to sign off state-machine
RTL.

Saving a diagram also runs lint silently and updates the toolbar lint-status
indicator. The indicator is advisory: green means clean, yellow means warnings,
red means errors, and gray means lint is stale or has not been run since the
last edit. Clicking the indicator opens the normal lint pane.

## First Implemented Checks

- Runs the existing structural validator and includes those errors in the lint
  report.
- Checks source-local transition priorities:
  - priority must be an integer from 0 to 10000,
  - duplicate priorities from the same source are errors,
  - an always-true/default transition above lower-priority transitions is an
    error because those lower-priority transitions cannot be reached.
- Checks fork structure:
  - no incoming transition is an error,
  - no outgoing transition is an error,
  - pass-through, fan-in, fan-out, and partial-branch forks are allowed.
- Checks transition equations for references to names that are not declared in
  the global input/output lists, machine parameters/defines, or as built-in FSM
  signals. Verilog constants and macro references are ignored.
- Checks reachability from `reset_state` through normal transitions, state-group
  exits, group default entries, and fork branches.
- Checks state coverage:
  - states with no outgoing transition are warned when implied loopback is off.
- Checks transition actions for blank RHS values and likely full-assignment text
  entered where only an expression should be used.
- Checks registered outputs for missing reset values.

## Example Diagram

The public testcase area includes
`testcases/generic_state_machine_lint_issues.fzm`. It is intentionally not a
golden regression input. Instead, it is a compact GUI showcase for the lint
interface. Open it in Fizzim and press the `Lint` toolbar button to see common
findings. It is rebuilt from the lint-clean generic source diagram and then adds
simple structural examples for an unreachable state, a transition into a fork
with no exit, and a fork branch with no incoming transition.

## RTL Rationale

Hardware FSMs are safest when their intent is explicit. In generated Verilog,
that means:

- every reset path lands in a known reachable state,
- every state has a clearly defined next-state behavior,
- priority logic is intentional and ordered,
- forked control flow resolves to concrete destination states,
- transition actions are RHS expressions that the backend can place safely into
  generated assignment statements.

Multiple incoming transitions to the same fork are valid in the Fizzim backend:
each incoming transition is crossed with each outgoing branch and the generated
transition condition becomes `(incoming_condition) && (branch_condition)`. This
is expected HDL behavior, so lint should not warn merely because a fork has
fan-in.

These checks line up with common RTL lint themes from Verilator and Verible:
incomplete decision coverage, unreachable branches, accidental priority logic,
bad assignment style, and synthesis/simulation mismatch risk.

They also borrow deliberately from safety-oriented statechart practice:

- MathWorks/Stateflow edit-time checks flag dangling transitions, transition
  shadowing, unreachable states and junctions, invalid default/entry paths, and
  unexpected backtracking.
- MAB/MAAB Stateflow guidelines call out unconnected states/transitions,
  default transition discipline, and backtracking prevention in transition
  paths.
- NASA safety guidance stresses known safe initialization, safe transitions
  between predefined states, and verifying transition preconditions and
  postconditions for safety-critical behavior.
- DO-331-style model-based development guidance emphasizes model verification
  and model coverage, which maps well to Fizzim checks for unreachable states,
  uncovered branches, and unintended behavior before Verilog generation.

## Good Next Checks

- Parse transition equations enough to detect exact duplicate conditions from
  the same source.
- Detect simple complementary branch coverage, such as `pass` plus `!pass`, and
  suppress default-branch warnings when the branch set is provably exhaustive.
- Detect transition shadowing where an earlier condition fully contains a later
  condition from the same source.
- Detect fork cycles and transition paths that can backtrack through forks
  before reaching a concrete destination.
- Add explicit precondition/postcondition style annotations for safety-critical
  transitions and lint when they are missing from selected states or groups.
- Add a project-level allow-list for intentionally external/package-scope
  equation identifiers.
- Flag state and transition actions that assign unknown outputs.
- Identify state groups whose shared exits shadow every child-state transition.
- Add severity filtering and clickable object selection from lint results.
- Add optional generated-Verilog lint integration with Verilator/Verible when
  those tools are available on the user's path.
