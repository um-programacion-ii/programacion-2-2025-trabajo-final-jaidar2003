package org.example.tf25.proxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tf25.catedra")
public class CatedraProperties {

    private String baseUrl;
    /**
     * Token estático opcional para llamadas a la cátedra. Si está presente, se usa y no se realiza login.
     */
    private String token;
    /**
     * Ruta a un archivo de texto que contenga el JWT (línea única). Si está presente y tiene contenido,
     * se usa como alternativa al token estático y no se realiza login.
     */
    private String tokenFile;

    private final Auth auth = new Auth();

    public static class Auth {
        private String loginPath;
        private String username;
        private String password;

        public String getLoginPath() {
            return loginPath;
        }

        public void setLoginPath(String loginPath) {
            this.loginPath = loginPath;
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
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenFile() {
        return tokenFile;
    }

    public void setTokenFile(String tokenFile) {
        this.tokenFile = tokenFile;
    }

    public Auth getAuth() {
        return auth;
    }
}
