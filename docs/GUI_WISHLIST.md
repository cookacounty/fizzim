# GUI Modernization Wishlist

This file tracks user-experience ideas that are useful but not required for the
current Fizzim 2.0 feature set. These are intentionally written as a practical
backlog, not as committed design promises.

## High-Value Ideas

- Add a left-side tool palette for common modes: Select, State, State Group,
  Fork, Transition, Loopback, and Text. This would make object creation more
  discoverable than relying mainly on right-click menus.
- Add a property inspector panel for the selected object. Keep modal dialogs as
  a fallback, but allow common edits without opening a separate window.
- Convert the lint interface into a docked issues panel at the bottom of the
  main window. Selecting an issue should continue to highlight/select the
  related diagram object.
- Add consistent hover and selection styling for states, state groups, forks,
  transitions, and labels. The current behavior works, but a modern editor
  should make clickable/editable targets obvious before the user clicks.
- Add automatic layout helpers for selected objects and transitions: align,
  distribute, tidy routes, reset labels, and possibly auto-space crowded fork
  branches.

## Nice-To-Have Refinements

- Add a small status/hint area for context-specific guidance, such as
  "Drag selected label" or "Ctrl-click to add to selection".
- Add keyboard shortcuts and toolbar buttons for the most common actions, with
  tooltips that show the shortcuts.
- Add severity filters to the lint issues list, such as Errors, Warnings, and
  Info.
- Add a minimap or page overview for large diagrams.
- Add a clearer page/tab management menu for rename, duplicate, reorder, and
  delete page operations.
- Add optional diagram themes, while keeping the default restrained and
  engineering-focused.
- Add import/export presets for screenshots, documentation images, and review
  packets.

## Longer-Term Ideas

- Add a real layout engine for initial diagram cleanup, possibly limited to
  selected objects so users stay in control.
- Add a "compare diagrams" view for reviewing generated Fizzim 1-compatible
  diagrams against Fizzim 2.0 source diagrams.
- Add integrated generated-Verilog preview with syntax highlighting.
- Add optional external lint integration for generated RTL when tools such as
  Verilator, Verible, Yosys, or simulator compilers are available.
