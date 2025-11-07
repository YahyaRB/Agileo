package com.agileo.AGILEO.config;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.remote")
public class SmbProps {
    private Server server = new Server();
    private Smb smb = new Smb();

    public static class Server {
        private String username;
        private String password;
        private String domain; // peut être null/vide

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

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }
    }
    public static class Smb {
        private String host;
        private String share;
        private String basePath; // ex: RB217/Fichiers/ficjoints_op

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getShare() {
            return share;
        }

        public void setShare(String share) {
            this.share = share;
        }

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Smb getSmb() {
        return smb;
    }

    public void setSmb(Smb smb) {
        this.smb = smb;
    }
}
