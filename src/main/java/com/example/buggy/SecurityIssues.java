package com.example.buggy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.yaml.snakeyaml.Yaml;

/** Security vulnerabilities and hotspots. Each planted bug is tagged "BUG Sn". */
public class SecurityIssues {

    private static final Logger LOG = LogManager.getLogger(SecurityIssues.class);

    // Database password constant (used below).
    private static final String DB_PASSWORD = "SuperSecret123!";

    // BUG S2: hard-coded AWS secret key
    private static final String AWS_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    // BUG S3: hard-coded IP address
    private static final String BACKEND_HOST = "192.168.10.42";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:test;USER=sa", "sa", DB_PASSWORD); // BUG S1: hard-coded database password
    }

    // BUG S4: SQL injection via string concatenation
    public ResultSet findUser(Connection conn, String username) throws SQLException {
        Statement st = conn.createStatement();
        return st.executeQuery("SELECT * FROM users WHERE name = '" + username + "'");
    }

    // BUG S5: OS command injection
    public Process ping(String host) throws IOException {
        return Runtime.getRuntime().exec("ping -c 1 " + host);
    }

    // BUG S6: path traversal - user input used as a file path
    public InputStream readUserFile(String fileName) throws IOException {
        return new FileInputStream(new File("/var/data/", fileName));
    }

    // BUG S7: weak hash MD5 for passwords
    public byte[] hashPassword(String password) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5").digest(password.getBytes());
    }

    // BUG S8: weak hash SHA-1
    public byte[] fingerprint(byte[] data) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-1").digest(data);
    }

    // BUG S9: predictable PRNG used for a security token
    public String resetToken() {
        return Long.toHexString(new Random().nextLong());
    }

    // BUG S10: SecureRandom with a constant seed
    public SecureRandom seededRandom() {
        return new SecureRandom("fixed-seed".getBytes());
    }

    // BUG S11: DES cipher in ECB mode
    public byte[] encryptDes(byte[] key, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("DES/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DES"));
        return c.doFinal(data);
    }

    // BUG S12: static (reused) IV for AES-CBC
    public byte[] encryptAes(byte[] key, byte[] data) throws Exception {
        byte[] iv = new byte[16];
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(data);
    }

    // BUG S13: XXE - DocumentBuilderFactory with external entities enabled (default)
    public Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    // BUG S14: insecure Java deserialization of untrusted bytes
    public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        return ois.readObject();
    }

    // BUG S15: TrustManager that accepts every certificate
    public SSLContext trustAll() throws Exception {
        TrustManager[] tms = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) { }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) { }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tms, null);
        return ctx;
    }

    // BUG S16: HostnameVerifier that accepts every host
    public HostnameVerifier anyHost() {
        return (hostname, session) -> true;
    }

    // BUG S17: weak TLS protocol version
    public SSLContext oldTls() throws NoSuchAlgorithmException {
        return SSLContext.getInstance("TLSv1");
    }

    // BUG S18: logging unsanitised user input (log injection; Log4Shell with log4j 2.14.1)
    public void logLogin(String username) {
        LOG.info("Login attempt for user: " + username);
    }

    // BUG S19: reflected XSS - request parameter written straight to the response
    public void greet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().write("<h1>Hello " + req.getParameter("name") + "</h1>");
    }

    // BUG S20: open redirect
    public void redirect(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getParameter("next"));
    }

    // BUG S21: cookie without Secure / HttpOnly flags
    public void setSession(HttpServletResponse resp, String sessionId) {
        Cookie c = new Cookie("SESSION", sessionId);
        resp.addCookie(c);
    }

    // BUG S22: ReDoS - regex compiled from user input
    public boolean matches(String userRegex, String input) {
        return Pattern.compile(userRegex).matcher(input).matches();
    }

    // BUG S23: code injection - user input evaluated by a script engine
    public Object calc(String expression) throws Exception {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
        return engine.eval(expression);
    }

    // BUG S24: unsafe YAML load (arbitrary type instantiation)
    public Object loadYaml(String yaml) {
        return new Yaml().load(yaml);
    }

    // BUG S25: Jackson default typing enables polymorphic deserialization gadgets
    public Object readJson(String json) throws IOException {
        ObjectMapper om = new ObjectMapper();
        om.enableDefaultTyping();
        return om.readValue(json, Object.class);
    }

    // BUG S26: Text4Shell - interpolating user input with the default lookups
    public String render(String template) {
        return StringSubstitutor.createInterpolator().replace(template);
    }

    // BUG S27: XPath injection
    public String lookupRole(Document doc, String user) throws Exception {
        XPath xp = XPathFactory.newInstance().newXPath();
        return xp.evaluate("/users/user[@name='" + user + "']/role", doc);
    }

    // BUG S28: world-writable file permissions
    public void share(File f) {
        f.setWritable(true, false);
        f.setReadable(true, false);
    }

    // BUG S29: insecure temp file creation in shared directory
    public File tempFile() throws IOException {
        return File.createTempFile("buggy", ".tmp");
    }

    // BUG S30: stack trace leaked to the HTTP client
    public void handle(HttpServletResponse resp) throws IOException {
        try {
            Integer.parseInt("not-a-number");
        } catch (NumberFormatException e) {
            e.printStackTrace(resp.getWriter());
        }
    }

    public String backendUrl() {
        return "http://" + BACKEND_HOST + "/api?key=" + AWS_SECRET_KEY;
    }
}
