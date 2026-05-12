module amclain_uart_rx_fsm (
  output reg [7:0] data_o,
  output reg ready_o,
  input wire ack_has_triggered,
  input wire ack_i,
  input wire clock_divider_valid,
  input wire clock_i,
  input wire packet_done,
  input wire packet_valid,
  input wire reset_i,
  input wire sample_now,
  input wire serial_i,
  input wire start_bit_invalid
);

  // state bits
  parameter 
  STATE_IDLE            = 2'b00, 
  STATE_RECEIVE_PACKET  = 2'b01, 
  STATE_VALIDATE_PACKET = 2'b10; 

  reg [1:0] state;
  reg [1:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    case (state)
      STATE_IDLE           : begin
        if (!serial_i && clock_divider_valid) begin
          nextstate = STATE_RECEIVE_PACKET;
        end
      end
      STATE_RECEIVE_PACKET : begin
        if (sample_now && start_bit_invalid) begin
          nextstate = STATE_IDLE;
        end
        else if (sample_now && packet_done) begin
          nextstate = STATE_VALIDATE_PACKET;
        end
      end
      STATE_VALIDATE_PACKET: begin
        begin
          nextstate = STATE_IDLE;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge clock_i or posedge reset_i) begin
    if (reset_i)
      state <= STATE_IDLE;
    else
      state <= nextstate;
  end

  // datapath sequential always block
  always @(posedge clock_i or posedge reset_i) begin
    if (reset_i) begin
      data_o[7:0] <= 8'h00;
      ready_o <= 1'b0;
    end
    else begin
      data_o[7:0] <= 8'h00; // default
      ready_o <= 1'b0; // default
        // Warning D9: Did not find any non-default values for any datapath outputs - suppressing case statement 
    end
  end

  // This code allows you to see state names in simulation
  `ifndef SYNTHESIS
  reg [2047:0] statename;
  always @* begin
    case (state)
      STATE_IDLE           :
        statename = "STATE_IDLE";
      STATE_RECEIVE_PACKET :
        statename = "STATE_RECEIVE_PACKET";
      STATE_VALIDATE_PACKET:
        statename = "STATE_VALIDATE_PACKET";
      default              :
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
