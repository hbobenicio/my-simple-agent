package mysimpleagent.skills;

import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkillSpecParser {

    private static final Pattern frontmatterPattern =
            Pattern.compile("^---\\r?\\n(.*?)\\r?\\n---", Pattern.DOTALL);

    private final String input;
    private final ObjectMapper yamlObjectMapper;

    public SkillSpecParser(String input, ObjectMapper yamlObjectMapper) {
        this.input = input;
        this.yamlObjectMapper = yamlObjectMapper;
    }

    public Optional<SkillSpec> parse() {
        Matcher matcher = frontmatterPattern.matcher(this.input);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String frontmatterData = matcher.group(1);
        SkillSpec skillSpec = this.yamlObjectMapper.readValue(frontmatterData, SkillSpec.class);
        return Optional.of(skillSpec);
    }
}
