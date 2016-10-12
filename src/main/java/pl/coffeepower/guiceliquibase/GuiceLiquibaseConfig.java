package pl.coffeepower.guiceliquibase;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
    }
}
