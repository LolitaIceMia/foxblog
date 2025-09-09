package com.foxsoftware.foxblog.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

@Component
public class MarkdownRenderer {
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();
    private final PolicyFactory policy = Sanitizers.BLOCKS
            .and(Sanitizers.FORMATTING)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES);

    public String render(String md) {
        if (md == null || md.isEmpty()) return "";
        String raw = renderer.render(parser.parse(md));
        return policy.sanitize(raw);
    }
}
