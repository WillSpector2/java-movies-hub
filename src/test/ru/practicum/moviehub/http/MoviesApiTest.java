package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoviesApiTest {

    private static final int PORT = 8080;

    private static MoviesServer server;
    private static MoviesStore store;

    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {

        store = new MoviesStore();

        server = new MoviesServer(
                store,
                PORT
        );

        server.start();

        client = HttpClient.newHttpClient();
        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    // GET /movies

    @Test
    void getMovies_whenEmpty_returnsEmptyArray()
            throws Exception {

        HttpResponse<String> response =
                get("/movies");

        assertEquals(
                200,
                response.statusCode()
        );

        assertEquals(
                "application/json; charset=UTF-8",
                response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
        );

        List<Movie> movies =
                gson.fromJson(
                        response.body(),
                        new ListOfMoviesTypeToken().getType()
                );

        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenMoviesExist_returnsMovies()
            throws Exception {

        postMovie("Interstellar", 2014);
        postMovie("The Matrix", 1999);

        HttpResponse<String> response =
                get("/movies");

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies =
                gson.fromJson(
                        response.body(),
                        new ListOfMoviesTypeToken().getType()
                );

        assertEquals(2, movies.size());
        assertEquals("Interstellar", movies.get(0).getTitle());
        assertEquals("The Matrix", movies.get(1).getTitle());
    }

    // POST /movies

    @Test
    void postMovie_withValidData_returns201()
            throws Exception {

        HttpResponse<String> response =
                postMovie("Interstellar", 2014);

        assertEquals(
                201,
                response.statusCode()
        );

        Movie movie =
                gson.fromJson(
                        response.body(),
                        Movie.class
                );

        assertEquals(
                1,
                movie.getId()
        );

        assertEquals(
                "Interstellar",
                movie.getTitle()
        );

        assertEquals(
                2014,
                movie.getYear()
        );
    }

    @Test
    void postMovie_withEmptyTitle_returns422()
            throws Exception {

        HttpResponse<String> response =
                postMovie("", 2014);

        assertEquals(
                422,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Ошибка валидации")
        );

        assertTrue(
                response.body()
                        .contains("название не должно быть пустым")
        );
    }

    @Test
    void postMovie_withTooLongTitle_returns422()
            throws Exception {

        String title = "a".repeat(101);

        HttpResponse<String> response =
                postMovie(title, 2014);

        assertEquals(
                422,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("100")
        );
    }

    @Test
    void postMovie_withYearBefore1888_returns422()
            throws Exception {

        HttpResponse<String> response =
                postMovie("Movie", 1887);

        assertEquals(
                422,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("1888")
        );
    }

    @Test
    void postMovie_withYearAfterAllowedRange_returns422()
            throws Exception {

        int invalidYear =
                java.time.Year.now().getValue() + 2;

        HttpResponse<String> response =
                postMovie(
                        "Movie",
                        invalidYear
                );

        assertEquals(
                422,
                response.statusCode()
        );
    }

    @Test
    void postMovie_withWrongContentType_returns415()
            throws Exception {

        String json = """
                {
                    "title": "Interstellar",
                    "year": 2014
                }
                """;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri("/movies"))
                        .header(
                                "Content-Type",
                                "text/plain"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(
                415,
                response.statusCode()
        );
    }

    @Test
    void postMovie_withInvalidJson_returns400()
            throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri("/movies"))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(
                                                "{ invalid json"
                                        )
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(
                400,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Некорректный JSON")
        );
    }

    // GET /movies/{id}

    @Test
    void getMovieById_whenExists_returnsMovie()
            throws Exception {

        HttpResponse<String> created =
                postMovie(
                        "Interstellar",
                        2014
                );

        Movie createdMovie =
                gson.fromJson(
                        created.body(),
                        Movie.class
                );

        HttpResponse<String> response =
                get(
                        "/movies/"
                                + createdMovie.getId()
                );

        assertEquals(
                200,
                response.statusCode()
        );

        Movie movie =
                gson.fromJson(
                        response.body(),
                        Movie.class
                );

        assertEquals(
                createdMovie.getId(),
                movie.getId()
        );

        assertEquals(
                "Interstellar",
                movie.getTitle()
        );
    }

    @Test
    void getMovieById_whenNotFound_returns404()
            throws Exception {

        HttpResponse<String> response =
                get("/movies/999");

        assertEquals(
                404,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Фильм не найден")
        );
    }

    @Test
    void getMovieById_withInvalidId_returns400()
            throws Exception {

        HttpResponse<String> response =
                get("/movies/abc");

        assertEquals(
                400,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Некорректный ID")
        );
    }

    // DELETE /movies/{id}

    @Test
    void deleteMovie_whenExists_returns204()
            throws Exception {

        HttpResponse<String> created =
                postMovie(
                        "Interstellar",
                        2014
                );

        Movie movie =
                gson.fromJson(
                        created.body(),
                        Movie.class
                );

        HttpResponse<String> response =
                delete(
                        "/movies/" + movie.getId()
                );

        assertEquals(
                204,
                response.statusCode()
        );

        HttpResponse<String> getResponse =
                get(
                        "/movies/" + movie.getId()
                );

        assertEquals(
                404,
                getResponse.statusCode()
        );
    }

    @Test
    void deleteMovie_whenNotFound_returns404()
            throws Exception {

        HttpResponse<String> response =
                delete("/movies/999");

        assertEquals(
                404,
                response.statusCode()
        );
    }

    @Test
    void deleteMovie_withInvalidId_returns400()
            throws Exception {

        HttpResponse<String> response =
                delete("/movies/abc");

        assertEquals(
                400,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Некорректный ID")
        );
    }

    // GET /movies?year=YYYY

    @Test
    void getMoviesByYear_returnsMatchingMovies()
            throws Exception {

        postMovie("Interstellar", 2014);
        postMovie("Another movie", 2014);
        postMovie("The Matrix", 1999);

        HttpResponse<String> response =
                get("/movies?year=2014");

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies =
                gson.fromJson(
                        response.body(),
                        new ListOfMoviesTypeToken().getType()
                );

        assertEquals(
                2,
                movies.size()
        );

        assertTrue(
                movies.stream()
                        .allMatch(movie ->
                                movie.getYear() == 2014)
        );
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray()
            throws Exception {

        HttpResponse<String> response =
                get("/movies?year=2000");

        assertEquals(
                200,
                response.statusCode()
        );

        List<Movie> movies =
                gson.fromJson(
                        response.body(),
                        new ListOfMoviesTypeToken().getType()
                );

        assertTrue(
                movies.isEmpty()
        );
    }

    @Test
    void getMoviesByYear_withInvalidYear_returns400()
            throws Exception {

        HttpResponse<String> response =
                get("/movies?year=abc");

        assertEquals(
                400,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains(
                                "Некорректный параметр"
                        )
        );
    }

    // 405 Method Not Allowed

    @Test
    void unsupportedMethod_returns405()
            throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri("/movies"))
                        .method(
                                "PATCH",
                                HttpRequest.BodyPublishers
                                        .noBody()
                        )
                        .build();

        HttpResponse<String> response =
                client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        assertEquals(
                405,
                response.statusCode()
        );

        assertTrue(
                response.body()
                        .contains("Метод не поддерживается")
        );
    }

    // Helpers

    private HttpResponse<String> postMovie(
            String title,
            int year
    ) throws Exception {

        String json = gson.toJson(
                new Movie(0, title, year)
        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri("/movies"))
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(json)
                        )
                        .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> get(
            String path
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri(path))
                        .GET()
                        .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> delete(
            String path
    ) throws Exception {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri(path))
                        .DELETE()
                        .build();

        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private URI uri(String path) {
        return URI.create(
                "http://localhost:"
                        + PORT
                        + path
        );
    }
}