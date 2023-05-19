package kuhn.example.sslclient;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.azure.core.util.Configuration;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.certificates.CertificateClient;
import com.azure.security.keyvault.certificates.CertificateClientBuilder;
import com.azure.security.keyvault.certificates.models.KeyVaultCertificateWithPolicy;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

@Service
public class KeyVaultService {

    private final char[] emptyPass = {};
    private String vaultUrl;
    private CertificateClient certificateClient;
    private SecretClient secretClient;

    @SuppressWarnings("deprecation")
    public KeyVaultService(final Environment env) {
        try {
            // These Azure credentials come from vmArgs, which are not in version-control.
            // In VS Code, you can configure your vmArgs in ./vscode/launch.json
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_CLIENT_ID, System.getProperty("azure.clientId"));
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_CLIENT_SECRET, System.getProperty("azure.clientSecret"));
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_TENANT_ID, System.getProperty("azure.tenantId"));
            vaultUrl = env.getProperty("azure.vaultUrl");
            
            certificateClient = new CertificateClientBuilder().vaultUrl(vaultUrl).credential(new DefaultAzureCredentialBuilder().build()).buildClient();
            secretClient = new SecretClientBuilder().vaultUrl(vaultUrl).credential(new DefaultAzureCredentialBuilder().build()).buildClient();
        } catch (Exception e) {
            // Swallow this exception for demonstration purposes
        }

    }

	public SSLContext getSslContext(final String alias) throws Exception {
        try {
            // Retrieving certificate
            final KeyVaultCertificateWithPolicy certificateWithPolicy = certificateClient.getCertificate(alias);
            final KeyVaultSecret secret = secretClient.getSecret(alias, certificateWithPolicy.getProperties().getVersion());
            final byte[] rawCertificate = certificateWithPolicy.getCer();
            final CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final ByteArrayInputStream certificateStream = new ByteArrayInputStream(rawCertificate);
            final Certificate certificate = cf.generateCertificate(certificateStream);
            close(certificateStream);
            // Retrieving certificate

            // Retrieving private key
            final String base64PrivateKey = secret.getValue();
            final byte[] rawPrivateKey = Base64.getDecoder().decode(base64PrivateKey);
            final KeyStore rsaKeyGenerator = KeyStore.getInstance(KeyStore.getDefaultType());
            final ByteArrayInputStream keyStream = new ByteArrayInputStream(rawPrivateKey);
            rsaKeyGenerator.load(keyStream, null);
            close(keyStream);
            final Key rsaPrivateKey = rsaKeyGenerator.getKey(rsaKeyGenerator.aliases().nextElement(), emptyPass);
            // Retrieving private key

            // Importing certificate and private key into the KeyStore
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setKeyEntry(alias, rsaPrivateKey, emptyPass, new Certificate[] {certificate});
            // Importing certificate and private key into the KeyStore
			
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keyStore, emptyPass);
			final SSLContext sslContext = SSLContext.getInstance("TLS"); 
            final KeyManager[] kms = kmf.getKeyManagers();
			sslContext.init(kms, null, null);

            return sslContext;
        } catch (final Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void close(final Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
