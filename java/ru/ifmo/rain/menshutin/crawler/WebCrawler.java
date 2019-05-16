package ru.ifmo.rain.menshutin.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final ExecutorService extractorPool;
    private final ExecutorService downloaderPool;
    private final LinkHandler handler;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        downloaderPool = Executors.newFixedThreadPool(downloaders);
        extractorPool = Executors.newFixedThreadPool(extractors);
        handler = new LinkHandler(perHost);
        this.downloader = downloader;
    }

    @Override
    public Result download(String url, int depth) {
        Phaser waiter = new Phaser(1);
        Set<String> downloaded = ConcurrentHashMap.newKeySet();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        myDownload(waiter, downloaded, errors, url, depth);
        waiter.arriveAndAwaitAdvance();
        downloaded.removeAll(errors.keySet());
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void errorCatchWrapper(Phaser waiter, Map<String, IOException> errors,
                                   String url, Callable<?> callable) {
        try {
            callable.call();
        } catch (IOException e) {
            errors.put(url, e);
        } catch (Exception e) {
            System.out.println(e.getMessage() + " " + e.getCause());
            e.printStackTrace();
        } finally {
            waiter.arrive();
        }
    }

    private void myDownload(Phaser waiter, Set<String> downloaded, Map<String, IOException> errors,
                            String url, int depth) {
        if (depth >= 1 && downloaded.add(url)) {
            final String host;
            try {
                host = URLUtils.getHost(url);
            } catch (MalformedURLException e) {
                errors.put(url, e);
                return;
            }
            waiter.register();
            handler.add(host, () -> errorCatchWrapper(waiter, errors, url,
                                                      () -> {
                                                          try {
                                                              final Document document = downloader.download(url);
                                                              if (depth != 1) {
                                                                  waiter.register();
                                                                  extractorPool.submit(() -> errorCatchWrapper(waiter, errors, url,
                                                                                                               () -> {
                                                                                                                   document.extractLinks().forEach(s -> myDownload(waiter,
                                                                                                                                                                   downloaded,
                                                                                                                                                                   errors,
                                                                                                                                                                   s,
                                                                                                                                                                   depth - 1));
                                                                                                                   return null;
                                                                                                               }));
                                                              }
                                                          } finally {
                                                              handler.finish(host);
                                                          }
                                                          return null;
                                                      }));
        }
    }

    @Override
    public void close() {
        downloaderPool.shutdown();
        extractorPool.shutdown();
    }

    private class LinkHandler {
        private final int maxPerHost;
        private final Map<String, HostData> countersAndQueue = new ConcurrentHashMap<>();

        LinkHandler(int maxPerHost) {
            this.maxPerHost = maxPerHost;
        }

        void add(String host, Runnable url) {
            HostData hostData = countersAndQueue.putIfAbsent(host, new HostData());
            if (hostData == null) {
                hostData = countersAndQueue.get(host);
            }
            hostData.locker.lock();
            if (hostData.connections == maxPerHost) {
                hostData.q.add(url);
            } else {
                downloaderPool.submit(url);
                hostData.connections++;
            }
            hostData.locker.unlock();
        }

        void finish(String host) {
            HostData hostData = countersAndQueue.get(host);
            hostData.locker.lock();
            if (!hostData.q.isEmpty()) {
                downloaderPool.submit(hostData.q.poll());
            } else {
                hostData.connections--;
            }
            hostData.locker.unlock();
        }
    }

    private class HostData {
        final Queue<Runnable> q;
        final ReentrantLock locker;
        int connections;

        HostData() {
            q = new LinkedBlockingQueue<>();
            locker = new ReentrantLock();
            connections = 0;
        }
    }

    /**
     * Run downloading.
     *
     * @param args = url [downloads [extractors [perHost]]]
     */
    public static void main(String[] args) {
        int defaultDownloads = 10;
        int defaultExtractors = 10;
        int defaultPerHost = 10;
        int defaultDepth = 1;

        if (args.length > 5) {
            System.out.println("Too many arguments.");
            return;
        }
        if (args.length < 1) {
            System.out.println("Too few arguments.");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Expected non null argument.");
                return;
            }
        }
        String url = args[0];
        int downloads;
        int extractors;
        int perHost;
        int depth;
        try {
            downloads = args.length > 1 ? Integer.parseInt(args[1]) : defaultDownloads;
            extractors = args.length > 2 ? Integer.parseInt(args[2]) : defaultExtractors;
            perHost = args.length > 3 ? Integer.parseInt(args[3]) : defaultPerHost;
            depth = args.length > 4 ? Integer.parseInt(args[4]) : defaultDepth;
        } catch (NumberFormatException e) {
            error("Can't parse some integer argument", e);
            return;
        }

        WebCrawler crawler;
        try {
            crawler = new WebCrawler(new CachingDownloader(), downloads, extractors, perHost);
        } catch (IOException e) {
            error("Problem with creating Downloader:", e);
            return;
        }
        crawler.download(url, depth);
    }

    private static void error(String msg, Exception e) {
        System.out.println(msg);
        System.out.println("Exception's message: " + e.getMessage());
    }
}
