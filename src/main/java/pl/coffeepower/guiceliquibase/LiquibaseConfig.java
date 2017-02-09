package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.util.Objects;

import javax.sql.DataSource;

public class LiquibaseConfig {

  private final DataSource dataSource;
  private final String changeLogPath;
  private final ResourceAccessor resourceAccessor;
  private final boolean dropFirst;

  /**
   * Creates new <code>LiquiBaseConfig</code> for defined DataSource, changelog file path and its
   * resource accessor and dropFirst switch.
   * <br>
   * http://www.liquibase.org/documentation/databasechangelog.html
   *
   * @param dataSource       DataSource where Liquibase will be running
   * @param changeLogPath    Liquibase changelog with all changesets
   * @param resourceAccessor Liquibase {@link ResourceAccessor} used for changelog file loading
   * @param dropFirst        Liquibase switch to drop all schemes and data in database
   * @throws NullPointerException     when <code>dataSource</code>/<code>resourceAccessor</code> are
   *                                  null
   * @throws IllegalArgumentException when <code>changeLogPath</code> is null or empty
   */
  private LiquibaseConfig(DataSource dataSource, String changeLogPath,
                          ResourceAccessor resourceAccessor, boolean dropFirst) {
    this.dataSource = dataSource;
    this.resourceAccessor = resourceAccessor;
    this.changeLogPath = changeLogPath;
    this.dropFirst = dropFirst;
  }

  public final DataSource getDataSource() {
    return dataSource;
  }

  public final String getChangeLogPath() {
    return changeLogPath;
  }

  public final ResourceAccessor getResourceAccessor() {
    return resourceAccessor;
  }

  public final boolean isDropFirst() {
    return dropFirst;
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
        && Objects.equals(resourceAccessor, that.resourceAccessor)
        && Objects.equals(dropFirst, that.dropFirst);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.dataSource, this.changeLogPath, this.resourceAccessor, this.dropFirst);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("changeLogPath", changeLogPath)
        .toString();
  }

  /**
   * Builder for <code>GuiceLiquibaseConfig</code>.
   */
  public static final class Builder {

    public static final String DEFAULT_CHANGE_LOG_PATH = "liquibase/changeLog.xml";
    private DataSource dataSource;
    private String changeLogPath;
    private ResourceAccessor resourceAccessor;
    private boolean dropFirst;

    private Builder(DataSource dataSource) {
      this.dataSource = dataSource;
      this.changeLogPath = DEFAULT_CHANGE_LOG_PATH;
      this.resourceAccessor = new ClassLoaderResourceAccessor(this.getClass().getClassLoader());
      this.dropFirst = false;
    }

    public static Builder of(DataSource dataSource) {
      return new Builder(Preconditions.checkNotNull(dataSource, "dataSource must be defined."));
    }

    public final Builder withChangeLogPath(String value) {
      this.changeLogPath = value;
      return this;
    }

    public final Builder withResourceAccessor(ResourceAccessor value) {
      this.resourceAccessor = value;
      return this;
    }

    public final Builder withDropFirst(boolean value) {
      this.dropFirst = value;
      return this;
    }

    public final LiquibaseConfig build() {
      Preconditions.checkArgument(
          !Strings.isNullOrEmpty(this.changeLogPath), "changeLogPath must be defined.");
      Preconditions.checkNotNull(this.resourceAccessor, "resourceAccessor must be defined.");
      return new LiquibaseConfig(
          this.dataSource,
          this.changeLogPath,
          this.resourceAccessor,
          this.dropFirst);
    }
  }
}
