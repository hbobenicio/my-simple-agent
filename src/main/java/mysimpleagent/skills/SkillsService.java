package mysimpleagent.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
//                      try {
//                          String content = Files.readString(path);
//                          System.out.println(content);
//                      } catch (IOException e) {
//                          logger.error("Error reading file: {}", path, e);
//                      }
                      var skillName = path.getParent().getFileName().toString();
                      String description = null;
                      try {
                          String skillContent = Files.readString(path);


                      } catch (IOException e) {
                          logger.atError()
                                  .addKeyValue("path", path.toString())
                                  .setCause(e)
                                  .log("skill spec could not be read");
                      }

//                      var skill = new SkillSpec(skillName, path, description);
                  });
        } catch (IOException e) {
            logger.error("Error traversing directory: {}", SKILLS_PROJECT_DIR, e);
        }
        return skills;
    }
}
