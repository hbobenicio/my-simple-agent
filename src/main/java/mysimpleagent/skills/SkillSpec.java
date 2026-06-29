package mysimpleagent.skills;

import java.nio.file.Path;

public class SkillSpec {
    private String name;
    private Path path;
    private String description;

    public SkillSpec() {}

    @Override
    public String toString() {
        return "name: " + name + "\ndescription: " + description + "\npath: " + path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
