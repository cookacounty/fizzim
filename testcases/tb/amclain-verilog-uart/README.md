# amclain-verilog-uart

This folder contains public reference FSM material used to manually review
Fizzim diagrams against existing HDL examples.

Source repository: https://github.com/amclain/verilog-uart

The `source/` checkout is intentionally ignored by Git. Re-clone the public
repository there when running source-vs-generated comparisons locally.

| Diagram | Source HDL | License | Comparison scope |
| --- | --- | --- | --- |
| `amclain_uart_tx_fsm.fzm` | `source/verilog/uart_tx.v` | MIT | Control-state graph is reconstructed; datapath counters/shifters remain in the source HDL. |
| `amclain_uart_rx_fsm.fzm` | `source/verilog/uart_rx.v` | MIT | Control-state graph is reconstructed; datapath counters/shifters remain in the source HDL. |

Generated RTL is written under `generated/` by `generate_public_reference_fsms.js` plus the backend flow.
