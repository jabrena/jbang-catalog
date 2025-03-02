///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.networknt:json-schema-validator:1.3.2
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.3
//DEPS info.picocli:picocli:4.7.5
//DEPS org.slf4j:slf4j-api:2.0.9
//DEPS ch.qos.logback:logback-classic:1.4.11

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * JBang Schema Validator
 * 
 * A utility to validate jbang-catalog.json against the corresponding schema.
 * Designed to be run in CI/CD pipelines and Docker containers.
 */
@Command(name = "validate", mixinStandardHelpOptions = true, version = "1.0",
         description = "Validates a JBang catalog JSON file against its schema.")
public class JbangSchemaValidator implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(JbangSchemaValidator.class);

    @Option(names = {"-s", "--schema"}, description = "Path to the schema file")
    private String schemaFile = System.getenv().getOrDefault("SCHEMA_PATH", "jbang-catalog-schema.json");

    @Option(names = {"-j", "--json"}, description = "Path to the JSON file to validate")
    private String jsonFile = System.getenv().getOrDefault("JSON_PATH", "jbang-catalog.json");

    @Option(names = {"--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JbangSchemaValidator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            logger.debug("Starting validation");
            logger.debug("Schema file: {}", schemaFile);
            logger.debug("JSON file: {}", jsonFile);
            
            // Define file paths
            Path schemaPath = Paths.get(schemaFile);
            Path jsonPath = Paths.get(jsonFile);
            
            // Check if files exist
            if (!Files.exists(schemaPath)) {
                logger.error("Schema file not found: {}", schemaPath.toAbsolutePath());
                return 1;
            }
            
            if (!Files.exists(jsonPath)) {
                logger.error("JSON file not found: {}", jsonPath.toAbsolutePath());
                return 1;
            }
            
            // Create ObjectMapper (Jackson)
            ObjectMapper objectMapper = new ObjectMapper();
            
            // Load schema file
            JsonNode schemaNode = objectMapper.readTree(schemaPath.toFile());
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = schemaFactory.getSchema(schemaNode);
            
            // Load and validate JSON file
            JsonNode jsonNode = objectMapper.readTree(jsonPath.toFile());
            
            // Perform validation
            Set<ValidationMessage> validationResult = schema.validate(jsonNode);
            
            if (validationResult.isEmpty()) {
                logger.info("Validation successful! ✓");
                logger.info("The file '{}' is valid against the schema.", jsonFile);
                return 0;
            } else {
                logger.error("Validation failed! ✗");
                logger.error("The file '{}' is not valid against the schema.", jsonFile);
                logger.error("Validation errors:");
                
                // Print all validation errors
                validationResult.forEach(msg -> logger.error(" - {}", msg));
                
                return 2; // Specific error code for validation failure
            }
        } catch (IOException e) {
            logger.error("Error reading files: {}", e.getMessage(), e);
            return 3; // File I/O error
        } catch (Exception e) {
            logger.error("Error during validation: {}", e.getMessage(), e);
            return 4; // General error
        }
    }
}
