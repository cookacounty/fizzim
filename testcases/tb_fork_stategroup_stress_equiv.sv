module tb_fork_stategroup_stress_equiv;
  reg clk, rst_l, start, break_req, option1, mode0, mode1;
  wire ref_busy, ref_rdy, ref_trigger;
  wire [100:0] ref_color;
  wire [1:0] ref_trigger1;
  wire dut_busy, dut_rdy, dut_trigger;
  wire [100:0] dut_color;
  wire [1:0] dut_trigger1;

  fork_stategroup_stress_ref u_ref (
    .busy(ref_busy), .color(ref_color), .rdy(ref_rdy), .trigger(ref_trigger), .trigger1(ref_trigger1),
    .break_req(break_req), .clk(clk), .mode0(mode0), .mode1(mode1), .option1(option1), .rst_l(rst_l), .start(start)
  );

  fork_stategroup_stress u_dut (
    .busy(dut_busy), .color(dut_color), .rdy(dut_rdy), .trigger(dut_trigger), .trigger1(dut_trigger1),
    .break_req(break_req), .clk(clk), .mode0(mode0), .mode1(mode1), .option1(option1), .rst_l(rst_l), .start(start)
  );

  initial clk = 1'b0;
  always #5 clk = ~clk;

  task drive(input s, input b, input o, input m0, input m1);
    begin
      start = s; break_req = b; option1 = o; mode0 = m0; mode1 = m1;
    end
  endtask

  task check(input [255:0] label);
    begin
      #1;
      if ({ref_busy, ref_color, ref_rdy, ref_trigger, ref_trigger1} !==
          {dut_busy, dut_color, dut_rdy, dut_trigger, dut_trigger1}) begin
        $display("MISMATCH %0s", label);
        $display("ref busy=%0b color=%0s rdy=%0b trigger=%0b trigger1=%0d",
                 ref_busy, ref_color, ref_rdy, ref_trigger, ref_trigger1);
        $display("dut busy=%0b color=%0s rdy=%0b trigger=%0b trigger1=%0d",
                 dut_busy, dut_color, dut_rdy, dut_trigger, dut_trigger1);
        $fatal(1);
      end
    end
  endtask

  task tick(input [255:0] label);
    begin
      @(negedge clk);
      @(posedge clk);
      check(label);
      drive(0, 0, 0, 0, 0);
    end
  endtask

  integer i;
  reg [4:0] random_vec;

  initial begin
    rst_l = 1'b0;
    drive(0, 0, 0, 0, 0);
    repeat (3) @(posedge clk);
    rst_l = 1'b1;
    @(posedge clk);
    check("reset");

    drive(1, 0, 0, 1, 0); tick("idle_to_group_a");
    drive(0, 0, 0, 0, 1); tick("a0_to_a1");
    drive(1, 0, 0, 0, 0); tick("a1_to_group_b");
    drive(0, 0, 0, 1, 0); tick("b0_to_b1");
    drive(0, 1, 1, 0, 0); tick("group_b_to_fork_to_c");
    drive(1, 0, 0, 1, 0); tick("c_to_done");
    tick("done_to_idle");
    drive(1, 1, 1, 0, 1); tick("idle_nested_forks_to_c");
    drive(1, 0, 0, 0, 0); tick("c_to_error");
    drive(1, 0, 0, 0, 0); tick("error_to_idle");

    for (i = 0; i < 600; i = i + 1) begin
      random_vec = $random;
      drive(random_vec[0], random_vec[1], random_vec[2], random_vec[3], random_vec[4]);
      tick("random");
    end

    $display("PASS fork/state-group stress equivalence test");
    $finish;
  end
endmodule
