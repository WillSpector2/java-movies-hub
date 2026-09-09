package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final String JSON_CONTENT_TYPE =
            "application/json; charset=UTF-8";

    protected final Gson gson = new Gson();

    protected void sendJson(
            HttpExchange exchange,
            int status,
            Object body
    ) throws IOException {

        byte[] response = gson.toJson(body)
                .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                JSON_CONTENT_TYPE
        );

        exchange.sendResponseHeaders(status, response.length);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    protected void sendEmpty(
            HttpExchange exchange,
            int status
    ) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                JSON_CONTENT_TYPE
        );

        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    protected void sendError(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {

        sendJson(
                exchange,
                status,
                new ErrorResponse(message)
        );
    }

    protected void sendValidationError(
            HttpExchange exchange,
            List<String> details
    ) throws IOException {

        sendJson(
                exchange,
                422,
                new ErrorResponse(
                        "Ошибка валидации",
                        details
                )
        );
    }

    protected boolean isJsonContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null) {
            return false;
        }

        return contentType
                .toLowerCase()
                .startsWith("application/json");
    }
}