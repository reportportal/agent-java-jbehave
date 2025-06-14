# Changelog

## [Unreleased]

## [5.4.0]
### Changed
- `ReportPortalFormat.finishLaunch` made public, by @HardNorth
- Client version updated on [5.3.14](https://github.com/reportportal/client-java/releases/tag/5.3.14), by @HardNorth

## [5.3.6]
### Fixed
- [Issue #72](https://github.com/reportportal/agent-java-jbehave/issues/72): Incorrect test count on Before and After stories steps, by @HardNorth
### Changed
- Client version updated on [5.2.25](https://github.com/reportportal/client-java/releases/tag/5.2.25), by @HardNorth

## [5.3.5]
### Changed
- Client version updated on [5.2.24](https://github.com/reportportal/client-java/releases/tag/5.2.24), by @HardNorth

## [5.3.4]
### Changed
- Client version updated on [5.2.13](https://github.com/reportportal/client-java/releases/tag/5.2.13), by @HardNorth

## [5.3.3]
### Changed
- JBehave dependency marked as `compileOnly` to force users specify their own versions, by @HardNorth
- Client version updated on [5.2.11](https://github.com/reportportal/client-java/releases/tag/5.2.11), by @HardNorth
### Removed
- `ItemTreeUtils.createKey(org.jbehave.core.model.Scenario)` unused method, by @HardNorth

## [5.3.2]
### Changed
- Client version updated on [5.2.4](https://github.com/reportportal/client-java/releases/tag/5.2.4), by @HardNorth

## [5.3.1]
### Changed
- Remove `commons-model` dependency to rely on `clinet-java` exclusions in security fixes, by @HardNorth
- Client version updated on [5.2.3](https://github.com/reportportal/client-java/releases/tag/5.2.3), by @HardNorth

## [5.3.0]
### Changed
- Examples are not reported in their own separate suite now, they are reported as separate scenarios instead, to conform other BDD framework implementation, by @HardNorth
- Client version updated on [5.2.2](https://github.com/reportportal/client-java/releases/tag/5.2.2), by @HardNorth
- JBehave dependency marked as `implementation` to force users specify their own versions, by @HardNorth

## [5.2.2]
### Changed
- Client version updated on [5.1.22](https://github.com/reportportal/client-java/releases/tag/5.1.22), by @HardNorth
- Jbehave version was updated on version 5.1.1, by @HardNorth

## [5.2.1]
### Changed
- Client version updated on [5.1.16](https://github.com/reportportal/client-java/releases/tag/5.1.16), by @HardNorth

## [5.2.0]
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
