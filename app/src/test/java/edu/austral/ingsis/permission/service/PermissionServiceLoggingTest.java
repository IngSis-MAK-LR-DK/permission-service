package edu.austral.ingsis.permission.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.austral.ingsis.permission.model.Permission;
import edu.austral.ingsis.permission.model.Role;
import edu.austral.ingsis.permission.repository.PermissionRepository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Cada cambio de estado deja una linea de log con el snippet y lo que paso. */
class PermissionServiceLoggingTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");
    private static final String SNIPPET = "snippet-1";
    private static final String OWNER = "auth0|ana";
    private static final String OTHER = "auth0|beto";

    private final PermissionRepository repository = mock(PermissionRepository.class);
    private final PermissionService service =
            new PermissionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(PermissionService.class);

    @BeforeEach
    void captureLogs() {
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void stopCapturing() {
        logger.detachAppender(logs);
    }

    @Test
    void registeringAnOwnerLogsTheSnippet() {
        service.registerOwner(SNIPPET, OWNER);

        ILoggingEvent event = onlyEvent();
        assertEquals("Snippet owner registered", event.getMessage());
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(Map.of("snippetId", SNIPPET), context(event));
    }

    @Test
    void sharingLogsTheSnippetAndWithWhom() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OWNER))
                .thenReturn(Optional.of(new Permission(SNIPPET, OWNER, Role.OWNER, NOW)));

        service.share(SNIPPET, OWNER, OTHER);

        ILoggingEvent event = onlyEvent();
        assertEquals("Snippet shared", event.getMessage());
        assertEquals(Map.of("snippetId", SNIPPET, "sharedWith", OTHER), context(event));
    }

    @Test
    void listingLogsHowManySnippetsWereReturned() {
        when(repository.findByUserId(OWNER))
                .thenReturn(List.of(new Permission(SNIPPET, OWNER, Role.OWNER, NOW)));

        service.listSnippets(OWNER, Relation.ALL);

        ILoggingEvent event = onlyEvent();
        assertEquals("Snippets listed", event.getMessage());
        assertEquals(Map.of("relation", "ALL", "count", "1"), context(event));
    }

    @Test
    void anEmptyListingIsLoggedWithCountZero() {
        service.listSnippets(OWNER, Relation.OWNER);

        assertEquals(Map.of("relation", "OWNER", "count", "0"), context(onlyEvent()));
    }

    @Test
    void deletingLogsTheSnippet() {
        when(repository.findBySnippetIdAndUserId(SNIPPET, OWNER))
                .thenReturn(Optional.of(new Permission(SNIPPET, OWNER, Role.OWNER, NOW)));

        service.deleteSnippet(SNIPPET, OWNER);

        ILoggingEvent event = onlyEvent();
        assertEquals("Snippet permissions deleted", event.getMessage());
        assertEquals(Map.of("snippetId", SNIPPET), context(event));
    }

    private ILoggingEvent onlyEvent() {
        assertEquals(1, logs.list.size(), "se esperaba una sola linea de log: " + logs.list);
        return logs.list.get(0);
    }

    private static Map<String, String> context(ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(pair -> pair.key, pair -> String.valueOf(pair.value)));
    }
}
