# timrudy-uart-verilog

This folder contains public reference FSM material used to manually review
Fizzim diagrams against existing HDL examples.

Source repository: https://github.com/TimRudy/uart-verilog

The `source/` checkout is intentionally ignored by Git. Re-clone the public
repository there when running source-vs-generated comparisons locally.

| Diagram | Source HDL | License | Comparison scope |
| --- | --- | --- | --- |
| `timrudy_uart8_tx_fsm.fzm` | `source/Uart8Transmitter.v` | GPL-3.0 | Control-state graph is reconstructed; datapath counters/shifters remain in the source HDL. |
| `timrudy_uart8_rx_fsm.fzm` | `source/Uart8Receiver.v` | GPL-3.0 | Control-state graph is reconstructed; datapath counters/shifters remain in the source HDL. |

Generated RTL is written under `generated/` by `generate_public_reference_fsms.js` plus the backend flow.
