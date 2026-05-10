# Fizzim 2.0

Finite State Machine design tool for building readable FSM diagrams and
generating Verilog/SystemVerilog.

![Fizzim 2.0 overview](docs/images/fizzim-2-overview.svg)

## Documentation

The main documentation now lives in the GitHub wiki:

https://github.com/cookacounty/fizzim/wiki

Useful starting points:

- [Getting Started](https://github.com/cookacounty/fizzim/wiki/Getting-Started)
- [Modeling Features](https://github.com/cookacounty/fizzim/wiki/Modeling-Features)
- [Validation and Lint](https://github.com/cookacounty/fizzim/wiki/Validation-and-Lint)
- [Backend and Regression Testing](https://github.com/cookacounty/fizzim/wiki/Backend-and-Regression-Testing)
- [Legacy Documentation](https://github.com/cookacounty/fizzim/wiki/Legacy-Documentation)
- [Changelog](CHANGELOG.md)

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
