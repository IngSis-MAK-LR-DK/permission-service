package edu.austral.ingsis.permission.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import edu.austral.ingsis.permission.exception.AlreadyExistsException;
import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.model.Permission;
import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.repository.PermissionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
    private static final String SNIPPET = "snippet-1";
    private static final String OWNER = "auth0|ana";
    private static final String OTHER = "auth0|beto";

    @Mock private PermissionRepository repository;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void registerOwnerSavesOwnerPermission() {
        when(repository.existsBySnippetIdAndRole(SNIPPET, Role.OWNER)).thenReturn(false);

        service.registerOwner(SNIPPET, OWNER);

        ArgumentCaptor<Permission> saved = ArgumentCaptor.forClass(Permission.class);
        verify(repository).save(saved.capture());
        assertEquals(SNIPPET, saved.getValue().getSnippetId());
        assertEquals(OWNER, saved.getValue().getUserId());
        assertEquals(Role.OWNER, saved.getValue().getRole());
        assertEquals(NOW, saved.getValue().getCreatedAt());
    }

    @Test
    void registerOwnerFailsIfSnippetAlreadyHasOwner() {
        when(repository.existsBySnippetIdAndRole(SNIPPET, Role.OWNER)).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> service.registerOwner(SNIPPET, OWNER));
        verify(repository, never()).save(any());
    }

    @Test
    void ownerCanShareSnippetAsReader() {
        asOwner();
        when(repository.existsBySnippetIdAndUserId(SNIPPET, OTHER)).thenReturn(false);

        service.share(SNIPPET, OWNER, OTHER);

        ArgumentCaptor<Permission> saved = ArgumentCaptor.forClass(Permission.class);
        verify(repository).save(saved.capture());
        assertEquals(OTHER, saved.getValue().getUserId());
        assertEquals(Role.READER, saved.getValue().getRole());
    }

    @Test
    void shareFailsIfAlreadyShared() {
        asOwner();
        when(repository.existsBySnippetIdAndUserId(SNIPPET, OTHER)).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> service.share(SNIPPET, OWNER, OTHER));
        verify(repository, never()).save(any());
    }

    @Test
    void readerCannotShare() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OTHER))
                .thenReturn(Optional.of(permission(OTHER, Role.READER)));

        assertThrows(ForbiddenOperationException.class, () -> service.share(SNIPPET, OTHER, "x"));
        verify(repository, never()).save(any());
    }

    @Test
    void userWithoutAccessCannotShare() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OTHER)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> service.share(SNIPPET, OTHER, "x"));
    }

    @Test
    void listSnippetsByOwnerRelation() {
        when(repository.findByUserIdAndRole(OWNER, Role.OWNER))
                .thenReturn(List.of(permission(OWNER, Role.OWNER)));

        List<SnippetAccess> result = service.listSnippets(OWNER, Relation.OWNER);

        assertEquals(List.of(new SnippetAccess(SNIPPET, Role.OWNER)), result);
    }

    @Test
    void listSnippetsBySharedRelation() {
        when(repository.findByUserIdAndRole(OTHER, Role.READER))
                .thenReturn(List.of(permission(OTHER, Role.READER)));

        List<SnippetAccess> result = service.listSnippets(OTHER, Relation.SHARED);

        assertEquals(List.of(new SnippetAccess(SNIPPET, Role.READER)), result);
    }

    @Test
    void listSnippetsWithAllRelation() {
        when(repository.findByUserId(OWNER)).thenReturn(List.of(permission(OWNER, Role.OWNER)));

        List<SnippetAccess> result = service.listSnippets(OWNER, Relation.ALL);

        assertEquals(1, result.size());
    }

    @Test
    void roleOfReturnsStoredRole() {
        asOwner();

        assertEquals(Role.OWNER, service.roleOf(SNIPPET, OWNER));
    }

    @Test
    void roleOfReturnsNoneWithoutPermission() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OTHER)).thenReturn(Optional.empty());

        assertEquals(Role.NONE, service.roleOf(SNIPPET, OTHER));
    }

    @Test
    void ownerCanDeleteSnippetPermissions() {
        asOwner();

        service.deleteSnippet(SNIPPET, OWNER);

        verify(repository).deleteBySnippetId(SNIPPET);
    }

    @Test
    void readerCannotDeleteSnippetPermissions() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OTHER))
                .thenReturn(Optional.of(permission(OTHER, Role.READER)));

        assertThrows(
                ForbiddenOperationException.class, () -> service.deleteSnippet(SNIPPET, OTHER));
        verify(repository, never()).deleteBySnippetId(any());
    }

    @Test
    void relationParseIsCaseInsensitive() {
        assertEquals(Relation.SHARED, Relation.parse(" Shared "));
        assertThrows(IllegalArgumentException.class, () -> Relation.parse("nope"));
    }

    private void asOwner() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OWNER))
                .thenReturn(Optional.of(permission(OWNER, Role.OWNER)));
    }

    private static Permission permission(String userId, Role role) {
        return new Permission(SNIPPET, userId, role, NOW);
    }
}
