package kuhn.example.sslclient;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@SpringBootApplication
public class SSLClientApplication {
	
	private final String serviceUrl;
	private RestTemplate restTemplate;
	
	public static void main(String[] args) {
		SpringApplication.run(SSLClientApplication.class, args);
	}

	public SSLClientApplication(final Environment env, final KeyVaultService kv) throws Exception {
		serviceUrl = env.getProperty("service.url");
		final HttpClient httpClient = HttpClients.custom().setSSLContext(kv.getSslContext("ssl-client")).build();
		final ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		restTemplate = new RestTemplate(requestFactory);
	   }
	
    @GetMapping("/request")
	public String request(@RequestParam String value) {
		try {
			System.out.println("Sending request for: [" + value + "]");
			return  restTemplate.getForObject(serviceUrl + "/request?value=" + value, String.class);
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}
}
