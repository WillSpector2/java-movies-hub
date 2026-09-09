package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {

    private final MoviesStore store;
    private final int port;

    private HttpServer server;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(
                    new InetSocketAddress("localhost", port),
                    0
            );

            server.createContext(
                    "/movies",
                    new MoviesHandler(store)
            );

            server.setExecutor(null);
            server.start();

        } catch (IOException e) {
            throw new RuntimeException(
                    "Не удалось запустить сервер",
                    e
            );
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}