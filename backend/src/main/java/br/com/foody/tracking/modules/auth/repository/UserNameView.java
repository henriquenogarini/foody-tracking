package br.com.foody.tracking.modules.auth.repository;

import java.util.UUID;

// Two-column projection: resolving actor names must not load password hashes or whole entities.
public interface UserNameView {

  UUID getPublicId();

  String getName();
}
