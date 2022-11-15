# Changelog

## [Unreleased]
### Added
- JBehave version 5 support, by @HardNorth
### Removed
- JBehave version 4.8 support, by @HardNorth

## [5.1.3]
### Changed
- Client version updated on [5.1.12](https://github.com/reportportal/client-java/releases/tag/5.1.12), by @HardNorth
### Fixed
- Skipped scenario handling, by @HardNorth

## [5.1.2]
### Added
- Test Case ID templating, by @HardNorth
### Changed
- Client version updated on [5.1.9](https://github.com/reportportal/client-java/releases/tag/5.1.9), by @HardNorth
- Slf4j version updated on 1.7.36, by @HardNorth
- Jbehave version was updated on version 4.8.3, by @HardNorth

## [5.1.1]
### Fixed
- [Issue #59](https://github.com/reportportal/agent-java-jbehave/issues/59): IAE when examples table parameter has $ character
- [Issue #57](https://github.com/reportportal/agent-java-jbehave/issues/57): Steps using the same Examples table parameter twice break reporting
- [Issue #56](https://github.com/reportportal/agent-java-jbehave/issues/56): Parameterised composite steps brake reporting

Special thanks to [Ivan Kalinin](https://github.com/ikalinin1)

## [5.1.0]
### Changed
- Version promoted to stable release
- Client version updated on [5.1.0](https://github.com/reportportal/client-java/releases/tag/5.1.0)

## [5.1.0-RC-3]
### Added
- ReportPortalStoryReporter#buildFinishTestItemRequest method
- JSR-305 annotations

## [5.1.0-RC-2]
### Changed
- Client version updated on [5.1.0-RC-12](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-12)

## [5.1.0-RC-1]
### Changed
- Client version updated on [5.1.0-RC-6](https://github.com/reportportal/client-java/releases/tag/5.1.0-RC-6)
- Version changed on 5.1.0

## [5.0.0-RC-3]
### Fixed
- Unsupported Operation exception on launch start
### Changed
- Client version updated on [5.0.21](https://github.com/reportportal/client-java/releases/tag/5.0.21)

## [5.0.0-RC-2]
### Added
- Comprehensive javadocs
- Composite steps support
- Many extension methods
### Changed
- Formatters class structure to reduce code amount and duplication
### Fixed
- Domain model dependency now switched on release version

## [5.0.0-BETA-9]
### Added
- `ReportPortalScenarioFormat` class, which is a formatter for reporting scenarios as steps

## [5.0.0-BETA-8]
### Changed
- Client version updated on [5.0.18](https://github.com/reportportal/client-java/releases/tag/5.0.18)
- The agent code was completely rewritten to ease configuration and reduce code amount 

## [5.0.0-BETA-4]
