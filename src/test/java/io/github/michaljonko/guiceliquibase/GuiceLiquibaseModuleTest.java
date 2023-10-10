package io.github.michaljonko.guiceliquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.jparams.verifier.tostring.NameStyle;
import com.jparams.verifier.tostring.ToStringVerifier;
import io.github.michaljonko.guiceliquibase.annotation.GuiceLiquibaseConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuiceLiquibaseModuleTest {

  private DatabaseFactory orgDatabaseFactory;

  @BeforeAll
  static void beforeClass() throws ClassNotFoundException {
    Class.forName("org.hsqldb.jdbc.JDBCDriver");
  }

  @BeforeEach
  public void setUp() {
    orgDatabaseFactory = DatabaseFactory.getInstance();
  }

  @AfterEach
  public void tearDown() {
    DatabaseFactory.setInstance(orgDatabaseFactory);
  }

  @Test
  void shouldExecuteLiquibaseUpdateWithSingleConfiguration() throws Exception {
    Guice.createInjector(
        new GuiceLiquibaseModule(),
        Fixtures.SINGLE_DATA_SOURCE_MODULE);

    try (Connection connection = Fixtures.SINGLE_DATA_SOURCE.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            Fixtures.GET_ALL_FROM_TABLE_FOR_TEST_QUERY);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      assertThat(resultSet.next())
          .isTrue();
      assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME))
          .isEqualTo(Fixtures.EXPECTED_ID);
      assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME))
          .isEqualTo(Fixtures.EXPECTED_NAME);
      assertThat(resultSet.getBoolean(Fixtures.ACTIVE_COLUMN_NAME))
          .isEqualTo(Fixtures.EXPECTED_ACTIVE);
      assertThat(resultSet.next())
          .isFalse();
    }
  }

  @Test
  void shouldExecuteLiquibaseUpdateWithMultipleConfigurations() throws Exception {
    Guice.createInjector(
        new GuiceLiquibaseModule(),
        Fixtures.MULTI_DATA_SOURCE_MODULE);

    try (Connection connection = Fixtures.MULTI_DATA_SOURCE.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(
            Fixtures.GET_ALL_FROM_TABLE_FOR_MULTI_TESTS_QUERY);
        ResultSet resultSet = preparedStatement.executeQuery()) {
      assertThat(resultSet.next())
          .isTrue();
      assertThat(resultSet.getInt(Fixtures.ID_COLUMN_NAME))
          .isEqualTo(Fixtures.EXPECTED_ID);
      assertThat(resultSet.getString(Fixtures.NAME_COLUMN_NAME))
          .isEqualTo(Fixtures.EXPECTED_NAME);
      assertThat(resultSet.next())
          .isFalse();
    }
  }

  @Test
  void shouldNotExecuteUpdateWhenShouldRunIsDisabled() {
    DataSource dataSource = mock(DataSource.class);

    Guice.createInjector(
        new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(GuiceLiquibaseConfig.Builder.of(
                LiquibaseConfig.Builder.of(dataSource).withShouldRun(false).build()).build()));

    verifyNoMoreInteractions(dataSource);
  }

  @Test
  void shouldNotExecuteUpdateSecondTime() {
    Injector injector = Guice.createInjector(
        new GuiceLiquibaseModule(),
        Fixtures.DATA_SOURCE_MODULE);

    injector.getInstance(GuiceLiquibaseModule.GuiceLiquibaseEngine.class)
        .process();

    Set<LiquibaseConfig> configs = injector.getInstance(GuiceLiquibaseModule.LIQUIBASE_CONFIG_KEY)
        .getConfigs();

    configs
        .forEach(liquibaseConfig -> {
          try {
            DataSource dataSource = liquibaseConfig.getDataSource();
            verify(dataSource, only()).getConnection();
          } catch (SQLException ex) {
            fail(ex);
          }
        });
  }

  @Test
  void shouldThrowExceptionForNotDefinedRequiredBinding() {
    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule()))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("Unable to create injector")
        .hasMessageContaining("No implementation for"
            + " GuiceLiquibaseConfig annotated with"
            + " @GuiceLiquibaseConfiguration() was bound");
  }

  @Test
  void shouldThrowExceptionForDefinedGuiceLiquibaseConfiguration() {
    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule()))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("No implementation for GuiceLiquibaseConfig");
  }

  @Test
  void shouldThrowExceptionForNullConfigValue() {
    assertThatThrownBy(() -> Guice.createInjector(
        new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(null)))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("Unable to create injector")
        .hasMessageContaining("Binding to null instances is not allowed.");
  }

  @Test
  void shouldThrowExceptionForEmptyConfigurationSet() {
    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(GuiceLiquibaseConfig.Builder.of().build())))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("Injected configuration set is empty.")
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowExceptionForNotDefinedDataSourceConnection() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(null);

    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(GuiceLiquibaseConfig.Builder.of()
                .withLiquibaseConfig(LiquibaseConfig.Builder.of(dataSource).build()).build())))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("Unable to create injector")
        .hasMessageContaining("DataSource returns null connection instance.")
        .hasCauseInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldThrowExceptionWhenProblemOccurredDuringDatabaseCreation() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getMetaData()).thenThrow(new SQLException("My SQLException."));

    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(
                GuiceLiquibaseConfig.Builder.of(LiquibaseConfig.Builder.of(dataSource).build())
                    .build())))
        .isInstanceOf(CreationException.class)
        .hasMessageContaining("My SQLException.")
        .hasCauseInstanceOf(UnexpectedLiquibaseException.class);

    verify(dataSource).getConnection();
    verify(connection).getMetaData();
    verify(connection).close();
    verifyNoMoreInteractions(connection, dataSource);
  }

  @Test
  void shouldThrowExceptionWhenProblemOccurredDuringLiquibaseUpdate()
      throws SQLException, DatabaseException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseFactory databaseFactory = mock(DatabaseFactory.class);
    Database database = mock(Database.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(databaseFactory.findCorrectDatabaseImplementation(any())).thenReturn(database);
    DatabaseFactory.setInstance(databaseFactory);

    assertThatThrownBy(() -> Guice.createInjector(new GuiceLiquibaseModule(),
        binder -> binder.bind(GuiceLiquibaseConfig.class)
            .annotatedWith(GuiceLiquibaseConfiguration.class)
            .toInstance(
                GuiceLiquibaseConfig.Builder.of(LiquibaseConfig.Builder.of(dataSource).build())
                    .build())))
        .isInstanceOf(CreationException.class)
        .hasCauseInstanceOf(UnexpectedLiquibaseException.class)
        .hasRootCauseInstanceOf(NullPointerException.class);

    verify(dataSource).getConnection();
    verify(connection).close();
    verify(database).close();
    verifyNoMoreInteractions(connection, dataSource);
  }

  @Test
  void shouldPassEqualsAndHashCodeContractsInGuiceLiquibaseEngine() {
    EqualsVerifier.forClass(getGuiceLiquibaseEngineClass())
        .usingGetClass()
        .withPrefabValues(
            GuiceLiquibaseConfig.class,
            GuiceLiquibaseConfig.Builder.of().build(),
            GuiceLiquibaseConfig.Builder.of(
                LiquibaseConfig.Builder.of(Fixtures.SINGLE_DATA_SOURCE).build()).build())
        .withPrefabValues(
            Monitor.class,
            new Monitor(true),
            new Monitor(false))
        .verify();
  }

  @Test
  void verifyToStringInGuiceLiquibaseEngine() {
    ToStringVerifier.forClass(getGuiceLiquibaseEngineClass()).withClassName(NameStyle.SIMPLE_NAME)
        .withIgnoredFields("dataSource", "resourceAccessor").verify();
  }

  @SuppressWarnings("unchecked")
  private Class<GuiceLiquibaseModule.GuiceLiquibaseEngine> getGuiceLiquibaseEngineClass() {
    try {
      return (Class<GuiceLiquibaseModule.GuiceLiquibaseEngine>)
          Class.forName(
              "io.github.michaljonko.guiceliquibase.GuiceLiquibaseModule$GuiceLiquibaseEngine");
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
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
    private static final DataSource SINGLE_DATA_SOURCE = createJdbcDataSource();
    private static final DataSource MULTI_DATA_SOURCE = createJdbcDataSource();
    private static final Module DATA_SOURCE_MODULE = binder -> {
      GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder.of(
          LiquibaseConfig.Builder.of(createJdbcDataSource()).build()).build();
      binder.bind(GuiceLiquibaseConfig.class)
          .annotatedWith(GuiceLiquibaseConfiguration.class)
          .toInstance(config);
    };
    private static final Module SINGLE_DATA_SOURCE_MODULE = binder -> {
      GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
          .of(LiquibaseConfig.Builder.of(SINGLE_DATA_SOURCE).build()).build();
      binder.bind(GuiceLiquibaseConfig.class)
          .annotatedWith(GuiceLiquibaseConfiguration.class)
          .toInstance(config);
    };
    private static final Module MULTI_DATA_SOURCE_MODULE = binder -> {
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
          .of()
          .withLiquibaseConfig(
              LiquibaseConfig.Builder.of(MULTI_DATA_SOURCE)
                  .withChangeLogPath("liquibase/emptyChangeLog.xml")
                  .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                  .withDropFirst(false)
                  .build())
          .withLiquibaseConfig(
              LiquibaseConfig.Builder.of(MULTI_DATA_SOURCE)
                  .withChangeLogPath("liquibase/changeLogMulti.xml")
                  .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                  .withDropFirst(true)
                  .withParameters(ImmutableMap.of("testParameter", "testValue"))
                  .build())
          .build();
      binder.bind(GuiceLiquibaseConfig.class)
          .annotatedWith(GuiceLiquibaseConfiguration.class)
          .toInstance(config);
    };

    private static JDBCDataSource createJdbcDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID());
      dataSource.setUser("SA");
      return spy(dataSource);
    }
  }
}
