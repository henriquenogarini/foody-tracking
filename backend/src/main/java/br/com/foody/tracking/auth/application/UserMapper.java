package br.com.foody.tracking.auth.application;

import br.com.foody.tracking.auth.application.dto.UserResponse;
import br.com.foody.tracking.auth.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "id", source = "publicId")
  UserResponse toResponse(User user);
}
