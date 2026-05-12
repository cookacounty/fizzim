#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const repo = path.resolve(__dirname, "..", "..");
const templatePath = path.join(repo, "testcases", "generic_state_machine.fzm");
const tbRoot = path.join(repo, "testcases", "tb");
const OBJECTS = new Set(["state", "transition", "fork", "stategroup", "textObj"]);
const trim = (s) => s.trim();

function readObjects(lines) {
  const pre = [], objects = [];
  let post = [], inObjects = false;
  for(let i = 0; i < lines.length;) {
    const token = trim(lines[i]);
    if(token === "## START OBJECTS") {
      inObjects = true;
      pre.push(lines[i++]);
      continue;
    }
    if(token === "## END OBJECTS") {
      post = lines.slice(i);
      break;
    }
    if(!inObjects) {
      pre.push(lines[i++]);
      continue;
    }
    const m = token.match(/^<([^/][^>]*)>$/);
    if(m && OBJECTS.has(m[1])) {
      const kind = m[1], end = `</${kind}>`, block = [lines[i++]];
      while(i < lines.length) {
        block.push(lines[i]);
        if(trim(lines[i++]) === end)
          break;
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
  for(let i = start + 1; i < block.length; i++)
    if(trim(block[i]) === end)
      return i;
  return -1;
}

function ranges(block, containerStart, containerEnd) {
  const start = block.findIndex((l) => trim(l) === containerStart);
  const end = block.findIndex((l) => trim(l) === containerEnd);
  const out = {};
  if(start < 0 || end < 0)
    return out;
  for(let i = start + 1; i < end;) {
    const m = trim(block[i]).match(/^<([^/][^>]*)>$/);
    if(m) {
      const close = findClose(block, i, m[1]);
      if(close > i && close < end) {
        out[m[1]] = [i, close + 1];
        i = close + 1;
        continue;
      }
    }
    i++;
  }
  return out;
}

function attrRanges(block) {
  return ranges(block, "<attributes>", "</attributes>");
}

function attrBlock(block, name) {
  const r = attrRanges(block)[name];
  return r ? block.slice(r[0], r[1]) : null;
}

function setAttrBlock(block, name, attr) {
  const existing = attrRanges(block)[name];
  if(existing)
    block.splice(existing[0], existing[1] - existing[0], ...attr);
  else {
    const insert = block.findIndex((l) => trim(l) === "</attributes>");
    if(insert >= 0)
      block.splice(insert, 0, ...attr);
  }
}

function setField(block, field, value) {
  const open = `<${field}>`;
  for(let i = 0; i < block.length - 1; i++) {
    if(trim(block[i]) === open) {
      block[i + 1] = block[i + 1].replace(/\S.*$/, String(value));
      return;
    }
  }
}

function setValueStatus(block, status) {
  let inValue = false;
  for(let i = 0; i < block.length - 1; i++) {
    const token = trim(block[i]);
    if(token === "<value>")
      inValue = true;
    else if(token === "</value>")
      inValue = false;
    else if(inValue && token === "<status>") {
      block[i + 1] = block[i + 1].replace(/\S.*$/, status);
      return;
    }
  }
}

function setSimple(block, tag, value) {
  const open = `<${tag}>`;
  for(let i = 0; i < block.length - 1; i++) {
    if(trim(block[i]) === open) {
      block[i + 1] = block[i + 1].replace(/\S.*$/, String(value));
      return;
    }
  }
}

function renameBlock(attr, oldName, newName) {
  const out = attr.slice();
  out[0] = out[0].replace(`<${oldName}>`, `<${newName}>`);
  out[out.length - 1] = out[out.length - 1].replace(`</${oldName}>`, `</${newName}>`);
  return out;
}

function clone(obj) {
  return { kind: obj.kind, block: obj.block.slice() };
}

function setAttrValue(block, name, value, status = "LOCAL") {
  const attr = attrBlock(block, name);
  if(!attr)
    return;
  setField(attr, "value", value);
  setValueStatus(attr, status);
  setAttrBlock(block, name, attr);
}

function setObjName(obj, name) {
  setAttrValue(obj.block, "name", name);
}

function replaceGlobalAttrValue(pre, attrName, value) {
  const text = pre.join("\n");
  const re = new RegExp(`(<${attrName}>[\\s\\S]*?<value>\\s*\\n\\s*)[\\s\\S]*?(\\n\\s*<status>)`);
  return text.replace(re, `$1${value}$2`).split("\n");
}

function replaceGlobalAttrType(pre, attrName, value) {
  const text = pre.join("\n");
  const re = new RegExp(`(<${attrName}>[\\s\\S]*?<type>\\s*\\n\\s*)[\\s\\S]*?(\\n\\s*<status>)`);
  return text.replace(re, `$1${value}$2`).split("\n");
}

function inputRange(pre, name) {
  return ranges(pre, "<inputs>", "</inputs>")[name];
}

function outputRange(pre, name) {
  return ranges(pre, "<outputs>", "</outputs>")[name];
}

function machineRange(pre, name) {
  return ranges(pre, "<machine>", "</machine>")[name];
}

function addInput(pre, sourceName, spec) {
  if(inputRange(pre, spec.name))
    return pre;
  const source = inputRange(pre, sourceName);
  if(!source)
    throw new Error(`input template ${sourceName} not found`);
  const block = renameBlock(pre.slice(source[0], source[1]), sourceName, spec.name);
  setField(block, "value", "");
  setField(block, "type", spec.type || "");
  setField(block, "vis", "2");
  const insert = pre.findIndex((l) => trim(l) === "</inputs>");
  pre.splice(insert, 0, ...block);
  return pre;
}

function addOutput(pre, sourceName, spec) {
  let block;
  const existing = outputRange(pre, spec.name);
  if(existing)
    block = pre.slice(existing[0], existing[1]);
  else {
    const source = outputRange(pre, sourceName);
    if(!source)
      throw new Error(`output template ${sourceName} not found`);
    block = renameBlock(pre.slice(source[0], source[1]), sourceName, spec.name);
  }
  setField(block, "value", spec.def || "0");
  setField(block, "type", spec.type || "comb");
  setField(block, "resetval", spec.reset || "0");
  setField(block, "vis", "2");
  setField(block, "useratts", spec.internal ? "suppress_portlist" : "");
  if(existing)
    pre.splice(existing[0], existing[1] - existing[0], ...block);
  else {
    const insert = pre.findIndex((l) => trim(l) === "</outputs>");
    pre.splice(insert, 0, ...block);
  }
  return pre;
}

function addParameter(pre, sourceName, spec) {
  if(machineRange(pre, spec.name))
    return pre;
  const source = machineRange(pre, sourceName);
  if(!source)
    throw new Error(`machine template ${sourceName} not found`);
  const block = renameBlock(pre.slice(source[0], source[1]), sourceName, spec.name);
  setField(block, "value", spec.value);
  setField(block, "type", "parameter");
  setField(block, "vis", "2");
  const insert = pre.findIndex((l) => trim(l) === "</machine>");
  pre.splice(insert, 0, ...block);
  return pre;
}

function pruneContainerEntries(pre, containerStart, containerEnd, keepNames) {
  const keep = new Set(keepNames);
  const found = ranges(pre, containerStart, containerEnd);
  const deletions = [];
  Object.keys(found).forEach((name) => {
    if(!keep.has(name))
      deletions.push(found[name]);
  });
  deletions.sort((a, b) => b[0] - a[0]).forEach(([start, end]) => {
    pre.splice(start, end - start);
  });
  return pre;
}

function addObjectSignalAttrs(obj, sourceName, outputs) {
  outputs.forEach((spec) => {
    if(attrBlock(obj.block, spec.name))
      return;
    const source = attrBlock(obj.block, sourceName);
    if(!source)
      throw new Error(`object output template ${sourceName} not found`);
    const block = renameBlock(source, sourceName, spec.name);
    setField(block, "value", spec.def || "0");
    setField(block, "type", "output");
    setField(block, "resetval", spec.reset || "0");
    setAttrBlock(obj.block, spec.name, block);
  });
}

function setSignalValues(obj, outputs, values, fillDefaults = false) {
  outputs.forEach((spec) => {
    const hasValue = Object.prototype.hasOwnProperty.call(values, spec.name);
    const raw = hasValue ? values[spec.name] : (fillDefaults ? (spec.def || "0") : "");
    const value = raw === null ? "" : raw;
    setAttrValue(obj.block, spec.name, value, value === "" ? "GLOBAL_VAR" : "LOCAL");
  });
}

function makeState(template, spec, outputs) {
  const obj = clone(template);
  setObjName(obj, spec.name);
  setSimple(obj.block, "x0", spec.x);
  setSimple(obj.block, "y0", spec.y);
  setSimple(obj.block, "x1", spec.x + (spec.w || 170));
  setSimple(obj.block, "y1", spec.y + (spec.h || 88));
  setSimple(obj.block, "page", "1");
  setSignalValues(obj, outputs, spec.outputs || {}, true);
  return obj;
}

function makeTransition(template, spec, page = "1") {
  const obj = clone(template);
  setObjName(obj, spec.name);
  setSimple(obj.block, "startState", spec.from);
  setSimple(obj.block, "endState", spec.to);
  setSimple(obj.block, "startStateIndex", "-1");
  setSimple(obj.block, "endStateIndex", "-1");
  setSimple(obj.block, "page", page);
  setAttrValue(obj.block, "equation", spec.eq || "1", spec.eq && spec.eq !== "1" ? "LOCAL" : "GLOBAL_VAR");
  setAttrValue(obj.block, "priority", String(spec.priority || 1), "LOCAL");
  return obj;
}

function makeFzm(spec) {
  const parsed = readObjects(fs.readFileSync(templatePath, "utf8").split(/\r?\n/));
  let pre = parsed.pre.join("\n").replace(/generic_state_action/g, spec.module).split("\n");
  pre = replaceGlobalAttrValue(pre, "clock", spec.clock || "clk");
  pre = replaceGlobalAttrType(pre, "clock", spec.clockType || "posedge");
  pre = replaceGlobalAttrValue(pre, "reset_signal", spec.reset || "rst_l");
  pre = replaceGlobalAttrType(pre, "reset_signal", spec.resetType || "negedge");
  pre = replaceGlobalAttrValue(pre, "reset_state", spec.resetState);
  pre = replaceGlobalAttrValue(pre, "HDL output", `./generated/${spec.module}.v`);

  const desiredInputs = [
    { name: spec.clock || "clk" },
    { name: spec.reset || "rst_l" },
    ...(spec.inputs || []),
  ];
  const inputNamesSeen = new Set();
  const uniqueInputs = desiredInputs.filter((input) => {
    if(inputNamesSeen.has(input.name))
      return false;
    inputNamesSeen.add(input.name);
    return true;
  });

  uniqueInputs.forEach((input) => { pre = addInput(pre, "start", input); });
  (spec.inputs || []).forEach((input) => { pre = addInput(pre, "start", input); });
  (spec.parameters || []).forEach((param) => { pre = addParameter(pre, "implied_loopback", param); });
  (spec.outputs || []).forEach((output) => { pre = addOutput(pre, "rdy", output); });
  pre = pruneContainerEntries(pre, "<inputs>", "</inputs>", uniqueInputs.map((input) => input.name));
  pre = pruneContainerEntries(pre, "<outputs>", "</outputs>", (spec.outputs || []).map((output) => output.name));

  const stateTemplate = clone(parsed.objects.find((o) => o.kind === "state"));
  const transitionTemplate = clone(parsed.objects.find((o) => o.kind === "transition"));
  addObjectSignalAttrs(stateTemplate, "rdy", spec.outputs || []);
  addObjectSignalAttrs(transitionTemplate, "rdy", spec.outputs || []);

  const states = spec.states.map((state) => makeState(stateTemplate, state, spec.outputs || []));
  const transitions = spec.transitions.map((transition) => makeTransition(transitionTemplate, transition));
  return pre.concat(states.flatMap((o) => o.block), transitions.flatMap((o) => o.block), parsed.post).join("\n") + "\n";
}

function grid(names, startX, startY, cols, dx, dy) {
  return names.map((name, index) => ({
    name,
    x: startX + (index % cols) * dx,
    y: startY + Math.floor(index / cols) * dy,
  }));
}

const off = "1'b0";
const on = "1'b1";

const specs = [
  {
    project: "wicker-systemverilog-fsm",
    module: "landing_gear_fsm",
    source: "source/landing-gear-controller/verilog/LandingGear.v",
    license: "BSD-3-Clause",
    clock: "Clock",
    reset: "Clear",
    resetType: "posedge",
    resetState: "TAXI",
    inputs: ["GearIsDown", "GearIsUp", "PlaneOnGround", "TimeUp", "Lever"].map((name) => ({ name })),
    parameters: [
      { name: "YES", value: "1'b1" }, { name: "NO", value: "1'b0" },
      { name: "UP", value: "1'b0" }, { name: "DOWN", value: "1'b1" },
    ],
    outputs: ["RedLED", "GrnLED", "Valve", "Pump", "Timer"].map((name) => ({ name, type: "comb", def: "1'b0" })),
    states: [
      { name: "TAXI", x: 120, y: 100, outputs: { RedLED: off, GrnLED: on, Valve: "DOWN", Pump: off, Timer: on } },
      { name: "TUP", x: 420, y: 70, outputs: { RedLED: off, GrnLED: on, Valve: "UP", Pump: off, Timer: off } },
      { name: "TDN", x: 420, y: 230, outputs: { RedLED: off, GrnLED: on, Valve: "DOWN", Pump: off, Timer: off } },
      { name: "GOUP", x: 720, y: 70, outputs: { RedLED: on, GrnLED: off, Valve: "UP", Pump: on, Timer: off } },
      { name: "GODN", x: 720, y: 390, outputs: { RedLED: on, GrnLED: off, Valve: "DOWN", Pump: on, Timer: off } },
      { name: "FLYUP", x: 1020, y: 70, outputs: { RedLED: off, GrnLED: off, Valve: "UP", Pump: off, Timer: off } },
      { name: "FLYDN", x: 1020, y: 250, outputs: { RedLED: off, GrnLED: on, Valve: "DOWN", Pump: off, Timer: off } },
    ],
    transitions: [
      { name: "taxi_takeoff_up", from: "TAXI", to: "TUP", eq: "(PlaneOnGround == NO) && (Lever == UP)", priority: 1 },
      { name: "taxi_takeoff_down", from: "TAXI", to: "TDN", eq: "(PlaneOnGround == NO) && (Lever == DOWN)", priority: 2 },
      { name: "tup_ground", from: "TUP", to: "TAXI", eq: "PlaneOnGround", priority: 1 },
      { name: "tup_gear_leaves_down", from: "TUP", to: "GOUP", eq: "GearIsDown == NO", priority: 2 },
      { name: "tup_timeout", from: "TUP", to: "FLYDN", eq: "TimeUp == YES", priority: 3 },
      { name: "tup_lever_down", from: "TUP", to: "TDN", eq: "(TimeUp == NO) && (Lever == DOWN)", priority: 4 },
      { name: "tdn_ground", from: "TDN", to: "TAXI", eq: "PlaneOnGround", priority: 1 },
      { name: "tdn_gear_leaves_down", from: "TDN", to: "GOUP", eq: "GearIsDown == NO", priority: 2 },
      { name: "tdn_timeout", from: "TDN", to: "FLYDN", eq: "TimeUp == YES", priority: 3 },
      { name: "tdn_lever_up", from: "TDN", to: "TUP", eq: "(TimeUp == NO) && (Lever == UP)", priority: 4 },
      { name: "goup_done", from: "GOUP", to: "FLYUP", eq: "GearIsUp == YES", priority: 1 },
      { name: "godn_ground_done", from: "GODN", to: "TAXI", eq: "(PlaneOnGround == YES) && (GearIsDown == YES)", priority: 1 },
      { name: "godn_done", from: "GODN", to: "FLYDN", eq: "GearIsDown == YES", priority: 2 },
      { name: "flyup_lever_down", from: "FLYUP", to: "GODN", eq: "Lever == DOWN", priority: 1 },
      { name: "flydn_ground", from: "FLYDN", to: "TAXI", eq: "PlaneOnGround == YES", priority: 1 },
      { name: "flydn_lever_up", from: "FLYDN", to: "GOUP", eq: "Lever == UP", priority: 2 },
    ],
  },
  {
    project: "wicker-systemverilog-fsm",
    module: "ticket_machine_fsm",
    source: "source/ticket-machine/verilog/TicketMachine.v",
    license: "BSD-3-Clause",
    clock: "Clock",
    reset: "Clear",
    resetType: "posedge",
    resetState: "RDY",
    inputs: ["Ten", "Twenty"].map((name) => ({ name })),
    parameters: [{ name: "ON", value: "1'b1" }, { name: "OFF", value: "1'b0" }],
    outputs: ["Ready", "Dispense", "Return", "Bill"].map((name) => ({ name, type: "comb", def: "1'b0" })),
    states: [
      { name: "RDY", x: 140, y: 120, outputs: { Ready: on, Bill: off, Dispense: off, Return: off } },
      { name: "BILL10", x: 420, y: 80, outputs: { Ready: off, Bill: on, Dispense: off, Return: off } },
      { name: "BILL20", x: 700, y: 80, outputs: { Ready: off, Bill: on, Dispense: off, Return: off } },
      { name: "BILL30", x: 980, y: 80, outputs: { Ready: off, Bill: on, Dispense: off, Return: off } },
      { name: "DISP", x: 1260, y: 20, outputs: { Ready: off, Bill: off, Dispense: on, Return: off } },
      { name: "RTN", x: 1260, y: 180, outputs: { Ready: off, Bill: off, Dispense: off, Return: on } },
    ],
    transitions: [
      { name: "ready_ten", from: "RDY", to: "BILL10", eq: "Ten", priority: 1 },
      { name: "ready_twenty", from: "RDY", to: "BILL20", eq: "Twenty", priority: 2 },
      { name: "bill10_ten", from: "BILL10", to: "BILL20", eq: "Ten", priority: 1 },
      { name: "bill10_twenty", from: "BILL10", to: "BILL30", eq: "Twenty", priority: 2 },
      { name: "bill20_ten", from: "BILL20", to: "BILL30", eq: "Ten", priority: 1 },
      { name: "bill20_twenty", from: "BILL20", to: "DISP", eq: "Twenty", priority: 2 },
      { name: "bill30_ten", from: "BILL30", to: "DISP", eq: "Ten", priority: 1 },
      { name: "bill30_twenty", from: "BILL30", to: "RTN", eq: "Twenty", priority: 2 },
      { name: "disp_reset", from: "DISP", to: "RDY", eq: "1", priority: 1 },
      { name: "return_reset", from: "RTN", to: "RDY", eq: "1", priority: 1 },
    ],
  },
  {
    project: "amclain-verilog-uart",
    module: "amclain_uart_tx_fsm",
    source: "source/verilog/uart_tx.v",
    license: "MIT",
    clock: "clock_i",
    reset: "reset_i",
    resetType: "posedge",
    resetState: "STATE_POST_RESET",
    inputs: ["write_i", "write_has_triggered", "two_stop_bits_i", "parity_bit_i", "parity_even_i", "bit_timer_done", "packet_count_done"].map((name) => ({ name })),
    parameters: [{ name: "TOTAL_BITS_TO_SEND", value: "4'd10" }],
    outputs: [{ name: "serial_o", type: "regdp", def: "1'b1", reset: "1'b1" }, { name: "busy_o", type: "comb", def: "1'b1" }],
    states: grid(["STATE_POST_RESET", "STATE_IDLE", "STATE_SEND_PACKET"], 160, 120, 3, 340, 180).map((state) => ({
      ...state,
      outputs: state.name === "STATE_IDLE" ? { serial_o: "1'b1", busy_o: "1'b0" } : { busy_o: "1'b1" },
    })),
    transitions: [
      { name: "post_reset_done", from: "STATE_POST_RESET", to: "STATE_IDLE", eq: "bit_timer_done && packet_count_done", priority: 1 },
      { name: "idle_write", from: "STATE_IDLE", to: "STATE_SEND_PACKET", eq: "write_i && !write_has_triggered", priority: 1 },
      { name: "send_done", from: "STATE_SEND_PACKET", to: "STATE_IDLE", eq: "packet_count_done", priority: 1 },
    ],
  },
  {
    project: "amclain-verilog-uart",
    module: "amclain_uart_rx_fsm",
    source: "source/verilog/uart_rx.v",
    license: "MIT",
    clock: "clock_i",
    reset: "reset_i",
    resetType: "posedge",
    resetState: "STATE_IDLE",
    inputs: ["serial_i", "ack_i", "ack_has_triggered", "clock_divider_valid", "sample_now", "start_bit_invalid", "packet_done", "packet_valid"].map((name) => ({ name })),
    outputs: [{ name: "ready_o", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "data_o[7:0]", type: "regdp", def: "8'h00", reset: "8'h00" }],
    states: grid(["STATE_IDLE", "STATE_RECEIVE_PACKET", "STATE_VALIDATE_PACKET"], 160, 120, 3, 360, 190),
    transitions: [
      { name: "idle_start", from: "STATE_IDLE", to: "STATE_RECEIVE_PACKET", eq: "!serial_i && clock_divider_valid", priority: 1 },
      { name: "rx_bad_start", from: "STATE_RECEIVE_PACKET", to: "STATE_IDLE", eq: "sample_now && start_bit_invalid", priority: 1 },
      { name: "rx_packet_done", from: "STATE_RECEIVE_PACKET", to: "STATE_VALIDATE_PACKET", eq: "sample_now && packet_done", priority: 2 },
      { name: "validate_done", from: "STATE_VALIDATE_PACKET", to: "STATE_IDLE", eq: "1", priority: 1 },
    ],
  },
  {
    project: "timrudy-uart-verilog",
    module: "timrudy_uart8_tx_fsm",
    source: "source/Uart8Transmitter.v",
    license: "GPL-3.0",
    clock: "clk",
    reset: "en",
    resetType: "negedge",
    resetState: "RESET",
    inputs: ["en", "start", "turbo_frames", "bit_index_done"].map((name) => ({ name })),
    outputs: [{ name: "busy", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "done", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "out", type: "regdp", def: "1'b1", reset: "1'b1" }],
    states: grid(["RESET", "IDLE", "START_BIT", "DATA_BITS", "STOP_BIT"], 140, 120, 5, 260, 180).map((state) => ({
      ...state,
      outputs: state.name === "RESET" ? { busy: "1'b0", done: "1'b0", out: "1'b1" } :
        state.name === "START_BIT" ? { busy: "1'b1", done: "1'b0", out: "1'b0" } :
        state.name === "STOP_BIT" ? { done: "1'b1", out: "1'b1" } : {},
    })),
    transitions: [
      { name: "reset_enabled", from: "RESET", to: "IDLE", eq: "en", priority: 1 },
      { name: "idle_start", from: "IDLE", to: "START_BIT", eq: "start", priority: 1 },
      { name: "start_to_data", from: "START_BIT", to: "DATA_BITS", eq: "1", priority: 1 },
      { name: "data_done", from: "DATA_BITS", to: "STOP_BIT", eq: "bit_index_done", priority: 1 },
      { name: "stop_no_start", from: "STOP_BIT", to: "RESET", eq: "!start", priority: 1 },
      { name: "stop_turbo", from: "STOP_BIT", to: "START_BIT", eq: "start && !done && turbo_frames", priority: 2 },
      { name: "stop_extra_done", from: "STOP_BIT", to: "START_BIT", eq: "start && done", priority: 3 },
    ],
  },
  {
    project: "timrudy-uart-verilog",
    module: "timrudy_uart8_rx_fsm",
    source: "source/Uart8Receiver.v",
    license: "GPL-3.0",
    clock: "clk",
    reset: "en",
    resetType: "negedge",
    resetState: "RESET",
    inputs: ["en", "in_sample", "start_low_accepted", "sample_done", "stop_midpoint", "stop_valid", "ready_done", "ready_reset"].map((name) => ({ name })),
    outputs: [{ name: "busy", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "done", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "err", type: "regdp", def: "1'b0", reset: "1'b0" }, { name: "out[7:0]", type: "regdp", def: "8'b0", reset: "8'b0" }],
    states: grid(["RESET", "IDLE", "START_BIT", "DATA_BITS", "STOP_BIT", "READY"], 120, 120, 3, 350, 210).map((state) => ({
      ...state,
      outputs: state.name === "RESET" ? { busy: "1'b0", done: "1'b0", err: "1'b0", "out[7:0]": "8'b0" } : {},
    })),
    transitions: [
      { name: "reset_enabled", from: "RESET", to: "IDLE", eq: "en", priority: 1 },
      { name: "idle_start_ok", from: "IDLE", to: "START_BIT", eq: "!in_sample && start_low_accepted", priority: 1 },
      { name: "start_done", from: "START_BIT", to: "DATA_BITS", eq: "sample_done", priority: 1 },
      { name: "data_done", from: "DATA_BITS", to: "STOP_BIT", eq: "sample_done", priority: 1 },
      { name: "stop_valid_ready", from: "STOP_BIT", to: "READY", eq: "stop_midpoint && stop_valid", priority: 1 },
      { name: "stop_next_start", from: "STOP_BIT", to: "IDLE", eq: "stop_midpoint && !in_sample", priority: 2 },
      { name: "stop_error_idle", from: "STOP_BIT", to: "IDLE", eq: "stop_midpoint && !stop_valid", priority: 3 },
      { name: "ready_idle", from: "READY", to: "IDLE", eq: "ready_done", priority: 1 },
      { name: "ready_reset", from: "READY", to: "RESET", eq: "ready_reset", priority: 2 },
    ],
  },
];

function writeProjectReadme(project, projectSpecs) {
  const dir = path.join(tbRoot, project);
  const urls = {
    "wicker-systemverilog-fsm": "https://github.com/wicker/SystemVerilog-FSM",
    "amclain-verilog-uart": "https://github.com/amclain/verilog-uart",
    "timrudy-uart-verilog": "https://github.com/TimRudy/uart-verilog",
  };
  const lines = [
    `# ${project}`,
    "",
    "This folder contains public reference FSM material used to manually review",
    "Fizzim diagrams against existing HDL examples.",
    "",
    `Source repository: ${urls[project] || "see source path below"}`,
    "",
    "The `source/` checkout is intentionally ignored by Git. Re-clone the public",
    "repository there when running source-vs-generated comparisons locally.",
    "",
    "| Diagram | Source HDL | License | Comparison scope |",
    "| --- | --- | --- | --- |",
  ];
  projectSpecs.forEach((spec) => {
    const scope = spec.project === "wicker-systemverilog-fsm"
      ? "Moore FSM control/output behavior is directly reconstructable."
      : "Control-state graph is reconstructed; datapath counters/shifters remain in the source HDL.";
    lines.push(`| \`${spec.module}.fzm\` | \`${spec.source}\` | ${spec.license} | ${scope} |`);
  });
  lines.push("", "Generated RTL is written under `generated/` by `generate_public_reference_fsms.js` plus the backend flow.");
  fs.writeFileSync(path.join(dir, "README.md"), lines.join("\n") + "\n");
}

function main() {
  const grouped = new Map();
  specs.forEach((spec) => {
    const dir = path.join(tbRoot, spec.project);
    fs.mkdirSync(path.join(dir, "generated"), { recursive: true });
    fs.writeFileSync(path.join(dir, `${spec.module}.fzm`), makeFzm(spec));
    if(!grouped.has(spec.project))
      grouped.set(spec.project, []);
    grouped.get(spec.project).push(spec);
  });
  for(const [project, projectSpecs] of grouped)
    writeProjectReadme(project, projectSpecs);
  fs.writeFileSync(path.join(tbRoot, "public_reference_fsms.json"), JSON.stringify(specs.map((spec) => ({
    project: spec.project,
    module: spec.module,
    source: spec.source,
    license: spec.license,
    states: spec.states.map((s) => s.name),
    transitions: spec.transitions.map((t) => ({ name: t.name, from: t.from, to: t.to, equation: t.eq || "1", priority: t.priority || 1 })),
  })), null, 2) + "\n");
  console.log(`Generated ${specs.length} public reference Fizzim diagrams.`);
}

main();
