package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MoviesStore {

    private final Map<Long, Movie> movies = Collections.synchronizedMap(new HashMap<>());
    private final AtomicLong nextId = new AtomicLong(1);

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Movie add(Movie movie) {
        long id = nextId.getAndIncrement();

        Movie savedMovie = new Movie(
                id,
                movie.getTitle(),
                movie.getYear()
        );

        movies.put(id, savedMovie);

        return savedMovie;
    }

    public Movie findById(long id) {
        return movies.get(id);
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public List<Movie> findByYear(int year) {
        List<Movie> result = new ArrayList<>();

        synchronized (movies) {
            for (Movie movie : movies.values()) {
                if (movie.getYear() == year) {
                    result.add(movie);
                }
            }
        }

        return result;
    }

    public void clear() {
        movies.clear();
        nextId.set(1);
    }
}