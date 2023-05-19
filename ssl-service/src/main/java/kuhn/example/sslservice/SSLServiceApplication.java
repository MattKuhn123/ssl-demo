package kuhn.example.sslservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class SSLServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(SSLServiceApplication.class, args);
	}

	/**
     * /request?value=${number}
     * @param value number to multiply by 2
     * @return the number multiplied by 2
     */
    @RequestMapping(value = "/request", method = RequestMethod.GET)
	public String request(final @RequestParam String value) {
		System.out.println("Received request for: [" + value + "]");
        int intValue = 0;
        try { intValue = Integer.parseInt(value); } catch (final Exception e) { }
        return String.valueOf(intValue * 2);
	}
}
