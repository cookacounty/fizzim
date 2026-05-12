# Release Flow

Use this checklist when preparing an official Fizzim release.

1. Update the wiki documentation if necessary.
2. Run the backend tests.
3. Merge the release-ready branch into `master`.
4. Push `master`.
5. Create the specified GitHub release.

Do not skip the backend tests before publishing a release. Include release
assets such as `fizzim.jar` and `fizzim.pl` while the Perl backend remains
supported.
