# Changelog
---

- #### 0.1.1
  - Newer version of guava - Guice uses Guava with known vulnerabilities
  - Fixes in build.gradle to make releases easier

- #### 0.1.0
  - New package for all classes - breaking changes
  - Upgraded to Gradle 7.6.2
  - Upgraded checkstyle and fix checks
  - Added SpotBugs instead of FindBugs
  - Upgraded Mockito
  - Removed Hamcrest
  - Added AssertJ
  - Upgraded ThirdParty Libraries
  - Upgraded Checkstyle and Mockito to Java 11 versions
  - Migrated Libraries to Junit 5
  - Upgraded Testcases to JUnit 5
  - Upgraded Liquibase to 4.20.0
  - Configured gradle publish to Sonatype
  - New checkstyle & codestyle - Google style guide

- #### 0.0.7
    - Updated Liquibase code version

- #### 0.0.6
    - Fix for closing database connection

- #### 0.0.5
    - Updated Guice to 5.0.1
    - Updated Liquibase to 3.10.3
    - Some fixes
     
- #### 0.0.3

- #### 0.0.2
    - Added CHANGELOG.md
    - Moved _LiquibaseConfig_ inner class to separate upper level
    - Removed hash value from constructor to hashCode() method in _LiquibaseConfig_
    - Increased Liquibase version to 3.5.3
    - Added SLF4J 1.7.1 for logging
    - Changed annotation from @LiquibaseConfig to @GuiceLiquibaseConfiguration
    - Added _dropFirst_ to LiquibaseConfig
    - Added _LiquibaseConfig_ Builder and make constructor private
    - Added PIT for tests quality and coverage
    - Added date signature to snapshot build
    - Added code coverage for _LiquibaseConfig_
    - Added ToStringVerifier 1.0.2
    - Added @Ignore to _Example_ in test path

- #### 0.0.1
    - Initial version
