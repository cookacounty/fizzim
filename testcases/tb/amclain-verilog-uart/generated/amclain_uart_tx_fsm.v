module amclain_uart_tx_fsm 
  #(
    parameter TOTAL_BITS_TO_SEND = 4'd10
  )(
  output reg busy_o,
  output reg serial_o,
  input wire bit_timer_done,
  input wire clock_i,
  input wire packet_count_done,
  input wire parity_bit_i,
  input wire parity_even_i,
  input wire reset_i,
  input wire two_stop_bits_i,
  input wire write_has_triggered,
  input wire write_i
);

  // state bits
  parameter 
  STATE_POST_RESET  = 2'b00, 
  STATE_IDLE        = 2'b01, 
  STATE_SEND_PACKET = 2'b10; 

  reg [1:0] state;
  reg [1:0] nextstate;

  // comb always block
  always @* begin
    nextstate = state; // default to hold value because implied_loopback is set
    busy_o = 1'b1; // default
    case (state)
      STATE_POST_RESET : begin
        if (bit_timer_done && packet_count_done) begin
          nextstate = STATE_IDLE;
        end
      end
      STATE_IDLE       : begin
        busy_o = 1'b0;
        if (write_i && !write_has_triggered) begin
          nextstate = STATE_SEND_PACKET;
        end
      end
      STATE_SEND_PACKET: begin
        if (packet_count_done) begin
          nextstate = STATE_IDLE;
        end
      end
    endcase
  end

  // Assign reg'd outputs to state bits

  // sequential always block
  always @(posedge clock_i or posedge reset_i) begin
    if (reset_i)
      state <= STATE_POST_RESET;
    else
      state <= nextstate;
  end

  // datapath sequential always block
  always @(posedge clock_i or posedge reset_i) begin
    if (reset_i) begin
      serial_o <= 1'b1;
    end
    else begin
      serial_o <= 1'b1; // default
        // Warning D9: Did not find any non-default values for any datapath outputs - suppressing case statement 
    end
  end

  // This code allows you to see state names in simulation
  `ifndef SYNTHESIS
  reg [2047:0] statename;
  always @* begin
    case (state)
      STATE_POST_RESET :
        statename = "STATE_POST_RESET";
      STATE_IDLE       :
        statename = "STATE_IDLE";
      STATE_SEND_PACKET:
        statename = "STATE_SEND_PACKET";
      default          :
        statename = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
    endcase
  end
  `endif

endmodule
