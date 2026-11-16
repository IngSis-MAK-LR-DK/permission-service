package edu.austral.ingsis.permission.web;

import java.util.List;

import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.security.TrustedServiceVerifier;
import edu.austral.ingsis.permission.service.PermissionService;
import edu.austral.ingsis.permission.service.Relation;
import edu.austral.ingsis.permission.service.SnippetAccess;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService service;
    private final CurrentUser currentUser;
    private final TrustedServiceVerifier trustedService;

    public PermissionController(
            PermissionService service,
            CurrentUser currentUser,
            TrustedServiceVerifier trustedService) {
        this.service = service;
        this.currentUser = currentUser;
        this.trustedService = trustedService;
    }

    /**
     * Solo lo pide el handler, con su token de servicio, al crear un snippet. Si lo pudiera pedir
     * cualquier usuario autenticado, podria quedarse como dueño de un snippetId que todavia no
     * tiene dueño.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void registerOwner(@Valid @RequestBody RegisterOwnerRequest request) {
        trustedService.requireTrustedService();
        service.registerOwner(request.snippetId(), request.ownerId());
    }

    @PostMapping("/{snippetId}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void share(@PathVariable String snippetId, @Valid @RequestBody ShareRequest request) {
        service.share(snippetId, currentUser.userId(), request.userId());
    }

    @GetMapping("/snippets")
    public List<SnippetAccess> listSnippets(@RequestParam(defaultValue = "all") String relation) {
        return service.listSnippets(currentUser.userId(), Relation.parse(relation));
    }

    @GetMapping("/{snippetId}")
    public RoleResponse roleOf(@PathVariable String snippetId) {
        Role role = service.roleOf(snippetId, currentUser.userId());
        return new RoleResponse(role);
    }

    @DeleteMapping("/{snippetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String snippetId) {
        service.deleteSnippet(snippetId, currentUser.userId());
    }
}
