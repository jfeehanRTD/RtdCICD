package com.jci.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TemplateEngine {

    private final MustacheFactory mustacheFactory;

    public TemplateEngine() {
        this.mustacheFactory = new DefaultMustacheFactory();
    }

    public String render(String templateName, Map<String, Object> context) throws IOException {
        String templatePath = "templates/" + templateName;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (is == null) {
                throw new IOException("Template not found: " + templatePath);
            }

            Mustache mustache = mustacheFactory.compile(new InputStreamReader(is), templateName);
            StringWriter writer = new StringWriter();
            mustache.execute(writer, context);
            return writer.toString();
        }
    }

    public void renderToFile(String templateName, Map<String, Object> context, Path outputPath) throws IOException {
        String content = render(templateName, context);

        // Ensure parent directories exist
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        Files.writeString(outputPath, content);
    }

    public boolean templateExists(String templateName) {
        String templatePath = "templates/" + templateName;
        return getClass().getClassLoader().getResource(templatePath) != null;
    }
}
