module tb_generic_ctrl_equiv;
  reg clk;
  reg rst_l;
  reg start;
  reg cfg_ok;
  reg mode;
  reg retry_ok;
  reg op_done;
  reg op_fail;
  reg flush_done;
  reg recover_done;
  reg timeout;
  reg abort;
  reg soft_reset;
  reg diag_req;

  wire ref_busy;
  wire ref_init_pulse;
  wire ref_op_enable;
  wire ref_flush_enable;
  wire ref_recover_enable;
  wire ref_done;
  wire ref_error;
  wire ref_diag_active;
  wire [2:0] ref_state_dbg;

  wire feat_busy;
  wire feat_init_pulse;
  wire feat_op_enable;
  wire feat_flush_enable;
  wire feat_recover_enable;
  wire feat_done;
  wire feat_error;
  wire feat_diag_active;
  wire [2:0] feat_state_dbg;

  generic_ctrl_fsm u_ref (
    .busy(ref_busy),
    .diag_active(ref_diag_active),
    .done(ref_done),
    .error(ref_error),
    .flush_enable(ref_flush_enable),
    .init_pulse(ref_init_pulse),
    .op_enable(ref_op_enable),
    .recover_enable(ref_recover_enable),
    .state_dbg(ref_state_dbg),
    .abort(abort),
    .cfg_ok(cfg_ok),
    .clk(clk),
    .diag_req(diag_req),
    .flush_done(flush_done),
    .mode(mode),
    .op_done(op_done),
    .op_fail(op_fail),
    .recover_done(recover_done),
    .retry_ok(retry_ok),
    .rst_l(rst_l),
    .soft_reset(soft_reset),
    .start(start),
    .timeout(timeout)
  );

  generic_ctrl_fsm_features u_feat (
    .busy(feat_busy),
    .diag_active(feat_diag_active),
    .done(feat_done),
    .error(feat_error),
    .flush_enable(feat_flush_enable),
    .init_pulse(feat_init_pulse),
    .op_enable(feat_op_enable),
    .recover_enable(feat_recover_enable),
    .state_dbg(feat_state_dbg),
    .abort(abort),
    .cfg_ok(cfg_ok),
    .clk(clk),
    .diag_req(diag_req),
    .flush_done(flush_done),
    .mode(mode),
    .op_done(op_done),
    .op_fail(op_fail),
    .recover_done(recover_done),
    .retry_ok(retry_ok),
    .rst_l(rst_l),
    .soft_reset(soft_reset),
    .start(start),
    .timeout(timeout)
  );

  initial clk = 1'b0;
  always #5 clk = ~clk;

  task clear_inputs;
    begin
      start = 1'b0;
      cfg_ok = 1'b0;
      mode = 1'b0;
      retry_ok = 1'b0;
      op_done = 1'b0;
      op_fail = 1'b0;
      flush_done = 1'b0;
      recover_done = 1'b0;
      timeout = 1'b0;
      abort = 1'b0;
      soft_reset = 1'b0;
      diag_req = 1'b0;
    end
  endtask

  task check_match(input [255:0] label);
    begin
      #1;
      if (ref_state_dbg !== feat_state_dbg) begin
        $display("STATE MISMATCH at %0s ref=%0d feat=%0d", label, ref_state_dbg, feat_state_dbg);
        $fatal(1);
      end
      if ({ref_busy, ref_init_pulse, ref_op_enable, ref_flush_enable,
           ref_recover_enable, ref_done, ref_error, ref_diag_active} !==
          {feat_busy, feat_init_pulse, feat_op_enable, feat_flush_enable,
           feat_recover_enable, feat_done, feat_error, feat_diag_active}) begin
        $display("OUTPUT MISMATCH at %0s", label);
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

  task reset_duts;
    begin
      clear_inputs();
      rst_l = 1'b0;
      repeat (3) @(posedge clk);
      rst_l = 1'b1;
      @(posedge clk);
      check_match("reset");
    end
  endtask

  integer i;
  reg [11:0] random_vec;

  initial begin
    rst_l = 1'b0;
    clear_inputs();
    reset_duts();

    start = 1'b1;
    mode = 1'b0;
    tick("idle_to_load");
    op_done = 1'b1;
    tick("load_to_run_a");
    if (u_feat.statename !== "G_RUN.S_RUN_A") begin
      $display("Expected grouped statename for S_RUN_A, got %0s", u_feat.statename);
      $fatal(1);
    end
    op_done = 1'b1;
    retry_ok = 1'b1;
    tick("run_a_to_run_b");
    if (u_feat.statename !== "G_RUN.S_RUN_B") begin
      $display("Expected grouped statename for S_RUN_B, got %0s", u_feat.statename);
      $fatal(1);
    end
    op_done = 1'b1;
    retry_ok = 1'b0;
    tick("run_b_to_done");
    start = 1'b1;
    mode = 1'b1;
    tick("done_restart_to_run_a");
    op_fail = 1'b1;
    tick("run_a_fail_to_error");
    soft_reset = 1'b1;
    tick("error_reset");

    start = 1'b1;
    mode = 1'b0;
    tick("restart_for_abort_load");
    abort = 1'b1;
    tick("load_abort_to_error");
    soft_reset = 1'b1;
    tick("abort_error_reset");

    for (i = 0; i < 300; i = i + 1) begin
      random_vec = $random;
      start = random_vec[0];
      cfg_ok = random_vec[1];
      mode = random_vec[2];
      retry_ok = random_vec[3];
      op_done = random_vec[4];
      op_fail = random_vec[5];
      flush_done = random_vec[6];
      recover_done = random_vec[7];
      timeout = random_vec[8];
      abort = random_vec[9];
      soft_reset = random_vec[10];
      diag_req = random_vec[11];
      tick("random");
    end

    $display("PASS generic_ctrl_fsm feature equivalence test");
    $finish;
  end
endmodule
