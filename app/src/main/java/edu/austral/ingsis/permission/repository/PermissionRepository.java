package edu.austral.ingsis.permission.repository;

import java.util.List;
import java.util.Optional;

import edu.austral.ingsis.permission.model.Permission;
import edu.austral.ingsis.permission.model.Role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findBySnippetIdAndUserId(String snippetId, String userId);

    boolean existsBySnippetIdAndUserId(String snippetId, String userId);

    boolean existsBySnippetIdAndRole(String snippetId, Role role);

    List<Permission> findByUserId(String userId);

    List<Permission> findByUserIdAndRole(String userId, Role role);

    void deleteBySnippetId(String snippetId);
}
