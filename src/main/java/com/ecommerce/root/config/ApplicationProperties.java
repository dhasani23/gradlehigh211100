package com.ecommerce.root.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties configuration class that reads from application.yml or application.properties
 * Handles all configuration properties for the application.
 */
@Component
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final Database database = new Database();

    public Database getDatabase() {
        return database;
    }

    /**
     * Database specific configuration properties
     */
    public static class Database {
        private String url;
        private String username;
        private String password;
        private String type = "mysql"; // default database type
        private String dialect;
        private boolean showSql = false;
        private boolean generateDdl = false;
        private String ddlAuto;
        private boolean readOnly = false;
        private boolean cacheEnabled = false;
        private boolean debugEnabled = false;
        private String isolationLevel;
        private boolean loadTestingMode = false;
        private boolean optimisticLocking = false;
        private boolean setTimezone = false;
        private boolean statisticsEnabled = false;
        
        // Read replica configuration
        private boolean replicaEnabled = false;
        private String replicaUrl;
        private String replicaUsername;
        private String replicaPassword;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDialect() {
            return dialect;
        }

        public void setDialect(String dialect) {
            this.dialect = dialect;
        }

        public boolean isShowSql() {
            return showSql;
        }

        public void setShowSql(boolean showSql) {
            this.showSql = showSql;
        }

        public boolean isGenerateDdl() {
            return generateDdl;
        }

        public void setGenerateDdl(boolean generateDdl) {
            this.generateDdl = generateDdl;
        }

        public String getDdlAuto() {
            return ddlAuto;
        }

        public void setDdlAuto(String ddlAuto) {
            this.ddlAuto = ddlAuto;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }

        public boolean isCacheEnabled() {
            return cacheEnabled;
        }

        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
        }

        public boolean isDebugEnabled() {
            return debugEnabled;
        }

        public void setDebugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
        }

        public String getIsolationLevel() {
            return isolationLevel;
        }

        public void setIsolationLevel(String isolationLevel) {
            this.isolationLevel = isolationLevel;
        }

        public boolean isLoadTestingMode() {
            return loadTestingMode;
        }

        public void setLoadTestingMode(boolean loadTestingMode) {
            this.loadTestingMode = loadTestingMode;
        }

        public boolean isOptimisticLocking() {
            return optimisticLocking;
        }

        public void setOptimisticLocking(boolean optimisticLocking) {
            this.optimisticLocking = optimisticLocking;
        }

        public boolean isSetTimezone() {
            return setTimezone;
        }

        public void setSetTimezone(boolean setTimezone) {
            this.setTimezone = setTimezone;
        }

        public boolean isStatisticsEnabled() {
            return statisticsEnabled;
        }

        public void setStatisticsEnabled(boolean statisticsEnabled) {
            this.statisticsEnabled = statisticsEnabled;
        }

        public boolean isReplicaEnabled() {
            return replicaEnabled;
        }

        public void setReplicaEnabled(boolean replicaEnabled) {
            this.replicaEnabled = replicaEnabled;
        }

        public String getReplicaUrl() {
            return replicaUrl;
        }

        public void setReplicaUrl(String replicaUrl) {
            this.replicaUrl = replicaUrl;
        }

        public String getReplicaUsername() {
            return replicaUsername;
        }

        public void setReplicaUsername(String replicaUsername) {
            this.replicaUsername = replicaUsername;
        }

        public String getReplicaPassword() {
            return replicaPassword;
        }

        public void setReplicaPassword(String replicaPassword) {
            this.replicaPassword = replicaPassword;
        }
    }
}