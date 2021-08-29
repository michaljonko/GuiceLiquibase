package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class GuiceLiquibaseModule extends PrivateModule {

    private static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
            Key.get(GuiceLiquibaseConfig.class, LiquibaseConfig.class);

    protected void configure() {
        requireBinding(LIQUIBASE_CONFIG_KEY);
        bind(GuiceLiquibase.class).asEagerSingleton();
        requestInjection(this);
    }

    @Inject
    private void executeGuiceLiquibase(GuiceLiquibase guiceLiquibase) {
        try {
            guiceLiquibase.executeUpdate();
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
        private GuiceLiquibase(@LiquibaseConfig GuiceLiquibaseConfig config) {
            LOGGER.info("Creating GuiceLiquibase for Liquibase " + LiquibaseUtil.getBuildVersion());
            checkArgument(config != null, "Injected GuiceLiquibaseConfig cannot be null.");
            checkArgument(!config.getConfigs().isEmpty(), "Injected configuration set is empty.");
            this.config = config;
        }

        void executeUpdate() throws LiquibaseException {
            monitor.enter();
            try {
                if (updated) {
                    LOGGER.warning("Liquibase update is already executed with success.");
                    return;
                }
                if (shouldExecuteLiquibaseUpdate()) {
                    for (GuiceLiquibaseConfig.LiquibaseConfig liquibaseConfig : config.getConfigs()) {
                        executeLiquibaseUpdate(liquibaseConfig);
                    }
                    updated = true;
                }
            } finally {
                monitor.leave();
            }
        }

        private boolean shouldExecuteLiquibaseUpdate() {
            LiquibaseConfiguration liquibaseConfiguration = LiquibaseConfiguration.getInstance();
            boolean shouldRun = liquibaseConfiguration.getConfiguration(GlobalConfiguration.class).getShouldRun();
            if (!shouldRun) {
                LOGGER.warning("Cannot run Liquibase updates because "
                        + liquibaseConfiguration.describeValueLookupLogic(GlobalConfiguration.class, GlobalConfiguration.SHOULD_RUN)
                        + " is set to false.");
            }
            return shouldRun;
        }

        private void executeLiquibaseUpdate(GuiceLiquibaseConfig.LiquibaseConfig liquibaseConfig) throws LiquibaseException {
            LOGGER.info("Applying changes for " + liquibaseConfig.toString());
            Database database = null;
            try {
                Connection connection =
                        checkNotNull(liquibaseConfig.getDataSource(), "DataSource must be defined.").getConnection();
                JdbcConnection jdbcConnection = new JdbcConnection(checkNotNull(connection,
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
