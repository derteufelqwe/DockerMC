package de.derteufelqwe.plugin.messages;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

public class LogDriver {

    public static class RCapabilities implements Serializable {

        public RCapabilities() {
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof RCapabilities)) return false;
            final RCapabilities other = (RCapabilities) o;
            if (!other.canEqual((Object) this)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof RCapabilities;
        }

        public int hashCode() {
            int result = 1;
            return result;
        }

        public String toString() {
            return "LogDriver.RCapabilities()";
        }
    }

    public static class Capabilities implements Serializable {

        @SerializedName("ReadLogs")
        private boolean readLogs = false;

        public Capabilities(boolean readLogs) {
            this.readLogs = readLogs;
        }

        public Capabilities() {
        }

        public boolean isReadLogs() {
            return this.readLogs;
        }

        public void setReadLogs(boolean readLogs) {
            this.readLogs = readLogs;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof Capabilities)) return false;
            final Capabilities other = (Capabilities) o;
            if (!other.canEqual((Object) this)) return false;
            if (this.isReadLogs() != other.isReadLogs()) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof Capabilities;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + (this.isReadLogs() ? 79 : 97);
            return result;
        }

        public String toString() {
            return "LogDriver.Capabilities(readLogs=" + this.isReadLogs() + ")";
        }
    }


    public static class RStartLogging implements Serializable {

        @SerializedName("File")
        private String file;

        @SerializedName("Info")
        private Info info;

        public RStartLogging(String file, Info info) {
            this.file = file;
            this.info = info;
        }

        public RStartLogging() {
        }

        public String getFile() {
            return this.file;
        }

        public Info getInfo() {
            return this.info;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public void setInfo(Info info) {
            this.info = info;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof RStartLogging)) return false;
            final RStartLogging other = (RStartLogging) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$file = this.getFile();
            final Object other$file = other.getFile();
            if (this$file == null ? other$file != null : !this$file.equals(other$file)) return false;
            final Object this$info = this.getInfo();
            final Object other$info = other.getInfo();
            if (this$info == null ? other$info != null : !this$info.equals(other$info)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof RStartLogging;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $file = this.getFile();
            result = result * PRIME + ($file == null ? 43 : $file.hashCode());
            final Object $info = this.getInfo();
            result = result * PRIME + ($info == null ? 43 : $info.hashCode());
            return result;
        }

        public String toString() {
            return "LogDriver.RStartLogging(file=" + this.getFile() + ", info=" + this.getInfo() + ")";
        }


        public static class Info implements Serializable {

            @SerializedName("Config")
            private Map<String, String> config;

            @SerializedName("ContainerID")
            private String containerID;

            @SerializedName("ContainerName")
            private String containerName;

            @SerializedName("ContainerEntrypoint")
            private String containerEntrypoint;

            @SerializedName("ContainerArgs")
            private List<String> containerArgs;

            @SerializedName("ContainerImageID")
            private String containerImageID;

            @SerializedName("ContainerImageName")
            private String containerImageName;

            @SerializedName("ContainerCreated")
            private Timestamp containerCreated;

            @SerializedName("ContainerEnv")
            private List<String> containerEnv;

            @SerializedName("ContainerLabels")
            private Map<String, String> containerLabels;

            @SerializedName("LogPath")
            private String logPath;

            @SerializedName("DaemonName")
            private String daemonName;

            public Info(Map<String, String> config, String containerID, String containerName, String containerEntrypoint, List<String> containerArgs, String containerImageID, String containerImageName, Timestamp containerCreated, List<String> containerEnv, Map<String, String> containerLabels, String logPath, String daemonName) {
                this.config = config;
                this.containerID = containerID;
                this.containerName = containerName;
                this.containerEntrypoint = containerEntrypoint;
                this.containerArgs = containerArgs;
                this.containerImageID = containerImageID;
                this.containerImageName = containerImageName;
                this.containerCreated = containerCreated;
                this.containerEnv = containerEnv;
                this.containerLabels = containerLabels;
                this.logPath = logPath;
                this.daemonName = daemonName;
            }

            public Info() {
            }

            public Map<String, String> getConfig() {
                return this.config;
            }

            public String getContainerID() {
                return this.containerID;
            }

            public String getContainerName() {
                return this.containerName;
            }

            public String getContainerEntrypoint() {
                return this.containerEntrypoint;
            }

            public List<String> getContainerArgs() {
                return this.containerArgs;
            }

            public String getContainerImageID() {
                return this.containerImageID;
            }

            public String getContainerImageName() {
                return this.containerImageName;
            }

            public Timestamp getContainerCreated() {
                return this.containerCreated;
            }

            public List<String> getContainerEnv() {
                return this.containerEnv;
            }

            public Map<String, String> getContainerLabels() {
                return this.containerLabels;
            }

            public String getLogPath() {
                return this.logPath;
            }

            public String getDaemonName() {
                return this.daemonName;
            }

            public void setConfig(Map<String, String> config) {
                this.config = config;
            }

            public void setContainerID(String containerID) {
                this.containerID = containerID;
            }

            public void setContainerName(String containerName) {
                this.containerName = containerName;
            }

            public void setContainerEntrypoint(String containerEntrypoint) {
                this.containerEntrypoint = containerEntrypoint;
            }

            public void setContainerArgs(List<String> containerArgs) {
                this.containerArgs = containerArgs;
            }

            public void setContainerImageID(String containerImageID) {
                this.containerImageID = containerImageID;
            }

            public void setContainerImageName(String containerImageName) {
                this.containerImageName = containerImageName;
            }

            public void setContainerCreated(Timestamp containerCreated) {
                this.containerCreated = containerCreated;
            }

            public void setContainerEnv(List<String> containerEnv) {
                this.containerEnv = containerEnv;
            }

            public void setContainerLabels(Map<String, String> containerLabels) {
                this.containerLabels = containerLabels;
            }

            public void setLogPath(String logPath) {
                this.logPath = logPath;
            }

            public void setDaemonName(String daemonName) {
                this.daemonName = daemonName;
            }

            public boolean equals(final Object o) {
                if (o == this) return true;
                if (!(o instanceof Info)) return false;
                final Info other = (Info) o;
                if (!other.canEqual((Object) this)) return false;
                final Object this$config = this.getConfig();
                final Object other$config = other.getConfig();
                if (this$config == null ? other$config != null : !this$config.equals(other$config)) return false;
                final Object this$containerID = this.getContainerID();
                final Object other$containerID = other.getContainerID();
                if (this$containerID == null ? other$containerID != null : !this$containerID.equals(other$containerID))
                    return false;
                final Object this$containerName = this.getContainerName();
                final Object other$containerName = other.getContainerName();
                if (this$containerName == null ? other$containerName != null : !this$containerName.equals(other$containerName))
                    return false;
                final Object this$containerEntrypoint = this.getContainerEntrypoint();
                final Object other$containerEntrypoint = other.getContainerEntrypoint();
                if (this$containerEntrypoint == null ? other$containerEntrypoint != null : !this$containerEntrypoint.equals(other$containerEntrypoint))
                    return false;
                final Object this$containerArgs = this.getContainerArgs();
                final Object other$containerArgs = other.getContainerArgs();
                if (this$containerArgs == null ? other$containerArgs != null : !this$containerArgs.equals(other$containerArgs))
                    return false;
                final Object this$containerImageID = this.getContainerImageID();
                final Object other$containerImageID = other.getContainerImageID();
                if (this$containerImageID == null ? other$containerImageID != null : !this$containerImageID.equals(other$containerImageID))
                    return false;
                final Object this$containerImageName = this.getContainerImageName();
                final Object other$containerImageName = other.getContainerImageName();
                if (this$containerImageName == null ? other$containerImageName != null : !this$containerImageName.equals(other$containerImageName))
                    return false;
                final Object this$containerCreated = this.getContainerCreated();
                final Object other$containerCreated = other.getContainerCreated();
                if (this$containerCreated == null ? other$containerCreated != null : !this$containerCreated.equals(other$containerCreated))
                    return false;
                final Object this$containerEnv = this.getContainerEnv();
                final Object other$containerEnv = other.getContainerEnv();
                if (this$containerEnv == null ? other$containerEnv != null : !this$containerEnv.equals(other$containerEnv))
                    return false;
                final Object this$containerLabels = this.getContainerLabels();
                final Object other$containerLabels = other.getContainerLabels();
                if (this$containerLabels == null ? other$containerLabels != null : !this$containerLabels.equals(other$containerLabels))
                    return false;
                final Object this$logPath = this.getLogPath();
                final Object other$logPath = other.getLogPath();
                if (this$logPath == null ? other$logPath != null : !this$logPath.equals(other$logPath)) return false;
                final Object this$daemonName = this.getDaemonName();
                final Object other$daemonName = other.getDaemonName();
                if (this$daemonName == null ? other$daemonName != null : !this$daemonName.equals(other$daemonName))
                    return false;
                return true;
            }

            protected boolean canEqual(final Object other) {
                return other instanceof Info;
            }

            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $config = this.getConfig();
                result = result * PRIME + ($config == null ? 43 : $config.hashCode());
                final Object $containerID = this.getContainerID();
                result = result * PRIME + ($containerID == null ? 43 : $containerID.hashCode());
                final Object $containerName = this.getContainerName();
                result = result * PRIME + ($containerName == null ? 43 : $containerName.hashCode());
                final Object $containerEntrypoint = this.getContainerEntrypoint();
                result = result * PRIME + ($containerEntrypoint == null ? 43 : $containerEntrypoint.hashCode());
                final Object $containerArgs = this.getContainerArgs();
                result = result * PRIME + ($containerArgs == null ? 43 : $containerArgs.hashCode());
                final Object $containerImageID = this.getContainerImageID();
                result = result * PRIME + ($containerImageID == null ? 43 : $containerImageID.hashCode());
                final Object $containerImageName = this.getContainerImageName();
                result = result * PRIME + ($containerImageName == null ? 43 : $containerImageName.hashCode());
                final Object $containerCreated = this.getContainerCreated();
                result = result * PRIME + ($containerCreated == null ? 43 : $containerCreated.hashCode());
                final Object $containerEnv = this.getContainerEnv();
                result = result * PRIME + ($containerEnv == null ? 43 : $containerEnv.hashCode());
                final Object $containerLabels = this.getContainerLabels();
                result = result * PRIME + ($containerLabels == null ? 43 : $containerLabels.hashCode());
                final Object $logPath = this.getLogPath();
                result = result * PRIME + ($logPath == null ? 43 : $logPath.hashCode());
                final Object $daemonName = this.getDaemonName();
                result = result * PRIME + ($daemonName == null ? 43 : $daemonName.hashCode());
                return result;
            }

            public String toString() {
                return "LogDriver.RStartLogging.Info(config=" + this.getConfig() + ", containerID=" + this.getContainerID() + ", containerName=" + this.getContainerName() + ", containerEntrypoint=" + this.getContainerEntrypoint() + ", containerArgs=" + this.getContainerArgs() + ", containerImageID=" + this.getContainerImageID() + ", containerImageName=" + this.getContainerImageName() + ", containerCreated=" + this.getContainerCreated() + ", containerEnv=" + this.getContainerEnv() + ", containerLabels=" + this.getContainerLabels() + ", logPath=" + this.getLogPath() + ", daemonName=" + this.getDaemonName() + ")";
            }
        }

    }

    public static class StartLogging implements Serializable {

        @SerializedName("Err")
        private String err = "";

        public StartLogging(String err) {
            this.err = err;
        }

        public StartLogging() {
        }

        public String getErr() {
            return this.err;
        }

        public void setErr(String err) {
            this.err = err;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof StartLogging)) return false;
            final StartLogging other = (StartLogging) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$err = this.getErr();
            final Object other$err = other.getErr();
            if (this$err == null ? other$err != null : !this$err.equals(other$err)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof StartLogging;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $err = this.getErr();
            result = result * PRIME + ($err == null ? 43 : $err.hashCode());
            return result;
        }

        public String toString() {
            return "LogDriver.StartLogging(err=" + this.getErr() + ")";
        }
    }


    public static class RStopLogging implements Serializable {

        @SerializedName("File")
        private String file;

        public RStopLogging(String file) {
            this.file = file;
        }

        public RStopLogging() {
        }

        public String getFile() {
            return this.file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof RStopLogging)) return false;
            final RStopLogging other = (RStopLogging) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$file = this.getFile();
            final Object other$file = other.getFile();
            if (this$file == null ? other$file != null : !this$file.equals(other$file)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof RStopLogging;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $file = this.getFile();
            result = result * PRIME + ($file == null ? 43 : $file.hashCode());
            return result;
        }

        public String toString() {
            return "LogDriver.RStopLogging(file=" + this.getFile() + ")";
        }
    }

    public static class StopLogging implements Serializable {

        @SerializedName("Err")
        private String err = "";

        public StopLogging(String err) {
            this.err = err;
        }

        public StopLogging() {
        }

        public String getErr() {
            return this.err;
        }

        public void setErr(String err) {
            this.err = err;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof StopLogging)) return false;
            final StopLogging other = (StopLogging) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$err = this.getErr();
            final Object other$err = other.getErr();
            if (this$err == null ? other$err != null : !this$err.equals(other$err)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof StopLogging;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $err = this.getErr();
            result = result * PRIME + ($err == null ? 43 : $err.hashCode());
            return result;
        }

        public String toString() {
            return "LogDriver.StopLogging(err=" + this.getErr() + ")";
        }
    }
}
