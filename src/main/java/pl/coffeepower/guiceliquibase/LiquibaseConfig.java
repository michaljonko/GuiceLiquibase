package pl.coffeepower.guiceliquibase;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sql.DataSource;

public final class LiquibaseConfig {

  private final DataSource dataSource;
  private final String changeLogPath;
  private final ResourceAccessor resourceAccessor;
  private final boolean dropFirst;
  private final Set<String> contexts;
  private final Set<String> labels;
  private final Map<String, String> parameters;

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
   * @param contexts         Liquibase contexts which will be used for changelog
   * @param labels           Liquibase labels
   * @param parameters       Liquibase parameters
   * @throws NullPointerException     when <code>dataSource</code>/<code>resourceAccessor</code> are
   *                                  null
   * @throws IllegalArgumentException when <code>changeLogPath</code> is null or empty
   */
  private LiquibaseConfig(DataSource dataSource,
                          String changeLogPath,
                          ResourceAccessor resourceAccessor,
                          boolean dropFirst,
                          Collection<String> contexts,
                          Collection<String> labels,
                          Map<String, String> parameters) {
    this.dataSource = Preconditions.checkNotNull(dataSource, "dataSource must be defined.");
    this.resourceAccessor =
        Preconditions.checkNotNull(resourceAccessor, "resourceAccessor must be defined.");
    this.changeLogPath =
        Preconditions.checkNotNull(changeLogPath, "changeLogPath must be defined.");
    this.dropFirst = dropFirst;
    this.contexts = ImmutableSet.copyOf(Preconditions.checkNotNull(contexts));
    this.labels = ImmutableSet.copyOf(Preconditions.checkNotNull(labels));
    this.parameters = ImmutableMap.copyOf(Preconditions.checkNotNull(parameters));
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

  public final Set<String> getContexts() {
    return contexts;
  }

  public final Set<String> getLabels() {
    return labels;
  }

  public Map<String, String> getParameters() {
    return parameters;
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
        && (dropFirst == that.dropFirst)
        && Objects.equals(contexts, that.contexts)
        && Objects.equals(labels, that.labels)
        && Objects.equals(parameters, that.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.dataSource, this.changeLogPath, this.resourceAccessor, this.dropFirst,
        this.contexts, this.labels, this.parameters);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("changeLogPath", changeLogPath)
        .add("dropFirst", dropFirst)
        .add("contexts", contexts)
        .add("labels", labels)
        .add("parameters", parameters)
        .toString();
  }

  /**
   * Builder for <code>GuiceLiquibaseConfig</code>.
   */
  public static final class Builder {

    private static final String DEFAULT_CHANGE_LOG_PATH = "liquibase/changeLog.xml";
    private static final Splitter CONTEXT_AND_LABEL_SPLITTER =
        Splitter.on(',').omitEmptyStrings().trimResults();
    private final DataSource dataSource;
    private String changeLogPath;
    private ResourceAccessor resourceAccessor;
    private boolean dropFirst;
    private Set<String> contexts;
    private Set<String> labels;
    private Map<String, String> parameters;

    private Builder(DataSource dataSource) {
      this.dataSource = dataSource;
      this.changeLogPath = DEFAULT_CHANGE_LOG_PATH;
      this.resourceAccessor = new ClassLoaderResourceAccessor(this.getClass().getClassLoader());
      this.dropFirst = false;
      this.contexts = new HashSet<>();
      this.labels = new HashSet<>();
      this.parameters = new HashMap<>();
    }

    public static Builder of(DataSource dataSource) {
      return new Builder(Preconditions.checkNotNull(dataSource, "dataSource must be defined."));
    }

    @VisibleForTesting
    static Builder of(Builder builder) {
      Builder copy = of(Preconditions.checkNotNull(builder, "builder cannot be null.").dataSource)
          .withChangeLogPath(builder.changeLogPath)
          .withDropFirst(builder.dropFirst)
          .withResourceAccessor(builder.resourceAccessor);
      builder.contexts.forEach(copy::addContext);
      builder.labels.forEach(copy::addLabel);
      builder.parameters.forEach(copy::addParameter);
      return copy;
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

    public final Builder addContext(String value) {
      this.contexts.addAll(CONTEXT_AND_LABEL_SPLITTER.splitToList(Strings.nullToEmpty(value)));
      return this;
    }

    public final Builder addLabel(String value) {
      this.labels.addAll(CONTEXT_AND_LABEL_SPLITTER.splitToList(Strings.nullToEmpty(value)));
      return this;
    }

    public final Builder addParameter(String key, String value) {
      if (!Strings.isNullOrEmpty(key)) {
        parameters.put(key, value);
      }
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
          this.dropFirst,
          this.contexts,
          this.labels,
          this.parameters);
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
      return dropFirst == builder.dropFirst
          && Objects.equals(dataSource, builder.dataSource)
          && Objects.equals(changeLogPath, builder.changeLogPath)
          && Objects.equals(resourceAccessor, builder.resourceAccessor)
          && Objects.equals(contexts, builder.contexts)
          && Objects.equals(labels, builder.labels)
          && Objects.equals(parameters, builder.parameters);
    }

    @Override
    public int hashCode() {
      return Objects.hash(dataSource);
    }
  }
}
