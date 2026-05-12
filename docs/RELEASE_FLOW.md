# Release Flow

Use this checklist when preparing an official Fizzim release.

1. Update the wiki documentation if necessary.
2. Update `src/FizzimVersion.java`:
   - set `RELEASE_VERSION` to the GitHub release tag without the leading `v`,
   - reset or increment `BUILD_NUMBER` for the build being published.
3. Run the backend tests.
4. Merge the release-ready branch into `master`.
5. Push `master`.
6. Create the specified GitHub release.

Do not skip the backend tests before publishing a release. Include release
assets such as `fizzim.jar` and `fizzim.pl` while the Perl backend remains
supported.
