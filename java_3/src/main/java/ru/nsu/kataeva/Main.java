package ru.nsu.kataeva;

public class Main {
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String baseUrl = "http://localhost:" + port;

        AsyncSpider spider = new AsyncSpider();
        spider.startCrawling(baseUrl);
    }
}