# GUI Modernization Wishlist

Detailed documentation now lives in the GitHub wiki:

https://github.com/cookacounty/fizzim/wiki/GUI-Wishlist

This file is kept as a repo-local backlog note and may lag the wiki.

This file tracks user-experience ideas that are useful but not required for the
current Fizzim 2.0 feature set. These are intentionally written as a practical
backlog, not as committed design promises.

## High-Value Ideas

- Add a left-side tool palette for common modes: Select, State, State Group,
  Fork, Transition, Loopback, and Text. This would make object creation more
  discoverable than relying mainly on right-click menus.
- Continue refining the property inspector panel for the selected object. The
  first version supports quick attribute edits and keeps modal dialogs as a
  fallback for full editors.
- Continue refining the docked lint issues panel at the bottom of the main
  window. The first version keeps canvas access live, highlights all issues when
  opened, and selects/highlights a specific object when an issue is selected.
- Continue polishing hover and selection styling for states, state groups,
  forks, transitions, and labels. The first version adds blue hover affordances
  for common canvas objects.
- Continue expanding automatic layout helpers for selected objects and
  transitions. The first version adds selected-route cleanup plus selected
  align/distribute commands.

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
