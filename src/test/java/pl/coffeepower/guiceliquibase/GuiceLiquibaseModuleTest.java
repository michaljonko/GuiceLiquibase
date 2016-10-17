package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.coffeepower.guiceliquibase.annotation.LiquibaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuiceLiquibaseModuleTest {

    private final Fixtures fixtures = new Fixtures();
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private DataSource dataSource;

    @BeforeClass
    public static void beforeClass() throws Exception {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new java.lang.NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
        }
    }

    @Before
    public void setUp() throws Exception {
        try (Connection connection = Fixtures.createJdbcDataSource("jdbc:hsqldb:mem:memdb").getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK")) {
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
                        bind(GuiceLiquibaseConfig.class).annotatedWith(LiquibaseConfig.class).toInstance(null);
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
                        bind(GuiceLiquibaseConfig.class).annotatedWith(LiquibaseConfig.class).toInstance(
                                GuiceLiquibaseConfig.Builder
                                        .aConfigSet()
                                        .build());
                    }
                });
    }

    @Test
    public void shouldThrowExceptionForNotDefinedDataSourceConnection() throws Exception {
        when(dataSource.getConnection()).thenReturn(null);
        GuiceLiquibaseConfig config = GuiceLiquibaseConfig.Builder
                .aConfigSet()
                .withLiquibaseConfig(new GuiceLiquibaseConfig.LiquibaseConfig(dataSource))
                .build();

        expectedException.expect(CreationException.class);
        expectedException.expectMessage(containsString("DataSource returns null connection instance."));

        Guice.createInjector(new GuiceLiquibaseModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(GuiceLiquibaseConfig.class).annotatedWith(LiquibaseConfig.class).toInstance(config);
                    }
                });
    }

    @Test
    public void shouldExecuteLiquibaseUpdateWithSingleConfiguration() throws Exception {
        Injector injector = Guice.createInjector(
                new GuiceLiquibaseModule(),
                fixtures.singleDataSourceModule);

        DataSource dataSource = injector.getInstance(Key.get(GuiceLiquibaseConfig.class, LiquibaseConfig.class))
                .getConfigs().iterator().next().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(fixtures.getAllFromTableForTestQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    assertThat(resultSet.next(), is(true));
                    assertThat(resultSet.getInt(fixtures.idColumnName), is(fixtures.expectedId));
                    assertThat(resultSet.getString(fixtures.nameColumnName), is(fixtures.expectedName));
                    assertThat(resultSet.getBoolean(fixtures.activeColumnName), is(fixtures.expectedActive));
                    assertThat(resultSet.next(), is(false));
                }
            }
        }
    }

    @Test
    public void shouldExecuteLiquibaseUpdateWithMultipleConfigurations() throws Exception {
        Injector injector = Guice.createInjector(
                new GuiceLiquibaseModule(),
                fixtures.multiDataSourceModule);

        DataSource dataSource = injector.getInstance(Key.get(GuiceLiquibaseConfig.class, LiquibaseConfig.class))
                .getConfigs().iterator().next().getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(fixtures.getAllFromTableForMultiTestsQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    assertThat(resultSet.next(), is(true));
                    assertThat(resultSet.getInt(fixtures.idColumnName), is(fixtures.expectedId));
                    assertThat(resultSet.getString(fixtures.nameColumnName), is(fixtures.expectedName));
                    assertThat(resultSet.next(), is(false));
                }
            }
        }
    }

    private static final class Fixtures {
        private final Module singleDataSourceModule = new AbstractModule() {

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createConfig() {
                return GuiceLiquibaseConfig.Builder
                        .aConfigSet()
                        .withLiquibaseConfig(new GuiceLiquibaseConfig.LiquibaseConfig(
                                createJdbcDataSource("jdbc:hsqldb:mem:memdb")))
                        .build();
            }

            @Override
            protected void configure() {
            }
        };
        private final Module multiDataSourceModule = new AbstractModule() {

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createConfig() {
                return GuiceLiquibaseConfig.Builder
                        .aConfigSet()
                        .withLiquibaseConfig(new GuiceLiquibaseConfig.LiquibaseConfig(
                                createJdbcDataSource("jdbc:hsqldb:mem:memdb"), "liquibase/emptyChangeLog.xml"))
                        .withLiquibaseConfig(new GuiceLiquibaseConfig.LiquibaseConfig(
                                createJdbcDataSource("jdbc:hsqldb:mem:memdb"), "liquibase/changeLogMulti.xml"))
                        .build();
            }

            @Override
            protected void configure() {
            }
        };
        private final String getAllFromTableForTestQuery = "SELECT * FROM table_for_test";
        private final String getAllFromTableForMultiTestsQuery = "SELECT * FROM table_for_multi_test";
        private final String idColumnName = "id";
        private final String nameColumnName = "name";
        private final String activeColumnName = "active";
        private final int expectedId = 1;
        private final String expectedName = "test";
        private final boolean expectedActive = true;

        private static JDBCDataSource createJdbcDataSource(String jdbcUrl) {
            JDBCDataSource dataSource = new JDBCDataSource();
            dataSource.setDatabase(jdbcUrl);
            dataSource.setUser("SA");
            return dataSource;
        }
    }
}
