# Changelog

## Fizzim 2.0 - Current Development

This summarizes the major changes made while evolving the original Fizzim tool
into Fizzim 2.0. It intentionally focuses on features and workflows that remain
part of the current codebase.

### FSM Modeling

- Added forked transitions. A transition can now target a small fork node, and
  the fork can branch to multiple destination states. This lets common
  conditions be written once before branch-specific conditions are evaluated.
- Added state groups. A state group can contain states and forks, share
  transition behavior across those contained states, and remove repeated state
  transition logic.
- Added state group default entry states. A transition into a state group must
  enter a concrete child state, and the default entry state is shown with a bold
  outline.
- Defined priority behavior for forks and state groups. State group exits take
  priority over internal state transitions, and fork outgoing transitions are
  resolved by transition priority.
- Added transition actions. Transitions can now assign `regdp` outputs directly,
  making one-cycle transition pulses easier to express without relying on
  `nextstate` expressions in destination states.
- Added a user-facing `Internals` global-attribute pane for FSM variables that
  should be generated internally but omitted from the module port list. These
  remain stored as outputs with `suppress_portlist` for parser and backend
  compatibility.
- Added parent-qualified debug state names for grouped states, so simulation
  debug can display names like `GROUP.CHILD`.
- Increased the generated debug `statename` width to 256 characters by default.

### Validation And Lint

- Added a separate `Tools > Validate / Lint Diagram` interface focused on
  RTL/FSM quality rather than only structural correctness.
- Added lint checks for transition priority ranges, duplicate source-local
  priorities, unreachable lower-priority branches behind default transitions,
  and missing default branches on prioritized transition sets.
- Added fork linting for missing incoming or outgoing transitions while allowing
  forks to be pass-through, fan-in, fan-out, or partial branch points.
- Added interactive lint results: selecting an issue selects/highlights the
  associated state, fork, state group, or transition in the diagram.
- Added transition-equation linting for references that are not declared in the
  global input/output lists or as built-in FSM signals.
- Added reset reachability linting for real states, including paths through
  state-group exits, group default entries, and forks.
- Added state coverage linting for states with no outgoing transition when
  implied loopback is disabled, and for multiple effective outgoing transitions
  without a default branch.
- Added transition-action linting for blank RHS values and likely full
  assignments entered where only RHS expressions should be used.
- Added registered-output linting for missing reset values.
- Added a validation/lint design note under `docs/VALIDATION_LINT.md`.
- Added `testcases/generic_state_machine_lint_issues.fzm`, a deliberately
  imperfect generic diagram for exercising and demonstrating common lint
  findings in the GUI.
- Expanded the lint showcase diagram with unreachable-state and malformed-fork
  examples, including a fork with an incoming transition but no exits and a fork
  branch with no incoming transition.
- Rebased the lint showcase diagram from the generic source diagram after
  example-layout edits, preserving the intentional lint issues.

### Diagram Editing

- Updated normal states to use rounded rectangles with white fill, improving
  readability compared with the old circular state shape.
- Updated state groups to use rounded rectangles with a very light blue fill, so
  grouped states remain visible while groups are still distinguishable.
- Kept state groups behind states and forks in the GUI so contained objects stay
  selectable.
- Added validation for states that ambiguously overlap state groups.
- Added Verilog/SystemVerilog reserved-word checks when adding or renaming FSM
  objects such as states and outputs.
- Improved fork nodes by making them larger, non-resizable, and easier to grab.
- Added fork tooltips that show the fork name on hover.
- Added propagation when renaming objects in the property editor, preventing
  assignments from being lost when a signal or state is renamed.
- Added up/down controls in the attribute editor tabs so attributes can be
  reordered.
- Added reset behavior for transition label placement.
- Improved transition route editing:
  - Larger invisible grab regions for route handles.
  - Hover cursor feedback over route handles and editable transition routes.
  - A `Reset Transition Route` context-menu action.
  - Dragging a transition line bends the existing Bezier route.
  - Dragging a transition line now lightly pulls the nearest endpoint while
    still favoring line bowing, making route shaping more natural.
  - Dragging a connection point along a state or state-group edge now moves the
    adjacent bend/control point with it, preserving the existing bend shape.
  - Transition labels now anchor to the actual visible Bezier curve midpoint, so
    labels stay near the visual line even after aggressive route edits.
  - Default `equation == 1` transition labels are hidden on the canvas and the
    corresponding always-true/default transition lines are drawn dark green.
  - Larger states and state groups expose finer connection points while keeping
    old saved connection indices compatible.
- Improved automatic transition alignment so multiple transitions entering or
  leaving the same state, fork, or state group are distributed along the relevant
  edge instead of stacking on the same connection point.
- Updated selection behavior to follow standard desktop conventions:
  - Plain click selects one object.
  - Ctrl-click toggles objects in and out of the current selection.
  - Shift-click adds objects to the current selection.
  - Drag selection replaces by default, Shift-drag adds, and Ctrl-drag toggles.
  - Ctrl+A selects all movable diagram objects.
- Added Ctrl+C/Ctrl+V diagram copy and paste for selected states, state groups,
  forks, and free text. Pasted state-machine objects get unique names, copied
  state groups keep their copied children, and transitions are copied when their
  endpoints are part of the copied selection.
- Added a selection status display showing the current selected object counts,
  including updates after Ctrl+A and empty-space deselection.
- Improved batch movement of selected states, state groups, forks, and text so
  connected transitions move with the selected objects and state-group children
  are not moved twice.
- Added keyboard nudging for selected states, state groups, and forks with arrow
  keys, plus larger Shift+arrow steps.
- Added zoom controls for the canvas:
  - `+` and `-` zoom buttons.
  - Zoom percentage display.
  - Zoom-to-fit button.
  - Ctrl + mouse wheel zoom targeting the mouse position.
- Added right-button drag panning while preserving quick right-click context
  menus.
- Expanded right-button panning so users can pan beyond the current object
  extents without being pinned to the existing canvas edge.
- Added touchpad-friendly canvas gestures: touchpad wheel/pinch streams zoom at
  the pointer, Shift+scroll pans, and dragging from empty canvas space pans
  without needing a right mouse button.
- Added fit-mode behavior. Opening a diagram or pressing `Fit` fits the diagram
  to the viewport, and resizing the window keeps refitting until the user
  manually zooms or pans.
- Replaced the fixed page-sized editing canvas with an object-extents canvas.
  The visible canvas, zoom fit, and image export now size around the current
  page's actual objects plus padding rather than the legacy page setup size.
- Improved zoom-to-fit behavior so it includes visible attribute labels and the
  global state-machine information table, uses a tighter fit margin, and avoids
  zooming above 100% on small or new diagrams.
- Removed the user-facing `Page Setup` menu item because page size no longer
  defines the editor canvas.
- Added Open Recent support for the last 10 opened diagram files.
- Allowed multiple diagram windows to be open at the same time.
- Added page-tab drag reordering while preserving the diagram content associated
  with each moved tab.
- Added a clean transition route command for resetting transition routing and
  labels without moving states.
- Added a right-side property inspector for the current selection. Single
  selected states, state groups, transitions, and loopbacks can be edited from
  the main window, with the full modal editors still available as a fallback.
- Expanded the property inspector so states, state groups, and transitions show
  available HDL variable/action rows, and compatible multi-selections can apply
  one common value across all selected objects.
- Converted the lint interface from a separate blocking workflow into a docked
  bottom panel with issue and report tabs, rerun/close controls, and live canvas
  highlighting.
- Added blue hover affordances for states, state groups, forks, transitions, and
  loopbacks so editable canvas targets are easier to discover before clicking.
- Added selected-object cleanup commands for selected-route cleanup, selected
  horizontal/vertical alignment, and selected horizontal/vertical distribution.
- Added automatic transition priority normalization for sources with multiple
  outgoing transitions. The highest-priority outgoing transition is highlighted
  in bold, while single outgoing transitions keep priority implied and hidden by
  default.
- Added default new-FSM settings for `posedge clk`, `negedge rst_l`, and implied
  loopback behavior.
- Switched canvas text, the global canvas table, and attribute-editor table
  cells to Java's logical `Monospaced` font so HDL names and expressions align
  consistently across Windows and Linux.
- Refined canvas selection behavior so initial drags prefer moving states and
  state groups instead of their internal text, states inside state groups remain
  easy to select, and objects drawn over the global summary table are not
  blocked by that table.
- Changed newly created output and internal variables to default to `0` instead
  of a blank value.
- Cleaned up the main menu structure: validation now uses one
  `Tools > Validate / Lint Diagram` command, transition cleanup actions live
  under `Tools > Clean Up Diagram`, view/default settings moved under
  `Settings`, the old print menu item was removed, and `Global Attributes` was
  renamed to `FSM Interface`.
- Added `docs/GUI_WISHLIST.md` to track future GUI modernization ideas.

### Verilog Backend

- Included the Perl backend script `fizzim.pl` in the repository so generated
  Verilog is reproducible from the checked-in source tree.
- Updated the backend for forked transitions, state groups, state group default
  entry behavior, transition actions, and widened debug `statename` output.
- Pruned lower-priority transitions that become unreachable behind an
  unconditional transition after state-group and fork expansion.
- Kept generated debug-only state-name logic guarded from synthesis.
- Preserved compatibility with existing diagrams by avoiding file-format changes
  for route editing and by preserving legacy connection-point indices.

### Test Infrastructure

- Reworked the public testcases around a generic feature-rich FSM that exercises
  the major Fizzim 2.0 features.
- Added generation of a Fizzim 1.0-compatible diagram from the generic feature
  diagram, allowing the legacy-compatible output to be compared against the new
  feature-driven output.
- Updated the Fizzim 1.0 compatibility generator to prune unreachable expanded
  transitions and emit explicit nonzero priorities for legacy-backend checks.
- Added a checked-in legacy testbench area under `testcases/tb/legacy/` with the
  old backend and GUI source used for comparison.
- Added Linux/Git Bash-oriented `make` targets for build, clean, jar generation,
  and test execution.
- Kept a Windows `make.cmd` fallback for users without GNU Make.
- Updated test scripts to prefer `xrun` when available and otherwise use OSS CAD
  Suite tools such as Icarus Verilog and Yosys when present.
- Added documentation for the test environment and how to rerun backend
  regressions.

### Build And Packaging

- Added `Makefile` targets for common workflows:
  - `make`
  - `make jar`
  - `make clean`
  - `make test`
- Added `make.cmd` equivalents for Windows environments without GNU Make.
- Updated jar generation to compile with `--release 11` by default, so jars built
  with newer JDKs can still run on Java 11 or newer.
- Added local helper script support for launching Fizzim with a user-provided JDK
  without requiring administrator permissions.
- Updated `.gitignore` to ignore Java build outputs and local helper scripts
  while keeping the public generic testcases shareable.

### Documentation And Branding

- Renamed the project presentation to Fizzim 2.0.
- Updated the README with current build instructions, backend behavior, forked
  transitions, state groups, transition actions, and the generic FSM example.
- Added diagrams for forks, state groups, priority behavior, and the Fizzim 2.0
  overview.
- Updated the app title bar and splash screen for Fizzim 2.0.
- Refined the Fizzim 2.0 branding with a simpler modern splash screen, an
  integrated `F²` wordmark, and a larger `F²` application icon for better
  Windows readability.
- Kept attribution to original author Michael Zimmer and added Aaron Cook for
  Fizzim 2.0 feature updates.
