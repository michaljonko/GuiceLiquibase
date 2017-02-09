package pl.coffeepower.guiceliquibase;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
import liquibase.util.LiquibaseUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.coffeepower.guiceliquibase.annotation.GuiceLiquibase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;

import javax.inject.Inject;

public final class GuiceLiquibaseModule extends PrivateModule {

  private static final Key<GuiceLiquibaseConfig> LIQUIBASE_CONFIG_KEY =
      Key.get(GuiceLiquibaseConfig.class, GuiceLiquibase.class);

  protected void configure() {
    requireBinding(LIQUIBASE_CONFIG_KEY);
    bind(GuiceLiquibaseEngine.class).asEagerSingleton();
    requestInjection(this);
  }

  @Inject
  private void executeGuiceLiquibase(GuiceLiquibaseEngine guiceLiquibaseEngine) {
    try {
      checkNotNull(guiceLiquibaseEngine, "GuiceLiquibaseEngine has to be defined.").executeUpdate();
    } catch (LiquibaseException exception) {
      throw new UnexpectedLiquibaseException(exception);
    }
  }

  private static final class GuiceLiquibaseEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiceLiquibaseEngine.class);
    private final Monitor monitor = new Monitor();
    private final GuiceLiquibaseConfig config;
    private boolean updated = false;

    @Inject
    private GuiceLiquibaseEngine(@GuiceLiquibase GuiceLiquibaseConfig config) {
      LOGGER.info("Creating GuiceLiquibase for Liquibase {}", LiquibaseUtil.getBuildVersion());
      checkArgument(config != null, "Injected GuiceLiquibaseConfig cannot be null.");
      checkArgument(!config.getConfigs().isEmpty(), "Injected configuration set is empty.");
      this.config = config;
    }

    private void executeUpdate() throws LiquibaseException {
      monitor.enter();
      try {
        if (updated) {
          LOGGER.warn("Liquibase update is already executed with success.");
          return;
        }
        if (shouldExecuteLiquibaseUpdate()) {
          for (LiquibaseConfig liquibaseConfig : config.getConfigs()) {
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
      boolean shouldRun = liquibaseConfiguration
          .getConfiguration(GlobalConfiguration.class)
          .getShouldRun();
      if (!shouldRun) {
        LOGGER.warn("Cannot run Liquibase updates because {}  is set to false.",
            liquibaseConfiguration
                .describeValueLookupLogic(GlobalConfiguration.class, GlobalConfiguration.SHOULD_RUN)
        );
      }
      return shouldRun;
    }

    private void executeLiquibaseUpdate(LiquibaseConfig config) throws LiquibaseException {
      LOGGER.info("Applying changes for {}", config.toString());
      Database database = null;
      try {
        Connection connection =
            checkNotNull(config.getDataSource(), "DataSource must be defined.")
                .getConnection();
        JdbcConnection jdbcConnection = new JdbcConnection(
            checkNotNull(connection, "DataSource returns null connection instance."));
        database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
        Liquibase liquibase = new Liquibase(
            config.getChangeLogPath(),
            config.getResourceAccessor(),
            database);
        if (config.isDropFirst()) {
          liquibase.dropAll();
        }
        liquibase.update(
            new Contexts(Collections.emptyList()),
            new LabelExpression(Collections.emptyList()));
      } catch (SQLException exception) {
        LOGGER.error("Problem while SQL and JDBC calls.", exception);
        throw new DatabaseException(exception);
      } catch (LiquibaseException exception) {
        LOGGER.error("Problem while Liquibase calls.", exception);
        throw exception;
      } finally {
        if (database != null) {
          database.close();
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
      return Objects.hash(config);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("config", config)
          .toString();
    }
  }
}
