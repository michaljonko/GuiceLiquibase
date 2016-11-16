package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

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
    private final ResourceAccessor resourceAccessor;
    private final int hash;

    /**
     * Creates new <code>LiquibaseConfig</code> for defined DataSource, default changelog file path
     * in ClassLoader resources (using {@link ClassLoaderResourceAccessor}).
     *
     * @param dataSource DataSource where Liquibase will be running
     * @throws NullPointerException when <code>dataSource</code> is null
     * @see #DEFAULT_CHANGE_LOG_PATH
     * @see ClassLoaderResourceAccessor
     */
    public LiquibaseConfig(DataSource dataSource) {
      this(dataSource, DEFAULT_CHANGE_LOG_PATH,
          new ClassLoaderResourceAccessor(LiquibaseConfig.class.getClassLoader()));
    }

    /**
     * Creates new <code>LiquiBaseConfig</code> for defined DataSource, changelog file path and
     * resource accessor.
     * <br>
     * http://www.liquibase.org/documentation/databasechangelog.html
     *
     * @param dataSource       DataSource where Liquibase will be running
     * @param changeLogPath    Liquibase changelog with all changesets
     * @param resourceAccessor Liquibase {@link ResourceAccessor} used for changelog file loading
     * @throws NullPointerException     when <code>dataSource</code>/<code>resourceAccessor</code>
     *                                  are null
     * @throws IllegalArgumentException when <code>changeLogPath</code> is null or empty
     */
    public LiquibaseConfig(DataSource dataSource, String changeLogPath,
                           ResourceAccessor resourceAccessor) {
      this.dataSource =
          Preconditions.checkNotNull(dataSource, "dataSource must be defined.");
      this.resourceAccessor =
          Preconditions.checkNotNull(resourceAccessor, "resourceAccessor cannot be null.");
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(changeLogPath), "changeLogPath must be defined.");
      this.changeLogPath = changeLogPath;
      this.hash = Objects.hash(this.dataSource, this.changeLogPath, this.resourceAccessor);
    }

    public DataSource getDataSource() {
      return dataSource;
    }

    public String getChangeLogPath() {
      return changeLogPath;
    }

    public ResourceAccessor getResourceAccessor() {
      return resourceAccessor;
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
          && Objects.equals(changeLogPath, that.changeLogPath)
          && Objects.equals(resourceAccessor, that.resourceAccessor);
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

    private final Set<LiquibaseConfig> configs;

    private Builder() {
      configs = new HashSet<>();
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
     * @return itself
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
     * @return itself
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
