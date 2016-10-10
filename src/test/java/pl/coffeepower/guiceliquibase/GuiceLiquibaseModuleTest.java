package pl.coffeepower.guiceliquibase;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import pl.coffeepower.guiceliquibase.annotation.LiquibaseConfig;
import pl.coffeepower.guiceliquibase.annotation.LiquibaseDataSource;

import java.sql.DriverManager;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class GuiceLiquibaseModuleTest {

    private final Fixtures fixtures = new Fixtures();

    @Test(expected = CreationException.class)
    public void shouldThrowExceptionForNotDefinedImplementations() throws Exception {
        Guice.createInjector(new GuiceLiquibaseModule());
    }

    @Test
    public void shouldUpdateDatabase() throws Exception {
        Guice.createInjector(fixtures.hsqldbModule, new GuiceLiquibaseModule());
    }

    private static final class Fixtures {
        private final DataSource dataSource = mock(DataSource.class);
        private final GuiceLiquibaseConfig guiceLiquibaseConfig = GuiceLiquibaseConfig.Builder.aConfig().build();
        private final Module properModule = new AbstractModule() {

            @Override
            protected void configure() {
            }

            @Provides
            @LiquibaseDataSource
            private DataSource createDataSource() {
                return dataSource;
            }

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createConfig() {
                return guiceLiquibaseConfig;
            }
        };
        private final Module hsqldbModule = new AbstractModule() {

            @Override
            protected void configure() {
                try {
                    Class.forName("org.hsqldb.jdbc.JDBCDriver");
                } catch (ClassNotFoundException e) {
                    throw new java.lang.NoClassDefFoundError("Cannot find org.hsqldb.jdbc.JDBCDriver");
                }
            }

            @Provides
            @LiquibaseDataSource
            private DataSource createDataSource() {
                JDBCDataSource dataSource = new JDBCDataSource();
                dataSource.setDatabase("jdbc:hsqldb:mem:mymemdb");
                dataSource.setUser("SA");
                return dataSource;
            }

            @Provides
            @LiquibaseConfig
            private GuiceLiquibaseConfig createConfig() {
                return guiceLiquibaseConfig;
            }
        };
    }
}