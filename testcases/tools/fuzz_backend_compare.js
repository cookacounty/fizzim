#!/usr/bin/env node
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repo = path.resolve(__dirname, "..", "..");
const source = path.join(repo, "testcases", "generic_state_machine.fzm");
const outDir = path.join(repo, "testcases", "generated", "fuzz");
const perl = process.env.PERL_BIN || process.env.FIZZIM_PERL || "perl";
const backend = process.env.BACKEND || path.join(repo, "fizzim.pl");
const java = process.env.JAVA_BIN || "java";
const count = parseInt(process.env.FUZZ_COUNT || "24", 10);

const equations = [
  "1",
  "start",
  "!start",
  "option1",
  "!option1",
  "break_req",
  "start && option1",
  "start && !option1",
  "break_req || option1",
  "(start && !break_req) || option1",
  "(trigger == 1'b0) && start",
  "(trigger1 == 2'b01) || break_req"
];
const boolValues = [
  "0",
  "1",
  "start",
  "option1",
  "break_req",
  "start && option1",
  "start || break_req",
  "!break_req"
];
const twoBitValues = [
  "2'b00",
  "2'b01",
  "2'b10",
  "2'b11",
  "{start, option1}",
  "{break_req, start}"
];

function rng(seed) {
  let s = seed >>> 0;
  return () => {
    s = (1664525 * s + 1013904223) >>> 0;
    return s / 0x100000000;
  };
}

function pick(rand, values) {
  return values[Math.floor(rand() * values.length)];
}

function replaceAttrValue(text, tag, chooser) {
  const escaped = tag.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const re = new RegExp(`(<${escaped}>[\\s\\S]*?<value>\\s*\\n\\s*)[\\s\\S]*?(\\n\\s*<status>)`, "g");
  return text.replace(re, (match, prefix, suffix) => `${prefix}${chooser()}${suffix}`);
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: repo,
    encoding: "utf8",
    maxBuffer: 20 * 1024 * 1024,
    ...options
  });
  if(result.status !== 0) {
    throw new Error([
      `${command} ${args.join(" ")} failed with exit ${result.status}`,
      result.stdout,
      result.stderr
    ].filter(Boolean).join("\n"));
  }
  return result;
}

fs.mkdirSync(outDir, { recursive: true });
const template = fs.readFileSync(source, "utf8");
let generated = 0;

for(let i = 0; i < count; i++) {
  const rand = rng(0xF122000 + i);
  const moduleName = `fuzz_fsm_${String(i).padStart(2, "0")}`;
  let text = template.replace(/generic_state_action/g, moduleName);

  text = text.replace(/(<equation>[\s\S]*?<value>\s*\n\s*)[\s\S]*?(\n\s*<status>)/g,
    (match, prefix, suffix) => `${prefix}${pick(rand, equations)}${suffix}`);
  text = text.replace(/(<priority>[\s\S]*?<value>\s*\n\s*)[\s\S]*?(\n\s*<status>)/g,
    (match, prefix, suffix) => `${prefix}${Math.floor(rand() * 1001)}${suffix}`);
  text = replaceAttrValue(text, "rdy", () => pick(rand, boolValues));
  text = replaceAttrValue(text, "busy", () => pick(rand, boolValues));
  text = replaceAttrValue(text, "trigger", () => pick(rand, boolValues));
  text = replaceAttrValue(text, "trigger1[1:0]", () => pick(rand, twoBitValues));

  const fzm = path.join(outDir, `${moduleName}.fzm`);
  const perlOut = path.join(outDir, `${moduleName}.perl.v`);
  const javaOut = path.join(outDir, `${moduleName}.java.v`);
  fs.writeFileSync(fzm, text);

  const perlResult = run(perl, [backend, "-noaddversion", fzm]);
  fs.writeFileSync(perlOut, perlResult.stdout);
  const javaResult = run(java, ["-cp", path.join(repo, "fizzim.jar"), "FizzimJavaBackend", "-noaddversion", fzm]);
  fs.writeFileSync(javaOut, javaResult.stdout);

  if(perlResult.stdout !== javaResult.stdout) {
    fs.writeFileSync(path.join(outDir, `${moduleName}.diff.txt`), simpleDiff(perlResult.stdout, javaResult.stdout));
    throw new Error(`Perl/Java backend mismatch for ${moduleName}`);
  }

  if(process.env.FUZZ_YOSYS !== "0" && commandExists("yosys")) {
    const yosys = path.join(outDir, `${moduleName}.ys`);
    fs.writeFileSync(yosys, [
      `read_verilog -sv "${perlOut.replace(/\\/g, "/")}"`,
      `hierarchy -check -top ${moduleName}`,
      "proc",
      "opt",
      "check",
      ""
    ].join("\n"));
    run("yosys", ["-q", "-s", yosys]);
  }
  generated++;
}

console.log(`PASS fuzzed ${generated} randomized Fizzim diagrams with Perl/Java line-for-line backend comparison`);

function commandExists(command) {
  if(process.platform === "win32")
    return spawnSync("where", [command], { stdio: "ignore" }).status === 0;
  return spawnSync("sh", ["-c", `command -v ${command}`], { stdio: "ignore" }).status === 0;
}

function simpleDiff(a, b) {
  const left = a.split(/\r?\n/);
  const right = b.split(/\r?\n/);
  const lines = [];
  const max = Math.max(left.length, right.length);
  for(let i = 0; i < max; i++) {
    if(left[i] !== right[i]) {
      lines.push(`@@ line ${i + 1} @@`);
      lines.push(`- ${left[i] ?? "<missing>"}`);
      lines.push(`+ ${right[i] ?? "<missing>"}`);
    }
  }
  return lines.join("\n");
}
