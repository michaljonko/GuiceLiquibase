# Changelog
---
- #### 0.0.7
    - updated Liquibase code version

- #### 0.0.6
    - fix for closing database connection

- #### 0.0.5
    - updated Guice to 5.0.1
    - updated Liquibase to 3.10.3
    - some fixes
     
- #### 0.0.3

- #### 0.0.2
    - added CHANGELOG.md
    - moved _LiquibaseConfig_ inner class to separate upper level
    - removed hash value from constructor to hashCode() method in _LiquibaseConfig_
    - increased Liquibase version to 3.5.3
    - added SLF4J 1.7.1 for logging
    - changed annotation from @LiquibaseConfig to @GuiceLiquibaseConfiguration
    - added _dropFirst_ to LiquibaseConfig
    - added _LiquibaseConfig_ Builder and make constructor private
    - added PIT for tests quality and coverage
    - added date signature to snapshot build
    - added code coverage for _LiquibaseConfig_
    - added ToStringVerifier 1.0.2
    - added @Ignore to _Example_ in test path

- #### 0.0.1
    - Initial version
