# Menu Audit

This checklist captures the current GUI menu review. It focuses on whether
each menu item is still wired to a sensible handler after the Fizzim 2.0 UI,
project, lint, HDL-generation, and canvas workflow changes.

## Reviewed Menus

### File

- `New` - prompts for unsaved changes, clears the current diagram, restores
  default FSM globals, and marks HDL/lint status stale.
- `Open` - opens one or more `.fzm` diagrams, reusing a blank window or opening
  additional windows as needed.
- `Open Recent` - prunes missing files before display and forgets entries that
  cannot be opened.
- `Open Recent Project` - prunes missing `.fzp` files before display and
  forgets entries that cannot be opened.
- `Project > New Project` - prompts for a `.fzp` path and creates an empty
  project.
- `Project > Open Project...` - loads `.fzp` paths relative to the project file
  and switches the side pane to Project.
- `Project > Save Project` / `Save Project As...` - writes relative diagram
  paths and enforces the `.fzp` extension with a confirmation message.
- `Project > Add Current Diagram` / `Add Diagrams...` - creates a project first
  if needed, then autosaves after adding diagrams.
- `Project > Build All` - saves the active project diagram first, then runs HDL
  generation from disk for every project diagram.
- `Project > Lint All` - saves the active project diagram first, then lint-checks
  each project diagram from disk.
- `Save` / `Save As` - save `.fzm` diagrams. `Save As` now starts in the current
  diagram directory instead of jumping back to the Java process directory.
- `Export to... > Clipboard` - copies a cropped diagram image. The crop bounds
  now clamp to the image edge so diagrams touching the canvas edge do not throw
  a subimage bounds exception.
- `Export to... > PNG/JPEG` - exports the current diagram image. The chooser now
  starts in the current diagram directory when one is open.
- `Exit` - prompts for unsaved changes and stores recent/restore state.

### Edit

- `Undo` / `Redo` - delegates to the canvas undo stack and refreshes passive lint
  status.
- `Delete` - delegates to the canvas delete operation for the current selection.

### Settings

- `View Settings` - opens the legacy view/preferences dialog.
- `Diagram Defaults` - edits defaults for new FSM clock/reset/implied-loopback
  globals.
- `HDL Generation` - edits backend command, backend path, output path, filename,
  options, and optional comparison backend settings.
- `Language` - switches localized menu/toolbar/pane strings only. HDL-facing
  fields remain English/user-authored.

### Tools

- `Validate / Lint Diagram` - opens the non-modal lint panel, reruns lint, and
  highlights all issues initially.
- `Generate HDL` - saves first when needed, runs the configured backend, and
  optionally compares with the Java backend.
- `Clean Up Diagram > Reset Transition Labels` - resets transition label offsets.
- `Clean Up Diagram > Clean Transition Routes` - resets all transition routes
  and label offsets.
- `Clean Up Diagram > Clean Selected Routes` - now affects only selected
  transition routes; with no selected transitions it does nothing.
- `Clean Up Diagram > Align/Distribute Selected...` - operates on selected
  states, state groups, and forks via their visual centers.

### FSM Interface

- `State Machine` - opens global FSM machine attributes.
- `Inputs` - opens input definitions.
- `Parameters` - opens Verilog parameter definitions while preserving backend
  storage as machine attributes.
- `Outputs` - opens port outputs.
- `Internals` - opens internal-only output definitions stored with
  `suppress_portlist`.

The old global `States` and `Transitions` menu entries are still declared in
the generated Swing field section but are no longer added to the menu. This is
intentional; state/transition data is edited through object property dialogs
and the quick property inspector.

### Help

- `Help` - opens the GitHub wiki when desktop browsing is available, otherwise
  displays the wiki URL.
- `About` - displays the current Fizzim 2.0 splash/about graphic.

## Findings

Fixed during this audit:

- `Save As` was resetting the file chooser directory to the Java working
  directory after selecting the current diagram file.
- `Export to... > Clipboard` could crop with negative coordinates if rendered
  content touched the top or left image edge.
- `Export to... > PNG/JPEG` used the current diagram file as a chooser directory
  instead of its parent directory.
- `Clean Selected Routes` unexpectedly cleaned every transition route when no
  transition was selected.

Items to watch:

- `Project > Build All` and `Generate HDL` depend on the user-configured backend
  command/path. Menu wiring is correct, but failures can still occur if Perl,
  Java, or backend paths are not available in the runtime environment.
- `Help` depends on desktop/browser integration. It has a fallback dialog when
  browsing is unsupported.
