# Fizzim 2.0

Finite State Machine design tool for building readable FSM diagrams and
generating Verilog/SystemVerilog.

![Fizzim 2.0 overview](docs/images/fizzim-2-overview.svg)

## Documentation

The detailed documentation lives in the GitHub wiki:

https://github.com/cookacounty/fizzim/wiki

Start with [Getting Started](https://github.com/cookacounty/fizzim/wiki/Getting-Started)
for build/run instructions, then see
[Modeling Features](https://github.com/cookacounty/fizzim/wiki/Modeling-Features)
for forks, state groups, and transition actions.

The repo keeps only short reference material here. The wiki is the source of
truth for detailed usage, lint, backend testing, and legacy documentation.

## Quick Build

```sh
make
java -jar fizzim.jar
```

Windows fallback when GNU Make is not installed:

```bat
make.cmd jar
make.cmd test
make.cmd clean
```

The jar builds with Java 11-compatible bytecode by default.

## Credits

Fizzim was originally written by Michael Zimmer of Zimmer Design Services.
Fizzim 2.0 feature updates were added by Aaron Cook.

Fizzim on the web: http://www.fizzim.com
