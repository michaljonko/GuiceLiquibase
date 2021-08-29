package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

public final class GuiceLiquibaseConfig {

  private final ImmutableSet<LiquibaseConfig> configs;

  private GuiceLiquibaseConfig(Collection<LiquibaseConfig> configs) {
    this.configs = ImmutableSet.copyOf(configs);
  }

  public Set<LiquibaseConfig> getConfigs() {
    return configs;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    GuiceLiquibaseConfig that = (GuiceLiquibaseConfig) obj;
    return Objects.equals(configs, that.configs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(configs);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("configs", configs)
        .toString();
  }

  public static final class LiquibaseConfig {

    static final String DEFAULT_CHANGE_LOG_PATH = "liquibase/changeLog.xml";
    private final DataSource dataSource;
    private final String changeLogPath;
    private final int hash;

    /**
     * Creates new <code>LiquibaseConfig</code> for passed DataSource and default changelog file
     * location.
     *
     * @param dataSource DataSource where Liquibase will be running
     * @throws NullPointerException when <code>dataSource</code> is null
     * @see #DEFAULT_CHANGE_LOG_PATH
     */
    public LiquibaseConfig(DataSource dataSource) {
      this(dataSource, DEFAULT_CHANGE_LOG_PATH);
    }

    /**
     * Creates new <code>LiquiBaseConfig</code> for passed DataSource and changelog file location.
     *
     * @param dataSource    DataSource where Liquibase will be running
     * @param changeLogPath Liquibase changelog with all changesets
     * @throws NullPointerException     when <code>dataSource</code> is null
     * @throws IllegalArgumentException when <code>changeLogPath</code> is null or empty
     * @link http://www.liquibase.org/documentation/databasechangelog.html
     */
    public LiquibaseConfig(DataSource dataSource, String changeLogPath) {
      this.dataSource = Preconditions.checkNotNull(dataSource, "dataSource must be defined.");
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(changeLogPath), "changeLogPath must be defined.");
      this.changeLogPath = changeLogPath;
      this.hash = Objects.hash(this.dataSource, this.changeLogPath);
    }

    public DataSource getDataSource() {
      return dataSource;
    }

    public String getChangeLogPath() {
      return changeLogPath;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      LiquibaseConfig that = (LiquibaseConfig) obj;
      return Objects.equals(dataSource, that.dataSource)
          && Objects.equals(changeLogPath, that.changeLogPath);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("changeLogPath", changeLogPath)
          .toString();
    }
  }

  /**
   * Builder for <code>GuiceLiquibaseConfig</code>.
   */
  public static final class Builder {

    private final Set<LiquibaseConfig> configs = new HashSet<>();

    private Builder() {
    }

    /**
     * Creates new builder for <code>GuiceLiquibaseConfig</code>.
     *
     * @return new Builder instance
     */
    public static Builder createConfigSet() {
      return new Builder();
    }

    /**
     * Adds <code>LiquibaseConfig</code> instance to the set of configuration.
     *
     * @param config <code>LiquibaseConfig</code> object
     */
    public Builder withLiquibaseConfig(LiquibaseConfig config) {
      Preconditions.checkNotNull(config);
      configs.add(config);
      return this;
    }

    /**
     * Adds <code>LiquibaseConfig</code> instances to the set of configuration.
     *
     * @param configs <code>LiquibaseConfig</code> object
     */
    public Builder withLiquibaseConfigs(Collection<LiquibaseConfig> configs) {
      Preconditions.checkNotNull(configs);
      this.configs.addAll(configs);
      return this;
    }

    /**
     * Creates new <code>GuiceLiquibaseConfig</code> object.
     *
     * @return new <code>GuiceLiquibaseConfig</code> object
     */
    public GuiceLiquibaseConfig build() {
      return new GuiceLiquibaseConfig(configs);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Builder builder = (Builder) obj;
      return Objects.equals(configs, builder.configs);
    }

    @Override
    public int hashCode() {
      return Objects.hash(configs);
    }
  }
}
