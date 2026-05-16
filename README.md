# Fizzim 2.0

Finite State Machine design tool for building readable FSM diagrams and
generating Verilog/SystemVerilog.

![Fizzim 2.0 splash](docs/images/fizzim-2-splash.png)

## Documentation

The detailed documentation lives in the GitHub wiki:

https://github.com/cookacounty/fizzim/wiki

Start with [Getting Started](https://github.com/cookacounty/fizzim/wiki/Getting-Started)
for build/run instructions, then see
[Modeling Features](https://github.com/cookacounty/fizzim/wiki/Modeling-Features)
for forks, state groups, and transition actions.

The repo keeps only short reference material here. The wiki is the source of
truth for detailed usage, lint, backend testing, and legacy documentation.
Developer notes for specific GUI behaviors are kept in `docs/`, including
`docs/COPY_PASTE.md` for copy/paste rules.

Canvas transitions can be created by dragging from the blue connection points
shown on states, state groups, and forks. Dropping on empty canvas opens a
small menu to create a new state or fork at that location. When zoomed out,
normal drags inside a shape prefer moving the shape; hold `Alt` while hovering
or dragging to suppress connection-point handles and force normal move/selection
behavior.

## Core Modeling Semantics

When the same output/internal is assigned in more than one modeling layer,
Fizzim resolves the generated HDL in this priority order:

1. Transition actions, highest priority.
2. Concrete state assignments.
3. State group assignments, lowest priority shared defaults.

State groups intentionally provide inherited defaults only. If a child state
locally sets the same variable, the child state's value wins. Values merely
inherited from the global output/internal defaults do not block the parent
state-group value. If a transition action sets that variable on the route into
the state, the transition action wins for that clock.

## Quick Start

Fizzim builds into a runnable Java jar. For normal use you need a JDK to build
it and a Java runtime to run it. Perl is needed when generating HDL through the
checked-in `fizzim.pl` backend or running the backend tests.

From the repository root:

```sh
make
java -jar fizzim.jar
```

If you are on Windows and do not have GNU Make installed, use the included
batch helper instead:

```bat
make.cmd jar
java -jar fizzim.jar
```

The jar builds with Java 11-compatible bytecode by default, so Java 11 or newer
is recommended.

## GUI Language

The GUI language can be changed from `Settings > Language`. English, Japanese,
Simplified Chinese, Traditional Chinese, Korean, German, French, Spanish,
Portuguese, Hindi, and Russian are currently supported.

Localization is intentionally limited to GUI chrome such as menus, toolbar
buttons, status labels, the Project pane, the Lint pane, and the quick property
inspector. FSM object names, HDL-facing fields, attribute keys, generated
Verilog/SystemVerilog, and diagram content remain in English-compatible text so
existing diagrams and backend generation behavior stay stable.

## Install Prerequisites

### Windows

The easiest Windows setup is:

- Install a JDK, such as Eclipse Temurin 17 or newer.
- Install Perl, such as Strawberry Perl.
- Install Git for Windows if you want Git Bash for shell-based tests.
- Optional: install GNU Make, or just use `make.cmd`.
- Optional: install Node.js if you want to run the fuzz tests.

Using `winget` from PowerShell:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install StrawberryPerl.StrawberryPerl
winget install Git.Git
winget install OpenJS.NodeJS.LTS
```

After installing, close and reopen PowerShell so `java`, `javac`, `perl`,
`bash`, and `node` are on `PATH`.

Verify the tools:

```powershell
java -version
javac -version
perl -v
bash --version
node --version
```

Build and run:

```bat
make.cmd jar
java -jar fizzim.jar
```

If you installed GNU Make, this also works from PowerShell or Git Bash:

```sh
make
java -jar fizzim.jar
```

### Linux

Install a JDK, Perl, GNU Make, and Bash with your distribution package manager.
Node.js is optional and only needed for fuzz tests.

Debian or Ubuntu:

```sh
sudo apt update
sudo apt install default-jdk perl make bash nodejs npm
```

Fedora:

```sh
sudo dnf install java-latest-openjdk-devel perl make bash nodejs npm
```

Arch Linux:

```sh
sudo pacman -S jdk-openjdk perl make bash nodejs npm
```

Verify the tools:

```sh
java -version
javac -version
perl -v
make --version
bash --version
node --version
```

Build and run:

```sh
make
java -jar fizzim.jar
```

## Common Commands

Build the jar:

```sh
make jar
```

Run the GUI:

```sh
java -jar fizzim.jar
```

Generate HDL directly with the Perl backend:

```sh
perl fizzim.pl -fizzimversion 2.0.2+build.11 -sourcefile generic_state_machine.fzm testcases/generic_state_machine.fzm
```

Generated Verilog includes a deterministic provenance header with the Fizzim
version and source diagram name. The GUI and project build command add this
automatically. If you want a source-file SHA-256 checksum in the header, enable
it in `Settings > HDL Generation...`; it is off by default to avoid diffs from
diagram-only edits.

Build every diagram referenced by a Fizzim project file:

```sh
java -jar fizzim.jar --build-project testcases/generic_project.fzp
```

This command uses the same configured HDL backend settings as the GUI
`Build All` command. Per-diagram HDL output paths are resolved relative to each
`.fzm` file and the generated-status metadata is written back into each diagram
after a successful build.

HDL output defaults to `.v`, but `Settings > HDL Generation...` can change the
module-name default extension to `.sv` or any custom extension. If you enter a
custom output filename or per-project HDL path with an explicit extension,
Fizzim preserves that extension instead of forcing `.v`.

On Windows, the same command can be run from PowerShell if Perl is on `PATH`:

```powershell
perl fizzim.pl -fizzimversion 2.0.2+build.11 -sourcefile generic_state_machine.fzm testcases\generic_state_machine.fzm
```

Run the public backend regression tests:

```sh
make test
```

Windows fallback:

```bat
make.cmd test
```

Run the optional Perl-vs-Java backend fuzz comparison:

```sh
make test-fuzz
```

Clean generated build files:

```sh
make clean
```

Windows fallback:

```bat
make.cmd clean
```

If your JDK is not on `PATH`, set `JAVA_HOME` before building:

```sh
JAVA_HOME=/path/to/jdk make jar
```

On Windows:

```bat
set JAVA_HOME=C:\Path\To\JDK
make.cmd jar
```

## Repository Layout

The root folder intentionally keeps the user-facing entry points small:

- `src/` contains the Java GUI and helper source files.
- `resources/` contains images and localization bundles that are packaged at
  the jar classpath root.
- `lib/` contains the pinned `org.jdesktop.layout` class files required by the
  legacy Swing UI.
- `fizzim.pl` remains in the root because the GUI, wrapper scripts, and tests
  use it as the default HDL backend location.
- `build/` is generated by `make jar` or `make.cmd jar` and is ignored.
- `testcases/` contains public diagrams, backend regressions, and public
  reference FSM fixtures.

## Branding

Fizzim 2.0 keeps the application name as Fizzim, but the splash and overview
art use the `F^2izz||m` wordmark as a visual nod to logic operators. The app
icon remains a compact `F^2`.

## Credits

Fizzim was originally written by Michael Zimmer of Zimmer Design Services.
Fizzim 2.0 feature updates were added by Aaron Cook.

Fizzim on the web: http://www.fizzim.com
