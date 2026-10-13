package com.ping.support;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class TestBrowser implements AutoCloseable {
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final String base;
    private String csrf;
    public TestBrowser(int port) { base = "http://127.0.0.1:" + port; }
    public HttpResponse<String> get(String path) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(base).resolve(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        var match = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(response.body());
        if (match.find()) csrf = match.group(1);
        return response;
    }
    public HttpResponse<String> post(String path, Map<String, String> values) throws Exception {
        var fields = new LinkedHashMap<>(values);
        if (csrf != null) fields.put("_csrf", csrf);
        String body = fields.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(base).resolve(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    public void close() { client.close(); }
}
