package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.BeforeClass;
import org.junit.Test;

import pl.coffeepower.guiceliquibase.annotation.LiquibaseConfig;

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
    public void shouldExecuteLiquibaseUpdate() throws Exception {
        Injector injector = Guice.createInjector(fixtures.singleDataSourceModule, new GuiceLiquibaseModule());

        DataSource dataSource = injector.getInstance(Key.get(GuiceLiquibaseConfig.class, LiquibaseConfig.class))
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

//    @Test
//    public void shouldExecuteLiquibaseUpdateOnMultipleConfigurations() throws Exception {
//        Guice.createInjector(fixtures.multiDataSourceModule, new GuiceLiquibaseModule());
//    }

    private static final class Fixtures {
        private final String jdbcUrl = "jdbc:hsqldb:mem:testdb";
        private final String jdbcUser = "SA";
        private final Module singleDataSourceModule = new AbstractModule() {

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createConfig() {
                JDBCDataSource dataSource = new JDBCDataSource();
                dataSource.setDatabase(jdbcUrl);
                dataSource.setUser(jdbcUser);
                return GuiceLiquibaseConfig.Builder.aConfig(dataSource).build();
            }

            @Override
            protected void configure() {
            }
        };
        private final Module multiDataSourceModule = new AbstractModule() {

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createFirstConfig() {
                JDBCDataSource dataSource = new JDBCDataSource();
                dataSource.setDatabase(jdbcUrl + "1");
                dataSource.setUser(jdbcUser);
                return GuiceLiquibaseConfig.Builder.aConfig(dataSource)
                        .build();
            }

            @Provides
            private GuiceLiquibaseConfig createSecConfig() {
                JDBCDataSource dataSource = new JDBCDataSource();
                dataSource.setDatabase(jdbcUrl + "2");
                dataSource.setUser(jdbcUser);
                return GuiceLiquibaseConfig.Builder.aConfig(dataSource)
                        .withChangeLog("liquibase/emptyChangeLog.xml")
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
    }
}
