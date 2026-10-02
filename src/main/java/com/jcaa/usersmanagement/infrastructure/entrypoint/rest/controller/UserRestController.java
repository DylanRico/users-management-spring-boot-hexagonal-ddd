package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.in.GetAllUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.in.UpdateUserUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CreateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.UpdateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.UserRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController implements UserRestControllerDocs {

  private final CreateUserUseCase createUserUseCase;
  private final UpdateUserUseCase updateUserUseCase;
  private final DeleteUserUseCase deleteUserUseCase;
  private final GetUserByIdUseCase getUserByIdUseCase;
  private final GetAllUsersUseCase getAllUsersUseCase;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN') or #request.role().trim().equalsIgnoreCase('MEMBER')")
  public UserRestResponse create(@Valid @RequestBody final CreateUserRestRequest request) {
    final CreateUserCommand command = UserRestMapper.toCreateCommand(request);
    final UserModel user = createUserUseCase.execute(command);
    return UserRestMapper.toResponse(user);
  }

  @GetMapping
  public List<UserRestResponse> getAll() {
    return UserRestMapper.toResponseList(getAllUsersUseCase.execute());
  }

  @GetMapping("/{id}")
  public UserRestResponse getById(@PathVariable final String id) {
    final GetUserByIdQuery query = UserRestMapper.toGetByIdQuery(id);
    final UserModel user = getUserByIdUseCase.execute(query);
    return UserRestMapper.toResponse(user);
  }

  @PutMapping("/{id}")
  public UserRestResponse update(
      @PathVariable final String id,
      @Valid @RequestBody final UpdateUserRestRequest request) {
    final UpdateUserCommand command = UserRestMapper.toUpdateCommand(id, request);
    final UserModel user = updateUserUseCase.execute(command);
    return UserRestMapper.toResponse(user);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable final String id) {
    final DeleteUserCommand command = UserRestMapper.toDeleteCommand(id);
    deleteUserUseCase.execute(command);
  }
}

