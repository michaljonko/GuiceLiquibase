package pl.coffeepower.guiceliquibase;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Stage;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.configuration.GlobalConfiguration;
import liquibase.configuration.LiquibaseConfiguration;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.util.LiquibaseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibaseConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public final class GuiceLiquibaseModule extends AbstractModule {

  private static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
      Key.get(GuiceLiquibaseConfig.class, GuiceLiquibaseConfiguration.class);

  @Override
  protected void configure() {
    requireBinding(LIQUIBASE_CONFIG_KEY);
    bind(LiquibaseEngine.class).to(GuiceLiquibaseEngine.class).asEagerSingleton();
    requestInjection(this);
  }

  @Inject
  private void executeGuiceLiquibaseEngine(LiquibaseEngine guiceLiquibaseEngine) {
    try {
      checkNotNull(guiceLiquibaseEngine, "LiquibaseEngine has to be defined.").process();
    } catch (LiquibaseException exception) {
      throw new UnexpectedLiquibaseException(exception);
    }
  }

  interface LiquibaseEngine {

    void process() throws LiquibaseException;
  }

  private static final class GuiceLiquibaseEngine implements LiquibaseEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceLiquibaseEngine.class);
    private final Monitor monitor;
    private final GuiceLiquibaseConfig config;
    private final AtomicBoolean updated;

    @Inject
    private GuiceLiquibaseEngine(@GuiceLiquibaseConfiguration GuiceLiquibaseConfig config) {
      LOGGER.info("Creating GuiceLiquibase for Liquibase {}", LiquibaseUtil.getBuildVersion());
      checkArgument(!config.getConfigs().isEmpty(), "Injected configuration set is empty.");
      this.config = config;
      this.updated = new AtomicBoolean(false);
      this.monitor = new Monitor(true);
    }

    @Override
    public void process() throws LiquibaseException {
      if (monitor.tryEnter()) {
        try {
          if (updated.get()) {
            LOGGER.warn("Liquibase update has been already executed.");
          } else if (shouldExecuteLiquibaseUpdate()) {
            for (LiquibaseConfig liquibaseConfig : config.getConfigs()) {
              executeLiquibaseUpdate(liquibaseConfig);
            }
          }
        } finally {
          updated.compareAndSet(false, true);
          monitor.leave();
        }
      } else {
        LOGGER.warn("Liquibase update is running.");
      }
    }

    private boolean shouldExecuteLiquibaseUpdate() {
      LiquibaseConfiguration liquibaseConfiguration = LiquibaseConfiguration.getInstance();
      boolean shouldRun = liquibaseConfiguration
          .getConfiguration(GlobalConfiguration.class)
          .getShouldRun();
      if (!shouldRun) {
        LOGGER.warn("Cannot run Liquibase updates because {} is set to false.",
            liquibaseConfiguration
                .describeValueLookupLogic(GlobalConfiguration.class, GlobalConfiguration.SHOULD_RUN)
        );
      }
      return shouldRun;
    }

    private void executeLiquibaseUpdate(LiquibaseConfig config) throws LiquibaseException {
      LOGGER.info("Applying changes for {}", config.toString());
      Connection connection = null;
      Database database = null;
      try {
        connection = checkNotNull(config.getDataSource(), "DataSource must be defined.")
            .getConnection();
        DatabaseConnection databaseConnection = new JdbcConnection(
            checkNotNull(connection, "DataSource returns null connection instance."));
        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
            checkNotNull(databaseConnection, "DatabaseConnection created from DataSource "
                + "cannot be null."));
        Liquibase liquibase = new Liquibase(
            config.getChangeLogPath(),
            config.getResourceAccessor(),
            database);
        checkNotNull(config.getParameters(), "Parameters map cannot be null.")
            .forEach(liquibase::setChangeLogParameter);
        if (config.isDropFirst()) {
          liquibase.dropAll();
        }
        liquibase.update(
            new Contexts(config.getContexts()),
            new LabelExpression(config.getLabels()));
      } catch (SQLException exception) {
        LOGGER.error("Problem during SQL and JDBC calls.", exception);
        throw new DatabaseException(exception);
      } catch (LiquibaseException exception) {
        LOGGER.error("Problem during Liquibase calls.", exception);
        throw exception;
      } finally {
        if (database != null) {
          database.close();
        } else if (connection != null) {
          try {
            connection.rollback();
            connection.close();
          } catch (SQLException exception) {
            LOGGER.error("Problem during closing connection.", exception);
          }
        }
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      GuiceLiquibaseEngine that = (GuiceLiquibaseEngine) obj;
      return Objects.equals(config, that.config)
          && Objects.equals(updated, that.updated)
          && Objects.equals(monitor, that.monitor);
    }

    @Override
    public int hashCode() {
      return Objects.hash(config, updated, monitor);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("config", config)
          .add("monitor", monitor)
          .add("updated", updated)
          .toString();
    }
  }
}
