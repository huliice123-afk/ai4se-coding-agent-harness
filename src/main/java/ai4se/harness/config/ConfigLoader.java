package ai4se.harness.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Path;

public class ConfigLoader {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static HarnessConfig load(Path yamlPath) throws IOException {
        return mapper.readValue(yamlPath.toFile(), HarnessConfig.class);
    }
}