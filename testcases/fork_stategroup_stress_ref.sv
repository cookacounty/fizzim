module fork_stategroup_stress_ref (
  output reg busy,
  output reg [100:0] color,
  output reg rdy,
  output reg trigger,
  output reg [1:0] trigger1,
  input wire break_req,
  input wire clk,
  input wire mode0,
  input wire mode1,
  input wire option1,
  input wire rst_l,
  input wire start
);
  localparam S_A0   = 4'd0;
  localparam S_A1   = 4'd1;
  localparam S_B0   = 4'd2;
  localparam S_B1   = 4'd3;
  localparam S_C    = 4'd4;
  localparam S_DONE = 4'd5;
  localparam S_ERR  = 4'd6;
  localparam S_IDLE = 4'd7;

  reg [3:0] state;
  reg [3:0] nextstate;

  always @* begin
    nextstate = state;
    case (state)
      S_IDLE: begin
        if (start) begin
          if (mode0) nextstate = S_A0;
          else if (break_req) begin
            if (mode1) begin
              if (option1) nextstate = S_C;
              else nextstate = S_ERR;
            end
            else nextstate = S_B0;
          end
          else nextstate = S_ERR;
        end
      end
      S_A0: begin
        if (break_req) begin
          if (mode0) nextstate = S_DONE;
          else nextstate = S_ERR;
        end
        else if (mode1) nextstate = S_A1;
      end
      S_A1: begin
        if (break_req) begin
          if (mode0) nextstate = S_DONE;
          else nextstate = S_ERR;
        end
        else if (start) nextstate = S_B0;
      end
      S_B0: begin
        if (break_req) begin
          if (option1) nextstate = S_C;
          else nextstate = S_ERR;
        end
        else if (mode0) nextstate = S_B1;
      end
      S_B1: begin
        if (break_req) begin
          if (option1) nextstate = S_C;
          else nextstate = S_ERR;
        end
        else if (mode1) nextstate = S_DONE;
        else if (start) nextstate = S_A0;
      end
      S_C: begin
        if (start) begin
          if (mode0) nextstate = S_DONE;
          else nextstate = S_ERR;
        end
      end
      S_DONE: nextstate = S_IDLE;
      S_ERR: if (start) nextstate = S_IDLE;
    endcase
  end

  always @(posedge clk or negedge rst_l) begin
    if (!rst_l)
      state <= S_IDLE;
    else
      state <= nextstate;
  end

  always @(posedge clk or negedge rst_l) begin
    if (!rst_l) begin
      busy <= 1'b0;
      color <= 101'd0;
      rdy <= 1'b0;
      trigger <= 1'b0;
      trigger1 <= 2'd0;
    end else begin
      busy <= 1'b0;
      color <= "none";
      rdy <= 1'b0;
      trigger <= 1'b0;
      trigger1 <= 2'd0;
      case (nextstate)
        S_IDLE: color <= "idle";
        S_A0: begin busy <= 1'b1; color <= "grp_a"; end
        S_A1: begin busy <= 1'b1; color <= "a1"; end
        S_B0: begin busy <= 1'b1; color <= "grp_b"; end
        S_B1: begin busy <= 1'b1; color <= "b1"; end
        S_C: color <= "check";
        S_DONE: begin rdy <= 1'b1; color <= "done"; end
        S_ERR: color <= "error";
      endcase
      case (state)
        S_IDLE: begin
          if (start) begin
            trigger <= 1'b1;
            if (mode0) trigger1 <= 2'd1;
            else if (break_req) begin
              if (mode1) begin
                if (option1) trigger1 <= 2'd1;
                else trigger1 <= 2'd2;
              end else begin
                trigger <= 1'b1;
                trigger1 <= 2'd2;
              end
            end else trigger1 <= 2'd3;
          end
        end
        S_A0, S_A1: begin
          if (break_req) begin
            trigger1 <= 2'd2;
            if (mode0) trigger <= 1'b1;
          end
          else if (state == S_A1 && start) begin
            trigger <= 1'b1;
          end
        end
        S_B0, S_B1: begin
          if (break_req) begin
            trigger1 <= option1 ? 2'd1 : 2'd3;
          end
        end
        S_C: begin
          if (start && mode0) trigger <= 1'b1;
        end
      endcase
    end
  end
endmodule
