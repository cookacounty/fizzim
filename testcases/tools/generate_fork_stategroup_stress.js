#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const repo = path.resolve(__dirname, "..", "..");
const templatePath = path.join(repo, "testcases", "generic_state_machine.fzm");
const outFzm = path.join(repo, "testcases", "fork_stategroup_stress.fzm");
const outRef = path.join(repo, "testcases", "fork_stategroup_stress_ref.sv");
const outTb = path.join(repo, "testcases", "tb_fork_stategroup_stress_equiv.sv");

const OBJECTS = new Set(["state", "transition", "fork", "stategroup", "textObj"]);
const trim = (s) => s.trim();

function readObjects(lines) {
  const pre = [], objects = [];
  let post = [], inObjects = false;
  for (let i = 0; i < lines.length;) {
    const token = trim(lines[i]);
    if (token === "## START OBJECTS") {
      inObjects = true;
      pre.push(lines[i++]);
      continue;
    }
    if (token === "## END OBJECTS") {
      post = lines.slice(i);
      break;
    }
    if (!inObjects) {
      pre.push(lines[i++]);
      continue;
    }
    const m = token.match(/^<([^/][^>]*)>$/);
    if (m && OBJECTS.has(m[1])) {
      const kind = m[1], end = `</${kind}>`, block = [lines[i++]];
      while (i < lines.length) {
        block.push(lines[i]);
        if (trim(lines[i++]) === end) break;
      }
      objects.push({ kind, block });
      continue;
    }
    i++;
  }
  return { pre, objects, post };
}

function findClose(block, start, tag) {
  const end = `</${tag}>`;
  for (let i = start + 1; i < block.length; i++) if (trim(block[i]) === end) return i;
  return -1;
}

function simple(block, tag) {
  const open = `<${tag}>`;
  for (let i = 0; i < block.length - 1; i++) if (trim(block[i]) === open) return trim(block[i + 1]);
  return "";
}

function setSimple(block, tag, value) {
  const open = `<${tag}>`;
  for (let i = 0; i < block.length - 1; i++) {
    if (trim(block[i]) === open) {
      block[i + 1] = block[i + 1].replace(/\S.*$/, String(value));
      return;
    }
  }
}

function attrRanges(block) {
  const start = block.findIndex((l) => trim(l) === "<attributes>");
  const end = block.findIndex((l) => trim(l) === "</attributes>");
  const ranges = {};
  if (start < 0 || end < 0) return ranges;
  for (let i = start + 1; i < end;) {
    const m = trim(block[i]).match(/^<([^/][^>]*)>$/);
    if (m) {
      const close = findClose(block, i, m[1]);
      if (close > i && close < end) {
        ranges[m[1]] = [i, close + 1];
        i = close + 1;
        continue;
      }
    }
    i++;
  }
  return ranges;
}

function attrBlock(block, name) {
  const r = attrRanges(block)[name];
  return r ? block.slice(r[0], r[1]) : null;
}

function setAttrBlock(block, name, attr) {
  const ranges = attrRanges(block);
  if (ranges[name]) block.splice(ranges[name][0], ranges[name][1] - ranges[name][0], ...attr);
}

function setAttrValue(block, name, value, status = "LOCAL") {
  const attr = attrBlock(block, name);
  if (!attr) return;
  for (let i = 0; i < attr.length - 1; i++) {
    if (trim(attr[i]) === "<value>") attr[i + 1] = attr[i + 1].replace(/\S.*$/, String(value));
    if (trim(attr[i]) === "<status>" && i > 0) {
      let inValue = false;
      for (let j = i; j >= 0; j--) {
        if (trim(attr[j]) === "</value>") break;
        if (trim(attr[j]) === "<value>") inValue = true;
      }
      if (inValue) attr[i + 1] = attr[i + 1].replace(/\S.*$/, status);
    }
  }
  setAttrBlock(block, name, attr);
}

function setObjName(obj, name) {
  setAttrValue(obj.block, "name", name);
}

function clone(obj) {
  return { kind: obj.kind, block: obj.block.slice() };
}

function setOutputValues(obj, values, fillDefaults = false) {
  const defaults = {
    rdy: "0",
    busy: "0",
    "color[100:0]": '"none"',
    trigger: "0",
    "trigger1[1:0]": "0",
  };
  ["rdy", "busy", "color[100:0]", "trigger", "trigger1[1:0]"].forEach((name) => {
    const hasValue = Object.prototype.hasOwnProperty.call(values, name);
    const rawValue = hasValue ? values[name] : (fillDefaults ? defaults[name] : "");
    const value = rawValue === null ? "" : rawValue;
    setAttrValue(obj.block, name, value, value === "" ? "GLOBAL_VAR" : "LOCAL");
  });
}

function makeState(template, name, x, y, values = {}) {
  const obj = clone(template);
  setObjName(obj, name);
  setSimple(obj.block, "x0", x);
  setSimple(obj.block, "y0", y);
  setSimple(obj.block, "x1", x + 140);
  setSimple(obj.block, "y1", y + 75);
  setSimple(obj.block, "page", "1");
  setOutputValues(obj, values, true);
  return obj;
}

function makeFork(template, name, x, y) {
  const obj = clone(template);
  setSimple(obj.block, "name", name);
  setSimple(obj.block, "x0", x);
  setSimple(obj.block, "y0", y);
  setSimple(obj.block, "x1", x + 20);
  setSimple(obj.block, "y1", y + 20);
  setSimple(obj.block, "page", "1");
  return obj;
}

function makeGroup(template, name, x0, y0, x1, y1, entry, children, values = {}) {
  const obj = clone(template);
  setObjName(obj, name);
  setSimple(obj.block, "x0", x0);
  setSimple(obj.block, "y0", y0);
  setSimple(obj.block, "x1", x1);
  setSimple(obj.block, "y1", y1);
  setSimple(obj.block, "entryState", entry);
  setOutputValues(obj, values, true);
  const start = obj.block.findIndex((l) => trim(l) === "<children>");
  const end = obj.block.findIndex((l) => trim(l) === "</children>");
  if (start >= 0 && end > start) {
    const childLines = ["   <children>"];
    children.forEach((child) => childLines.push("      <child>", `      ${child}`, "      </child>"));
    childLines.push("   </children>");
    obj.block.splice(start, end - start + 1, ...childLines);
  }
  return obj;
}

function makeTransition(template, name, start, end, equation, priority, actions = {}) {
  const obj = clone(template);
  setObjName(obj, name);
  setSimple(obj.block, "startState", start);
  setSimple(obj.block, "endState", end);
  setSimple(obj.block, "startStateIndex", "-1");
  setSimple(obj.block, "endStateIndex", "-1");
  setSimple(obj.block, "page", "1");
  setAttrValue(obj.block, "equation", equation, equation === "1" ? "GLOBAL_VAR" : "LOCAL");
  setAttrValue(obj.block, "priority", String(priority), "LOCAL");
  setOutputValues(obj, actions);
  return obj;
}

function replaceGlobalAttrValue(text, attrName, value) {
  const re = new RegExp(`(<${attrName}>[\\s\\S]*?<value>\\s*\\n\\s*)[\\s\\S]*?(\\n\\s*<status>)`);
  return text.replace(re, `$1${value}$2`);
}

function addInput(preText, sourceName, targetName) {
  if (preText.includes(`<${targetName}>`)) return preText;
  const source = new RegExp(`(      <${sourceName}>[\\s\\S]*?      </${sourceName}>\\n)`);
  const match = preText.match(source);
  if (!match) throw new Error(`could not find input template ${sourceName}`);
  const block = match[1]
    .replace(new RegExp(`<${sourceName}>`, "g"), `<${targetName}>`)
    .replace(new RegExp(`</${sourceName}>`, "g"), `</${targetName}>`);
  return preText.replace("   </inputs>", block + "   </inputs>");
}

function writeReferenceRtl() {
  fs.writeFileSync(outRef, `module fork_stategroup_stress_ref (
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
        S_A0: begin
          if (break_req) begin
            trigger1 <= 2'd2;
            if (mode0) trigger <= 1'b1;
          end
          else if (mode1) color <= "xact";
        end
        S_A1: begin
          if (break_req) begin
            trigger1 <= 2'd2;
            if (mode0) trigger <= 1'b1;
          end
          else if (start) trigger <= 1'b1;
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
`);
}

function writeTestbench() {
  fs.writeFileSync(outTb, `module tb_fork_stategroup_stress_equiv;
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
`);
}

function main() {
  const lines = fs.readFileSync(templatePath, "utf8").split(/\r?\n/);
  let { pre, objects, post } = readObjects(lines);
  let preText = pre.join("\n").replace(/generic_state_action/g, "fork_stategroup_stress");
  preText = replaceGlobalAttrValue(preText, "reset_state", "S_IDLE");
  preText = addInput(preText, "start", "mode0");
  preText = addInput(preText, "start", "mode1");
  pre = preText.split("\n");

  const stateTemplate = objects.find((o) => o.kind === "state");
  const transitionTemplate = objects.find((o) => o.kind === "transition");
  const forkTemplate = objects.find((o) => o.kind === "fork");
  const groupTemplate = objects.find((o) => o.kind === "stategroup");

  const states = [
    makeState(stateTemplate, "S_IDLE", 100, 100, { "color[100:0]": '"idle"' }),
    makeState(stateTemplate, "S_A0", 420, 140, { busy: null, "color[100:0]": null }),
    makeState(stateTemplate, "S_A1", 620, 140, { busy: null, "color[100:0]": '"a1"' }),
    makeState(stateTemplate, "S_B0", 420, 420, { busy: null, "color[100:0]": null }),
    makeState(stateTemplate, "S_B1", 620, 420, { busy: null, "color[100:0]": '"b1"' }),
    makeState(stateTemplate, "S_C", 930, 260, { "color[100:0]": '"check"' }),
    makeState(stateTemplate, "S_DONE", 1180, 160, { rdy: "1", "color[100:0]": '"done"' }),
    makeState(stateTemplate, "S_ERR", 1180, 420, { "color[100:0]": '"error"' }),
  ];

  const groups = [
    makeGroup(groupTemplate, "SG_A", 380, 80, 820, 260, "S_A0", ["S_A0", "S_A1"],
      { busy: "1", "color[100:0]": '"grp_a"' }),
    makeGroup(groupTemplate, "SG_B", 380, 360, 820, 540, "S_B0", ["S_B0", "S_B1"],
      { busy: "1", "color[100:0]": '"grp_b"' }),
  ];

  const forks = [
    makeFork(forkTemplate, "F0", 270, 120),
    makeFork(forkTemplate, "F1", 270, 260),
    makeFork(forkTemplate, "F2", 860, 360),
    makeFork(forkTemplate, "F3", 900, 120),
  ];

  const transitions = [
    makeTransition(transitionTemplate, "T_IDLE_F0", "S_IDLE", "F0", "start", 0, { trigger: "1" }),
    makeTransition(transitionTemplate, "T_F0_GA", "F0", "SG_A", "mode0", 0, { "trigger1[1:0]": "1" }),
    makeTransition(transitionTemplate, "T_F0_F1", "F0", "F1", "break_req", 1, { "trigger1[1:0]": "2" }),
    makeTransition(transitionTemplate, "T_F0_ERR", "F0", "S_ERR", "1", 2, { "trigger1[1:0]": "3" }),
    makeTransition(transitionTemplate, "T_F1_F2", "F1", "F2", "mode1", 0, {}),
    makeTransition(transitionTemplate, "T_F1_GB", "F1", "SG_B", "1", 1, { trigger: "1" }),
    makeTransition(transitionTemplate, "T_F2_C", "F2", "S_C", "option1", 0, { "trigger1[1:0]": "1" }),
    makeTransition(transitionTemplate, "T_F2_ERR", "F2", "S_ERR", "1", 1, {}),
    makeTransition(transitionTemplate, "T_GA_F3", "SG_A", "F3", "break_req", 0, { "trigger1[1:0]": "2" }),
    makeTransition(transitionTemplate, "T_A0_A1", "S_A0", "S_A1", "mode1", 5, { "color[100:0]": '"xact"' }),
    makeTransition(transitionTemplate, "T_A1_GB", "S_A1", "SG_B", "start", 5, { trigger: "1" }),
    makeTransition(transitionTemplate, "T_F3_DONE", "F3", "S_DONE", "mode0", 0, { trigger: "1" }),
    makeTransition(transitionTemplate, "T_F3_ERR", "F3", "S_ERR", "1", 1, {}),
    makeTransition(transitionTemplate, "T_GB_F2", "SG_B", "F2", "break_req", 0, { "trigger1[1:0]": "3" }),
    makeTransition(transitionTemplate, "T_B0_B1", "S_B0", "S_B1", "mode0", 5, {}),
    makeTransition(transitionTemplate, "T_B1_DONE", "S_B1", "S_DONE", "mode1", 5, {}),
    makeTransition(transitionTemplate, "T_B1_GA", "S_B1", "SG_A", "start", 6, {}),
    makeTransition(transitionTemplate, "T_C_F3", "S_C", "F3", "start", 0, {}),
    makeTransition(transitionTemplate, "T_DONE_IDLE", "S_DONE", "S_IDLE", "1", 0, {}),
    makeTransition(transitionTemplate, "T_ERR_IDLE", "S_ERR", "S_IDLE", "start", 0, {}),
  ];

  const finalObjects = states.concat(groups, forks, transitions);
  const fzmLines = pre.concat(finalObjects.flatMap((o) => o.block), post)
    .map((line) => line.replace(/\s+$/, ""));
  while (fzmLines.length && fzmLines[fzmLines.length - 1] === "") {
    fzmLines.pop();
  }
  fs.writeFileSync(outFzm, fzmLines.join("\n") + "\n");
  writeReferenceRtl();
  writeTestbench();
}

main();
