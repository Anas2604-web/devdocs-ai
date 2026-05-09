package com.devdocsai.ingestion;

import com.devdocsai.common.DevDocsException;
import com.devdocsai.ingestion.model.ApiChunk;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    /**
     * Parses an OpenAPI spec string and returns a list of chunks.
     * Each HTTP endpoint becomes one chunk with all its details as text.
     */
    public List<ApiChunk> parseAndChunk(String specContent, UUID specId, UUID tenantId) {
        SwaggerParseResult result = new OpenAPIParser().readContents(specContent, null, null);

        if (result.getOpenAPI() == null) {
            String errors = result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown parse error";
            log.error("Failed to parse OpenAPI spec: {}", errors);
            throw new DevDocsException("INVALID_SPEC_FORMAT",
                "Could not parse OpenAPI spec: " + errors, HttpStatus.BAD_REQUEST);
        }

        OpenAPI openAPI = result.getOpenAPI();
        List<ApiChunk> chunks = new ArrayList<>();

        if (openAPI.getPaths() == null) {
            log.warn("Spec has no paths defined");
            return chunks;
        }

        // Create one chunk per HTTP method+path combination
        for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
            String path = pathEntry.getKey();
            PathItem pathItem = pathEntry.getValue();

            Map<String, Operation> operations = getOperations(pathItem);

            for (Map.Entry<String, Operation> opEntry : operations.entrySet()) {
                String method = opEntry.getKey();
                Operation operation = opEntry.getValue();

                String chunkText = buildChunkText(method, path, operation, openAPI);
                ApiChunk chunk = new ApiChunk(specId, tenantId, method.toUpperCase(), path, chunkText);
                chunks.add(chunk);

                log.debug("Created chunk: {} {}", method.toUpperCase(), path);
            }
        }

        // Also create a summary chunk for the whole API
        String summaryText = buildSummaryChunk(openAPI);
        chunks.add(new ApiChunk(specId, tenantId, "SUMMARY", "/", summaryText));

        log.info("Created {} chunks from spec", chunks.size());
        return chunks;
    }

    /**
     * Builds rich text for one endpoint — method, path, description,
     * parameters, request body, and response schemas.
     * The richer the text, the better the embedding quality.
     */
    private String buildChunkText(String method, String path, Operation operation, OpenAPI openAPI) {
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP Method: ").append(method.toUpperCase()).append("\n");
        sb.append("Path: ").append(path).append("\n");

        if (operation.getSummary() != null) {
            sb.append("Summary: ").append(operation.getSummary()).append("\n");
        }
        if (operation.getDescription() != null) {
            sb.append("Description: ").append(operation.getDescription()).append("\n");
        }
        if (operation.getTags() != null) {
            sb.append("Tags: ").append(String.join(", ", operation.getTags())).append("\n");
        }
        if (operation.getOperationId() != null) {
            sb.append("Operation ID: ").append(operation.getOperationId()).append("\n");
        }

        // Parameters
        if (operation.getParameters() != null && !operation.getParameters().isEmpty()) {
            sb.append("\nParameters:\n");
            for (Parameter param : operation.getParameters()) {
                sb.append("  - ").append(param.getName())
                  .append(" (").append(param.getIn()).append(")")
                  .append(Boolean.TRUE.equals(param.getRequired()) ? " [required]" : " [optional]");
                if (param.getDescription() != null) {
                    sb.append(": ").append(param.getDescription());
                }
                sb.append("\n");
            }
        }

        // Request body
        if (operation.getRequestBody() != null) {
            sb.append("\nRequest Body:\n");
            if (operation.getRequestBody().getDescription() != null) {
                sb.append("  Description: ").append(operation.getRequestBody().getDescription()).append("\n");
            }
            if (operation.getRequestBody().getContent() != null) {
                sb.append("  Content types: ")
                  .append(String.join(", ", operation.getRequestBody().getContent().keySet()))
                  .append("\n");
            }
        }

        // Responses
        if (operation.getResponses() != null) {
            sb.append("\nResponses:\n");
            for (Map.Entry<String, ApiResponse> resp : operation.getResponses().entrySet()) {
                sb.append("  ").append(resp.getKey());
                if (resp.getValue().getDescription() != null) {
                    sb.append(": ").append(resp.getValue().getDescription());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Creates a summary chunk for the whole API — title, description, servers, auth schemes.
     * Useful for "what does this API do?" questions.
     */
    private String buildSummaryChunk(OpenAPI openAPI) {
        StringBuilder sb = new StringBuilder();
        sb.append("API Summary\n");

        if (openAPI.getInfo() != null) {
            if (openAPI.getInfo().getTitle() != null)
                sb.append("Title: ").append(openAPI.getInfo().getTitle()).append("\n");
            if (openAPI.getInfo().getDescription() != null)
                sb.append("Description: ").append(openAPI.getInfo().getDescription()).append("\n");
            if (openAPI.getInfo().getVersion() != null)
                sb.append("Version: ").append(openAPI.getInfo().getVersion()).append("\n");
        }
        if (openAPI.getServers() != null) {
            sb.append("Servers: ");
            openAPI.getServers().forEach(s -> sb.append(s.getUrl()).append(" "));
            sb.append("\n");
        }
        if (openAPI.getPaths() != null) {
            sb.append("Total endpoints: ").append(openAPI.getPaths().size()).append("\n");
        }
        if (openAPI.getComponents() != null && openAPI.getComponents().getSecuritySchemes() != null) {
            sb.append("Authentication schemes: ")
              .append(String.join(", ", openAPI.getComponents().getSecuritySchemes().keySet()))
              .append("\n");
        }
        return sb.toString();
    }

    private Map<String, Operation> getOperations(PathItem item) {
        Map<String, Operation> ops = new LinkedHashMap<>();
        if (item.getGet()    != null) ops.put("GET",    item.getGet());
        if (item.getPost()   != null) ops.put("POST",   item.getPost());
        if (item.getPut()    != null) ops.put("PUT",    item.getPut());
        if (item.getPatch()  != null) ops.put("PATCH",  item.getPatch());
        if (item.getDelete() != null) ops.put("DELETE", item.getDelete());
        if (item.getHead()   != null) ops.put("HEAD",   item.getHead());
        if (item.getOptions()!= null) ops.put("OPTIONS",item.getOptions());
        return ops;
    }
}
