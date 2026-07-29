# [2.0.0](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/compare/1.0.0...2.0.0) (2026-07-29)


### Bug Fixes

* read modern JDK class files in architecture rules ([bcfa9fd](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/commit/bcfa9fd9f02408674c1c662c835b8d39f13842a9))


### Features

* compile the plugin for Java 25 ([68879ef](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/commit/68879ef8b34d6bdea8b43e6511474e68cbc354cf))


### BREAKING CHANGES

* the plugin is compiled for Java 25 and no longer loads on
a Maven JVM older than 25. Builds still running on JDK 21-24 must stay on
the 1.x line. Note that 1.x parses no class file above Java 22: on JDK 23
or newer it reports every rule as passing while importing nothing, so
pinning it is only safe while the build itself stays on JDK 21-22.

# 1.0.0 (2026-04-02)


### Features

* first implementation ([707d221](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/commit/707d2216b0f0d7f4f41cd62e2f57cce52efe4576))
