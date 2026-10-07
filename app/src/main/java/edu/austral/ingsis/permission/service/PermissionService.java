package edu.austral.ingsis.permission.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import edu.austral.ingsis.permission.exception.AlreadyExistsException;
import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.model.Permission;
import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.repository.PermissionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {

    private final PermissionRepository repository;
    private final Clock clock;

    public PermissionService(PermissionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Registra a userId como dueño de un snippet recien creado. */
    @Transactional
    public void registerOwner(String snippetId, String userId) {
        boolean hasOwner = repository.existsBySnippetIdAndRole(snippetId, Role.OWNER);
        if (hasOwner) {
            throw new AlreadyExistsException("El snippet " + snippetId + " ya tiene dueño");
        }
        repository.save(new Permission(snippetId, userId, Role.OWNER, Instant.now(clock)));
    }

    /** Da permiso de lectura a targetUserId. Solo el dueño puede compartir. */
    @Transactional
    public void share(String snippetId, String ownerId, String targetUserId) {
        requireOwner(snippetId, ownerId);
        if (repository.existsBySnippetIdAndUserId(snippetId, targetUserId)) {
            throw new AlreadyExistsException(
                    "El snippet " + snippetId + " ya esta compartido con ese usuario");
        }
        repository.save(new Permission(snippetId, targetUserId, Role.READER, Instant.now(clock)));
    }

    /** Snippets a los que el usuario tiene acceso, segun su relacion con ellos. */
    @Transactional(readOnly = true)
    public List<SnippetAccess> listSnippets(String userId, Relation relation) {
        List<Permission> permissions =
                switch (relation) {
                    case OWNER -> repository.findByUserIdAndRole(userId, Role.OWNER);
                    case SHARED -> repository.findByUserIdAndRole(userId, Role.READER);
                    case ALL -> repository.findByUserId(userId);
                };
        return permissions.stream()
                .map(p -> new SnippetAccess(p.getSnippetId(), p.getRole()))
                .toList();
    }

    /** Rol del usuario sobre el snippet; NONE si no tiene acceso. */
    @Transactional(readOnly = true)
    public Role roleOf(String snippetId, String userId) {
        return repository
                .findBySnippetIdAndUserId(snippetId, userId)
                .map(Permission::getRole)
                .orElse(Role.NONE);
    }

    /** Borra todos los permisos del snippet. Solo el dueño puede. */
    @Transactional
    public void deleteSnippet(String snippetId, String userId) {
        requireOwner(snippetId, userId);
        repository.deleteBySnippetId(snippetId);
    }

    private void requireOwner(String snippetId, String userId) {
        if (roleOf(snippetId, userId) != Role.OWNER) {
            throw new ForbiddenOperationException(
                    "Solo el dueño del snippet " + snippetId + " puede hacer esta operacion");
        }
    }
}
