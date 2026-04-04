package dev.parallaxsports.auth.client;

import dev.parallaxsports.auth.dto.VerificationEmailRequest;
import dev.parallaxsports.core.config.properties.AlertProperties;
import dev.parallaxsports.core.exception.UpstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class EmailVerificationClient {

	private static final String VERIFICATION_PATH = "/internal/email/verify";

	private final RestClient restClient;

	public EmailVerificationClient(RestClient.Builder restClientBuilder, AlertProperties alertProperties) {
		this.restClient = restClientBuilder
			.baseUrl(alertProperties.getKtorBaseUrl())
			.build();
	}

	public void sendVerificationEmail(VerificationEmailRequest request) {
		try {
			restClient.post()
				.uri(VERIFICATION_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.toBodilessEntity();
			log.info("Verification email request sent to={}", request.to());
		} catch (Exception ex) {
			log.error("Failed to send verification email to={}: {}", request.to(), ex.getMessage());
			throw new UpstreamServiceException("Failed to send verification email", ex);
		}
	}
}
