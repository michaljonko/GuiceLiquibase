# GuiceLiquibase
Liquibase extension to easy use with Guice. Inspired by [*CDI Liquibase*](http://www.liquibase.org/documentation/cdi.html) - there is no support for *Google Guice* at the moment.
Please check Example.java file for development details.

## Example
```java
public class Example {

  public static void main(String[] args) throws Exception {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    } 
    Guice.createInjector(new GuiceLiquibaseModule(), new MyLiquibaseConfigModule());
  }

  private static final class MyLiquibaseConfigModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Singleton
    @Provides
    private DataSource createDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:memdb");
      dataSource.setUser("SA");
      return dataSource;
    }
    
    @GuiceLiquibaseConfiguration
    @Provides
    @Inject
    private GuiceLiquibaseConfig createLiquibaseConfig(DataSource dataSource) {
      return GuiceLiquibaseConfig.Builder
          .of(LiquibaseConfig.Builder.of(dataSource)
              .withChangeLogPath("liquibase/exampleChangeLog.xml")
              .build())
          .build();
    }    
  }
}
```