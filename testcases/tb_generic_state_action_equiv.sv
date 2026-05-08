module tb_generic_state_action_equiv;
  reg clk;
  reg rst_l;
  reg start;
  reg break_i;
  reg option1;

  wire ref_busy;
  wire [100:0] ref_color;
  wire ref_rdy;
  wire ref_trigger;
  wire [1:0] ref_trigger1;

  wire feat_busy;
  wire [100:0] feat_color;
  wire feat_rdy;
  wire feat_trigger;
  wire [1:0] feat_trigger1;

  generic_state_action_fizzim u_ref (
    .busy(ref_busy),
    .color(ref_color),
    .rdy(ref_rdy),
    .trigger(ref_trigger),
    .trigger1(ref_trigger1),
    .break_req(break_i),
    .clk(clk),
    .option1(option1),
    .rst_l(rst_l),
    .start(start)
  );

  generic_state_action u_feat (
    .busy(feat_busy),
    .color(feat_color),
    .rdy(feat_rdy),
    .trigger(feat_trigger),
    .trigger1(feat_trigger1),
    .break_req(break_i),
    .clk(clk),
    .option1(option1),
    .rst_l(rst_l),
    .start(start)
  );

  initial clk = 1'b0;
  always #5 clk = ~clk;

  task clear_inputs;
    begin
      start = 1'b0;
      break_i = 1'b0;
      option1 = 1'b0;
    end
  endtask

  task check_match(input [255:0] label);
    begin
      #1;
      if (u_ref.state !== u_feat.state) begin
        $display("STATE MISMATCH at %0s ref=%0d feat=%0d", label, u_ref.state, u_feat.state);
        $fatal(1);
      end
      if ({ref_busy, ref_color, ref_rdy, ref_trigger, ref_trigger1} !==
          {feat_busy, feat_color, feat_rdy, feat_trigger, feat_trigger1}) begin
        $display("OUTPUT MISMATCH at %0s", label);
        $display("  ref  busy=%0b color=%0s rdy=%0b trigger=%0b trigger1=%0b",
                 ref_busy, ref_color, ref_rdy, ref_trigger, ref_trigger1);
        $display("  feat busy=%0b color=%0s rdy=%0b trigger=%0b trigger1=%0b",
                 feat_busy, feat_color, feat_rdy, feat_trigger, feat_trigger1);
        $fatal(1);
      end
    end
  endtask

  task tick(input [255:0] label);
    begin
      @(negedge clk);
      @(posedge clk);
      check_match(label);
      clear_inputs();
    end
  endtask

  integer i;
  reg [2:0] random_vec;

  initial begin
    rst_l = 1'b0;
    clear_inputs();
    repeat (3) @(posedge clk);
    rst_l = 1'b1;
    @(posedge clk);
    check_match("reset");

    tick("reset_path_to_s2");
    start = 1'b1;
    tick("start_to_entry");
    if (u_feat.statename !== "SG_ACTIVE.S_GREEN") begin
      $display("Expected grouped default-entry statename, got %0s", u_feat.statename);
      $fatal(1);
    end

    tick("green_default_step");
    break_i = 1'b1;
    option1 = 1'b1;
    tick("group_exit_fork_branch_1");
    start = 1'b1;
    option1 = 1'b1;
    tick("group_to_group2_default_entry");
    start = 1'b1;
    tick("return_to_active_group");

    for (i = 0; i < 300; i = i + 1) begin
      random_vec = $random;
      start = random_vec[0];
      break_i = random_vec[1];
      option1 = random_vec[2];
      tick("random");
    end

    $display("PASS generic_state_action Fizzim 1.0 compatibility equivalence test");
    $finish;
  end
endmodule
