package archer.model;

import netscape.javascript.JSObject;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class RepositoryConfig extends BaseModel {
    private static final Logger LOGGER = Logger.getLogger("RepositoryConfig");
    private static final Set<String> SUPPORTED_PROTOCOLS = new HashSet<>();
    private static final String PROTOCOL_SVN_SSH = "svn+ssh";
    private static final String PROTOCOL_SVN = "svn";
    private static final String PROTOCOL_FILE = "file";
    private static final Set<String> SUPPORTED_AUTH_TYPES = new HashSet<>();
    private static final String AUTH_TYPE_PASSWORD = "password";
    private static final String AUTH_TYPE_PRIVATE_KEY = "privateKey";
    private static final String DEFAULT_TITLE = "ROOT";

    private String protocol;
    private String host;
    private int port;
    private String path;
    private String authType;
    private String userName;
    private String password;
    private String privateKey;
    private String passphrase;

    static {
        SUPPORTED_PROTOCOLS.add(PROTOCOL_SVN_SSH);
        SUPPORTED_PROTOCOLS.add(PROTOCOL_SVN);
        SUPPORTED_PROTOCOLS.add(PROTOCOL_FILE);
        SUPPORTED_AUTH_TYPES.add(AUTH_TYPE_PASSWORD);
        SUPPORTED_AUTH_TYPES.add(AUTH_TYPE_PRIVATE_KEY);
    }

    private RepositoryConfig() {
    }

    public RepositoryConfig(JSObject params) throws Exception {
        protocol = (String) params.getMember("protocol");
        host = (String) params.getMember("host");
        String portString = (String) params.getMember("port");
        path = (String) params.getMember("path");
        authType = (String) params.getMember("authType");
        userName = (String) params.getMember("userName");
        password = (String) params.getMember("password");
        privateKey = (String) params.getMember("privateKey");
        passphrase = (String) params.getMember("passphrase");

        protocol = protocol.toLowerCase();
        if (!SUPPORTED_PROTOCOLS.contains(protocol)) {
            throw new Exception("不支持的协议类型（" + protocol + "）");
        }
        String defaultHost = !PROTOCOL_FILE.equals(protocol) ? "localhost" : null;
        host = host == null || host.isEmpty() || PROTOCOL_FILE.equals(protocol) ? defaultHost : host;
        port = -1;
        if (!portString.isEmpty() && !PROTOCOL_FILE.equals(protocol)) {
            try {
                port = Integer.parseInt(portString);
            } catch (Exception e) {
                throw new Exception("端口（" + portString + "）不合法");
            }
        }
        path = path == null || path.isEmpty() ? null : path;
        if (!SUPPORTED_AUTH_TYPES.contains(authType)) {
            throw new Exception("不支持的验证方式（" + authType + "）");
        }
        userName = userName == null || userName.isEmpty() ? null : userName;
        password = password == null || password.isEmpty() || !AUTH_TYPE_PASSWORD.equals(authType) ? null : password;
        String defaultPrivateKey = AUTH_TYPE_PRIVATE_KEY.equals(authType) ? Paths.get(USER_HOME, ".ssh/id_rsa").toString() : null;
        privateKey = privateKey == null || privateKey.isEmpty() || !AUTH_TYPE_PRIVATE_KEY.equals(authType) ? defaultPrivateKey : privateKey;
        passphrase = passphrase == null || passphrase.isEmpty() || !AUTH_TYPE_PRIVATE_KEY.equals(authType) ? null : passphrase;
    }

    public SVNRepository getRepository() throws Exception {
        char[] passwordCharArray = password != null ? password.toCharArray() : null;
        File privateKeyFile = privateKey != null ? new File(privateKey) : null;
        char[] passphraseCharArray = passphrase != null ? passphrase.toCharArray() : null;
        SVNRepository repository = SVNRepositoryFactory.create(getSvnUrl());
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(
                null, userName, passwordCharArray, privateKeyFile, passphraseCharArray, false);
        repository.setAuthenticationManager(authManager);
        repository.testConnection();
        return repository;
    }

    public SVNURL getSvnUrl() {
        try {
            String userInfo = host != null ? userName : null;
            return SVNURL.create(protocol, userInfo, host, port, path, true);
        } catch (Exception e) {
            LOGGER.warning(e.toString());
            return null;
        }
    }

    private boolean isPathSeparator(char ch) {
        return ch == '/' || ch == '\\';
    }

    public String getTitle() {
        String path = this.path.trim();
        int ep = path.length() - 1;
        while ((ep >= 0) && isPathSeparator(path.charAt(ep))) {
            ep--;
        }
        if (ep < 0) {
            return DEFAULT_TITLE;
        }
        String realPath = path.substring(0, ep + 1);
        int sp = Math.max(realPath.lastIndexOf('/'), realPath.lastIndexOf('\\'));
        String title = (sp >= 0 ? realPath.substring(sp + 1) : realPath).trim();
        return title.length() > 0 ? title : DEFAULT_TITLE;
    }

    public String getUrl() {
        return getSvnUrl().toString();
    }

    @XmlRootElement(name = "RepositoryConfigList")
    private static class RepositoryConfigList {
        private static final String XML_PATH = Paths.get(APP_HOME, "RepositoryConfigList.xml").toString();

        private LinkedList<RepositoryConfig> repositoryConfigs;

        public RepositoryConfigList() {
            repositoryConfigs = new LinkedList<>();
        }

        public RepositoryConfigList(LinkedList<RepositoryConfig> repositoryConfigs) {
            this.repositoryConfigs = repositoryConfigs;
        }

        public LinkedList<RepositoryConfig> getRepositoryConfigs() {
            return repositoryConfigs;
        }

        @XmlElement(name = "RepositoryConfig")
        public void setRepositoryConfigs(LinkedList<RepositoryConfig> repositoryConfigs) {
            this.repositoryConfigs = repositoryConfigs;
        }
    }

    public static LinkedList<RepositoryConfig> loadAll() {
        try {
            File xmlFile = new File(RepositoryConfigList.XML_PATH);
            if (xmlFile.isFile()) {
                return ((RepositoryConfigList) JAXBContext.newInstance(RepositoryConfigList.class)
                        .createUnmarshaller()
                        .unmarshal(xmlFile))
                        .getRepositoryConfigs();
            }
        } catch (Exception e) {
            LOGGER.warning(e.toString());
        }
        return new LinkedList<>();
    }

    public static void saveAll(LinkedList<RepositoryConfig> repositoryConfigs) throws Exception {
        Marshaller marshaller = JAXBContext.newInstance(RepositoryConfigList.class)
                .createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(new RepositoryConfigList(repositoryConfigs), new File(RepositoryConfigList.XML_PATH));
    }

    public static RepositoryConfig load(int index) {
        LinkedList<RepositoryConfig> repositoryConfigs = loadAll();
        return repositoryConfigs.get(index);
    }

    public static RepositoryConfig loadAndMoveFirst(int index) throws Exception {
        LinkedList<RepositoryConfig> repositoryConfigs = loadAll();
        RepositoryConfig config = repositoryConfigs.get(index);
        if (!Objects.equals(repositoryConfigs.peekFirst(), config)) {
            repositoryConfigs.remove(index);
            repositoryConfigs.addFirst(config);
            saveAll(repositoryConfigs);
        }
        return config;
    }

    public static void remove(int index) throws Exception {
        LinkedList<RepositoryConfig> repositoryConfigs = loadAll();
        repositoryConfigs.remove(index);
        saveAll(repositoryConfigs);
    }

    public void save() throws Exception {
        LinkedList<RepositoryConfig> repositoryConfigs = loadAll();
        if (!repositoryConfigs.contains(this)) {
            repositoryConfigs.addFirst(this);
            saveAll(repositoryConfigs);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, host, port, path,
                authType, userName, password, privateKey, passphrase);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof RepositoryConfig)
                && Objects.equals(((RepositoryConfig) obj).protocol, protocol)
                && Objects.equals(((RepositoryConfig) obj).host, host)
                && Objects.equals(((RepositoryConfig) obj).port, port)
                && Objects.equals(((RepositoryConfig) obj).path, path)
                && Objects.equals(((RepositoryConfig) obj).authType, authType)
                && Objects.equals(((RepositoryConfig) obj).userName, userName)
                && Objects.equals(((RepositoryConfig) obj).password, password)
                && Objects.equals(((RepositoryConfig) obj).privateKey, privateKey)
                && Objects.equals(((RepositoryConfig) obj).passphrase, passphrase);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }
}
