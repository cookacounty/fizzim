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
- Changed transition-action defaults to blank, so an empty transition action
  means "do not assign this variable on this transition" rather than inheriting
  the state/output default.
- Updated transition-action display in the GUI: conditions remain plain, while
  transition actions are shown in bold `/ output <= value` notation and the
  quick property inspector labels them as actions.
- Grouped transition conditions and actions into one left-aligned transition
  label block so users do not have to manage several independent floating text
  labels on one transition.
- New and reset loopback transitions now start as a conventional side loop
  instead of snapping through odd border points on the state.
- Added a Verilog Parameters pane to the FSM Interface/global property editor
  and a matching `FSM Interface > Parameters` menu item. Parameters remain
  stored as backend-compatible machine attributes with `type == "parameter"`.
- Added Verilog parameters to the main FSM summary text under a separate
  `PARAMETERS` section.
- Parameter rows may now include HDL type/declaration text such as `integer`,
  `real`, packed ranges, or array ranges before/after the parameter identifier.
  Lint and instantiation helpers extract the actual parameter name from the
  declaration so typed parameters still work in expressions.
- Added a user-facing `Internals` global-attribute pane for FSM variables that
  should be generated internally but omitted from the module port list. These
  remain stored as outputs with `suppress_portlist` for parser and backend
  compatibility.
- Added one-click conversion between Outputs and Internals in the global
  property editor while preserving the existing HDL-compatible storage.
- Removed the exposed States and Transitions tabs/menu entries from the global
  property editor; those derived backend attributes remain managed through the
  object property editors and output/internal definitions.
- Kept transition priority as a built-in transition property so it remains
  editable from transition dialogs and the property inspector without needing
  the old global Transitions editor.
- Forced transition attribute synchronization before showing transition
  property editors/inspector rows, restoring `priority` for older diagrams that
  did not already carry the built-in priority attribute.
- Normalized transition priority so the property row appears immediately after
  `equation`, and default/implied priority labels stay hidden on the canvas.
- Normalized older transition priority edit flags so the priority value can be
  edited from transition property dialogs and the property inspector.
- Transition priority edits now stop the active table edit before saving, and
  automatic priority assignment preserves user-local priority values.
- Stopped generic diagram commits from renumbering existing transition
  priorities. Automatic priority assignment now applies only to newly created
  transitions, preventing older diagrams from changing HDL branch order after
  unrelated edits.
- Deleting an output or internal now removes the matching per-state,
  per-state-group, and per-transition attributes as well as the global
  definition, so stale local assignments cannot survive the delete.
- Output/internal deletion now ignores legacy name-column edit flags, so
  signals from older diagrams can still be deleted from the Outputs or
  Internals tab.
- Global property deletion now resolves selected rows to attribute objects
  before removing anything, avoiding stale-index crashes in filtered tabs such
  as Outputs, Internals, and Parameters.
- Global property deletion now keeps the active tab fixed for the entire delete
  operation and falls back to name/type matching if the selected table row is
  not the same object instance as the global list row.
- Fixed global property tab-to-list mapping for Inputs and Outputs, so Delete
  and Add operate on the intended backend list after the Parameters and
  Internals panes are inserted into the editor.
- Audited filtered global-property tables so selection, move between
  Output/Internal, and Up/Down reordering consistently translate visible rows
  back to the underlying attribute list.
- Fixed the Inputs global-property tab so Add/Delete immediately refreshes the
  table instead of deferring visible changes until a later click or dialog
  action.
- Selecting a diagram object now automatically switches the left sidebar from
  Project back to Properties so the quick inspector follows canvas selection.
- The FSM summary text is now selectable and movable as one protected text
  group. Double-clicking a summary section opens the matching FSM Interface tab
  such as State Machine, Inputs, Parameters, Outputs, or Internals.
- The FSM summary is ordered last in hit-testing, so overlapping states, forks,
  state groups, and transitions are selected before the summary text.
- To keep selection behavior conservative, the FSM summary is direct-click
  selectable but is not included in Ctrl+A or drag-box selection.
- Selecting the FSM summary now draws both the overall summary box and a smaller
  section box; double-clicking opens the FSM Interface tab for that selected
  section. The summary also uses the same blue hover highlight as other canvas
  objects.
- Property tables now support spreadsheet-like keyboard editing: typing on a
  selected text cell replaces the current value, while arrow keys, Enter, and
  Tab commit edits and move the selected cell.
- In property dialogs, Enter activates OK and Esc activates Cancel when a table
  cell is not actively being edited; active edits still get the first Esc/Enter.
- Simplified property editor panes by hiding backend-oriented columns that do
  not make sense for the selected item type. Parameters are treated as always
  visible in the FSM summary, and inputs no longer show unused default-value
  controls.
- Property table selection now uses a pale selected-row cue with a white active
  cell and bold blue border, and typing into a selected text cell more reliably
  replaces the previous value instead of appending.
- The side property pane now uses the same spreadsheet-style table setup as the
  full property editor.
- Transition property tables and the side property inspector now add visual
  dividers between identity/condition, priority, and transition action rows.
- The main window now remembers its last size, position, and maximized state in
  per-user Java preferences, restoring only when the saved bounds intersect a
  current screen.
- Added parent-qualified debug state names for grouped states, so simulation
  debug can display names like `GROUP.CHILD`.
- Increased the generated debug `statename` width to 256 characters by default.

### Validation And Lint

- Added a separate `Tools > Validate / Lint Diagram` interface focused on
  RTL/FSM quality rather than only structural correctness.
- Added lint checks for transition priority ranges, duplicate source-local
  priorities, unreachable lower-priority branches behind default transitions,
  and missing default branches on prioritized transition sets.
- Increased the valid transition-priority lint range to 0 through 10000.
- Added fork linting for missing incoming or outgoing transitions while allowing
  forks to be pass-through, fan-in, fan-out, or partial branch points.
- Added interactive lint results: selecting an issue selects/highlights the
  associated state, fork, state group, or transition in the diagram.
- Added a passive toolbar lint-status indicator. Opening or saving a diagram
  runs lint silently and updates the indicator without opening the lint pane.
- Added transition-equation linting for references that are not declared in the
  global input/output lists or as built-in FSM signals.
- Transition-equation reference lint now treats parameters as declared
  expression names and ignores identifiers used only inside bit-select or
  part-select brackets.
- Output/internal attribute editing now defaults new `regdp` entries to a
  reset value of `0`, clears reset values when switching to non-resettable
  `comb` style entries, and commits active table edits before deleting rows.
- Backend fork expansion now walks multi-level fork decision trees until each
  route reaches a concrete state or state group, preserving route priority,
  combined conditions, and transition actions along the flattened path.
- Clarified and regression-tested assignment precedence: transition actions
  override concrete state assignments, and concrete state assignments override
  inherited state-group assignments.
- Added an `Instantiate` toolbar button that shows a copyable parent-module
  Verilog snippet with IO wire declarations, optional parameter defaults, named
  parameter overrides, and named port connections.
- The instantiation helper now notes when internal-only outputs are omitted
  from the module port list, and the canvas summary separates `OUTPUTS` from
  `INTERNALS` so suppressed internal variables are less confusing.
- Audited all main GUI menu items after the Fizzim 2.0 UI changes and added
  `docs/MENU_AUDIT.md`.
- Fixed `Save As` and export file chooser starting directories so they use the
  current diagram directory when available.
- Fixed clipboard export cropping at the canvas edge and made `Clean Selected
  Routes` a no-op when no transition route is selected.
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
- Dragging a state group now snapshots its contained states/forks at mouse-down
  and does not capture additional objects until the mouse is released.
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
- Cleared stale transition hover highlighting during click-selection changes so
  a previously selected/hovered transition cannot remain blue after another
  transition is selected.
- Changed state and state-group internal text to a deterministic HDL-friendly
  layout: bold top-centered names with left-aligned visible values underneath.
  Existing diagrams automatically use the new visual style when opened, while
  saved attributes and backend generation remain unchanged.
- Kept state and transition variable/action display order synchronized with the
  global Outputs/Internals order, so reordering interface variables immediately
  changes the deterministic canvas display order.
- Added selected-object cleanup commands for selected-route cleanup, selected
  horizontal/vertical alignment, and selected horizontal/vertical distribution.
- Added automatic transition priority normalization for sources with multiple
  outgoing transitions. The highest-priority outgoing transition is highlighted
  in bold, while single outgoing transitions keep priority implied and hidden by
  default.
- Duplicate source-local transition priorities now draw thick red on every
  conflicting transition, overriding both normal highest-priority bold styling
  and always-true dark-green styling.
- Single outgoing transitions are also treated as the highest-priority
  transition for their source and therefore render bold.
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
- Added GUI language support under `Settings > Language`, with English,
  Japanese, Simplified Chinese, Traditional Chinese, Korean, German, French,
  Spanish, Portuguese, Hindi, and Russian translations for main-window chrome
  such as menus, toolbar buttons, status indicators, the Project pane, the Lint
  pane, and the quick property inspector. HDL-facing names, attributes, diagram
  content, and generated code remain unchanged for backend compatibility.
- Updated the Help menu to open the Fizzim GitHub wiki in the system browser.
- Added `docs/GUI_WISHLIST.md` to track future GUI modernization ideas.

### Verilog Backend

- Included the Perl backend script `fizzim.pl` in the repository so generated
  Verilog is reproducible from the checked-in source tree.
- Added GUI-driven HDL generation using the checked-in Perl backend. The main
  window now has a Generate button, `Tools > Generate HDL`, and
  `Settings > HDL Generation` for backend command, backend script, output
  directory, filename behavior, and backend options. By default HDL is emitted
  next to the `.fzm` file as `<module_name>.v`.
- Added a power-user HDL comparison mode that can run a second configurable
  backend, emit sidecar comparison HDL, and report a generated diff file when
  the two outputs do not match.
- Clean HDL comparison runs now delete the temporary comparison Verilog output
  after confirming the diff is empty, while mismatches keep the comparison file
  for debug.
- Added an HDL generation status indicator. The main toolbar shows yellow when
  HDL is stale or not yet generated, and green after successful generation until
  the diagram changes again.
- Added a conventional unsaved-change marker in the window title. Modified
  diagrams now show `*Fizzim 2.0 - filename.fzm` until saved.
- Persisted HDL generation state into the diagram file. After successful GUI
  generation, the `.fzm` records that the HDL is in sync and remembers the
  generated HDL path, so reopening the diagram restores the green status when
  the generated file is still present.
- Project Build All now honors each diagram's stored HDL output path when one
  is present, allowing diagrams in the same project to generate to different
  destinations while still using the global HDL settings as defaults.
- Added a Project-pane right-click action to set an individual diagram's HDL
  output path. The path is stored in the selected `.fzm` relative to that FSM
  file, so it stays portable across projects.
- HDL generation metadata now marks the diagram as unsaved instead of silently
  saving the `.fzm`, so the title-bar `*` reflects that the XML changed.
- Tightened GUI dirty-state tracking so opening property dialogs or pressing OK
  without real content changes no longer creates undo points, marks the diagram
  modified, or makes HDL look stale.
- Added a Java HDL backend entry point, `FizzimJavaBackend`, plus Windows and
  Linux launcher scripts. The entry point currently delegates to the checked-in
  Perl backend so Java-launched generation is line-for-line compatible while the
  backend is ported incrementally.
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
- Fixed Fizzim 1.0 compatibility generation for state-group exits that branch
  through forks. The generator now keeps state-group-exit priority as an
  in-memory sort bias instead of encoding it as a negative numeric priority,
  preserving fork branch order before priorities are normalized to 1..1000.
- Strengthened the generic Fizzim 1.0 equivalence testbench to force both DUTs
  into every state and test every input combination for one transition step, so
  priority mismatches in otherwise-unreached grouped states are caught.
- Added a checked-in legacy testbench area under `testcases/tb/legacy/` with the
  old backend and GUI source used for comparison.
- Added Linux/Git Bash-oriented `make` targets for build, clean, jar generation,
  and test execution.
- Kept a Windows `make.cmd` fallback for users without GNU Make.
- Updated test scripts to prefer `xrun` when available and otherwise use OSS CAD
  Suite tools such as Icarus Verilog and Yosys when present.
- Added documentation for the test environment and how to rerun backend
  regressions.
- Added a generated fork/state-group stress regression with nested forks,
  group-level output inheritance, transition actions through fork paths, a
  self-checking equivalence testbench, and Yosys syntax/synthesis checks.
- Added `testcases/tools/fuzz_backend_compare.js` and `make test-fuzz` /
  `make.cmd test-fuzz` for randomized non-human-readable FSM backend fuzzing.
  The fuzz flow mutates the generic feature diagram, exercises forks, state
  groups, transition actions, priorities, internals, and equations, then checks
  direct Perl generation against Java-launched generation line-for-line.
- Added a lightweight project manager. `.fzp` project files are simple text
  lists of `.fzm` diagrams, support relative paths, and can be opened/saved from
  `File > Project`. `Build All` generates HDL for every diagram in the project
  using the configured backend settings.
- Project diagrams are now sorted by project-relative path, so adding a new
  diagram groups it with files in the same folder and keeps the Project tree
  ordered A-Z.
- Root-level project diagrams are normalized into the same `.` tree group even
  when paths were saved with a mix of `foo.fzm` and `./foo.fzm`, avoiding split
  groups and out-of-order entries.
- Normal diagram edits now preserve the current zoom/pan. In fit mode, moving
  objects can zoom out only when needed to keep the expanded diagram visible;
  reducing the diagram extents no longer zooms in automatically.
- The Space shortcut for zoom-to-fit now works even after clicking a toolbar
  button, while still leaving text/table editing alone.
- Scoped the Space zoom-to-fit shortcut to the main canvas/viewport so it does
  not fire from side panes, toolbar controls, or dialogs.
- Transition label groups now draw on a subtle light background so labels remain
  readable when a transition line passes behind them.
- Improved project navigation. Opening a project diagram now reuses the current
  window by default, prompts before unsaved edits are discarded, marks the active
  dirty project file in the tree, and offers right-click Open in New Window and
  Remove from Project actions.
- Added project-level Lint All from both `File > Project` and the Project pane,
  with an aggregated multi-diagram lint report.
- Made project creation explicit. New Project now prompts for a `.fzp` path,
  adding diagrams requires a saved project file, and add/remove changes
  auto-save the project so relative paths stay predictable.
- Added recent projects under `File > Open Recent Project`, keeping a separate
  quick-open list for `.fzp` project files.
- Recent file and project menus now prune entries whose target files have been
  moved or deleted, avoiding stale-path errors.
- Project create/save dialogs now explicitly show that project files use the
  enforced `.fzp` extension and confirm the final path when the extension is
  added automatically.
- Opening a project now automatically switches the side panel to the Project tab.
- Fizzim now attempts to reopen the last opened project or diagram at startup,
  while safely ignoring missing files.
- Opening a diagram from the Project pane no longer replaces the startup restore
  target; Fizzim will restore the project on the next launch.
- Startup restore now also remembers the last opened FSM inside the restored
  project. If no project was active, Fizzim still restores the last standalone
  FSM diagram.
- Closing Fizzim with a project open refreshes the startup restore target to the
  project, making restore more robust after mixed project and diagram activity.

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
- Refined the Fizzim 2.0 branding with a simpler modern splash screen, the
  `F^2izz||m` logic-inspired wordmark, and a compact `F^2` application icon.
- Kept attribution to original author Michael Zimmer and added Aaron Cook for
  Fizzim 2.0 feature updates.
