package mysimpleagent.skills;

import mysimpleagent.App;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkillsService {

    private static final Logger logger = LoggerFactory.getLogger(SkillsService.class.getSimpleName());

    private static final Path SKILLS_PROJECT_DIR = Paths.get(".agent/skills");

    public SkillsService() {}

    public List<SkillSpec> loadAll() {
        List<SkillSpec> skills = new ArrayList<>();
        if (!Files.exists(SKILLS_PROJECT_DIR) || !Files.isDirectory(SKILLS_PROJECT_DIR)) {
            return skills;
        }
        try (var stream = Files.walk(SKILLS_PROJECT_DIR)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.getFileName().toString().equals("SKILL.md"))
                  .forEach(path -> {
                      logger.atInfo()
                              .addKeyValue("path", path.toString())
                              .log("skill found");

                      var skillName = path.getParent().getFileName().toString();

                      final String skillContent;
                      try {
                          skillContent = Files.readString(path);

                      } catch (IOException e) {
                          logger.atError()
                                  .addKeyValue("path", path.toString())
                                  .setCause(e)
                                  .log("skill spec could not be read");
                          return;
                      }
                      var skillParser = new SkillSpecParser(skillContent, App.getContext().getYamlObjectMapper());
                      Optional<SkillSpec> maybeSkillSpec = skillParser.parse();
                      maybeSkillSpec.ifPresent(skill -> {
                          skill.setPath(path);
                          if (!skill.getName().equals(skillName)) {
                              logger.atWarn()
                                      .addKeyValue("path", path.toString())
                                      .addKeyValue("frontmatterSkillName", skill.getName())
                                      .addKeyValue("fileSkillName", skillName)
                                      .log("skill names mismatch. ignoring it");
                              return;
                          }
                          skills.add(skill);
                      });
                  });
        } catch (IOException e) {
            logger.error("Error traversing directory: {}", SKILLS_PROJECT_DIR, e);
        }
        return skills;
    }
}
