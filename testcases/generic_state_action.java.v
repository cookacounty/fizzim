module generic_state_action (
  output reg busy,
  output reg [100:0] color,
  output reg rdy,
  output reg trigger,
  output reg [1:0] trigger1,
  input wire break_req,
  input wire clk,
  input wire option1,
  input wire rst_l,
  input wire start
);

  // state bits
  parameter 
  S1       = 4'b0000, 
  S2       = 4'b0001, 
  S3       = 4'b0010, 
  S_BLUE   = 4'b0011, 
  S_GREEN  = 4'b0100, 
  S_RED    = 4'b0101, 
  S_YELLOW = 4'b0110, 
  state49  = 4'b0111, 
  state51  = 4'b1000; 

  reg [3:0] state;
  reg [3:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    case (state)
      S1      : begin
        begin
          nextstate = S2;
        end
      end
      S2      : begin
        if ((trigger) && (start)) begin
          nextstate = S_GREEN;
        end
        else begin
          nextstate = S1;
        end
      end
      S3      : begin
        if ((1) && (start)) begin
          nextstate = S_GREEN;
        end
      end
      S_BLUE  : begin
        if ((break_req) && (option1)) begin
          nextstate = S3;
        end
        else if ((break_req) && (1)) begin
          nextstate = S2;
        end
        else begin
          nextstate = state51;
        end
      end
      S_GREEN : begin
        if ((break_req) && (option1)) begin
          nextstate = S3;
        end
        else if ((break_req) && (1)) begin
          nextstate = S2;
        end
        else begin
          nextstate = state51;
        end
      end
      S_RED   : begin
        if ((break_req) && (option1)) begin
          nextstate = S3;
        end
        else if ((break_req) && (1)) begin
          nextstate = S2;
        end
        else begin
          nextstate = state51;
        end
      end
      S_YELLOW: begin
        if ((break_req) && (option1)) begin
          nextstate = S3;
        end
        else if ((break_req) && (1)) begin
          nextstate = S2;
        end
        else begin
          nextstate = state51;
        end
      end
      state49 : begin
        begin
          nextstate = S1;
        end
      end
      state51 : begin
        begin
          nextstate = state49;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge clk or negedge rst_l) begin
    if (!rst_l)
      state <= S1;
    else
      state <= nextstate;
  end

  // datapath sequential always block
  always @(posedge clk or negedge rst_l) begin
    if (!rst_l) begin
      busy <= 0;
      color[100:0] <= 0;
      rdy <= 0;
      trigger <= 0;
      trigger1[1:0] <= 0;
    end
    else begin
      busy <= 0; // default
      color[100:0] <= "none"; // default
      rdy <= 0; // default
      trigger <= 0; // default
      trigger1[1:0] <= 0; // default
      case (nextstate)
        S2      : begin
          rdy <= state==S1 && nextstate==S2;
        end
        S_BLUE  : begin
          color[100:0] <= "blue";
        end
        S_GREEN : begin
          color[100:0] <= "green";
        end
        S_RED   : begin
          color[100:0] <= "red";
        end
        S_YELLOW: begin
          color[100:0] <= "yellow";
        end
        state49 : begin
          trigger1[1:0] <= 2;
        end
      endcase
      // datapath transition actions
      case (state)
        S2      : begin
          if ((trigger) && (start)) begin
            trigger <= 1;
          end
        end
        S3      : begin
          if ((1) && (start)) begin
            trigger <= 1;
          end
        end
        state49 : begin
          begin
            trigger1[1:0] <= 1;
          end
        end
      endcase
    end
  end

  // This code allows you to see state names in simulation
  `ifndef SYNTHESIS
  reg [2047:0] statename;
  always @* begin
    case (state)
      S1      :
        statename = "S1";
      S2      :
        statename = "S2";
      S3      :
        statename = "S3";
      S_BLUE  :
        statename = "SG_ACTIVE.S_BLUE";
      S_GREEN :
        statename = "SG_ACTIVE.S_GREEN";
      S_RED   :
        statename = "SG_ACTIVE.S_RED";
      S_YELLOW:
        statename = "SG_ACTIVE.S_YELLOW";
      state49 :
        statename = "SG_2.state49";
      state51 :
        statename = "SG_2.state51";
      default :
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
