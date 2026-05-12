module timrudy_uart8_tx_fsm (
  output reg busy,
  output reg done,
  output reg out,
  input wire bit_index_done,
  input wire clk,
  input wire en,
  input wire start,
  input wire turbo_frames
);

  // state bits
  parameter 
  RESET     = 3'b000, 
  DATA_BITS = 3'b001, 
  IDLE      = 3'b010, 
  START_BIT = 3'b011, 
  STOP_BIT  = 3'b100; 

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
        if (bit_index_done) begin
          nextstate = STOP_BIT;
        end
      end
      IDLE     : begin
        if (start) begin
          nextstate = START_BIT;
        end
      end
      START_BIT: begin
        begin
          nextstate = DATA_BITS;
        end
      end
      STOP_BIT : begin
        if (!start) begin
          nextstate = RESET;
        end
        else if (start && !done && turbo_frames) begin
          nextstate = START_BIT;
        end
        else if (start && done) begin
          nextstate = START_BIT;
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
      out <= 1'b1;
    end
    else begin
      busy <= 1'b0; // default
      done <= 1'b0; // default
      out <= 1'b1; // default
      case (nextstate)
        START_BIT: begin
          busy <= 1'b1;
          out <= 1'b0;
        end
        STOP_BIT : begin
          done <= 1'b1;
        end
      endcase
      // datapath transition actions
      case (state)
        RESET    : begin
          if (en) begin
            busy <= 0;
          end
        end
        DATA_BITS: begin
          if (bit_index_done) begin
            busy <= 0;
          end
        end
        IDLE     : begin
          if (start) begin
            busy <= 0;
          end
        end
        START_BIT: begin
          begin
            busy <= 0;
          end
        end
        STOP_BIT : begin
          if (!start) begin
            busy <= 0;
          end
          else if ((start && !done && turbo_frames) && !((!start))) begin
            busy <= 0;
          end
          else if ((start && done) && !((!start) || (start && !done && turbo_frames))) begin
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
