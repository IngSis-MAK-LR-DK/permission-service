# permission-service

## Observabilidad

### Qué se puede seguir

| Qué | Dónde |
|---|---|
| Un pedido de punta a punta | Header `X-Request-Id`. Si el pedido ya lo trae (proxy o handler) se respeta; si no, se genera. Siempre vuelve en la respuesta, también en un 401. |
| Quién lo hizo | Campo `userId` en todos los logs del pedido: el `sub` del JWT. Nunca el mail ni el token. |
| Qué cambió | Una línea por cambio de estado: `Snippet owner registered`, `Snippet shared`, `Snippet permissions deleted`, con el `snippetId`. |
| Qué devolvió un listado | `Snippets listed` con `relation` y `count`. Un listado vacío es un 200 igual que uno con datos: el log es lo que permite notar que a alguien "le desaparecieron" los snippets. |
| Por qué se rechazó algo | `Operation refused` (403, WARN) y `Conflicting request` (409, INFO), con el `reason`. |

El mensaje cuenta solo lo que pasó; el contexto (`requestId`, `userId`, `snippetId`...) va en campos aparte.

### Formato

- **Local:** texto legible, con el `requestId` entre corchetes y el contexto al final.
- **Stack (JSON):** se prende con la variable de entorno `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`.
  Cada línea es un JSON con `message`, `requestId`, `userId`, `service.name`... que se puede filtrar
  por campo en vez de buscar texto.

```json
{"@timestamp":"2026-11-15T20:42:35.978052Z","log":{"level":"WARN"},"service":{"name":"permission-service"},"message":"Operation refused","userId":"auth0|beto","requestId":"7f3a91c2-5d1e-4b7a-9c20-0a1b2c3d4e5f","reason":"Solo el dueño puede compartir"}
```

### Qué no se loguea

Tokens, secretos ni mails. Un identificador (`auth0|...`) alcanza para saber quién fue.

### Pendiente

Mandar los logs, métricas y trazas a New Relic (agente Java) y alertas. Hoy la salida es la consola.
