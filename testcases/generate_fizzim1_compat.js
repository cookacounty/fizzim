#!/usr/bin/env node
const fs = require("fs");
const path = require("path");

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
    pre.push(lines[i++]);
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
  if (ranges[name]) {
    block.splice(ranges[name][0], ranges[name][1] - ranges[name][0], ...attr);
    return;
  }
  const insert = block.findIndex((l) => trim(l) === "</attributes>");
  if (insert >= 0) block.splice(insert, 0, ...attr);
}

function removeAttrBlock(block, name) {
  const r = attrRanges(block)[name];
  if (r) block.splice(r[0], r[1] - r[0]);
}

function attrValue(attr) {
  if (!attr) return "";
  for (let i = 0; i < attr.length - 1; i++) if (trim(attr[i]) === "<value>") return trim(attr[i + 1]);
  return "";
}

function setAttrValue(attr, value) {
  for (let i = 0; i < attr.length - 1; i++) {
    if (trim(attr[i]) === "<value>") {
      attr[i + 1] = attr[i + 1].replace(/\S.*$/, String(value));
      return;
    }
  }
}

function attrType(attr) {
  if (!attr) return "";
  for (let i = 0; i < attr.length - 1; i++) if (trim(attr[i]) === "<type>") return trim(attr[i + 1]);
  return "";
}

function valueStatus(attr) {
  let inValue = false;
  for (let i = 0; i < attr.length - 1; i++) {
    const t = trim(attr[i]);
    if (t === "<value>") inValue = true;
    else if (t === "</value>") inValue = false;
    else if (inValue && t === "<status>") return trim(attr[i + 1]);
  }
  return "";
}

function setValueStatus(attr, status) {
  let inValue = false;
  for (let i = 0; i < attr.length - 1; i++) {
    const t = trim(attr[i]);
    if (t === "<value>") inValue = true;
    else if (t === "</value>") inValue = false;
    else if (inValue && t === "<status>") {
      attr[i + 1] = attr[i + 1].replace(/\S.*$/, status);
      return;
    }
  }
}

function objName(obj) {
  if (["state", "transition", "stategroup"].includes(obj.kind)) return attrValue(attrBlock(obj.block, "name"));
  if (obj.kind === "fork") return simple(obj.block, "name");
  return "";
}

function setObjName(obj, name) {
  const a = attrBlock(obj.block, "name");
  if (a) {
    setAttrValue(a, name);
    setAttrBlock(obj.block, "name", a);
  }
}

function tinfo(obj) {
  return {
    name: objName(obj),
    start: simple(obj.block, "startState"),
    end: simple(obj.block, "endState"),
    equation: attrValue(attrBlock(obj.block, "equation")),
    priority: attrValue(attrBlock(obj.block, "priority")),
  };
}

function setTransition(obj, field, value) {
  if (field === "name") setObjName(obj, value);
  else if (field === "equation" || field === "priority") {
    const a = attrBlock(obj.block, field);
    if (a) {
      setAttrValue(a, value);
      setAttrBlock(obj.block, field, a);
    }
  } else {
    setSimple(obj.block, field, value);
  }
}

function clone(obj) {
  return { ...obj, block: obj.block.slice() };
}

function autoRoute(obj, states) {
  const start = simple(obj.block, "startState");
  const page = states[start] ? simple(states[start].block, "page") || "1" : "1";
  for (const tag of ["startPtX", "startPtY", "endPtX", "endPtY", "startCtrlPtX", "startCtrlPtY", "endCtrlPtX", "endCtrlPtY", "pageSX", "pageSY", "pageSCX", "pageSCY", "pageEX", "pageEY", "pageECX", "pageECY"]) {
    setSimple(obj.block, tag, "0.0");
  }
  setSimple(obj.block, "startStateIndex", "-1");
  setSimple(obj.block, "endStateIndex", "-1");
  setSimple(obj.block, "page", page);
}

function combineEq(a, b) {
  a = (a || "").trim();
  b = (b || "").trim();
  if (a && b) return `(${a}) && (${b})`;
  return a || b;
}

function combinePriority(inPri, outPri, index, count) {
  inPri = (inPri || "").trim();
  outPri = (outPri || "").trim();
  if (inPri) {
    if (count <= 1) return inPri;
    const match = inPri.match(/^(-?\d+)(?:\.(\d+))?$/);
    if (!match) return inPri;
    return `${match[1]}.${match[2] || ""}${String(index + 1).padStart(3, "0")}`;
  }
  return outPri;
}

function sortKey(info) {
  const pri = Number(info.priority || 0);
  return [Number.isFinite(pri) ? pri : 0, (info.equation || "").trim() === "1" ? 1 : 0, info.name];
}

function compareInfo(a, b, objA = null, objB = null) {
  const groupBiasA = objA && objA.groupExit ? 0 : 1;
  const groupBiasB = objB && objB.groupExit ? 0 : 1;
  if (groupBiasA !== groupBiasB) return groupBiasA - groupBiasB;
  const ka = sortKey(a), kb = sortKey(b);
  return ka[0] - kb[0] || ka[1] - kb[1] || ka[2].localeCompare(kb[2]);
}

function overlayAttrs(base, overlay) {
  const result = clone(base);
  const ranges = attrRanges(overlay.block);
  for (const [name, r] of Object.entries(ranges)) {
    const incoming = attrBlock(result.block, name);
    const outgoing = overlay.block.slice(r[0], r[1]);
    if (incoming && attrType(outgoing) === "output" && attrValue(incoming) !== "" && attrValue(outgoing) === "") {
      continue;
    }
    setAttrBlock(result.block, name, outgoing);
  }
  return result;
}

function mergeFork(inObj, outObj, index, count, states) {
  const merged = overlayAttrs(inObj, outObj);
  const i = tinfo(inObj), o = tinfo(outObj);
  setTransition(merged, "name", `${i.name}__${o.name}`);
  setTransition(merged, "startState", i.start);
  setTransition(merged, "endState", o.end);
  setTransition(merged, "equation", combineEq(i.equation, o.equation));
  setTransition(merged, "priority", combinePriority(i.priority, o.priority, index, count));
  autoRoute(merged, states);
  return merged;
}

function expandForkRoutes(pathObj, fork, outgoing, forkNames, states, visited = new Set()) {
  const routes = [];
  if (visited.has(fork)) return routes;
  const branches = (outgoing.get(fork) || []).slice().sort((a, b) => compareInfo(tinfo(a), tinfo(b), a, b));
  if (!branches.length) return routes;
  const nextVisited = new Set(visited);
  nextVisited.add(fork);
  branches.forEach((tout, idx) => {
    const merged = mergeFork(pathObj, tout, idx, branches.length, states);
    const end = tinfo(merged).end;
    if (forkNames.has(end)) {
      routes.push(...expandForkRoutes(merged, end, outgoing, forkNames, states, nextVisited));
    } else {
      routes.push(merged);
    }
  });
  return routes;
}

function expandForks(transitions, forkNames, states) {
  const outgoing = new Map();
  for (const transition of transitions) {
    const start = tinfo(transition).start;
    if (!forkNames.has(start)) continue;
    if (!outgoing.has(start)) outgoing.set(start, []);
    outgoing.get(start).push(transition);
  }

  const expanded = [];
  for (const transition of transitions) {
    const info = tinfo(transition);
    if (forkNames.has(info.start)) continue;
    if (forkNames.has(info.end)) {
      expanded.push(...expandForkRoutes(transition, info.end, outgoing, forkNames, states));
    } else {
      expanded.push(transition);
    }
  }
  return expanded;
}

function expandGroups(transitions, groups, states) {
  const out = [];
  for (const t of transitions) {
    const info = tinfo(t);
    const starts = groups[info.start] ? groups[info.start].children : [info.start];
    const end = groups[info.end] ? (groups[info.end].entry || groups[info.end].children[0] || "") : info.end;
    for (const start of starts) {
      const c = clone(t);
      setTransition(c, "name", starts.length > 1 ? `${info.name}__${start}` : info.name);
      setTransition(c, "startState", start);
      setTransition(c, "endState", end);
      if (groups[info.start]) c.groupExit = true;
      autoRoute(c, states);
      out.push(c);
    }
  }
  return out;
}

function normalizePriorities(transitions) {
  const bySource = new Map();
  for (const t of transitions) {
    const source = tinfo(t).start;
    if (!bySource.has(source)) bySource.set(source, []);
    bySource.get(source).push(t);
  }

  const kept = new Set();
  for (const list of bySource.values()) {
    list.sort((a, b) => compareInfo(tinfo(a), tinfo(b), a, b));
    const reachable = [];
    for (const t of list) {
      reachable.push(t);
      if ((tinfo(t).equation || "").trim() === "1") break;
    }
    for (const t of reachable) kept.add(t);
    list.length = 0;
    list.push(...reachable);
    for (let i = 0; i < list.length; i++) {
      const priority = list.length <= 1 ? 1000 : Math.round(1 + (i * 999) / (list.length - 1));
      setTransition(list[i], "priority", String(priority));
    }
  }
  return transitions.filter((t) => kept.has(t));
}

function inheritGroupAttrs(states, groups, groupObjs) {
  for (const [gname, group] of Object.entries(groups)) {
    const gobj = groupObjs[gname];
    for (const child of group.children) {
      const sobj = states[child];
      if (!sobj) continue;
      const ranges = attrRanges(gobj.block);
      for (const [name, r] of Object.entries(ranges)) {
        if (name === "name") continue;
        const ga = gobj.block.slice(r[0], r[1]);
        if (attrType(ga) !== "output" || attrValue(ga) === "") continue;
        const sa = attrBlock(sobj.block, name);
        if (sa && !hasLocalStateAssignment(sa)) {
          setAttrValue(sa, attrValue(ga));
          setValueStatus(sa, "LOCAL");
          setAttrBlock(sobj.block, name, sa);
        }
      }
    }
  }
}

function hasLocalStateAssignment(attr) {
  const value = attrValue(attr);
  if (value === "") return false;
  const status = valueStatus(attr);
  if (status === "LOCAL") return true;
  if (status.startsWith("GLOBAL_")) return false;
  return true;
}

function outputDefaults(pre) {
  const defaults = {}, types = {};
  let inOutputs = false;
  for (let i = 0; i < pre.length; i++) {
    const token = trim(pre[i]);
    if (token === "<outputs>") inOutputs = true;
    else if (token === "</outputs>") inOutputs = false;
    else if (inOutputs) {
      const m = token.match(/^<([^/][^>]*)>$/);
      if (m) {
        const close = findClose(pre, i, m[1]);
        if (close > i) {
          const attr = pre.slice(i, close + 1);
          defaults[m[1]] = attrValue(attr);
          types[m[1]] = attrType(attr);
          i = close;
        }
      }
    }
  }
  return { defaults, types };
}

function selectedTransitionConditions(transitions) {
  const bySource = new Map();
  for(const t of transitions) {
    const source = tinfo(t).start;
    if(!bySource.has(source))
      bySource.set(source, []);
    bySource.get(source).push(t);
  }

  const selected = new Map();
  for(const list of bySource.values()) {
    list.sort((a, b) => compareInfo(tinfo(a), tinfo(b), a, b));
    const prior = [];
    for(const t of list) {
      const info = tinfo(t);
      const equation = (info.equation || "1").trim() || "1";
      const blocked = prior.length ? ` && !(${prior.join(" || ")})` : "";
      selected.set(t, `(state==${info.start}) && (${equation})${blocked} && (nextstate==${info.end})`);
      prior.push(`(${equation})`);
      if(equation === "1")
        break;
    }
  }
  return selected;
}

function renameMachine(pre, suffix) {
  let renamed = false;
  return pre.map((line, index) => {
    if(renamed)
      return line;
    if(trim(line) !== "<value>")
      return line;
    for(let i = index - 1; i >= 0; i--) {
      const token = trim(pre[i]);
      if(token === "<name>")
        break;
      if(token === "<machine>" || token === "</machine>")
        return line;
    }
    for(let i = index - 1; i >= 0; i--) {
      const token = trim(pre[i]);
      if(token === "<machine>")
        break;
      if(token === "</machine>")
        return line;
    }
    const valueIndex = index + 1;
    if(valueIndex < pre.length) {
      const current = trim(pre[valueIndex]);
      if(current) {
        pre[valueIndex] = pre[valueIndex].replace(/\S.*$/, `${current}${suffix}`);
        renamed = true;
      }
    }
    return line;
  });
}

function convertActions(transitions, states, defaults, types) {
  const actions = new Map();
  const selectedConditions = selectedTransitionConditions(transitions);
  for (const t of transitions) {
    const info = tinfo(t);
    for (const [name, r] of Object.entries(attrRanges(t.block)).reverse()) {
      const a = t.block.slice(r[0], r[1]);
      if (attrType(a) !== "output" || types[name] !== "regdp") continue;
      if (attrValue(a) !== "" && valueStatus(a) === "LOCAL") {
        const key = `${info.end}\u0000${name}`;
        if (!actions.has(key)) actions.set(key, []);
        actions.get(key).push({ info, value: attrValue(a), condition: selectedConditions.get(t) || `state==${info.start} && nextstate==${info.end}` });
      }
      removeAttrBlock(t.block, name);
    }
  }
  for (const [key, list] of actions.entries()) {
    const [state, output] = key.split("\u0000");
    const sobj = states[state];
    if (!sobj) continue;
    const a = attrBlock(sobj.block, output);
    if (!a) continue;
    let expr = attrValue(a) || defaults[output] || "0";
    list.sort((x, y) => compareInfo(x.info, y.info)).reverse().forEach(({ condition, value }) => {
      expr = `((${condition}) ? (${value}) : (${expr}))`;
    });
    setAttrValue(a, expr);
    setValueStatus(a, "LOCAL");
    setAttrBlock(sobj.block, output, a);
  }
}

function main() {
  if (process.argv.length !== 4) {
    console.error("usage: generate_fizzim1_compat.js <input.fzm> <output.fzm>");
    process.exit(2);
  }
  const input = process.argv[2], output = process.argv[3];
  const lines = fs.readFileSync(input, "utf8").split(/\r?\n/);
  let { pre, objects, post } = readObjects(lines);
  pre = renameMachine(pre, "_fizzim");

  const states = Object.fromEntries(objects.filter((o) => o.kind === "state").map((o) => [objName(o), o]));
  const forks = new Set(objects.filter((o) => o.kind === "fork").map(objName));
  const groupObjs = Object.fromEntries(objects.filter((o) => o.kind === "stategroup").map((o) => [objName(o), o]));
  const groups = {};
  for (const [name, obj] of Object.entries(groupObjs)) {
    const children = [];
    obj.block.forEach((line, i) => { if (trim(line) === "<child>" && i + 1 < obj.block.length) children.push(trim(obj.block[i + 1])); });
    groups[name] = { entry: simple(obj.block, "entryState"), children };
  }

  inheritGroupAttrs(states, groups, groupObjs);
  let transitions = objects.filter((o) => o.kind === "transition").map(clone);
  transitions = expandGroups(transitions, groups, states);
  transitions = expandForks(transitions, forks, states);
  const { defaults, types } = outputDefaults(pre);
  convertActions(transitions, states, defaults, types);
  transitions = normalizePriorities(transitions);

  const finalObjects = objects.filter((o) => !["fork", "stategroup", "transition"].includes(o.kind));
  transitions.sort((a, b) => objName(a).localeCompare(objName(b))).forEach((t) => finalObjects.push(t));
  fs.mkdirSync(path.dirname(output), { recursive: true });
  fs.writeFileSync(output, pre.concat(finalObjects.flatMap((o) => o.block), post).join("\n") + "\n");
}

main();
