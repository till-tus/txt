# Contributing

Thanks for helping make Text Launcher calmer, clearer, and more useful.

## Project Direction

Text Launcher is a minimal Android launcher for intentional phone use. Contributions should preserve that spirit:

- Prefer quiet, focused interactions over attention-grabbing UI.
- Keep features understandable without onboarding-heavy explanations.
- Favor local-first behavior and avoid unnecessary network dependencies.
- Treat privacy, accessibility, and battery impact as core product concerns.

## Getting Started

1. Fork and clone the repository.
2. Open the project in Android Studio, or use the Gradle wrapper from the command line.
3. Install JDK 17 and Android SDK 36.
4. Run the tests:

```sh
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

## Development Guidelines

- Keep changes focused and easy to review.
- Add or update tests when behavior changes.
- Use Kotlin and XML/ViewBinding patterns already present in the project.
- Avoid committing generated files, local SDK paths, signing keys, or IDE state.
- Document user-visible behavior in the README when adding substantial features.

## Pull Requests

Before opening a pull request:

- Run `./gradlew test`.
- Explain the user problem and the chosen solution.
- Include screenshots or short recordings for UI changes when helpful.
- Note any permission, privacy, accessibility, or battery implications.

## Issues

Please use the bug report and feature request templates. For security-sensitive reports, follow `SECURITY.md` instead of opening a public issue with exploit details.
