package pl.coffeepower.guiceliquibase;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;

import com.google.common.util.concurrent.Monitor;
import com.google.inject.AbstractModule;
import com.google.inject.Key;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.integration.commandline.LiquibaseCommandLineConfiguration;
import liquibase.util.LiquibaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibaseConfiguration;


public final class GuiceLiquibaseModule extends AbstractModule {

  static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
      Key.get(GuiceLiquibaseConfig.class, GuiceLiquibaseConfiguration.class);

  @Override
  protected void configure() {
    requireBinding(LIQUIBASE_CONFIG_KEY);
    bind(GuiceLiquibaseEngine.class).asEagerSingleton();
    requestInjection(this);
  }

  @Inject
  private void executeGuiceLiquibaseEngine(GuiceLiquibaseEngine guiceLiquibaseEngine) {
    checkNotNull(guiceLiquibaseEngine, "LiquibaseEngine has to be defined.")
        .process();
  }

  static final class GuiceLiquibaseEngine {

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

    void process() {
      if (monitor.tryEnter()) {
        try {
          if (updated.get()) {
            LOGGER.warn("Liquibase update has been already executed.");
          } else if (shouldExecuteLiquibaseUpdate()) {
            config.getConfigs().forEach(this::executeLiquibaseUpdate);
          }
        } finally {
          updated.getAndSet(true);
          monitor.leave();
        }
      } else {
        LOGGER.warn("Liquibase update is running.");
      }
    }

    private boolean shouldExecuteLiquibaseUpdate() {
      boolean globalShouldRun = LiquibaseCommandLineConfiguration.SHOULD_RUN.getCurrentValue();
      if (!globalShouldRun) {
        LOGGER.warn("Cannot run Liquibase updates because {} is set to false (global setting).",
            LiquibaseCommandLineConfiguration.SHOULD_RUN.getDescription()
        );
      }
      return globalShouldRun;
    }

    private void executeLiquibaseUpdate(LiquibaseConfig config) {
      if (!config.shouldRun()) {
        LOGGER.info("Liquibase did not run on config with changeLogPath {} because LiquibaseConfig.shouldRun was set to false.",
                config.getChangeLogPath());
        return;
      }
      LOGGER.info("Applying changes for {}", config);

      Connection connection = null;
      Database database = null;
      Liquibase liquibase = null;
      try {
        connection = checkNotNull(config.getDataSource(), "DataSource must be defined.")
            .getConnection();
        database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(checkNotNull(connection, "DataSource returns null connection instance.")));
        liquibase = new Liquibase(
            config.getChangeLogPath(),
            config.getResourceAccessor(),
            database);
        checkNotNull(config.getParameters(), "Parameters map cannot be null.")
            .forEach(liquibase::setChangeLogParameter);
        if (config.dropFirst()) {
          liquibase.dropAll();
        }
        liquibase.update(
            new Contexts(config.getContexts()),
            new LabelExpression(config.getLabels()));
      } catch (SQLException exception) {
        LOGGER.error("Problem during SQL and JDBC calls.", exception);
        throw new UnexpectedLiquibaseException(exception);
      } catch (LiquibaseException exception) {
        LOGGER.error("Problem during Liquibase calls.", exception);
        throw new UnexpectedLiquibaseException(exception);
      } finally {
        if (nonNull(liquibase)) {
          try {
            liquibase.close();
          } catch (Exception exception) {
            LOGGER.error("Problem during liquibase.close() call.", exception);
          }
        }
        if (nonNull(database) && nonNull(database.getConnection())) {
          try {
            if (!database.getConnection().isClosed()) {
              database.close();
            }
          } catch (DatabaseException exception) {
            LOGGER.error("Problem during database.close() call.", exception);
          }
        }
        if (nonNull(connection)) {
          try {
            connection.close();
          } catch (SQLException exception) {
            LOGGER.error("Problem during connection.close() call.", exception);
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
      return new StringJoiner(", ", GuiceLiquibaseEngine.class.getSimpleName() + "[", "]")
          .add("monitor=" + monitor)
          .add("config=" + config)
          .add("updated=" + updated)
          .toString();
    }
  }
}
