package com.jcaa.usersmanagement.infrastructure.adapter.email;

import com.jcaa.usersmanagement.domain.model.EmailDestinationModel;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GmailApiEmailSenderAdapterTest {
  @Test
  void preservesUtf8MimeContentAndRejectsFailedHttpResponses() throws Exception {
    SmtpConfig sender = new SmtpConfig("", 0, "", "", "sender@example.com", "Gestión de Usuarios");
    String raw = GmailApiEmailSenderAdapter.encodeMessage(sender,
        new EmailDestinationModel("member@example.com", "María", "Activación de cuenta", "<p>Cuenta válida</p>"));
    assertFalse(raw.contains("="));
    MimeMessage parsed = new MimeMessage(Session.getInstance(new Properties()),
        new ByteArrayInputStream(Base64.getUrlDecoder().decode(raw)));
    assertEquals("Activación de cuenta", parsed.getSubject());
    assertEquals("<p>Cuenta válida</p>", parsed.getContent());
    assertTrue(parsed.getAllRecipients()[0].toString().contains("member@example.com"));
    assertDoesNotThrow(() -> GmailApiEmailSenderAdapter.requireSuccess(200));
    assertThrows(IllegalStateException.class, () -> GmailApiEmailSenderAdapter.requireSuccess(401));
  }
}
