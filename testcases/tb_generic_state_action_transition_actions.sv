module tb_generic_state_action_transition_actions;
  reg clk = 1'b0;
  reg rst_l = 1'b0;
  wire golden_rdy;
  wire action_rdy;

  generic_state_action golden (
    .rdy(golden_rdy),
    .clk(clk),
    .rst_l(rst_l)
  );

  generic_state_action_transition_actions action (
    .rdy(action_rdy),
    .clk(clk),
    .rst_l(rst_l)
  );

  always #5 clk = ~clk;

  task check_rdy;
    input expected;
    begin
      #1;
      if (golden_rdy !== expected || action_rdy !== expected || golden_rdy !== action_rdy) begin
        $display("FAIL expected=%0b golden_rdy=%0b action_rdy=%0b", expected, golden_rdy, action_rdy);
        $finish;
      end
    end
  endtask

  initial begin
    repeat (2) @(posedge clk);
    rst_l = 1'b1;

    force golden.state = golden.S1;
    force action.state = action.S1;
    #1;
    @(posedge clk);
    check_rdy(1'b1);

    force golden.state = golden.S2;
    force action.state = action.S2;
    #1;
    @(posedge clk);
    check_rdy(1'b0);

    $display("PASS generic_state_action transition-actions equivalence test");
    $finish;
  end
endmodule
