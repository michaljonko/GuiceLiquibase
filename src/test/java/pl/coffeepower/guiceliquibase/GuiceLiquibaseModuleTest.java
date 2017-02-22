package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;

import liquibase.resource.ClassLoaderResourceAccessor;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

@RunWith(MockitoJUnitRunner.class)
public class GuiceLiquibaseModuleTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Mock
  private DataSource dataSource;

  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    }
  }

  @Before
  public void setUp() throws Exception {
    try (Connection connection =
             Fixtures.createJdbcDataSource("jdbc:hsqldb:mem:memdb").getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(
          "TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK")) {
        statement.execute();
      }
    }
  }

  @Test
  public void shouldThrowExceptionForRequiredBinding() throws Exception {
    expectedException.expect(CreationException.class);
    Guice.createInjector(new GuiceLiquibaseModule());
  }

  @Test
  public void shouldThrowExceptionForNullConfigValue() throws Exception {
    expectedException.expect(CreationException.class);
    Guice.createInjector(new GuiceLiquibaseModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(GuiceLiquibaseConfig.class)
                .annotatedWith(GuiceLiquibase.class)
                .toInstance(null);
          }
        });
  }

  @Test
  public void shouldThrowExceptionForEmptyConfigurationSet() throws Exception {
    expectedException.expect(CreationException.class);
    expectedException.expectMessage(containsString("Injected configuration set is empty."));
    Guice.createInjector(new GuiceLiquibaseModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(GuiceLiquibaseConfig.class)
                .annotatedWith(GuiceLiquibase.class)
                .toInstance(
                    GuiceLiquibaseConfig.Builder
                        .of()
                        .build());
          }
        });
  }

  @Test
  public void shouldThrowExceptionForNotDefinedDataSourceConnection() throws Exception {
    when(dataSource.getConnection()).thenReturn(null);
    GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
        .of()
        .withLiquibaseConfig(
            LiquibaseConfig.Builder.of(dataSource).build())
        .build();

    expectedException.expect(CreationException.class);
    expectedException.expectMessage(
        containsString("DataSource returns null connection instance."));

    Guice.createInjector(new GuiceLiquibaseModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(GuiceLiquibaseConfig.class)
                .annotatedWith(GuiceLiquibase.class)
                .toInstance(config);
          }
        });
  }

  @Test
  public void shouldExecuteLiquibaseUpdateWithSingleConfiguration() throws Exception {
    Injector injector = Guice.createInjector(
        new GuiceLiquibaseModule(),
        Fixtures.SINGLE_DATA_SOURCE_MODULE);

    DataSource dataSource = injector
        .getInstance(Key.get(GuiceLiquibaseConfig.class, GuiceLiquibase.class))
        .getConfigs().iterator().next().getDataSource();
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement preparedStatement =
               connection.prepareStatement(Fixtures.GET_ALL_FROM_TABLE_FOR_TEST_QUERY)) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          assertThat(resultSet.next(), is(true));
          assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME), is(Fixtures.EXPECTED_ID));
          assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME),
              is(Fixtures.EXPECTED_NAME));
          assertThat(resultSet.getBoolean(Fixtures.ACTIVE_COLUMN_NAME),
              is(Fixtures.EXPECTED_ACTIVE));
          assertThat(resultSet.next(), is(false));
        }
      }
    }
  }

  @Test
  public void shouldExecuteLiquibaseUpdateWithMultipleConfigurations() throws Exception {
    Injector injector = Guice.createInjector(
        new GuiceLiquibaseModule(),
        Fixtures.MULTI_DATA_SOURCE_MODULE);

    DataSource dataSource = injector
        .getInstance(Key.get(GuiceLiquibaseConfig.class, GuiceLiquibase.class))
        .getConfigs().iterator().next().getDataSource();
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement preparedStatement = connection.prepareStatement(
          Fixtures.GET_ALL_FROM_TABLE_FOR_MULTI_TESTS_QUERY)) {
        try (ResultSet resultSet = preparedStatement.executeQuery()) {
          assertThat(resultSet.next(), is(true));
          assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME), is(Fixtures.EXPECTED_ID));
          assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME),
              is(Fixtures.EXPECTED_NAME));
          assertThat(resultSet.next(), is(false));
        }
      }
    }
  }

  private static final class Fixtures {

    private static final String ID_COLUMN_NAME = "id";
    private static final String NAME_COLUMN_NAME = "name";
    private static final String ACTIVE_COLUMN_NAME = "active";
    private static final String EXPECTED_NAME = "test";
    private static final String GET_ALL_FROM_TABLE_FOR_TEST_QUERY =
        "SELECT * FROM table_for_test";
    private static final String GET_ALL_FROM_TABLE_FOR_MULTI_TESTS_QUERY =
        "SELECT * FROM table_for_multi_test";
    private static final int EXPECTED_ID = 1;
    private static final boolean EXPECTED_ACTIVE = true;
    private static final Module SINGLE_DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @GuiceLiquibase
      private GuiceLiquibaseConfig createConfig() {
        return GuiceLiquibaseConfig.Builder
            .of()
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(createJdbcDataSource("jdbc:hsqldb:mem:memdb")).build())
            .build();
      }

      @Override
      protected void configure() {
      }
    };
    private static final Module MULTI_DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @GuiceLiquibase
      private GuiceLiquibaseConfig createConfig() {
        ClassLoader classLoader = getClass().getClassLoader();
        return GuiceLiquibaseConfig.Builder
            .of()
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(createJdbcDataSource("jdbc:hsqldb:mem:memdb"))
                    .withChangeLogPath("liquibase/emptyChangeLog.xml")
                    .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                    .withDropFirst(false)
                    .build())
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(createJdbcDataSource("jdbc:hsqldb:mem:memdb"))
                    .withChangeLogPath("liquibase/changeLogMulti.xml")
                    .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                    .withDropFirst(true)
                    .build())
            .build();
      }

      @Override
      protected void configure() {
      }
    };

    private static JDBCDataSource createJdbcDataSource(String jdbcUrl) {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase(jdbcUrl);
      dataSource.setUser("SA");
      return dataSource;
    }
  }
}
