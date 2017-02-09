# Changelog
---

- #### 0.0.2
    - added CHANGELOG.md
    - moved _LiquibaseConfig_ inner class to separate upper level
    - removed hash value from constructor to  hashCode() method in _LiquibaseConfig_
    - increased Liquibase version to 3.5.3
    - added SLF4J 1.7.1 for logging
    - changed annotation from @LiquibaseConfig to @GuiceLiquibase
    - added _dropFirst_ to LiquibaseConfig
    - added _LiquibaseConfig_ Builder and make constructor private
    
- #### 0.0.1
    - Initial version