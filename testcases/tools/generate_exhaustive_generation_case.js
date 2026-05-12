#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

const repo = path.resolve(__dirname, "..", "..");
const templatePath = path.join(repo, "testcases", "generic_state_machine.fzm");
const outFzm = path.join(repo, "testcases", "exhaustive_generation.fzm");
const outTb = path.join(repo, "testcases", "tb_exhaustive_generation_equiv.sv");

const OBJECTS = new Set(["state", "transition", "fork", "stategroup", "textObj"]);
const trim = (s) => s.trim();

const controls = [
  { name: "in_a", port: "in_a", width: "" },
  { name: "in_b", port: "in_b", width: "" },
  { name: "in_c", port: "in_c", width: "" },
  { name: "in_d", port: "in_d", width: "" },
  { name: "in_e", port: "in_e", width: "" },
  { name: "in_bus[2:0]", port: "in_bus", width: "[2:0]" },
];

const parameters = [
  { name: "P_ZERO", value: "0" },
  { name: "P_ONE", value: "1" },
  { name: "P_TWO", value: "2" },
  { name: "P_LIMIT", value: "4" },
  { name: "P_MASK", value: "4'hA" },
];

const signals = [
  { name: "rdy", port: "rdy", width: "", globalType: "regdp", objectType: "output", def: "0", reset: "0", internal: false },
  { name: "busy", port: "busy", width: "", globalType: "reg", objectType: "output", def: "0", reset: "0", internal: false },
  { name: "color[100:0]", port: "color", width: "[100:0]", globalType: "comb", objectType: "output", def: "101'd0", reset: "0", internal: false },
  { name: "trigger", port: "trigger", width: "", globalType: "regdp", objectType: "output", def: "0", reset: "0", internal: false },
  { name: "trigger1[1:0]", port: "trigger1", width: "[1:0]", globalType: "comb", objectType: "output", def: "2'd0", reset: "0", internal: false },
  { name: "int_reg", port: "int_reg", width: "", globalType: "regdp", objectType: "output", def: "0", reset: "0", internal: true },
  { name: "int_state", port: "int_state", width: "", globalType: "reg", objectType: "output", def: "0", reset: "0", internal: true },
  { name: "int_comb", port: "int_comb", width: "", globalType: "comb", objectType: "output", def: "0", reset: "0", internal: true },
  { name: "int_bus[3:0]", port: "int_bus", width: "[3:0]", globalType: "regdp", objectType: "output", def: "4'd0", reset: "0", internal: true },
  { name: "int_shadow[1:0]", port: "int_shadow", width: "[1:0]", globalType: "reg", objectType: "output", def: "2'd0", reset: "0", internal: true },
];

const portSignals = signals.filter((s) => !s.internal);

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

function simple(block, tag) {
  const open = `<${tag}>`;
  for(let i = 0; i < block.length - 1; i++)
    if(trim(block[i]) === open)
      return trim(block[i + 1]);
  return "";
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
  if(existing) {
    block.splice(existing[0], existing[1] - existing[0], ...attr);
    return;
  }
  const insert = block.findIndex((l) => trim(l) === "</attributes>");
  if(insert >= 0)
    block.splice(insert, 0, ...attr);
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

function renameBlock(attr, oldName, newName) {
  const out = attr.slice();
  out[0] = out[0].replace(`<${oldName}>`, `<${newName}>`);
  out[out.length - 1] = out[out.length - 1].replace(`</${oldName}>`, `</${newName}>`);
  return out;
}

function clone(obj) {
  return { kind: obj.kind, block: obj.block.slice() };
}

function setObjName(obj, name) {
  const attr = attrBlock(obj.block, "name");
  if(attr) {
    setField(attr, "value", name);
    setAttrBlock(obj.block, "name", attr);
  }
}

function setAttrValue(block, name, value, status = "LOCAL") {
  const attr = attrBlock(block, name);
  if(!attr)
    return;
  setField(attr, "value", value);
  setValueStatus(attr, status);
  setAttrBlock(block, name, attr);
}

function addInput(pre, sourceName, spec) {
  const text = pre.join("\n");
  if(text.includes(`<${spec.name}>`))
    return pre;
  const re = new RegExp(`(      <${sourceName}>[\\s\\S]*?      </${sourceName}>\\n)`);
  const match = text.match(re);
  if(!match)
    throw new Error(`could not find input template ${sourceName}`);
  const block = match[1]
    .replace(new RegExp(`<${sourceName}>`, "g"), `<${spec.name}>`)
    .replace(new RegExp(`</${sourceName}>`, "g"), `</${spec.name}>`);
  return text.replace("   </inputs>", block + "   </inputs>").split("\n");
}

function outputRange(pre, name) {
  return ranges(pre, "<outputs>", "</outputs>")[name];
}

function addOrUpdateGlobalOutput(pre, sourceName, spec) {
  let block;
  const existing = outputRange(pre, spec.name);
  if(existing)
    block = pre.slice(existing[0], existing[1]);
  else {
    const source = outputRange(pre, sourceName);
    if(!source)
      throw new Error(`could not find output template ${sourceName}`);
    block = renameBlock(pre.slice(source[0], source[1]), sourceName, spec.name);
  }
  setField(block, "value", spec.def);
  setField(block, "type", spec.globalType);
  setField(block, "resetval", spec.reset);
  setField(block, "vis", "2");
  setField(block, "useratts", spec.internal ? "suppress_portlist" : "");
  if(existing)
    pre.splice(existing[0], existing[1] - existing[0], ...block);
  else {
    const insert = pre.findIndex((l) => trim(l) === "</outputs>");
    pre.splice(insert, 0, ...block);
  }
}

function addMachineParameter(pre, sourceName, param) {
  const text = pre.join("\n");
  if(text.includes(`<${param.name}>`))
    return pre;
  const re = new RegExp(`(      <${sourceName}>[\\s\\S]*?      </${sourceName}>\\n)`);
  const match = text.match(re);
  if(!match)
    throw new Error(`could not find machine template ${sourceName}`);
  const block = match[1]
    .replace(new RegExp(`<${sourceName}>`, "g"), `<${param.name}>`)
    .replace(new RegExp(`</${sourceName}>`, "g"), `</${param.name}>`)
    .split("\n");
  setField(block, "value", param.value);
  setField(block, "type", "parameter");
  setField(block, "vis", "2");
  return text.replace("   </machine>", block.join("\n") + "   </machine>").split("\n");
}

function addObjectSignalAttrs(obj, sourceName, specs) {
  specs.forEach((spec) => {
    if(attrBlock(obj.block, spec.name))
      return;
    const source = attrBlock(obj.block, sourceName);
    if(!source)
      throw new Error(`could not find object signal template ${sourceName}`);
    const block = renameBlock(source, sourceName, spec.name);
    setField(block, "value", spec.def);
    setField(block, "type", "output");
    setField(block, "resetval", spec.reset);
    setAttrBlock(obj.block, spec.name, block);
  });
}

function prepareTemplates(pre, objects) {
  let nextPre = pre.join("\n").replace(/generic_state_action/g, "exhaustive_generation").split("\n");
  nextPre = replaceGlobalAttrValue(nextPre, "reset_state", "S_RESET");
  controls.forEach((spec) => { nextPre = addInput(nextPre, "start", spec); });
  parameters.forEach((param) => { nextPre = addMachineParameter(nextPre, "implied_loopback", param); });
  signals.forEach((spec) => addOrUpdateGlobalOutput(nextPre, "rdy", spec));

  const stateTemplate = clone(objects.find((o) => o.kind === "state"));
  const transitionTemplate = clone(objects.find((o) => o.kind === "transition"));
  const forkTemplate = clone(objects.find((o) => o.kind === "fork"));
  const groupTemplate = clone(objects.find((o) => o.kind === "stategroup"));
  [stateTemplate, transitionTemplate, groupTemplate].forEach((obj) => addObjectSignalAttrs(obj, "rdy", signals));
  return { pre: nextPre, stateTemplate, transitionTemplate, forkTemplate, groupTemplate };
}

function replaceGlobalAttrValue(pre, attrName, value) {
  const text = pre.join("\n");
  const re = new RegExp(`(<${attrName}>[\\s\\S]*?<value>\\s*\\n\\s*)[\\s\\S]*?(\\n\\s*<status>)`);
  return text.replace(re, `$1${value}$2`).split("\n");
}

function setSignalValues(obj, values, fillDefaults = false) {
  signals.forEach((spec) => {
    const hasValue = Object.prototype.hasOwnProperty.call(values, spec.name);
    const raw = hasValue ? values[spec.name] : (fillDefaults ? spec.def : "");
    const value = raw === null ? "" : raw;
    setAttrValue(obj.block, spec.name, value, value === "" ? "GLOBAL_VAR" : "LOCAL");
  });
}

function makeState(template, name, x, y, values = {}) {
  const obj = clone(template);
  setObjName(obj, name);
  setSimple(obj.block, "x0", x);
  setSimple(obj.block, "y0", y);
  setSimple(obj.block, "x1", x + 170);
  setSimple(obj.block, "y1", y + 82);
  setSimple(obj.block, "page", "1");
  setSignalValues(obj, values, true);
  return obj;
}

function makeFork(template, name, x, y) {
  const obj = clone(template);
  setSimple(obj.block, "name", name);
  setSimple(obj.block, "x0", x);
  setSimple(obj.block, "y0", y);
  setSimple(obj.block, "x1", x + 24);
  setSimple(obj.block, "y1", y + 24);
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
  setSimple(obj.block, "page", "1");
  setSignalValues(obj, values, true);
  const start = obj.block.findIndex((l) => trim(l) === "<children>");
  const end = obj.block.findIndex((l) => trim(l) === "</children>");
  if(start >= 0 && end > start) {
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
  setAttrValue(obj.block, "priority", String(priority + 1), "LOCAL");
  setSignalValues(obj, actions);
  return obj;
}

function stateValues(index, extra = {}) {
  return {
    busy: index % 2 ? "1" : "0",
    "color[100:0]": `(in_bus + ${index})`,
    "trigger1[1:0]": `(in_bus[1:0] ^ ${index % 4})`,
    int_state: index % 2 ? "1" : "0",
    int_comb: `(in_a ^ in_b ^ ${index % 2})`,
    "int_shadow[1:0]": `2'd${index % 4}`,
    ...extra,
  };
}

function gridStates(template) {
  const names = [
    "S_RESET", "S_OUT0", "S_OUT1", "S_OUT2", "S_OUT3", "S_OUT4",
    "S_DONE", "S_ERR", "S_A0", "S_A1", "S_A2", "S_B0", "S_B1",
    "S_B2", "S_C0", "S_C1", "S_C2", "S_D0", "S_D1", "S_D2",
  ];
  const states = [];
  names.forEach((name, i) => {
    const col = i % 5;
    const row = Math.floor(i / 5);
    states.push(makeState(template, name, 160 + col * 270, 120 + row * 210,
      stateValues(i, name === "S_DONE" ? { rdy: "P_ONE" } : {})));
  });
  return states;
}

function buildObjects(templates) {
  const { stateTemplate, transitionTemplate, forkTemplate, groupTemplate } = templates;
  const states = gridStates(stateTemplate);
  const forks = [
    makeFork(forkTemplate, "F_ENTRY0", 95, 210),
    makeFork(forkTemplate, "F_ENTRY1", 95, 390),
    makeFork(forkTemplate, "F_SHARED0", 1260, 210),
    makeFork(forkTemplate, "F_SHARED1", 1260, 390),
    makeFork(forkTemplate, "F_OUT0", 230, 990),
    makeFork(forkTemplate, "F_OUT1", 500, 990),
    makeFork(forkTemplate, "F_OUT2", 770, 990),
    makeFork(forkTemplate, "F_OUT3", 1040, 990),
    makeFork(forkTemplate, "F_GA0", 520, 1470),
    makeFork(forkTemplate, "F_GA1", 720, 1470),
    makeFork(forkTemplate, "F_GB0", 520, 1800),
    makeFork(forkTemplate, "F_GB1", 720, 1800),
    makeFork(forkTemplate, "F_GC0", 520, 2130),
    makeFork(forkTemplate, "F_GC1", 720, 2130),
    makeFork(forkTemplate, "F_GD0", 520, 2460),
    makeFork(forkTemplate, "F_GD1", 720, 2460),
  ];
  const groups = [
    makeGroup(groupTemplate, "SG_A", 420, 1350, 990, 1615, "S_A0", ["S_A0", "S_A1", "S_A2"],
      { busy: "1", "color[100:0]": "P_LIMIT", int_state: "1", int_comb: "in_c" }),
    makeGroup(groupTemplate, "SG_B", 420, 1680, 990, 1945, "S_B0", ["S_B0", "S_B1", "S_B2"],
      { busy: "1", "color[100:0]": "P_MASK", int_state: "1", int_comb: "in_d" }),
    makeGroup(groupTemplate, "SG_C", 420, 2010, 990, 2275, "S_C0", ["S_C0", "S_C1", "S_C2"],
      { busy: "0", "color[100:0]": "P_TWO", int_state: "0", int_comb: "in_e" }),
    makeGroup(groupTemplate, "SG_D", 420, 2340, 990, 2605, "S_D0", ["S_D0", "S_D1", "S_D2"],
      { busy: "1", "color[100:0]": "P_ONE", int_state: "1", int_comb: "in_bus[0]" }),
  ];

  const transitions = [];
  const transitionSpecs = [];
  const add = (...args) => {
    const [name, start, end, equation, priority, actions] = args;
    transitionSpecs.push({ name, start, end, equation, priority: priority + 1, actions: actions || {} });
    transitions.push(makeTransition(transitionTemplate, ...args));
  };
  const pulse = (v) => ({ trigger: "P_ONE", int_reg: "P_ONE", "int_bus[3:0]": v });

  add("T_RESET_ENTRY", "S_RESET", "F_ENTRY0", "in_a || in_b || in_c", 0, pulse("4'd1"));
  add("T_RESET_OUT0", "S_RESET", "S_OUT0", "1", 9, {});
  add("T_ENTRY0_SGA", "F_ENTRY0", "SG_A", "in_a && (in_bus < P_LIMIT)", 0, pulse("4'd2"));
  add("T_ENTRY0_ENTRY1", "F_ENTRY0", "F_ENTRY1", "in_b || int_comb", 1, { int_reg: "in_b" });
  add("T_ENTRY0_SHARED0", "F_ENTRY0", "F_SHARED0", "in_c && !in_d", 2, { trigger: "in_c" });
  add("T_ENTRY0_ERR", "F_ENTRY0", "S_ERR", "1", 3, { "int_bus[3:0]": "P_MASK" });
  add("T_ENTRY1_SGB", "F_ENTRY1", "SG_B", "in_d", 0, pulse("4'd3"));
  add("T_ENTRY1_OUT1", "F_ENTRY1", "S_OUT1", "in_e", 1, { trigger: "in_e" });
  add("T_ENTRY1_SHARED0", "F_ENTRY1", "F_SHARED0", "1", 2, {});
  add("T_SHARED0_SHARED1", "F_SHARED0", "F_SHARED1", "in_bus[0]", 0, { int_reg: "in_bus[0]" });
  add("T_SHARED0_SGC", "F_SHARED0", "SG_C", "in_bus[1]", 1, pulse("4'd4"));
  add("T_SHARED0_OUT2", "F_SHARED0", "S_OUT2", "1", 2, {});
  add("T_SHARED1_DONE", "F_SHARED1", "S_DONE", "in_a && in_e", 0, { rdy: "P_ONE" });
  add("T_SHARED1_SGD", "F_SHARED1", "SG_D", "in_b && (in_bus != P_ZERO)", 1, pulse("4'd5"));
  add("T_SHARED1_ERR", "F_SHARED1", "S_ERR", "in_c", 2, {});
  add("T_SHARED1_OUT3", "F_SHARED1", "S_OUT3", "1", 3, {});

  add("T_OUT0_F0", "S_OUT0", "F_OUT0", "in_a || int_reg", 0, pulse("4'd6"));
  add("T_OUT0_OUT4", "S_OUT0", "S_OUT4", "1", 5, {});
  add("T_FOUT0_SGA", "F_OUT0", "SG_A", "in_b", 0, {});
  add("T_FOUT0_OUT1", "F_OUT0", "S_OUT1", "in_c", 1, { trigger: "in_c" });
  add("T_FOUT0_ENTRY1", "F_OUT0", "F_ENTRY1", "in_d", 2, {});
  add("T_FOUT0_ERR", "F_OUT0", "S_ERR", "1", 3, {});
  add("T_OUT1_F1", "S_OUT1", "F_OUT1", "in_b || (int_bus == P_MASK)", 0, { int_reg: "in_b" });
  add("T_OUT1_OUT2", "S_OUT1", "S_OUT2", "1", 6, {});
  add("T_FOUT1_SGB", "F_OUT1", "SG_B", "in_a", 0, pulse("4'd7"));
  add("T_FOUT1_SHARED1", "F_OUT1", "F_SHARED1", "in_e", 1, {});
  add("T_FOUT1_DONE", "F_OUT1", "S_DONE", "1", 2, { rdy: "P_ONE" });
  add("T_OUT2_F2", "S_OUT2", "F_OUT2", "in_c || in_d", 0, {});
  add("T_OUT2_OUT3", "S_OUT2", "S_OUT3", "1", 7, {});
  add("T_FOUT2_SGC", "F_OUT2", "SG_C", "in_bus[2]", 0, pulse("4'd8"));
  add("T_FOUT2_OUT4", "F_OUT2", "S_OUT4", "in_a", 1, {});
  add("T_FOUT2_ERR", "F_OUT2", "S_ERR", "1", 2, {});
  add("T_OUT3_F3", "S_OUT3", "F_OUT3", "in_d || in_e", 0, {});
  add("T_OUT3_DONE", "S_OUT3", "S_DONE", "1", 8, { rdy: "P_ONE" });
  add("T_FOUT3_SGD", "F_OUT3", "SG_D", "in_b", 0, pulse("4'd9"));
  add("T_FOUT3_SHARED0", "F_OUT3", "F_SHARED0", "in_c", 1, {});
  add("T_FOUT3_OUT0", "F_OUT3", "S_OUT0", "1", 2, {});
  add("T_OUT4_RESET", "S_OUT4", "S_RESET", "in_a && in_b", 0, {});
  add("T_OUT4_ENTRY0", "S_OUT4", "F_ENTRY0", "1", 9, {});
  add("T_DONE_RESET", "S_DONE", "S_RESET", "1", 0, { rdy: "P_ZERO" });
  add("T_ERR_RESET", "S_ERR", "S_RESET", "in_a || in_e", 0, {});
  add("T_ERR_OUT0", "S_ERR", "S_OUT0", "1", 9, {});

  [
    ["A", "SG_A", "S_A0", "S_A1", "S_A2", "F_GA0", "F_GA1", "SG_B"],
    ["B", "SG_B", "S_B0", "S_B1", "S_B2", "F_GB0", "F_GB1", "SG_C"],
    ["C", "SG_C", "S_C0", "S_C1", "S_C2", "F_GC0", "F_GC1", "SG_D"],
    ["D", "SG_D", "S_D0", "S_D1", "S_D2", "F_GD0", "F_GD1", "S_DONE"],
  ].forEach(([tag, group, s0, s1, s2, f0, f1, next], idx) => {
    add(`T_${tag}_GROUP_EXIT`, group, idx % 2 ? "F_SHARED1" : "F_SHARED0", `in_e && (in_bus != P_ZERO)`, 0, pulse(`4'd${10 + idx}`));
    add(`T_${tag}_0_F0`, s0, f0, `in_a || int_comb`, 5, { int_reg: "in_a", "int_bus[3:0]": `4'd${idx}` });
    add(`T_${tag}_0_1`, s0, s1, "1", 9, {});
    add(`T_${tag}_F0_1`, f0, s1, "in_b", 0, { trigger: "in_b" });
    add(`T_${tag}_F0_F1`, f0, f1, "in_c", 1, { int_reg: "in_c" });
    add(`T_${tag}_F0_OUT`, f0, "S_OUT2", "in_d", 2, {});
    add(`T_${tag}_F0_2`, f0, s2, "1", 3, {});
    add(`T_${tag}_F1_2`, f1, s2, "in_bus[0]", 0, { "int_bus[3:0]": "P_MASK" });
    add(`T_${tag}_F1_NEXT`, f1, next, "1", 1, pulse(`4'd${(14 + idx) % 16}`));
    add(`T_${tag}_1_2`, s1, s2, "in_b && !in_c", 5, {});
    add(`T_${tag}_2_NEXT`, s2, next, "in_d || in_e", 5, pulse(`4'd${(18 + idx) % 16}`));
    add(`T_${tag}_2_RESET`, s2, "S_RESET", "1", 9, {});
  });

  return { states, groups, forks, transitions, transitionSpecs };
}

function transitionPairMask(transitionSpecs) {
  const stateNames = exhaustiveStateNames();
  const stateSet = new Set(stateNames);
  const forkSet = new Set([
    "F_ENTRY0", "F_ENTRY1", "F_SHARED0", "F_SHARED1", "F_OUT0", "F_OUT1", "F_OUT2", "F_OUT3",
    "F_GA0", "F_GA1", "F_GB0", "F_GB1", "F_GC0", "F_GC1", "F_GD0", "F_GD1",
  ]);
  const groups = {
    SG_A: { entry: "S_A0", children: ["S_A0", "S_A1", "S_A2"] },
    SG_B: { entry: "S_B0", children: ["S_B0", "S_B1", "S_B2"] },
    SG_C: { entry: "S_C0", children: ["S_C0", "S_C1", "S_C2"] },
    SG_D: { entry: "S_D0", children: ["S_D0", "S_D1", "S_D2"] },
  };
  const grouped = [];
  transitionSpecs.forEach((t) => {
    const starts = groups[t.start] ? groups[t.start].children : [t.start];
    const end = groups[t.end] ? groups[t.end].entry : t.end;
    starts.forEach((start) => grouped.push({ ...t, start, end }));
  });
  const outgoing = new Map();
  grouped.forEach((t) => {
    if(!forkSet.has(t.start))
      return;
    if(!outgoing.has(t.start))
      outgoing.set(t.start, []);
    outgoing.get(t.start).push(t);
  });
  for(const list of outgoing.values())
    list.sort((a, b) => a.priority - b.priority || a.name.localeCompare(b.name));
  const pairs = new Set();
  function trace(start, end, visited = new Set()) {
    if(stateSet.has(end)) {
      pairs.add(`${start}\u0000${end}`);
      return;
    }
    if(!forkSet.has(end) || visited.has(end))
      return;
    const nextVisited = new Set(visited);
    nextVisited.add(end);
    (outgoing.get(end) || []).forEach((t) => trace(start, groups[t.end] ? groups[t.end].entry : t.end, nextVisited));
  }
  grouped.forEach((t) => {
    if(forkSet.has(t.start))
      return;
    trace(t.start, t.end);
  });
  let mask = 0n;
  pairs.forEach((pair) => {
    const [from, to] = pair.split("\u0000");
    const fromIndex = stateNames.indexOf(from);
    const toIndex = stateNames.indexOf(to);
    if(fromIndex >= 0 && toIndex >= 0)
      mask |= 1n << BigInt(fromIndex * stateNames.length + toIndex);
  });
  return { mask, count: pairs.size };
}

function exhaustiveStateNames() {
  return [
    "S_RESET", "S_OUT0", "S_OUT1", "S_OUT2", "S_OUT3", "S_OUT4",
    "S_DONE", "S_ERR", "S_A0", "S_A1", "S_A2", "S_B0", "S_B1",
    "S_B2", "S_C0", "S_C1", "S_C2", "S_D0", "S_D1", "S_D2",
  ];
}

function writeTestbench(transitionSpecs) {
  const outputDecls = portSignals.map((s) => `  wire ${s.width ? `${s.width} ` : ""}ref_${s.port};\n  wire ${s.width ? `${s.width} ` : ""}feat_${s.port};`).join("\n");
  const outputConcat = `{${portSignals.map((s) => `ref_${s.port}`).join(", ")}} !==\n          {${portSignals.map((s) => `feat_${s.port}`).join(", ")}}`;
  const outputDisplays = portSignals.map((s) => `%0h`).join(" ");
  const outputArgsRef = portSignals.map((s) => `ref_${s.port}`).join(", ");
  const outputArgsFeat = portSignals.map((s) => `feat_${s.port}`).join(", ");
  const fixedLegacyInputs = ["    .break_req(1'b0)", "    .option1(1'b0)", "    .start(1'b0)"];
  const portsRef = portSignals.map((s) => `    .${s.port}(ref_${s.port})`).concat(fixedLegacyInputs, controls.map((c) => `    .${c.port}(${c.port})`), ["    .clk(clk)", "    .rst_l(rst_l)"]).join(",\n");
  const portsFeat = portSignals.map((s) => `    .${s.port}(feat_${s.port})`).concat(fixedLegacyInputs, controls.map((c) => `    .${c.port}(${c.port})`), ["    .clk(clk)", "    .rst_l(rst_l)"]).join(",\n");
  const inputDecls = controls.map((c) => `  reg ${c.width ? `${c.width} ` : ""}${c.port};`).join("\n");
  const clearInputs = controls.map((c) => `      ${c.port} = '0;`).join("\n");
  const comboAssigns = [
    "      in_a = combo[0];",
    "      in_b = combo[1];",
    "      in_c = combo[2];",
    "      in_d = combo[3];",
    "      in_e = combo[4];",
    "      in_bus = combo[7:5];",
  ].join("\n");
  const stateNames = exhaustiveStateNames();
  const stateCount = stateNames.length;
  const coverage = transitionPairMask(transitionSpecs);
  const expectedHexDigits = Math.ceil((stateCount * stateCount) / 4);
  const expectedHex = coverage.mask.toString(16).padStart(expectedHexDigits, "0");
  const stateIndexCases = stateNames.map((name, i) => `        u_feat.${name}: state_index = ${i};`).join("\n");
  const forceCalls = stateNames.map((name, i) => `    check_all_inputs_from_state(${i}, u_ref.${name}, u_feat.${name}, "${name}");`).join("\n");
  fs.writeFileSync(outTb, `module tb_exhaustive_generation_equiv;
  reg clk;
  reg rst_l;
${inputDecls}
${outputDecls}

  localparam integer STATE_COUNT = ${stateCount};
  localparam integer PAIR_COUNT = ${stateCount * stateCount};
  localparam integer EXPECTED_PAIR_COUNT = ${coverage.count};
  localparam integer MIN_EXPECTED_PAIR_COUNT = (EXPECTED_PAIR_COUNT * 90 + 99) / 100;
  localparam [PAIR_COUNT-1:0] EXPECTED_PAIRS = ${stateCount * stateCount}'h${expectedHex};
  reg [PAIR_COUNT-1:0] observed_pairs;

  exhaustive_generation_fizzim u_ref (
${portsRef}
  );

  exhaustive_generation u_feat (
${portsFeat}
  );

  initial clk = 1'b0;
  always #5 clk = ~clk;

  task clear_inputs;
    begin
${clearInputs}
    end
  endtask

  task drive_combo(input [7:0] combo);
    begin
${comboAssigns}
    end
  endtask

  task check_match(input [255:0] label);
    begin
      #1;
      if (u_ref.state !== u_feat.state) begin
        $display("STATE MISMATCH %0s ref=%0d feat=%0d", label, u_ref.state, u_feat.state);
        $fatal(1);
      end
      if (${outputConcat}) begin
        $display("OUTPUT MISMATCH %0s", label);
        $display("ref  ${outputDisplays}", ${outputArgsRef});
        $display("feat ${outputDisplays}", ${outputArgsFeat});
        $fatal(1);
      end
    end
  endtask

  function integer state_index(input [31:0] state_value);
    begin
      case (state_value)
${stateIndexCases}
        default: state_index = -1;
      endcase
    end
  endfunction

  task mark_pair(input integer from_index);
    integer to_index;
    begin
      to_index = state_index(u_feat.state);
      if (from_index >= 0 && to_index >= 0)
        observed_pairs[from_index * STATE_COUNT + to_index] = 1'b1;
    end
  endtask

  task force_state_and_tick(input integer state_idx, input [31:0] ref_forced_state, input [31:0] feat_forced_state, input [7:0] combo, input [255:0] label);
    begin
      @(negedge clk);
      u_ref.state = ref_forced_state;
      u_feat.state = feat_forced_state;
      drive_combo(combo);
      @(posedge clk);
      check_match(label);
      mark_pair(state_idx);
      clear_inputs();
    end
  endtask

  task check_all_inputs_from_state(input integer state_idx, input [31:0] ref_forced_state, input [31:0] feat_forced_state, input [255:0] label);
    integer combo;
    begin
      for (combo = 0; combo < 256; combo = combo + 1)
        force_state_and_tick(state_idx, ref_forced_state, feat_forced_state, combo[7:0], label);
    end
  endtask

  function integer count_bits(input [PAIR_COUNT-1:0] bits);
    integer i;
    begin
      count_bits = 0;
      for (i = 0; i < PAIR_COUNT; i = i + 1)
        if (bits[i])
          count_bits = count_bits + 1;
    end
  endfunction

  initial begin
    observed_pairs = '0;
    rst_l = 1'b0;
    clear_inputs();
    repeat (3) @(posedge clk);
    rst_l = 1'b1;
    @(posedge clk);
    check_match("reset");
${forceCalls}
    if (count_bits(observed_pairs & EXPECTED_PAIRS) < MIN_EXPECTED_PAIR_COUNT) begin
      $display("TRANSITION PAIR COVERAGE FAILURE observed=%0d expected=%0d minimum=%0d",
               count_bits(observed_pairs & EXPECTED_PAIRS), EXPECTED_PAIR_COUNT, MIN_EXPECTED_PAIR_COUNT);
      $display("missing_pairs_mask=%0h", EXPECTED_PAIRS & ~observed_pairs);
      $fatal(1);
    end
    $display("Observed %0d/%0d expected transition source/destination pairs; minimum required=%0d",
             count_bits(observed_pairs & EXPECTED_PAIRS), EXPECTED_PAIR_COUNT, MIN_EXPECTED_PAIR_COUNT);
    if ((observed_pairs & EXPECTED_PAIRS) !== EXPECTED_PAIRS)
      $display("Uncovered expected pair mask=%0h", EXPECTED_PAIRS & ~observed_pairs);
    $display("PASS exhaustive generation Fizzim 2.0 vs Fizzim 1.0-compatible equivalence test");
    $finish;
  end
endmodule
`);
}

function main() {
  const lines = fs.readFileSync(templatePath, "utf8").split(/\r?\n/);
  const parsed = readObjects(lines);
  const templates = prepareTemplates(parsed.pre, parsed.objects);
  const { states, groups, forks, transitions, transitionSpecs } = buildObjects(templates);
  const allObjects = [...states, ...groups, ...forks, ...transitions];
  fs.writeFileSync(outFzm, templates.pre.concat(allObjects.flatMap((o) => o.block), parsed.post).join("\n") + "\n");
  writeTestbench(transitionSpecs);
  console.log(`Generated ${path.relative(repo, outFzm)} with ${states.length} states, ${groups.length} state groups, ${forks.length} forks, and ${transitions.length} transitions.`);
}

main();
