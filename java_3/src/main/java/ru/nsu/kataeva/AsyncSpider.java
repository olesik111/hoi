package ru.nsu.kataeva;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncSpider {
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern SUCCESSORS_PATTERN = Pattern.compile("\"successors\"\\s*:\\s*\\[(.*?)\\]");
    private static final Pattern PATH_PATTERN = Pattern.compile("\"([^\"]+)\"");

    public void startCrawling(String baseUrl) {
        Set<String> visitedPaths = ConcurrentHashMap.newKeySet();
        Queue<String> messages = new ConcurrentLinkedQueue<>();

        Phaser phaser = new Phaser(1);

        long startTime = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            crawl(executor, phaser, visitedPaths, messages, baseUrl, "/");
            phaser.arriveAndAwaitAdvance();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("collected msgs: " + messages.size());
        System.out.println("duration: " + (endTime - startTime) / 1000.0 + " sek");
        System.out.println("result:");

        messages.stream()
                .sorted()
                .forEach(System.out::println);
    }

    private void crawl(ExecutorService executor, Phaser phaser, Set<String> visitedPaths,
                       Queue<String> messages, String baseUrl, String path) {
        if (!visitedPaths.add(path)) {
            return;
        }

        phaser.register();
        executor.submit(() -> {
            try {
                String safePath = path.startsWith("/") ? path : "/" + path;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + safePath))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String json = response.body();

                    Matcher msgMatcher = MESSAGE_PATTERN.matcher(json);
                    if (msgMatcher.find()) {
                        messages.add(msgMatcher.group(1));
                    }

                    Matcher succMatcher = SUCCESSORS_PATTERN.matcher(json);
                    if (succMatcher.find()) {
                        String arrayContent = succMatcher.group(1);
                        Matcher pathMatcher = PATH_PATTERN.matcher(arrayContent);

                        while (pathMatcher.find()) {
                            String nextPath = pathMatcher.group(1);
                            crawl(executor, phaser, visitedPaths, messages, baseUrl, nextPath);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("error " + path + ": " + e.getMessage());
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }
}