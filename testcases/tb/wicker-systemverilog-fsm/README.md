# wicker-systemverilog-fsm

This folder contains public reference FSM material used to manually review
Fizzim diagrams against existing HDL examples.

Source repository: https://github.com/wicker/SystemVerilog-FSM

The `source/` checkout is intentionally ignored by Git. Re-clone the public
repository there when running source-vs-generated comparisons locally.

| Diagram | Source HDL | License | Comparison scope |
| --- | --- | --- | --- |
| `landing_gear_fsm.fzm` | `source/landing-gear-controller/verilog/LandingGear.v` | BSD-3-Clause | Moore FSM control/output behavior is directly reconstructable. |
| `ticket_machine_fsm.fzm` | `source/ticket-machine/verilog/TicketMachine.v` | BSD-3-Clause | Moore FSM control/output behavior is directly reconstructable. |

Generated RTL is written under `generated/` by `generate_public_reference_fsms.js` plus the backend flow.
