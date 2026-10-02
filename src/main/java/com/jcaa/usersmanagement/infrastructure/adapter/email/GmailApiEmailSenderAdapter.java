package com.jcaa.usersmanagement.infrastructure.adapter.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Envio por HTTPS para plataformas que restringen los puertos SMTP. */
@Slf4j
@Component
@ConditionalOnProperty(name = "email.provider", havingValue = "gmail-api")
public class GmailApiEmailSenderAdapter implements EmailSenderPort {
  private final ObjectMapper json;
  private final SmtpConfig sender;
  private final String clientId;
  private final String clientSecret;
  private final String refreshToken;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public GmailApiEmailSenderAdapter(ObjectMapper json, SmtpConfig sender,
      @Value("${gmail.client-id}") String clientId,
      @Value("${gmail.client-secret}") String clientSecret,
      @Value("${gmail.refresh-token}") String refreshToken) {
    if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank() || sender.fromAddress().isBlank()) {
      throw new IllegalArgumentException("Gmail API requiere CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN y SMTP_FROM_ADDRESS");
    }
    this.json = json;
    this.sender = sender;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.refreshToken = refreshToken;
  }

  @Override
  public void send(EmailDestinationModel destination) {
    try {
      String form = "grant_type=refresh_token&client_id=" + encode(clientId)
          + "&client_secret=" + encode(clientSecret) + "&refresh_token=" + encode(refreshToken);
      HttpResponse<String> tokenResponse = http.send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
          .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
      requireSuccess(tokenResponse.statusCode());
      String token = json.readTree(tokenResponse.body()).path("access_token").asText();
      if (token.isBlank()) throw new IllegalStateException("Google no devolvio un token de acceso");
      String body = json.writeValueAsString(Map.of("raw", encodeMessage(sender, destination)));
      HttpResponse<String> sent = http.send(HttpRequest.newBuilder(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
          .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token)
          .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
          HttpResponse.BodyHandlers.ofString());
      requireSuccess(sent.statusCode());
      log.info("[GmailApiEmailSenderAdapter] correo enviado exitosamente por HTTPS.");
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw EmailSenderException.becauseSendFailed(failure);
    } catch (Exception failure) {
      throw EmailSenderException.becauseSendFailed(failure);
    }
  }

  static String encodeMessage(SmtpConfig sender, EmailDestinationModel destination) throws Exception {
    MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    message.setFrom(new InternetAddress(sender.fromAddress(), sender.fromName(), "UTF-8"));
    message.setRecipient(Message.RecipientType.TO,
        new InternetAddress(destination.getDestinationEmail(), destination.getDestinationName(), "UTF-8"));
    message.setSubject(destination.getSubject(), "UTF-8");
    message.setContent(destination.getBody(), "text/html; charset=UTF-8");
    try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
      message.writeTo(bytes);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    }
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  static void requireSuccess(int status) {
    if (status < 200 || status >= 300) throw new IllegalStateException("Gmail API respondio HTTP " + status);
  }
}
