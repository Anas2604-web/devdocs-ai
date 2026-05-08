# Error handling — DevDocs AI

## Standard error response format

Every API error returns this JSON structure — no exceptions:

```json
{
  "success": false,
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "An account with this email already exists.",
    "details": null,
    "timestamp": "2026-05-06T10:30:00Z",
    "requestId": "req_a1b2c3d4"
  }
}
```

## Error codes by domain

### Auth errors (4xx)

| Code | HTTP | When |
|---|---|---|
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `TOKEN_EXPIRED` | 401 | Access token expired |
| `TOKEN_INVALID` | 401 | Malformed or tampered token |
| `REFRESH_TOKEN_INVALID` | 401 | Refresh token not found or expired |
| `EMAIL_ALREADY_EXISTS` | 409 | Registration with duplicate email |
| `WEAK_PASSWORD` | 400 | Password doesn't meet requirements |
| `ACCESS_DENIED` | 403 | Role doesn't have permission |

### Spec / ingestion errors

| Code | HTTP | When |
|---|---|---|
| `INVALID_SPEC_FORMAT` | 400 | File is not valid OpenAPI YAML/JSON |
| `SPEC_TOO_LARGE` | 400 | File exceeds 5MB |
| `SPEC_NOT_FOUND` | 404 | Spec ID doesn't exist for this tenant |
| `INGESTION_FAILED` | 500 | Pipeline failed (check logs for cause) |
| `UPLOAD_LIMIT_EXCEEDED` | 429 | >10 uploads per day on free plan |

### RAG / chat errors

| Code | HTTP | When |
|---|---|---|
| `RATE_LIMIT_EXCEEDED` | 429 | >60 questions/hour for this tenant |
| `NO_SPECS_UPLOADED` | 400 | Tenant has no READY specs to search |
| `SESSION_NOT_FOUND` | 404 | Chat session ID is invalid or expired |
| `OPENAI_ERROR` | 503 | OpenAI API returned an error |
| `CONTEXT_TOO_LONG` | 400 | Question + history exceeds context window |

### General errors

| Code | HTTP | When |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Request body fails @Valid checks |
| `RESOURCE_NOT_FOUND` | 404 | Generic not found |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

## GlobalExceptionHandler implementation

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DevDocsException.class)
    public ResponseEntity<ErrorResponse> handleDevDocsException(
            DevDocsException ex, HttpServletRequest request) {
        log.warn("Business error: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(ex.getCode(), ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of("VALIDATION_ERROR", message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex); // full stack trace for 500s
        return ResponseEntity.internalServerError()
            .body(ErrorResponse.of("INTERNAL_ERROR", "Something went wrong.", request));
    }
}
```

## Rule: never leak internals

Bad response (leaks stack trace):
```json
{ "error": "org.springframework.dao.DataIntegrityViolationException: ERROR: duplicate key value violates unique constraint..." }
```

Good response (user-facing message only):
```json
{ "error": { "code": "EMAIL_ALREADY_EXISTS", "message": "An account with this email already exists." } }
```

Log the full exception server-side. Return only the error code and clean message to the client.
