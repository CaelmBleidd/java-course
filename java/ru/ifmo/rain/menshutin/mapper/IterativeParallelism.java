package ru.ifmo.rain.menshutin.mapper;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        mapper = null;
    }

    private static <T> List<Stream<? extends T>> split(final int threads, final List<? extends T> list) {
        final int n = Math.min(threads, list.size());
        final int len = list.size() / n;
        int g = list.size() % n;
        List<Stream<? extends T>> subStreams = new ArrayList<>();
        for (int cur = 0; cur < list.size(); g--) {
            final int prev = cur;
            cur += len + (g > 0 ? 1 : 0);
            subStreams.add(list.subList(prev, cur).stream());
        }
        return subStreams;
    }

    private static void joinThreads(final List<Thread> threads) throws InterruptedException {
        InterruptedException exception = null;
        for (final Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("One or more threads were not joined");
                }
                exception.addSuppressed(e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private <T, R> R baseSupply(int threads, final List<? extends T> list,
                                final Function<Stream<? extends T>, ? extends R> function,
                                final Function<? super Stream<R>, R> resultCollector) throws InterruptedException {
        final List<R> result;
        final List<Stream<? extends T>> subStreams = split(threads, list);
        if (mapper != null) {
            result = mapper.map(function, subStreams);
        } else {

            result = new ArrayList<>(Collections.nCopies(subStreams.size(), null));
            final List<Thread> myThreads = IntStream.range(0, subStreams.size())
                                                    .mapToObj(threadPosition ->
                                                                      new Thread(() -> result.set(threadPosition,
                                                                                                  function.apply(subStreams.get(threadPosition)))))
                                                    .collect(Collectors.toList());
            myThreads.forEach(Thread::start);
            joinThreads(myThreads);
        }

        return resultCollector.apply(result.stream());
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param values  values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        Function<Stream<?>, String> function = s -> s.map(Object::toString).collect(Collectors.joining());
        Function<Stream<String>, String> merger = s -> s.collect(Collectors.joining());
        return baseSupply(threads, values, function, merger);
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Function<Stream<? extends T>, List<T>> function = s -> s.filter(predicate).collect(Collectors.toList());
        Function<Stream<List<T>>, List<T>> merger = s -> s.flatMap(List::stream).collect(Collectors.toList());
        return baseSupply(threads, values, function, merger);
    }

    /**
     * Maps values.
     *
     * @param threads number of concurrent threads.
     * @param values  values to filter.
     * @param f       mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        Function<Stream<? extends T>, List<U>> function = s -> s.map(f)
                                                                .collect(Collectors.toList());

        Function<Stream<List<U>>, List<U>> merger = s -> s.flatMap(List::stream)
                                                          .collect(Collectors.toList());
        return baseSupply(threads, values, function, merger);
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        Function<Stream<? extends T>, T> function = s -> s.max(comparator).orElse(null);
        return baseSupply(threads, values, function, function);
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        Function<Stream<? extends T>, Boolean> function = s -> s.allMatch(predicate);
        Function<Stream<Boolean>, Boolean> merger = s -> s.allMatch(Boolean::booleanValue);
        return baseSupply(threads, values, function, merger);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }
}
