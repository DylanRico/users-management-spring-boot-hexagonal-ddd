package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.in.GetAllUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.in.UpdateUserUseCase;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class
})
@DisplayName("UserRestController")
class UserRestControllerTest {

  @Test
  void anonymousRegistrationCannotCreatePrivilegedRoles() throws Exception {
    for (String role : java.util.List.of("ADMIN", "REVIEWER")) {
      mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(
          "{\"id\":\"" + ID + "\",\"name\":\"Usuario Taller\",\"email\":\"test@example.com\",\"password\":\"SecurePass123\",\"role\":\"" + role + "\"}"))
          .andExpect(status().isForbidden());
    }
    org.mockito.Mockito.verifyNoInteractions(createUserUseCase);
  }

  @Test
  void anonymousMemberRegistrationIsAllowed() throws Exception {
    assertAllowedRegistration("MEMBER");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void administratorCanCreatePrivilegedRoles() throws Exception {
    assertAllowedRegistration("ADMIN");
  }

  private void assertAllowedRegistration(String role) throws Exception {
    when(createUserUseCase.execute(org.mockito.ArgumentMatchers.any())).thenReturn(
        new UserModel(new UserId(ID), new UserName("Usuario Taller"), new UserEmail("test@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.fromString(role), UserStatus.PENDING));
    mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(
        "{\"id\":\"" + ID + "\",\"name\":\"Usuario Taller\",\"email\":\"test@example.com\",\"password\":\"SecurePass123\",\"role\":\"" + role + "\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value(role));
  }

  private static final String ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";

  @Autowired private MockMvc mockMvc;

  @MockBean private CreateUserUseCase createUserUseCase;
  @MockBean private UpdateUserUseCase updateUserUseCase;
  @MockBean private DeleteUserUseCase deleteUserUseCase;
  @MockBean private GetUserByIdUseCase getUserByIdUseCase;
  @MockBean private GetAllUsersUseCase getAllUsersUseCase;
  @MockBean private JwtTokenService jwtTokenService;

  @Test
  @WithMockUser(roles = "REVIEWER")
  @DisplayName("GET /api/users/{id} debe responder usando el caso de uso inyectado por Spring")
  void shouldGetUserByIdThroughSpringMvc() throws Exception {
    // Arrange
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(getUserByIdUseCase.execute(new GetUserByIdQuery(ID))).thenReturn(user);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.name").value("Ada Lovelace"))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("POST /api/users debe rechazar una solicitud inválida antes del caso de uso")
  void shouldRejectInvalidCreateRequest() throws Exception {
    // Arrange
    final String invalidRequest =
        """
        {
          "id": "",
          "name": "A",
          "email": "invalid",
          "password": "short",
          "role": ""
        }
        """;

    // Act & Assert
    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("GET /api/users/{id} debe requerir autenticacion")
  void shouldRejectUnauthenticatedRequest() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  @DisplayName("GET /api/users/{id} debe rechazar un rol sin permisos")
  void shouldRejectUnauthorizedRole() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }
}
