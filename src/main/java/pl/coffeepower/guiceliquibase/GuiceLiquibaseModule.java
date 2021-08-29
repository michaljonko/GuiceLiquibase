package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.util.LiquibaseUtil;
import pl.coffeepower.guiceliquibase.annotation.LiquibaseConfig;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public final class GuiceLiquibaseModule extends PrivateModule {

    private static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
            Key.get(GuiceLiquibaseConfig.class, LiquibaseConfig.class);

    protected void configure() {
        requireBinding(LIQUIBASE_CONFIG_KEY);
//        Multibinder.newSetBinder(binder(), GuiceLiquibaseConfig.class)
//                .addBinding().to(Key.get(GuiceLiquibaseConfig.class, Annotation.class));
        bind(GuiceLiquibase.class).asEagerSingleton();
        requestInjection(this);
    }

    @Inject
    private void executeGuiceLiquibase(GuiceLiquibase guiceLiquibase) {
        try {
            requireNonNull(guiceLiquibase, "GuiceLiquibase instance cannot be null.")
                    .executeUpdate();
        } catch (LiquibaseException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }

    private static final class GuiceLiquibase {

        private static final Logger LOGGER = Logger.getLogger(GuiceLiquibase.class.getName());
        private final ClassLoaderResourceAccessor resourceAccessor =
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader());
        private final Monitor monitor = new Monitor();
        private final GuiceLiquibaseConfig config;
        private boolean updated = false;

        @Inject
        GuiceLiquibase(@LiquibaseConfig GuiceLiquibaseConfig config) {
            LOGGER.info("Creating GuiceLiquibase for Liquibase " + LiquibaseUtil.getBuildVersion());
            Preconditions.checkArgument(config != null, "Injected GuiceLiquibaseConfig cannot be null.");
            Preconditions.checkArgument(!config.getConfigs().isEmpty(), "Injected configuration set is empty.");
            this.config = config;
        }

        private void executeUpdate() throws LiquibaseException {
            monitor.enter();
            try {
                if (updated) {
                    LOGGER.warning("Liquibase update is already executed with success.");
                    return;
                }
                LiquibaseConfiguration liquibaseConfiguration =
                        LiquibaseConfiguration.getInstance();
                if (!liquibaseConfiguration.getConfiguration(GlobalConfiguration.class)
                        .getShouldRun()) {
                    String shouldRunValue = liquibaseConfiguration
                            .describeValueLookupLogic(GlobalConfiguration.class, GlobalConfiguration.SHOULD_RUN);
                    LOGGER.warning("Cannot run Liquibase updates because " + shouldRunValue
                            + " is set to false.");
                    return;
                }
                for (GuiceLiquibaseConfig.LiquibaseConfig liquibaseConfig : config.getConfigs()) {
                    LOGGER.info("Applying changes for " + liquibaseConfig.toString());
                    Database database = null;
                    try {
                        Connection connection = requireNonNull(liquibaseConfig.getDataSource(), "DataSource must be defined.")
                                .getConnection();
                        JdbcConnection jdbcConnection = new JdbcConnection(requireNonNull(connection,
                                "DataSource returns null connection instance."));
                        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
                        Liquibase liquibase = new Liquibase(liquibaseConfig.getChangeLogPath(), resourceAccessor, database);
                        liquibase.update(new Contexts(Collections.emptyList()),
                                new LabelExpression(Collections.emptyList()));
                    } catch (SQLException e) {
                        LOGGER.severe("Problem while SQL and JDBC calls.");
                        throw new DatabaseException(e);
                    } catch (LiquibaseException e) {
                        LOGGER.severe("Problem while Liquibase calls.");
                        throw e;
                    } finally {
                        if (database != null) {
                            database.close();
                        }
                    }
                }
                updated = true;
            } finally {
                monitor.leave();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GuiceLiquibase that = (GuiceLiquibase) o;
            return Objects.equals(config, that.config) &&
                    Objects.equals(resourceAccessor, that.resourceAccessor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(config, resourceAccessor);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("config", config)
                    .add("resourceAccessor", resourceAccessor)
                    .toString();
        }
    }
}
