package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.multibindings.Multibinder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

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

import static java.util.Objects.requireNonNull;

public final class GuiceLiquibaseModule extends PrivateModule {

    private static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
            Key.get(GuiceLiquibaseConfig.class);

    protected void configure() {
        requireBinding(LIQUIBASE_CONFIG_KEY);
        Multibinder.newSetBinder(binder(), GuiceLiquibaseConfig.class)
                .addBinding().to(LIQUIBASE_CONFIG_KEY);
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
        private static volatile boolean INITIALIZED = false;
        private static volatile boolean UPDATED = false;
        private final Iterable<GuiceLiquibaseConfig> configs;
        private final ClassLoaderResourceAccessor resourceAccessor =
                new ClassLoaderResourceAccessor(this.getClass().getClassLoader());

        @Inject
        GuiceLiquibase(Set<GuiceLiquibaseConfig> configs) {
            LOGGER.info("Creating GuiceLiquibase for Liquibase "
                    + LiquibaseUtil.getBuildVersion());
            Preconditions.checkArgument(configs != null && !configs.isEmpty(),
                    "Injected GuiceLiquibaseConfig set cannot be null or empty.");
            this.configs = configs;
        }

        void executeUpdate() throws LiquibaseException {
            if (UPDATED) {
                LOGGER.warning("Liquibase update is already executed with success.");
                return;
            }
            if (!INITIALIZED) {
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
                INITIALIZED = true;
                for (GuiceLiquibaseConfig config : configs) {
                    LOGGER.info("Applying changes for " + config.toString());
                    Database database = null;
                    try {
                        Connection connection = requireNonNull(config.getDataSource(), "DataSource must be defined.")
                                .getConnection();
                        JdbcConnection jdbcConnection = new JdbcConnection(requireNonNull(connection,
                                "DataSource returns null connection instance."));
                        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
                        Liquibase liquibase = new Liquibase(config.getChangeLogPath(), resourceAccessor, database);
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
                UPDATED = true;
            } else {
                LOGGER.warning("GuiceLiquibase has been INITIALIZED and executed.");
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(configs, resourceAccessor);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GuiceLiquibase that = (GuiceLiquibase) o;
            return Objects.equals(configs, that.configs) &&
                    Objects.equals(resourceAccessor, that.resourceAccessor);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("configs", configs)
                    .add("resourceAccessor", resourceAccessor)
                    .toString();
        }
    }
}
