package mysimpleagent.tools.functions;

import mysimpleagent.tools.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class GetCurrentWeather implements Tool {

    private static record Args(
            String location,
            String unit
    ) {}

    private static final Logger logger = LoggerFactory.getLogger(GetCurrentWeather.class.getSimpleName());

    private final ObjectMapper objectMapper;

    public GetCurrentWeather(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "get_current_weather";
    }

    @Override
    public String call(String argsString) {
        logger.atInfo()
                .addKeyValue("args", argsString)
                .log("executing tool `get_current_weather`...");
        Args args = objectMapper.readValue(argsString, Args.class);
        return String.format("The current weather at %s is fine. Temperature is 25º %s", args.location, args.unit);
    }
}
