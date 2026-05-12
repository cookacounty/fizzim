module timrudy_uart8_rx_fsm (
  output reg busy,
  output reg done,
  output reg err,
  output reg [7:0] out,
  input wire clk,
  input wire en,
  input wire in_sample,
  input wire ready_done,
  input wire ready_reset,
  input wire sample_done,
  input wire start_low_accepted,
  input wire stop_midpoint,
  input wire stop_valid
);

  // state bits
  parameter 
  RESET     = 3'b000, 
  DATA_BITS = 3'b001, 
  IDLE      = 3'b010, 
  READY     = 3'b011, 
  START_BIT = 3'b100, 
  STOP_BIT  = 3'b101; 

  reg [2:0] state;
  reg [2:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    case (state)
      RESET    : begin
        if (en) begin
          nextstate = IDLE;
        end
      end
      DATA_BITS: begin
        if (sample_done) begin
          nextstate = STOP_BIT;
        end
      end
      IDLE     : begin
        if (!in_sample && start_low_accepted) begin
          nextstate = START_BIT;
        end
      end
      READY    : begin
        if (ready_done) begin
          nextstate = IDLE;
        end
        else if (ready_reset) begin
          nextstate = RESET;
        end
      end
      START_BIT: begin
        if (sample_done) begin
          nextstate = DATA_BITS;
        end
      end
      STOP_BIT : begin
        if (stop_midpoint && stop_valid) begin
          nextstate = READY;
        end
        else if (stop_midpoint && !in_sample) begin
          nextstate = IDLE;
        end
        else if (stop_midpoint && !stop_valid) begin
          nextstate = IDLE;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge clk or negedge en) begin
    if (!en)
      state <= RESET;
    else
      state <= nextstate;
  end

  // datapath sequential always block
  always @(posedge clk or negedge en) begin
    if (!en) begin
      busy <= 1'b0;
      done <= 1'b0;
      err <= 1'b0;
      out[7:0] <= 8'b0;
    end
    else begin
      busy <= 1'b0; // default
      done <= 1'b0; // default
      err <= 1'b0; // default
      out[7:0] <= 8'b0; // default
      // datapath transition actions
      case (state)
        RESET    : begin
          if (en) begin
            busy <= 0;
          end
        end
        DATA_BITS: begin
          if (sample_done) begin
            busy <= 0;
          end
        end
        IDLE     : begin
          if (!in_sample && start_low_accepted) begin
            busy <= 0;
          end
        end
        READY    : begin
          if (ready_done) begin
            busy <= 0;
          end
          else if ((ready_reset) && !((ready_done))) begin
            busy <= 0;
          end
        end
        START_BIT: begin
          if (sample_done) begin
            busy <= 0;
          end
        end
        STOP_BIT : begin
          if (stop_midpoint && stop_valid) begin
            busy <= 0;
          end
          else if ((stop_midpoint && !in_sample) && !((stop_midpoint && stop_valid))) begin
            busy <= 0;
          end
          else if ((stop_midpoint && !stop_valid) && !((stop_midpoint && stop_valid) || (stop_midpoint && !in_sample))) begin
            busy <= 0;
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
      RESET    :
        statename = "RESET";
      DATA_BITS:
        statename = "DATA_BITS";
      IDLE     :
        statename = "IDLE";
      READY    :
        statename = "READY";
      START_BIT:
        statename = "START_BIT";
      STOP_BIT :
        statename = "STOP_BIT";
      default  :
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
