# Testbench Area

This folder holds pinned reference material used by the public regression flow.

Detailed documentation now lives in the wiki:

https://github.com/cookacounty/fizzim/wiki/Backend-and-Regression-Testing

Legacy reference documentation is summarized here:

https://github.com/cookacounty/fizzim/wiki/Legacy-Documentation

## Contents

- `legacy/fizzim_5_20.pl` - pinned legacy Fizzim backend used by regression tests.
- `legacy/FizzimGui_5_20.java` - legacy GUI source kept for reference only.
- `legacy/fizzim_tutorial_20160423.pdf` - original tutorial/full documentation PDF.

Run the regression from the repository root:

```bash
make test
```

Windows fallback:

```powershell
.\make.cmd test
```
