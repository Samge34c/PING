package com.ping.web;

import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.*;

/** Cache only validated, content-addressed files from the public classpath. */
@Configuration
public class PublicResourceConfiguration implements WebMvcConfigurer {
    private static final Pattern VERSIONED = Pattern.compile(
            "^/(?:css|js|img|fonts|vendor)/.+-[a-f0-9]{32}\\.[a-zA-Z0-9]+$");
    private final ObjectProvider<ResourceUrlProvider> urls;

    public PublicResourceConfiguration(ObjectProvider<ResourceUrlProvider> urls) { this.urls = urls; }

    @Bean ResourceUrlEncodingFilter resourceUrlEncodingFilter() { return new ResourceUrlEncodingFilter(); }

    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"))
                .addTransformer(new CssLinkResourceTransformer());
    }

    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response, Object handler) {
                if (!(handler instanceof ResourceHttpRequestHandler)
                        || !(request.getMethod().equals("GET") || request.getMethod().equals("HEAD"))) return true;
                String path = request.getRequestURI().substring(request.getContextPath().length());
                if (VERSIONED.matcher(path).matches()) {
                    String original = path.replaceFirst("-[a-f0-9]{32}(?=\\.[^/]+$)", "");
                    // A hash-looking URL alone must not make an error or an old file immutable.
                    if (path.equals(urls.getObject().getForLookupPath(original))) {
                        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
                    }
                }
                return true;
            }
        });
    }
}
