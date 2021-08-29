package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class GuiceLiquibaseConfig implements Serializable {

    private static final long serialVersionUID = -7299278874840015843L;
    private final ImmutableSet<LiquibaseConfig> configs;

    private GuiceLiquibaseConfig(Collection<LiquibaseConfig> configs) {
        this.configs = ImmutableSet.copyOf(configs);
    }

    public Set<LiquibaseConfig> getConfigs() {
        return configs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuiceLiquibaseConfig that = (GuiceLiquibaseConfig) o;
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

    public static final class LiquibaseConfig implements Serializable {

        public static final String DEFAULT_CHANGE_LOG_PATH = "liquibase/changeLog.xml";
        private static final long serialVersionUID = 896869151897407896L;
        private final DataSource dataSource;
        private final String changeLogPath;
        private final int hash;

        public LiquibaseConfig(DataSource dataSource, String changeLogPath) {
            this.dataSource = Preconditions.checkNotNull(dataSource, "dataSource must be defined.");
            this.changeLogPath = Strings.isNullOrEmpty(changeLogPath) ? DEFAULT_CHANGE_LOG_PATH : changeLogPath;
            this.hash = Objects.hash(this.dataSource, this.changeLogPath);
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public String getChangeLogPath() {
            return changeLogPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LiquibaseConfig that = (LiquibaseConfig) o;
            return Objects.equals(dataSource, that.dataSource) &&
                    Objects.equals(changeLogPath, that.changeLogPath);
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

    public static final class Builder {

        private final Set<LiquibaseConfig> configs = new HashSet<>();

        private Builder() {
        }

        public static Builder aConfigSet() {
            return new Builder();
        }

        public Builder withLiquibaseConfig(LiquibaseConfig config) {
            Preconditions.checkNotNull(config);
            configs.add(config);
            return this;
        }

        public Builder withLiquibaseConfigs(Collection<LiquibaseConfig> configs) {
            Preconditions.checkNotNull(configs);
            this.configs.addAll(configs);
            return this;
        }

        public GuiceLiquibaseConfig build() {
            return new GuiceLiquibaseConfig(configs);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Builder builder = (Builder) o;
            return Objects.equals(configs, builder.configs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(configs);
        }
    }
}
