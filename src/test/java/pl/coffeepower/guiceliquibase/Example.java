package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;

import liquibase.resource.ClassLoaderResourceAccessor;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Ignore
public class Example {

  @SuppressWarnings("checkstyle:javadocmethod")
  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    }
  }

  @Test
  public void shouldExecuteLiquibaseMigration() throws Exception {
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

    @GuiceLiquibase
    @Provides
    @Inject
    private GuiceLiquibaseConfig createLiquibaseConfig(DataSource dataSource) {
      return GuiceLiquibaseConfig.Builder
          .of(LiquibaseConfig.Builder.of(dataSource)
              .withChangeLogPath("liquibase/exampleChangeLog.xml")
              .withResourceAccessor(new ClassLoaderResourceAccessor(getClass().getClassLoader()))
              .withDropFirst(false)
              .build())
          .build();
    }
  }
}
