module ticket_machine_fsm 
  #(
    parameter OFF = 1'b0,
    parameter ON = 1'b1
  )(
  output reg Bill,
  output reg Dispense,
  output reg Ready,
  output reg Return,
  input wire Clear,
  input wire Clock,
  input wire Ten,
  input wire Twenty
);

  // state bits
  parameter 
  RDY    = 3'b000, 
  BILL10 = 3'b001, 
  BILL20 = 3'b010, 
  BILL30 = 3'b011, 
  DISP   = 3'b100, 
  RTN    = 3'b101; 

  reg [2:0] state;
  reg [2:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    Bill = 1'b0; // default
    Dispense = 1'b0; // default
    Ready = 1'b0; // default
    Return = 1'b0; // default
    case (state)
      RDY   : begin
        Ready = 1'b1;
        if (Ten) begin
          nextstate = BILL10;
        end
        else if (Twenty) begin
          nextstate = BILL20;
        end
      end
      BILL10: begin
        Bill = 1'b1;
        if (Ten) begin
          nextstate = BILL20;
        end
        else if (Twenty) begin
          nextstate = BILL30;
        end
      end
      BILL20: begin
        Bill = 1'b1;
        if (Ten) begin
          nextstate = BILL30;
        end
        else if (Twenty) begin
          nextstate = DISP;
        end
      end
      BILL30: begin
        Bill = 1'b1;
        if (Ten) begin
          nextstate = DISP;
        end
        else if (Twenty) begin
          nextstate = RTN;
        end
      end
      DISP  : begin
        Dispense = 1'b1;
        begin
          nextstate = RDY;
        end
      end
      RTN   : begin
        Return = 1'b1;
        begin
          nextstate = RDY;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge Clock or posedge Clear) begin
    if (Clear)
      state <= RDY;
    else
      state <= nextstate;
  end

  // This code allows you to see state names in simulation
  `ifndef SYNTHESIS
  reg [2047:0] statename;
  always @* begin
    case (state)
      RDY   :
        statename = "RDY";
      BILL10:
        statename = "BILL10";
      BILL20:
        statename = "BILL20";
      BILL30:
        statename = "BILL30";
      DISP  :
        statename = "DISP";
      RTN   :
        statename = "RTN";
      default:
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
