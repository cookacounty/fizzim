# Copy and Paste Behavior

Fizzim copy/paste is intended to duplicate explicit diagram objects without
silently changing existing logic or state-group membership.

## Single Object Behavior

- States can be copied. A pasted state keeps its assignments and visual style,
  receives a unique copied name such as `S_IDLE_copy`, and is offset from the
  source.
- Forks can be copied. A pasted fork is named like a newly created fork using
  the next fork/state counter, such as `fork12`; fork names are not preserved.
- Free text can be copied if it is a movable user text object.
- State groups cannot be copied. A pasted group could capture unrelated states
  or forks by overlap, so groups are intentionally ignored by copy.
- FSM summary text is not copied.
- Transitions are copied only when their endpoints are also copied. A selected
  transition by itself is not pasted as a dangling duplicate or as a duplicate
  still attached to the original endpoints.

## Multiple Selection Behavior

- Selected states, forks, and movable free text are copied together.
- Any transition whose endpoints are both in the copied endpoint set is copied
  with the selection, even if the transition itself was not directly selected.
- A transition connected to an unselected endpoint is not copied.
- Pasted transitions reconnect only to pasted endpoints, never to the original
  states or forks.
- Pasted objects preserve their relative layout and transition route shape, then
  shift as a group so repeated paste operations do not stack exactly.
- Pasted objects are selected after paste.
- One paste operation creates one undo step.
- Pasting marks the diagram modified and makes generated HDL stale.

## Safety Goals

- Copy/paste must not mutate, move, reroute, or rewire existing transitions.
- Copy/paste must not create implicit state-group membership by pasting a group.
- Transition conditions, actions, priorities, labels, and manually adjusted
  route geometry should be preserved on the pasted transition.
