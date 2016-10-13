package pl.coffeepower.guiceliquibase;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Objects;

import javax.sql.DataSource;

public final class GuiceLiquibaseConfig {

    private final DataSource dataSource;
    private final String changeLogPath;

    private GuiceLiquibaseConfig(DataSource dataSource, String changeLogPath) {
        this.dataSource = dataSource;
        this.changeLogPath = changeLogPath;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getChangeLogPath() {
        return changeLogPath;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSource, changeLogPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuiceLiquibaseConfig that = (GuiceLiquibaseConfig) o;
        return Objects.equals(dataSource, that.dataSource) &&
                Objects.equals(changeLogPath, that.changeLogPath);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("changeLogPath", changeLogPath)
                .toString();
    }

    public static final class Builder {

        private String changeLogPath = "liquibase/changeLog.xml";
        private DataSource dataSource;

        private Builder(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public static Builder aConfig(DataSource dataSource) {
            return new Builder(dataSource);
        }

        public Builder withChangeLog(String changeLog) {
            this.changeLogPath = changeLog;
            return this;
        }

        public GuiceLiquibaseConfig build() {
            Preconditions.checkNotNull(dataSource, "DataSource must be defined.");
            Preconditions.checkArgument(
                    !Strings.isNullOrEmpty(changeLogPath), "changeLogPath cannot be empty.");
            return new GuiceLiquibaseConfig(dataSource, changeLogPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changeLogPath, dataSource);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Builder builder = (Builder) o;
            return Objects.equals(changeLogPath, builder.changeLogPath) &&
                    Objects.equals(dataSource, builder.dataSource);
        }
    }
}
