package pl.coffeepower.guiceliquibase;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;

import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.inject.Singleton;
import javax.sql.DataSource;

import be.joengenduvel.java.verifiers.ToStringVerifier;

public class GuiceLiquibaseModuleTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Class.forName("org.hsqldb.jdbc.JDBCDriver");
    } catch (ClassNotFoundException exception) {
      throw new NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
    }
  }

  @Test
  public void shouldThrowExceptionForRequiredBinding() throws Exception {
    expectedException.expect(CreationException.class);
    expectedException.expectMessage(containsString("Unable to create injector"));
    expectedException.expectMessage(containsString("No implementation for " +
        "pl.coffeepower.guiceliquibase.GuiceLiquibaseConfig annotated with interface " +
        "pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase was bound."));

    Guice.createInjector(new GuiceLiquibaseModule());
  }

  @Test
  public void shouldThrowExceptionForNullConfigValue() throws Exception {
    expectedException.expect(CreationException.class);
    expectedException.expectMessage(containsString("Unable to create injector"));
    expectedException.expectMessage(containsString("Binding to null instances is not allowed."));

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
    expectedException.expectCause(instanceOf(IllegalArgumentException.class));

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
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(null);

    expectedException.expect(CreationException.class);
    expectedException.expectMessage(containsString("Unable to create injector"));
    expectedException.expectMessage(containsString("DataSource returns null connection instance."));
    expectedException.expectCause(instanceOf(NullPointerException.class));

    Guice.createInjector(new GuiceLiquibaseModule(),
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(GuiceLiquibaseConfig.class)
                .annotatedWith(GuiceLiquibase.class)
                .toInstance(GuiceLiquibaseConfig.Builder.of()
                    .withLiquibaseConfig(
                        LiquibaseConfig.Builder.of(dataSource).build())
                    .build());
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

  @Test
  public void shouldNotExecuteUpdateWhenShouldRunIsDisabled() throws Exception {
    DataSource dataSource = mock(DataSource.class);

    try {
      LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class)
          .setShouldRun(false);

      Guice.createInjector(
          new GuiceLiquibaseModule(),
          new AbstractModule() {

            @Override
            protected void configure() {
              bind(GuiceLiquibaseConfig.class)
                  .annotatedWith(GuiceLiquibase.class)
                  .toInstance(GuiceLiquibaseConfig.Builder
                      .of(LiquibaseConfig.Builder
                          .of(dataSource)
                          .build())
                      .build());
            }
          });

      verifyZeroInteractions(dataSource);
    } finally {
      LiquibaseConfiguration.getInstance().getConfiguration(GlobalConfiguration.class)
          .setShouldRun(true);
    }
  }

  @Test
  public void shouldThrowExceptionWhenProblemOccurredDuringProcessing() throws Exception {
    expectedException.expect(CreationException.class);
    expectedException.expectCause(instanceOf(UnexpectedLiquibaseException.class));

    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException());

    Guice.createInjector(
        new GuiceLiquibaseModule(),
        new AbstractModule() {

          @Override
          protected void configure() {
            bind(GuiceLiquibaseConfig.class)
                .annotatedWith(GuiceLiquibase.class)
                .toInstance(GuiceLiquibaseConfig.Builder
                    .of(LiquibaseConfig.Builder
                        .of(dataSource)
                        .build())
                    .build());
          }
        });

    verify(dataSource).getConnection();
    verifyNoMoreInteractions(dataSource);
  }

  @Test
  public void shouldPassEqualsAndHashCodeContractsInGuiceLiquibaseEngine() throws Exception {
    EqualsVerifier.forClass(getGuiceLiquibaseEngineClass())
        .usingGetClass()
        .withPrefabValues(
            GuiceLiquibaseConfig.class,
            Fixtures.EMPTY_CONFIG_SET,
            Fixtures.SINGLE_CONFIG_SET)
        .withPrefabValues(
            Monitor.class,
            new Monitor(true),
            new Monitor(false))
        .verify();
  }

  @Test
  public void verifyToStringInGuiceLiquibaseEngine() throws Exception {
    GuiceLiquibaseModule.LiquibaseEngine liquibaseEngine = Guice.createInjector(
        new AbstractModule() {

          @Override
          protected void configure() {
            bind(Key.get(GuiceLiquibaseConfig.class, GuiceLiquibase.class))
                .toInstance(Fixtures.SINGLE_CONFIG_SET);
            bind(GuiceLiquibaseModule.LiquibaseEngine.class)
                .to(getGuiceLiquibaseEngineClass());
          }
        })
        .getInstance(GuiceLiquibaseModule.LiquibaseEngine.class);

    ToStringVerifier.forClass(getGuiceLiquibaseEngineClass())
        .ignore("dataSource", "resourceAccessor")
        .containsClassName(liquibaseEngine);
  }

  private Class<GuiceLiquibaseModule.LiquibaseEngine> getGuiceLiquibaseEngineClass() {
    try {
      return (Class<GuiceLiquibaseModule.LiquibaseEngine>)
          Class.forName("pl.coffeepower.guiceliquibase.GuiceLiquibaseModule$GuiceLiquibaseEngine");
    } catch (ClassNotFoundException e) {
      fail(e.getMessage());
      throw new IllegalStateException(e);
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
    private static final GuiceLiquibaseConfig EMPTY_CONFIG_SET =
        GuiceLiquibaseConfig.Builder.of().build();
    private static final GuiceLiquibaseConfig SINGLE_CONFIG_SET =
        GuiceLiquibaseConfig.Builder
            .of(LiquibaseConfig.Builder.of(createJdbcDataSource()).build())
            .build();
    private static final Module SINGLE_DATA_SOURCE_MODULE = new AbstractModule() {

      @Provides
      @Singleton
      @GuiceLiquibase
      private GuiceLiquibaseConfig createConfig() {
        return SINGLE_CONFIG_SET;
      }

      @Override
      protected void configure() {
      }
    };
    private static final Module MULTI_DATA_SOURCE_MODULE = new AbstractModule() {

      private final JDBCDataSource dataSource = createJdbcDataSource();

      @Provides
      @Singleton
      @GuiceLiquibase
      private GuiceLiquibaseConfig createConfig() {
        ClassLoader classLoader = getClass().getClassLoader();
        return GuiceLiquibaseConfig.Builder
            .of()
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(dataSource)
                    .withChangeLogPath("liquibase/emptyChangeLog.xml")
                    .withResourceAccessor(new ClassLoaderResourceAccessor(classLoader))
                    .withDropFirst(false)
                    .build())
            .withLiquibaseConfig(
                LiquibaseConfig.Builder.of(dataSource)
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

    private static JDBCDataSource createJdbcDataSource() {
      JDBCDataSource dataSource = new JDBCDataSource();
      dataSource.setDatabase("jdbc:hsqldb:mem:" + UUID.randomUUID().toString());
      dataSource.setUser("SA");
      return spy(dataSource);
    }
  }
}
