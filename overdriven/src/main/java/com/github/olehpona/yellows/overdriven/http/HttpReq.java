package com.github.olehpona.yellows.overdriven.http;

import com.github.olehpona.yellows.api.context.GlobalContextFactory;
import com.github.olehpona.yellows.api.context.PluginReadWrapper;
import com.github.olehpona.yellows.api.context.PluginWriteWrapper;
import com.github.olehpona.yellows.api.plugins.Plugin;
import com.github.olehpona.yellows.api.plugins.PluginCallback;
import com.github.olehpona.yellows.api.plugins.PluginNode;
import com.google.auto.service.AutoService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@AutoService(PluginNode.class)
@Plugin(id = "overdriven.http.http_request")
public class HttpReq implements PluginNode {

    @Override
    public void execute(PluginReadWrapper input, PluginCallback cb) {
        PluginReadWrapper method =  input.resolvePath("method");
        PluginReadWrapper headers = input.resolvePath("headers");
        PluginReadWrapper url = input.resolvePath("url");
        PluginReadWrapper body = input.resolvePath("body");

        if (!method.isString()) {
            cb.fail(new IllegalArgumentException("method is not a string"));
        }
        if (!headers.isObject() && !headers.isMissing()) {
            cb.fail(new IllegalArgumentException("headers is not a object"));
        }
        if (!url.isString()) {
            cb.fail(new IllegalArgumentException("url is not a string"));
        }
        if (!body.isString() && !body.isMissing()) {
            cb.fail(new IllegalArgumentException("body is not a string"));
        }

        try (HttpClient client = HttpClient.newHttpClient()){
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url.asString()));
            if (headers.isObject()) {
                for (var entry: headers.entries()) {
                    requestBuilder.setHeader(entry.getKey(),  entry.getValue().toString());
                }
            }

            switch (method.asString()) {
                case "GET":
                    requestBuilder = requestBuilder.GET();
                    break;
                case "POST":
                    requestBuilder = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body.asString()));
                    break;
                case "PUT":
                    requestBuilder = requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body.asString()));
                    break;
                case "DELETE":
                    requestBuilder = requestBuilder.DELETE();
                    break;
                default:
                    cb.fail(new IllegalArgumentException("method is not GET/POST/PUT/DELETE"));
                    return;
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            PluginWriteWrapper output = GlobalContextFactory.createObject();
            output.putPath("code", response.statusCode());
            output.putPath("body", response.body());

            cb.completeAndReturn(output, List.of());

        } catch (Exception e) {
            cb.fail(e);
        };
    }
}
