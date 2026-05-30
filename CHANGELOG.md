# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Releases are generated automatically by
[release-please](https://github.com/googleapis/release-please) from
[Conventional Commits](https://www.conventionalcommits.org/).

## [2.0.1](https://github.com/tarcisiojr/AutoClean/compare/v2.0.0...v2.0.1) (2026-05-30)


### Documentation

* adicionar AGENTS.md + aprendizados em /docs ([11dae39](https://github.com/tarcisiojr/AutoClean/commit/11dae39a148a7d9bf0be0f111a1fbb3a86313974))
* preparar repositório para distribuição pública ([765d56d](https://github.com/tarcisiojr/AutoClean/commit/765d56d97e76eebc9859f3e9b57827c938e6530a))
* **readme:** add PIX donation section ([0bf3aeb](https://github.com/tarcisiojr/AutoClean/commit/0bf3aeb4b93f9d45258e6fe3addb50fd52a6f62d))


### Continuous Integration

* add CI build workflow and release-please automation ([326de27](https://github.com/tarcisiojr/AutoClean/commit/326de27e7e49112b37c599190bb48109e3419360))

## [2.0.0] - Initial public release

- Standby-triggered RAM cleanup via `ActivityManager.killBackgroundProcesses`.
- 27-package allow-list curated for TCL C7K (Android TV 12).
- Headless `ForegroundService` hosting a dynamic `SCREEN_OFF` receiver.
- ~1.2 MB APK, ~22 MB idle RSS.
