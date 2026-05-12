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

  reg seen_blue;
  reg seen_green;
  reg seen_red;
  reg seen_yellow;

  localparam [3:0] S1_CODE       = 4'b0000;
  localparam [3:0] S2_CODE       = 4'b0001;
  localparam [3:0] S3_CODE       = 4'b0010;
  localparam [3:0] S_BLUE_CODE   = 4'b0011;
  localparam [3:0] S_GREEN_CODE  = 4'b0100;
  localparam [3:0] S_RED_CODE    = 4'b0101;
  localparam [3:0] S_YELLOW_CODE = 4'b0110;
  localparam [3:0] STATE49_CODE  = 4'b0111;
  localparam [3:0] STATE51_CODE  = 4'b1000;

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
      if (u_feat.state == S_BLUE_CODE) begin
        seen_blue = 1'b1;
      end
      if (u_feat.state == S_GREEN_CODE) begin
        seen_green = 1'b1;
      end
      if (u_feat.state == S_RED_CODE) begin
        seen_red = 1'b1;
      end
      if (u_feat.state == S_YELLOW_CODE) begin
        seen_yellow = 1'b1;
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

  task force_state_and_tick(input [3:0] forced_state, input [2:0] inputs, input [255:0] label);
    begin
      @(negedge clk);
      u_ref.state = forced_state;
      u_feat.state = forced_state;
      mark_seen_state(forced_state);
      start = inputs[0];
      break_i = inputs[1];
      option1 = inputs[2];
      @(posedge clk);
      check_match(label);
      clear_inputs();
    end
  endtask

  task mark_seen_state(input [3:0] state_code);
    begin
      if (state_code == S_BLUE_CODE) begin
        seen_blue = 1'b1;
      end
      if (state_code == S_GREEN_CODE) begin
        seen_green = 1'b1;
      end
      if (state_code == S_RED_CODE) begin
        seen_red = 1'b1;
      end
      if (state_code == S_YELLOW_CODE) begin
        seen_yellow = 1'b1;
      end
    end
  endtask

  task check_all_inputs_from_state(input [3:0] forced_state, input [255:0] label);
    integer combo;
    begin
      for (combo = 0; combo < 8; combo = combo + 1) begin
        force_state_and_tick(forced_state, combo[2:0], label);
      end
    end
  endtask

  integer i;
  reg [2:0] random_vec;

  initial begin
    seen_blue = 1'b0;
    seen_green = 1'b0;
    seen_red = 1'b0;
    seen_yellow = 1'b0;
    rst_l = 1'b0;
    clear_inputs();
    repeat (3) @(posedge clk);
    rst_l = 1'b1;
    @(posedge clk);
    check_match("reset");

    tick("reset_path_to_s2");
    start = 1'b1;
    tick("start_to_entry");

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

    check_all_inputs_from_state(S1_CODE, "forced_s1");
    check_all_inputs_from_state(S2_CODE, "forced_s2");
    check_all_inputs_from_state(S3_CODE, "forced_s3");
    check_all_inputs_from_state(S_BLUE_CODE, "forced_blue");
    check_all_inputs_from_state(S_GREEN_CODE, "forced_green");
    check_all_inputs_from_state(S_RED_CODE, "forced_red");
    check_all_inputs_from_state(S_YELLOW_CODE, "forced_yellow");
    check_all_inputs_from_state(STATE49_CODE, "forced_state49");
    check_all_inputs_from_state(STATE51_CODE, "forced_state51");

    if ({seen_blue, seen_green, seen_red, seen_yellow} !== 4'b1111) begin
      $display("COVERAGE FAILURE seen_blue=%0b seen_green=%0b seen_red=%0b seen_yellow=%0b",
               seen_blue, seen_green, seen_red, seen_yellow);
      $fatal(1);
    end

    $display("PASS generic_state_action Fizzim 1.0 compatibility equivalence test");
    $finish;
  end
endmodule
