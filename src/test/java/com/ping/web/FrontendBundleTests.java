package com.ping.web;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Recursos derivados para móvil: el subconjunto de Bootstrap no puede perder una clase que el marcado usa. */
class FrontendBundleTests {
    private static final Path RES = Path.of("src/main/resources");
    private static final Pattern CLASS = Pattern.compile("\\.(-?[_a-zA-Z][_a-zA-Z0-9-]*)");

    @Test void bootstrapSubsetKeepsEveryBootstrapClassUsedByTemplatesScriptsOrJava() throws IOException {
        Set<String> full = classes(RES.resolve("static/vendor/bootstrap/bootstrap.min.css"));
        Set<String> subset = classes(RES.resolve("static/vendor/bootstrap/bootstrap.ping.min.css"));
        Set<String> words = new HashSet<>();
        for (Path root : List.of(RES.resolve("templates"), RES.resolve("static/js"), Path.of("src/main/java")))
            try (Stream<Path> files = Files.walk(root)) {
                for (Path f : files.filter(Files::isRegularFile).toList())
                    words.addAll(Arrays.asList(Files.readString(f).split("[^A-Za-z0-9_-]+")));
            }
        words.retainAll(full);
        // Si falla: python docs/performance/frontend/tools/bootstrap_subset.py
        assertThat(subset).containsAll(words);
    }

    private static Set<String> classes(Path css) throws IOException {
        String text = Files.readString(css).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("url\\([^)]*\\)", "").replaceAll("\\{[^{}]*}", "{}");
        Set<String> out = new HashSet<>();
        var m = CLASS.matcher(text);
        while (m.find()) out.add(m.group(1));
        return out;
    }
}
