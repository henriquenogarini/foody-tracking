package br.com.foody.tracking.modules.auth.mapper;

import br.com.foody.tracking.modules.auth.dto.UserResponse;
import br.com.foody.tracking.modules.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "id", source = "publicId")
  UserResponse toResponse(User user);
}
