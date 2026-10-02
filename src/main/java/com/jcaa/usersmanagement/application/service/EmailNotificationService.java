package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.EmailSenderPort;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private static final String SUBJECT_CREATED = "Tu cuenta ha sido creada — Gestion de Usuarios";
  private static final String SUBJECT_UPDATED  = "Tu cuenta ha sido actualizada — Gestion de Usuarios";

  private static final String TOKEN_NAME     = "name";
  private static final String TOKEN_EMAIL    = "email";
  private static final String TOKEN_ROLE     = "role";
  private static final String TOKEN_STATUS   = "status";

  private static final String LOG_SEND_FAILED =
      "[EmailNotificationService] correo no enviado. Causa: {}";

  private final EmailSenderPort emailSenderPort;

  public void notifyUserCreated(final UserModel user) {
    final String template = loadTemplate("user-created.html");
    final String body =
        renderTemplate(
            template,
            Map.of(
                TOKEN_NAME,     user.getName().value(),
                TOKEN_EMAIL,    user.getEmail().value(),
                TOKEN_ROLE,     user.getRole().name()));
    final EmailDestinationModel destination = buildDestination(user, SUBJECT_CREATED, body);
    sendOrLog(destination);
  }

  public void notifyUserUpdated(final UserModel user) {
    final String template = loadTemplate("user-updated.html");
    final String body =
        renderTemplate(
            template,
            Map.of(
                TOKEN_NAME,   user.getName().value(),
                TOKEN_EMAIL,  user.getEmail().value(),
                TOKEN_ROLE,   user.getRole().name(),
                TOKEN_STATUS, user.getStatus().name()));
    final EmailDestinationModel destination = buildDestination(user, SUBJECT_UPDATED, body);
    sendOrLog(destination);
  }

  private static EmailDestinationModel buildDestination(
      final UserModel user, final String subject, final String body) {
    return new EmailDestinationModel(
        user.getEmail().value(), user.getName().value(), subject, body);
  }

  private String loadTemplate(final String templateName) {
    final String path = "/templates/" + templateName;
    try (InputStream inputStream = openResourceStream(path)) {
      if (Objects.isNull(inputStream)) {
        throw EmailSenderException.becauseSendFailed(
            new IllegalStateException("Template not found: " + path));
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (final IOException ioException) {
      throw EmailSenderException.becauseSendFailed(ioException);
    }
  }

  InputStream openResourceStream(final String path) {
    return getClass().getResourceAsStream(path);
  }

  private String renderTemplate(String template, final Map<String, String> values) {
    String result = template;
    for (final Map.Entry<String, String> tokenEntry : values.entrySet()) {
      final String token = "{{" + tokenEntry.getKey() + "}}";
      result = result.replace(token, org.springframework.web.util.HtmlUtils.htmlEscape(tokenEntry.getValue()));
    }
    return result;
  }

  private void sendOrLog(final EmailDestinationModel destination) {
    try {
      emailSenderPort.send(destination);
    } catch (final EmailSenderException senderException) {
      log.warn(LOG_SEND_FAILED, senderException.getMessage(), senderException);
    }
  }
}
