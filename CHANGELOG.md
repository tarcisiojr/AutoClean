# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Releases are generated automatically by
[release-please](https://github.com/googleapis/release-please) from
[Conventional Commits](https://www.conventionalcommits.org/).

## [2.0.0] - Initial public release

- Standby-triggered RAM cleanup via `ActivityManager.killBackgroundProcesses`.
- 27-package allow-list curated for TCL C7K (Android TV 12).
- Headless `ForegroundService` hosting a dynamic `SCREEN_OFF` receiver.
- ~1.2 MB APK, ~22 MB idle RSS.
