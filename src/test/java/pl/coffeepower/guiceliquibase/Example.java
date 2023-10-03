package pl.coffeepower.guiceliquibase;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;

import liquibase.resource.ClassLoaderResourceAccessor;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibaseConfiguration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

@Disabled("Integration test - example how to use library")
class Example {

  @SuppressWarnings("checkstyle:javadocmethod")
  @BeforeAll
  static void beforeClass() throws ClassNotFoundException {
    Class.forName("org.hsqldb.jdbc.JDBCDriver");
  }

  @Test
  void shouldExecuteLiquibaseMigration() throws Exception {
    Injector injector = Guice.createInjector(
        new GuiceLiquibaseModule(), new MyLiquibaseConfigModule());

    Set<String> createdTables = getTablesFromDataSource(injector.getInstance(DataSource.class));
    assertThat(createdTables)
        .containsExactlyInAnyOrder(
            "DATABASECHANGELOG",
            "DATABASECHANGELOGLOCK",
            "EXAMPLE_TABLE");
  }

  private Set<String> getTablesFromDataSource(DataSource dataSource) throws SQLException {
    Set<String> createdTables = Sets.newHashSet();
    try (Connection connection = dataSource.getConnection();
         ResultSet tables = connection.getMetaData()
             .getTables("PUBLIC", "PUBLIC", null, new String[] {"TABLE"})) {
      while (tables.next()) {
        createdTables.add(tables.getString("TABLE_NAME"));
      }
    }
    return createdTables;
  }

  private static final class MyLiquibaseConfigModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Singleton
    @Provides
    private DataSource createDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID());
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
              .withResourceAccessor(new ClassLoaderResourceAccessor(getClass().getClassLoader()))
              .withDropFirst(true)
              .withContext("")
              .withLabel("")
              .withParameter("param", "value")
              .build())
          .build();
    }
  }
}
