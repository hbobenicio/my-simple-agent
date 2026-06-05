package mysimpleagent.utils;

import mysimpleagent.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ResourceUtils {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class.getSimpleName());

    public static String loadResourceAsString(String resourcePath) {
        logger.atInfo().addKeyValue("path", resourcePath).log("carregando recurso...");
        try (InputStream is = Config.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new FileNotFoundException(resourcePath);
            }
            byte[] contents = is.readAllBytes();
            var result = new String(contents, StandardCharsets.UTF_8);
            logger.atInfo().addKeyValue("path", resourcePath).log("recurso carregado com sucesso.");
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
