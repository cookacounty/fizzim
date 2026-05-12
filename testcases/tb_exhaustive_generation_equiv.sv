module tb_exhaustive_generation_equiv;
  reg clk;
  reg rst_l;
  reg in_a;
  reg in_b;
  reg in_c;
  reg in_d;
  reg in_e;
  reg [2:0] in_bus;
  wire ref_rdy;
  wire feat_rdy;
  wire ref_busy;
  wire feat_busy;
  wire [100:0] ref_color;
  wire [100:0] feat_color;
  wire ref_trigger;
  wire feat_trigger;
  wire [1:0] ref_trigger1;
  wire [1:0] feat_trigger1;

  localparam integer STATE_COUNT = 20;
  localparam integer PAIR_COUNT = 400;
  localparam integer EXPECTED_PAIR_COUNT = 132;
  localparam integer MIN_EXPECTED_PAIR_COUNT = (EXPECTED_PAIR_COUNT * 90 + 99) / 100;
  localparam [PAIR_COUNT-1:0] EXPECTED_PAIRS = 400'h200d1a00d0e00d8240d9340d83c0d8240d1220d0270d8248d9244d824ed80000300001249dd240da040b0208d8249fc249de;
  reg [PAIR_COUNT-1:0] observed_pairs;

  exhaustive_generation_fizzim u_ref (
    .rdy(ref_rdy),
    .busy(ref_busy),
    .color(ref_color),
    .trigger(ref_trigger),
    .trigger1(ref_trigger1),
    .break_req(1'b0),
    .option1(1'b0),
    .start(1'b0),
    .in_a(in_a),
    .in_b(in_b),
    .in_c(in_c),
    .in_d(in_d),
    .in_e(in_e),
    .in_bus(in_bus),
    .clk(clk),
    .rst_l(rst_l)
  );

  exhaustive_generation u_feat (
    .rdy(feat_rdy),
    .busy(feat_busy),
    .color(feat_color),
    .trigger(feat_trigger),
    .trigger1(feat_trigger1),
    .break_req(1'b0),
    .option1(1'b0),
    .start(1'b0),
    .in_a(in_a),
    .in_b(in_b),
    .in_c(in_c),
    .in_d(in_d),
    .in_e(in_e),
    .in_bus(in_bus),
    .clk(clk),
    .rst_l(rst_l)
  );

  initial clk = 1'b0;
  always #5 clk = ~clk;

  task clear_inputs;
    begin
      in_a = '0;
      in_b = '0;
      in_c = '0;
      in_d = '0;
      in_e = '0;
      in_bus = '0;
    end
  endtask

  task drive_combo(input [7:0] combo);
    begin
      in_a = combo[0];
      in_b = combo[1];
      in_c = combo[2];
      in_d = combo[3];
      in_e = combo[4];
      in_bus = combo[7:5];
    end
  endtask

  task check_match(input [255:0] label);
    begin
      #1;
      if (u_ref.state !== u_feat.state) begin
        $display("STATE MISMATCH %0s ref=%0d feat=%0d", label, u_ref.state, u_feat.state);
        $fatal(1);
      end
      if ({ref_rdy, ref_busy, ref_color, ref_trigger, ref_trigger1} !==
          {feat_rdy, feat_busy, feat_color, feat_trigger, feat_trigger1}) begin
        $display("OUTPUT MISMATCH %0s", label);
        $display("ref  %0h %0h %0h %0h %0h", ref_rdy, ref_busy, ref_color, ref_trigger, ref_trigger1);
        $display("feat %0h %0h %0h %0h %0h", feat_rdy, feat_busy, feat_color, feat_trigger, feat_trigger1);
        $fatal(1);
      end
    end
  endtask

  function integer state_index(input [31:0] state_value);
    begin
      case (state_value)
        u_feat.S_RESET: state_index = 0;
        u_feat.S_OUT0: state_index = 1;
        u_feat.S_OUT1: state_index = 2;
        u_feat.S_OUT2: state_index = 3;
        u_feat.S_OUT3: state_index = 4;
        u_feat.S_OUT4: state_index = 5;
        u_feat.S_DONE: state_index = 6;
        u_feat.S_ERR: state_index = 7;
        u_feat.S_A0: state_index = 8;
        u_feat.S_A1: state_index = 9;
        u_feat.S_A2: state_index = 10;
        u_feat.S_B0: state_index = 11;
        u_feat.S_B1: state_index = 12;
        u_feat.S_B2: state_index = 13;
        u_feat.S_C0: state_index = 14;
        u_feat.S_C1: state_index = 15;
        u_feat.S_C2: state_index = 16;
        u_feat.S_D0: state_index = 17;
        u_feat.S_D1: state_index = 18;
        u_feat.S_D2: state_index = 19;
        default: state_index = -1;
      endcase
    end
  endfunction

  task mark_pair(input integer from_index);
    integer to_index;
    begin
      to_index = state_index(u_feat.state);
      if (from_index >= 0 && to_index >= 0)
        observed_pairs[from_index * STATE_COUNT + to_index] = 1'b1;
    end
  endtask

  task force_state_and_tick(input integer state_idx, input [31:0] ref_forced_state, input [31:0] feat_forced_state, input [7:0] combo, input [255:0] label);
    begin
      @(negedge clk);
      u_ref.state = ref_forced_state;
      u_feat.state = feat_forced_state;
      drive_combo(combo);
      @(posedge clk);
      check_match(label);
      mark_pair(state_idx);
      clear_inputs();
    end
  endtask

  task check_all_inputs_from_state(input integer state_idx, input [31:0] ref_forced_state, input [31:0] feat_forced_state, input [255:0] label);
    integer combo;
    begin
      for (combo = 0; combo < 256; combo = combo + 1)
        force_state_and_tick(state_idx, ref_forced_state, feat_forced_state, combo[7:0], label);
    end
  endtask

  function integer count_bits(input [PAIR_COUNT-1:0] bits);
    integer i;
    begin
      count_bits = 0;
      for (i = 0; i < PAIR_COUNT; i = i + 1)
        if (bits[i])
          count_bits = count_bits + 1;
    end
  endfunction

  initial begin
    observed_pairs = '0;
    rst_l = 1'b0;
    clear_inputs();
    repeat (3) @(posedge clk);
    rst_l = 1'b1;
    @(posedge clk);
    check_match("reset");
    check_all_inputs_from_state(0, u_ref.S_RESET, u_feat.S_RESET, "S_RESET");
    check_all_inputs_from_state(1, u_ref.S_OUT0, u_feat.S_OUT0, "S_OUT0");
    check_all_inputs_from_state(2, u_ref.S_OUT1, u_feat.S_OUT1, "S_OUT1");
    check_all_inputs_from_state(3, u_ref.S_OUT2, u_feat.S_OUT2, "S_OUT2");
    check_all_inputs_from_state(4, u_ref.S_OUT3, u_feat.S_OUT3, "S_OUT3");
    check_all_inputs_from_state(5, u_ref.S_OUT4, u_feat.S_OUT4, "S_OUT4");
    check_all_inputs_from_state(6, u_ref.S_DONE, u_feat.S_DONE, "S_DONE");
    check_all_inputs_from_state(7, u_ref.S_ERR, u_feat.S_ERR, "S_ERR");
    check_all_inputs_from_state(8, u_ref.S_A0, u_feat.S_A0, "S_A0");
    check_all_inputs_from_state(9, u_ref.S_A1, u_feat.S_A1, "S_A1");
    check_all_inputs_from_state(10, u_ref.S_A2, u_feat.S_A2, "S_A2");
    check_all_inputs_from_state(11, u_ref.S_B0, u_feat.S_B0, "S_B0");
    check_all_inputs_from_state(12, u_ref.S_B1, u_feat.S_B1, "S_B1");
    check_all_inputs_from_state(13, u_ref.S_B2, u_feat.S_B2, "S_B2");
    check_all_inputs_from_state(14, u_ref.S_C0, u_feat.S_C0, "S_C0");
    check_all_inputs_from_state(15, u_ref.S_C1, u_feat.S_C1, "S_C1");
    check_all_inputs_from_state(16, u_ref.S_C2, u_feat.S_C2, "S_C2");
    check_all_inputs_from_state(17, u_ref.S_D0, u_feat.S_D0, "S_D0");
    check_all_inputs_from_state(18, u_ref.S_D1, u_feat.S_D1, "S_D1");
    check_all_inputs_from_state(19, u_ref.S_D2, u_feat.S_D2, "S_D2");
    if (count_bits(observed_pairs & EXPECTED_PAIRS) < MIN_EXPECTED_PAIR_COUNT) begin
      $display("TRANSITION PAIR COVERAGE FAILURE observed=%0d expected=%0d minimum=%0d",
               count_bits(observed_pairs & EXPECTED_PAIRS), EXPECTED_PAIR_COUNT, MIN_EXPECTED_PAIR_COUNT);
      $display("missing_pairs_mask=%0h", EXPECTED_PAIRS & ~observed_pairs);
      $fatal(1);
    end
    $display("Observed %0d/%0d expected transition source/destination pairs; minimum required=%0d",
             count_bits(observed_pairs & EXPECTED_PAIRS), EXPECTED_PAIR_COUNT, MIN_EXPECTED_PAIR_COUNT);
    if ((observed_pairs & EXPECTED_PAIRS) !== EXPECTED_PAIRS)
      $display("Uncovered expected pair mask=%0h", EXPECTED_PAIRS & ~observed_pairs);
    $display("PASS exhaustive generation Fizzim 2.0 vs Fizzim 1.0-compatible equivalence test");
    $finish;
  end
endmodule
