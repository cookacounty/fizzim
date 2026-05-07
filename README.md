fizzim
======

Finite State Machine design tool.

To compile, run:

```sh
make
```

This creates a jar file with a title based on the `currVer` variable in
`FizzimGui.java`. Update this variable whenever building for a release, or set
it to the current date by running:

```sh
make SETVERSION=true
```

Fizzim on the web: www.fizzim.com

Verilog backend
---------------

The Verilog/SystemVerilog backend is `fizzim.pl`. By default, simulation
state-name debug output reserves 256 ASCII characters:

```verilog
`ifndef SYNTHESIS
reg [2047:0] statename;
...
`endif
```

The width can be overridden with `-statenamechars <value>`, but the default is
intentionally large because this debug register is not synthesized.

Backend regression scripts live under `testcases/`:

```sh
# Windows, using OSS CAD Suite / Icarus / Yosys
powershell -ExecutionPolicy Bypass -File testcases/run_backend_flow.ps1

# Linux, using xrun / Yosys
bash testcases/run_backend_flow.sh
```

Both scripts regenerate Verilog from `.fzm` files using the repo-local
`fizzim.pl` before compiling or simulating, so stale generated RTL cannot hide
backend regressions.

Forked transitions
------------------

Forks let one transition condition feed several branch conditions. This avoids
duplicating the common part of an expression on every outgoing transition.

![Forked transition example](docs/images/fork-transition.svg)

Without a fork, a state like `START` may need repeated logic:

```verilog
START: begin
  if (rdy && error)
    nextstate = ERROR;
  else if (rdy && !error)
    nextstate = FINISHED;
end
```

With a fork, `rdy` is written once on the transition into the fork. The branch
transitions only describe how to choose the final destination:

```verilog
START: begin
  if (rdy) begin
    if (error)
      nextstate = ERROR;
    else
      nextstate = FINISHED;
  end
end
```

In the GUI, a fork is drawn as a small filled dot. It is a transition endpoint,
not an encoded state.

State groups
------------

State groups let several existing states share a transition or shared state
behavior. The group is drawn as a rounded rectangle around the member states.
Only one level of grouping is supported.

![State group example](docs/images/state-group.svg)

For example, two states can share the same shutdown transition:

```verilog
RDY: begin
  if (shutdown_req)
    nextstate = SHUTDOWN;
end

FUSE_RDY: begin
  if (shutdown_req)
    nextstate = SHUTDOWN;
end
```

With a state group, the common transition is authored once on the group and is
expanded for each member state during generation:

```verilog
RDY: begin
  if (shutdown_req)
    nextstate = SHUTDOWN;
end

FUSE_RDY: begin
  if (shutdown_req)
    nextstate = SHUTDOWN;
end
```

State groups are also used for simulation debug naming. When a state is inside
a group, `statename` should include both names:

```verilog
`ifndef SYNTHESIS
reg [2047:0] statename; // 256 characters
always @* begin
  case (state)
    RDY:
      statename = "READY_GROUP.RDY";
    FUSE_RDY:
      statename = "READY_GROUP.FUSE_RDY";
    default:
      statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
  endcase
end
`endif
```

The state group itself is not an encoded state. The generated FSM still uses
the original member states, so synthesis-visible behavior should match the
ungrouped machine.
