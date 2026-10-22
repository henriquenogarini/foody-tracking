package br.com.foody.tracking.modules.auth.repository;

import java.util.UUID;

public interface UserNameView {

  UUID getPublicId();

  String getName();
}
