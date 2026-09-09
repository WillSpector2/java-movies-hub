package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        handleMovies(exchange);
    }

    private void handleMovies(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();

        String path = uri.getPath();

        if ("/movies".equals(path) || "/movies/".equals(path)) {

            switch (method) {
                case "GET":
                    handleGetMovies(exchange);
                    return;

                case "POST":
                    handlePostMovie(exchange);
                    return;

                default:
                    sendError(
                            exchange,
                            405,
                            "Метод не поддерживается"
                    );
                    return;
            }
        }

        if (path.startsWith("/movies/")) {

            String idString = path.substring("/movies/".length());

            switch (method) {
                case "GET":
                    handleGetMovieById(exchange, idString);
                    return;

                case "DELETE":
                    handleDeleteMovie(exchange, idString);
                    return;

                default:
                    sendError(
                            exchange,
                            405,
                            "Метод не поддерживается"
                    );
                    return;
            }
        }

        sendError(
                exchange,
                404,
                "Ресурс не найден"
        );
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {

        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isEmpty()) {
            sendJson(
                    exchange,
                    200,
                    store.findAll()
            );
            return;
        }

        String yearValue = getQueryParameter(query, "year");

        if (yearValue == null) {
            sendError(
                    exchange,
                    400,
                    "Некорректный параметр запроса — 'year'"
            );
            return;
        }

        try {
            int year = Integer.parseInt(yearValue);

            sendJson(
                    exchange,
                    200,
                    store.findByYear(year)
            );

        } catch (NumberFormatException e) {
            sendError(
                    exchange,
                    400,
                    "Некорректный параметр запроса — 'year'"
            );
        }
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {

        if (!isJsonContentType(exchange)) {
            sendError(
                    exchange,
                    415,
                    "Неподдерживаемый Content-Type"
            );
            return;
        }

        String requestBody;

        try {
            requestBody = readBody(exchange);
        } catch (IOException e) {
            sendError(
                    exchange,
                    400,
                    "Не удалось прочитать тело запроса"
            );
            return;
        }

        Movie movie;

        try {
            movie = gson.fromJson(requestBody, Movie.class);

            if (movie == null) {
                sendError(
                        exchange,
                        400,
                        "Некорректный JSON"
                );
                return;
            }

        } catch (JsonSyntaxException e) {
            sendError(
                    exchange,
                    400,
                    "Некорректный JSON"
            );
            return;
        }

        List<String> validationErrors = validateMovie(movie);

        if (!validationErrors.isEmpty()) {
            sendValidationError(
                    exchange,
                    validationErrors
            );
            return;
        }

        Movie savedMovie = store.add(movie);

        sendJson(
                exchange,
                201,
                savedMovie
        );
    }

    private void handleGetMovieById(HttpExchange exchange, String idString) throws IOException {

        Long id = parseId(idString);

        if (id == null) {
            sendError(
                    exchange,
                    400,
                    "Некорректный ID"
            );
            return;
        }

        Movie movie = store.findById(id);

        if (movie == null) {
            sendError(
                    exchange,
                    404,
                    "Фильм не найден"
            );
            return;
        }

        sendJson(
                exchange,
                200,
                movie
        );
    }

    private void handleDeleteMovie(HttpExchange exchange, String idString) throws IOException {

        Long id = parseId(idString);

        if (id == null) {
            sendError(
                    exchange,
                    400,
                    "Некорректный ID"
            );
            return;
        }

        boolean deleted = store.delete(id);

        if (!deleted) {
            sendError(
                    exchange,
                    404,
                    "Фильм не найден"
            );
            return;
        }

        sendEmpty(
                exchange,
                204
        );
    }

    private List<String> validateMovie(Movie movie) {

        List<String> errors = new ArrayList<>();

        String title = movie.getTitle();

        if (title == null || title.trim().isEmpty()) {
            errors.add("название не должно быть пустым");
        } else if (title.length() > 100) {
            errors.add(
                    "название не должно содержать более 100 символов"
            );
        }

        int currentYear = Year.now().getValue();
        int maxYear = currentYear + 1;

        if (movie.getYear() < 1888 ||
                movie.getYear() > maxYear) {

            errors.add(
                    "год должен быть между 1888 и " + maxYear
            );
        }

        return errors;
    }

    private Long parseId(String value) {

        try {
            if (value == null || value.isEmpty()) {
                return null;
            }

            return Long.parseLong(value);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {

        try (InputStream inputStream =
                     exchange.getRequestBody()) {

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }
    }

    private String getQueryParameter(String query, String parameter) {

        for (String pair : query.split("&")) {

            String[] parts = pair.split("=", 2);

            String key = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
            );

            if (parameter.equals(key)) {

                if (parts.length < 2) {
                    return "";
                }

                return URLDecoder.decode(
                        parts[1],
                        StandardCharsets.UTF_8
                );
            }
        }

        return null;
    }
}