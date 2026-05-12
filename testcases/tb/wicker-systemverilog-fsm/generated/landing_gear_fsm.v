module landing_gear_fsm 
  #(
    parameter DOWN = 1'b1,
    parameter NO = 1'b0,
    parameter UP = 1'b0,
    parameter YES = 1'b1
  )(
  output reg GrnLED,
  output reg Pump,
  output reg RedLED,
  output reg Timer,
  output reg Valve,
  input wire Clear,
  input wire Clock,
  input wire GearIsDown,
  input wire GearIsUp,
  input wire Lever,
  input wire PlaneOnGround,
  input wire TimeUp
);

  // state bits
  parameter 
  TAXI  = 3'b000, 
  FLYDN = 3'b001, 
  FLYUP = 3'b010, 
  GODN  = 3'b011, 
  GOUP  = 3'b100, 
  TDN   = 3'b101, 
  TUP   = 3'b110; 

  reg [2:0] state;
  reg [2:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    GrnLED = 1'b0; // default
    Pump = 1'b0; // default
    RedLED = 1'b0; // default
    Timer = 1'b0; // default
    Valve = 1'b0; // default
    case (state)
      TAXI : begin
        GrnLED = 1'b1;
        Timer = 1'b1;
        Valve = DOWN;
        if ((PlaneOnGround == NO) && (Lever == UP)) begin
          nextstate = TUP;
        end
        else if ((PlaneOnGround == NO) && (Lever == DOWN)) begin
          nextstate = TDN;
        end
      end
      FLYDN: begin
        GrnLED = 1'b1;
        Valve = DOWN;
        if (PlaneOnGround == YES) begin
          nextstate = TAXI;
        end
        else if (Lever == UP) begin
          nextstate = GOUP;
        end
      end
      FLYUP: begin
        Valve = UP;
        if (Lever == DOWN) begin
          nextstate = GODN;
        end
      end
      GODN : begin
        Pump = 1'b1;
        RedLED = 1'b1;
        Valve = DOWN;
        if ((PlaneOnGround == YES) && (GearIsDown == YES)) begin
          nextstate = TAXI;
        end
        else if (GearIsDown == YES) begin
          nextstate = FLYDN;
        end
      end
      GOUP : begin
        Pump = 1'b1;
        RedLED = 1'b1;
        Valve = UP;
        if (GearIsUp == YES) begin
          nextstate = FLYUP;
        end
      end
      TDN  : begin
        GrnLED = 1'b1;
        Valve = DOWN;
        if (PlaneOnGround) begin
          nextstate = TAXI;
        end
        else if (GearIsDown == NO) begin
          nextstate = GOUP;
        end
        else if (TimeUp == YES) begin
          nextstate = FLYDN;
        end
        else if ((TimeUp == NO) && (Lever == UP)) begin
          nextstate = TUP;
        end
      end
      TUP  : begin
        GrnLED = 1'b1;
        Valve = UP;
        if (PlaneOnGround) begin
          nextstate = TAXI;
        end
        else if (GearIsDown == NO) begin
          nextstate = GOUP;
        end
        else if (TimeUp == YES) begin
          nextstate = FLYDN;
        end
        else if ((TimeUp == NO) && (Lever == DOWN)) begin
          nextstate = TDN;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge Clock or posedge Clear) begin
    if (Clear)
      state <= TAXI;
    else
      state <= nextstate;
  end

  // This code allows you to see state names in simulation
  `ifndef SYNTHESIS
  reg [2047:0] statename;
  always @* begin
    case (state)
      TAXI :
        statename = "TAXI";
      FLYDN:
        statename = "FLYDN";
      FLYUP:
        statename = "FLYUP";
      GODN :
        statename = "GODN";
      GOUP :
        statename = "GOUP";
      TDN  :
        statename = "TDN";
      TUP  :
        statename = "TUP";
      default:
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
