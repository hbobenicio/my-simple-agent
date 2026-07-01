package br.com.hbobenicio.mysimpleagent.skills;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.util.Optional;

public class SkillSpecParserTest {

    private static ObjectMapper YAML_OBJECT_MAPPER;

    @BeforeAll
    public static void init() {
        YAML_OBJECT_MAPPER = new YAMLMapper();
    }

    @Test
    public void parse() {
        var source =
                """
                ---
                name: foo
                description: bar
                ---
                
                # Instructions
                
                - Must Work!
                """;

        var parser = new SkillSpecParser(source, YAML_OBJECT_MAPPER);
        Optional<SkillSpec> maybeSkillSpec = parser.parse();
        Assertions.assertTrue(maybeSkillSpec.isPresent());
        Assertions.assertEquals("foo", maybeSkillSpec.get().getName());
        Assertions.assertEquals("bar", maybeSkillSpec.get().getDescription());
    }

    @Test
    public void missingFrontMatter() {
        var source =
                """
                # Instructions
                
                - Must Work!
                """;

        var parser = new SkillSpecParser(source, YAML_OBJECT_MAPPER);
        Optional<SkillSpec> maybeSkillSpec = parser.parse();
        Assertions.assertFalse(maybeSkillSpec.isPresent());
    }
}
