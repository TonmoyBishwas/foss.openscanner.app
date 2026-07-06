# Contributing to OpenScanner

Thanks for your interest in making OpenScanner better.

## Getting started

1. Fork and clone the repo.
2. Open the project in Android Studio (or build with `./gradlew assembleDebug`).
3. Pick an issue (or open one describing what you'd like to change) and leave a comment so work isn't duplicated.

## Guidelines

- **Keep it FOSS.** No proprietary dependencies, no Google Play Services, no analytics/tracking SDKs. The app must keep building on F-Droid.
- **Keep it offline.** OpenScanner must never require the network. Do not add the INTERNET permission.
- **Match the design system.** UI follows the Calm Material design language: tonal surfaces instead of shadows, 12/16dp radii, Doto for headlines, Hanken Grotesk for everything else, sentence case copy, no exclamation marks.
- **Small PRs are easier to review** than big ones. One change per PR.
- Run `./gradlew test` before opening a PR.

## Commit messages

Short imperative subject line ("Add Sauvola binarization"), body explaining *why* when it isn't obvious.

## License

By contributing you agree that your contributions are licensed under GPL-3.0.
