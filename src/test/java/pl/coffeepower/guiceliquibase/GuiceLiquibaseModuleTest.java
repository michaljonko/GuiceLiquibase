package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class GuiceLiquibaseModuleTest {

    private final Fixtures fixtures = new Fixtures();

    @BeforeClass
    public static void beforeClass() throws Exception {
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
        } catch (ClassNotFoundException e) {
            throw new java.lang.NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
        }
    }

    @Test(expected = CreationException.class)
    public void shouldThrowExceptionForNotDefinedImplementations() throws Exception {
        Guice.createInjector(new GuiceLiquibaseModule());
    }

    @Test
    public void shouldExecuteLiquibaseUpdateWithSingleConfiguration() throws Exception {
        Injector injector = Guice.createInjector(new GuiceLiquibaseModule(),
                fixtures.singleDataSourceModule);

        DataSource dataSource = injector.getInstance(GuiceLiquibaseConfig.class)
                .getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(fixtures.getAllQuery)) {
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
        Guice.createInjector(new GuiceLiquibaseModule(),
                fixtures.firstDataSourceModule,
                fixtures.secondDataSourceModule);
    }

    private static final class Fixtures {
        private final Module singleDataSourceModule = new AbstractModule() {

            @Provides
            private GuiceLiquibaseConfig createConfig() {
                return GuiceLiquibaseConfig.Builder
                        .aConfig(createJdbcDataSource("jdbc:hsqldb:mem:singleDb"))
                        .build();
            }

            @Override
            protected void configure() {
            }
        };
        private final Module firstDataSourceModule = new AbstractModule() {

            @Provides
            private GuiceLiquibaseConfig createConfig() {
                return GuiceLiquibaseConfig.Builder
                        .aConfig(createJdbcDataSource("jdbc:hsqldb:mem:firstDb"))
                        .withChangeLog("liquibase/emptyChangeLog.xml")
                        .build();
            }

            @Override
            protected void configure() {
            }
        };
        private final Module secondDataSourceModule = new AbstractModule() {

            @Provides
            private GuiceLiquibaseConfig createConfig() {
                return GuiceLiquibaseConfig.Builder
                        .aConfig(createJdbcDataSource("jdbc:hsqldb:mem:secondDb"))
                        .withChangeLog("liquibase/changeLogMulti.xml")
                        .build();
            }

            @Override
            protected void configure() {
            }
        };
        private final String getAllQuery = "SELECT * FROM table_for_tests";
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
