module tb_ticket_machine_reference_equiv;
  reg Clock;
  reg Clear;
  reg Ten;
  reg Twenty;

  wire ref_Ready;
  wire ref_Dispense;
  wire ref_Return;
  wire ref_Bill;
  wire gen_Ready;
  wire gen_Dispense;
  wire gen_Return;
  wire gen_Bill;

  TicketMachine ref_dut (
    .Clock(Clock),
    .Clear(Clear),
    .Ten(Ten),
    .Twenty(Twenty),
    .Ready(ref_Ready),
    .Dispense(ref_Dispense),
    .Return(ref_Return),
    .Bill(ref_Bill)
  );

  ticket_machine_fsm gen (
    .Clock(Clock),
    .Clear(Clear),
    .Ten(Ten),
    .Twenty(Twenty),
    .Ready(gen_Ready),
    .Dispense(gen_Dispense),
    .Return(gen_Return),
    .Bill(gen_Bill)
  );

  function integer ref_state_index(input [5:0] value);
    begin
      case (value)
        ref_dut.RDY: ref_state_index = 0;
        ref_dut.BILL10: ref_state_index = 1;
        ref_dut.BILL20: ref_state_index = 2;
        ref_dut.BILL30: ref_state_index = 3;
        ref_dut.DISP: ref_state_index = 4;
        ref_dut.RTN: ref_state_index = 5;
        default: ref_state_index = -1;
      endcase
    end
  endfunction

  function integer gen_state_index(input [2:0] value);
    begin
      case (value)
        gen.RDY: gen_state_index = 0;
        gen.BILL10: gen_state_index = 1;
        gen.BILL20: gen_state_index = 2;
        gen.BILL30: gen_state_index = 3;
        gen.DISP: gen_state_index = 4;
        gen.RTN: gen_state_index = 5;
        default: gen_state_index = -1;
      endcase
    end
  endfunction

  task check_state(input [5:0] ref_state, input [2:0] gen_state, input [255:0] label);
    integer combo;
    begin
      for (combo = 0; combo < 4; combo = combo + 1) begin
        ref_dut.State = ref_state;
        gen.state = gen_state;
        Ten = combo[0];
        Twenty = combo[1];
        #1;
        if ({ref_Ready, ref_Dispense, ref_Return, ref_Bill} !==
            {gen_Ready, gen_Dispense, gen_Return, gen_Bill}) begin
          $display("OUTPUT MISMATCH %0s combo=%0d", label, combo);
          $fatal(1);
        end
        if (ref_state_index(ref_dut.NextState) !== gen_state_index(gen.nextstate)) begin
          $display("NEXTSTATE MISMATCH %0s combo=%0d ref_dut=%0d gen=%0d",
                   label, combo, ref_state_index(ref_dut.NextState), gen_state_index(gen.nextstate));
          $fatal(1);
        end
      end
    end
  endtask

  initial begin
    Clock = 1'b0;
    Clear = 1'b0;
    check_state(ref_dut.RDY, gen.RDY, "RDY");
    check_state(ref_dut.BILL10, gen.BILL10, "BILL10");
    check_state(ref_dut.BILL20, gen.BILL20, "BILL20");
    check_state(ref_dut.BILL30, gen.BILL30, "BILL30");
    check_state(ref_dut.DISP, gen.DISP, "DISP");
    check_state(ref_dut.RTN, gen.RTN, "RTN");
    $display("PASS ticket machine reference equivalence");
    $finish;
  end
endmodule
