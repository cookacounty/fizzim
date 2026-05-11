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

## Core Modeling Semantics

When the same output/internal is assigned in more than one modeling layer,
Fizzim resolves the generated HDL in this priority order:

1. Transition actions, highest priority.
2. Concrete state assignments.
3. State group assignments, lowest priority shared defaults.

State groups intentionally provide inherited defaults only. If a child state
sets the same variable, the child state's value wins. If a transition action
sets that variable on the route into the state, the transition action wins for
that clock.

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
perl fizzim.pl -noaddversion testcases/generic_state_machine.fzm
```

On Windows, the same command can be run from PowerShell if Perl is on `PATH`:

```powershell
perl fizzim.pl -noaddversion testcases\generic_state_machine.fzm
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

## Branding

Fizzim 2.0 keeps the application name as Fizzim, but the splash and overview
art use the `F^2izz||m` wordmark as a visual nod to logic operators. The app
icon remains a compact `F^2`.

## Credits

Fizzim was originally written by Michael Zimmer of Zimmer Design Services.
Fizzim 2.0 feature updates were added by Aaron Cook.

Fizzim on the web: http://www.fizzim.com
