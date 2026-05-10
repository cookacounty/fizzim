# Fizzim RTL/FSM Lint

This document captures the intent behind the Fizzim 2.0 lint interface. The
existing `Validate Diagram` command catches structural errors that should block
generation. `Lint Diagram` is meant to be a deeper ASIC/RTL review pass: it
looks for legal diagrams that may still generate risky, surprising, or hard to
sign off state-machine RTL.

## First Implemented Checks

- Runs the existing structural validator and includes those errors in the lint
  report.
- Checks source-local transition priorities:
  - priority must be an integer from 0 to 1000,
  - duplicate priorities from the same source are errors,
  - an always-true/default transition above lower-priority transitions is an
    error because those lower-priority transitions cannot be reached,
  - multiple prioritized transitions without a default branch are warnings.
- Checks fork structure:
  - no incoming transition is an error,
  - fewer than two outgoing transitions is an error,
  - multiple fork branches without a default branch is a warning.
- Checks transition equations for references to names that are not declared in
  the global input/output lists or as built-in FSM signals.
- Checks reachability from `reset_state` through normal transitions, state-group
  exits, group default entries, and fork branches.
- Checks state coverage:
  - states with no outgoing transition are warned when implied loopback is off,
  - states with multiple effective outgoing transitions are warned when no
    default branch exists.
- Checks transition actions for blank RHS values and likely full-assignment text
  entered where only an expression should be used.
- Checks registered outputs for missing reset values.

## RTL Rationale

Hardware FSMs are safest when their intent is explicit. In generated Verilog,
that means:

- every reset path lands in a known reachable state,
- every state has a clearly defined next-state behavior,
- priority logic is intentional and ordered,
- default/else behavior is explicit when conditions do not cover every case,
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
