module tb_landing_gear_reference_equiv;
  reg Clock;
  reg Clear;
  reg GearIsDown;
  reg GearIsUp;
  reg PlaneOnGround;
  reg TimeUp;
  reg Lever;

  wire ref_RedLED;
  wire ref_GrnLED;
  wire ref_Valve;
  wire ref_Pump;
  wire ref_Timer;
  wire gen_RedLED;
  wire gen_GrnLED;
  wire gen_Valve;
  wire gen_Pump;
  wire gen_Timer;

  LandingGearController ref_dut (
    .Clock(Clock),
    .Clear(Clear),
    .GearIsDown(GearIsDown),
    .GearIsUp(GearIsUp),
    .PlaneOnGround(PlaneOnGround),
    .TimeUp(TimeUp),
    .Lever(Lever),
    .RedLED(ref_RedLED),
    .GrnLED(ref_GrnLED),
    .Valve(ref_Valve),
    .Pump(ref_Pump),
    .Timer(ref_Timer)
  );

  landing_gear_fsm gen (
    .Clock(Clock),
    .Clear(Clear),
    .GearIsDown(GearIsDown),
    .GearIsUp(GearIsUp),
    .PlaneOnGround(PlaneOnGround),
    .TimeUp(TimeUp),
    .Lever(Lever),
    .RedLED(gen_RedLED),
    .GrnLED(gen_GrnLED),
    .Valve(gen_Valve),
    .Pump(gen_Pump),
    .Timer(gen_Timer)
  );

  function integer ref_state_index(input [6:0] value);
    begin
      case (value)
        ref_dut.TAXI: ref_state_index = 0;
        ref_dut.TUP: ref_state_index = 1;
        ref_dut.TDN: ref_state_index = 2;
        ref_dut.GOUP: ref_state_index = 3;
        ref_dut.GODN: ref_state_index = 4;
        ref_dut.FLYUP: ref_state_index = 5;
        ref_dut.FLYDN: ref_state_index = 6;
        default: ref_state_index = -1;
      endcase
    end
  endfunction

  function integer gen_state_index(input [2:0] value);
    begin
      case (value)
        gen.TAXI: gen_state_index = 0;
        gen.TUP: gen_state_index = 1;
        gen.TDN: gen_state_index = 2;
        gen.GOUP: gen_state_index = 3;
        gen.GODN: gen_state_index = 4;
        gen.FLYUP: gen_state_index = 5;
        gen.FLYDN: gen_state_index = 6;
        default: gen_state_index = -1;
      endcase
    end
  endfunction

  task drive_combo(input [4:0] combo);
    begin
      GearIsDown = combo[0];
      GearIsUp = combo[1];
      PlaneOnGround = combo[2];
      TimeUp = combo[3];
      Lever = combo[4];
    end
  endtask

  task check_state(input [6:0] ref_state, input [2:0] gen_state, input [255:0] label);
    integer combo;
    begin
      for (combo = 0; combo < 32; combo = combo + 1) begin
        ref_dut.State = ref_state;
        gen.state = gen_state;
        drive_combo(combo[4:0]);
        #1;
        if ({ref_RedLED, ref_GrnLED, ref_Valve, ref_Pump, ref_Timer} !==
            {gen_RedLED, gen_GrnLED, gen_Valve, gen_Pump, gen_Timer}) begin
          $display("OUTPUT MISMATCH %0s combo=%0d", label, combo);
          $display("ref_dut=%b gen=%b",
                   {ref_RedLED, ref_GrnLED, ref_Valve, ref_Pump, ref_Timer},
                   {gen_RedLED, gen_GrnLED, gen_Valve, gen_Pump, gen_Timer});
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
    check_state(ref_dut.TAXI, gen.TAXI, "TAXI");
    check_state(ref_dut.TUP, gen.TUP, "TUP");
    check_state(ref_dut.TDN, gen.TDN, "TDN");
    check_state(ref_dut.GOUP, gen.GOUP, "GOUP");
    check_state(ref_dut.GODN, gen.GODN, "GODN");
    check_state(ref_dut.FLYUP, gen.FLYUP, "FLYUP");
    check_state(ref_dut.FLYDN, gen.FLYDN, "FLYDN");
    $display("PASS landing gear reference equivalence");
    $finish;
  end
endmodule
