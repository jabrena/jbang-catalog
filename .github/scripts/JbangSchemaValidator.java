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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = {"-s", "--schema"}, description = "Path to the schema file")
    private String schemaFile = System.getenv().getOrDefault("SCHEMA_PATH", ".github/scripts/jbang-catalog-schema.json");

    @Option(names = {"-j", "--json"}, description = "Path to the JSON file to validate")
    private String jsonFile = System.getenv().getOrDefault("JSON_PATH", "jbang-catalog.json");

    @Option(names = {"--verbose"}, description = "Enable verbose output")
    private boolean verbose = false;
    
    // Validation result holder
    private record ValidationResult(boolean isValid, Set<ValidationMessage> errors, int exitCode) {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JbangSchemaValidator()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        logger.debug("Starting validation");
        logger.debug("Schema file: {}", schemaFile);
        logger.debug("JSON file: {}", jsonFile);
        
        // Define the validation pipeline using function composition
        return validateFiles(schemaFile, jsonFile);
    }
    
    // Pure function to validate files
    private Integer validateFiles(String schemaFilePath, String jsonFilePath) {
        // Function to check if file exists
        Function<String, Optional<Path>> fileExists = path -> {
            Path filePath = Paths.get(path);
            return Files.exists(filePath) ? Optional.of(filePath) : Optional.empty();
        };
        
        // Function to read JSON from file
        Function<Path, Optional<JsonNode>> readJson = path -> {
            try {
                return Optional.of(objectMapper.readTree(path.toFile()));
            } catch (IOException e) {
                logger.error("Error reading file {}: {}", path, e.getMessage());
                return Optional.empty();
            }
        };
        
        // Function to validate JSON against schema
        BiFunction<JsonNode, JsonNode, ValidationResult> validate = (schemaNode, jsonNode) -> {
            try {
                JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
                JsonSchema schema = schemaFactory.getSchema(schemaNode);
                Set<ValidationMessage> validationResult = schema.validate(jsonNode);
                
                return validationResult.isEmpty() 
                    ? new ValidationResult(true, Collections.emptySet(), 0) 
                    : new ValidationResult(false, validationResult, 2);
            } catch (Exception e) {
                logger.error("Validation error: {}", e.getMessage());
                return new ValidationResult(false, Collections.emptySet(), 4);
            }
        };
        
        // Function to log validation result
        Function<ValidationResult, Integer> logResult = result -> {
            if (result.isValid()) {
                logger.info("Validation successful! ✓");
                logger.info("The file '{}' is valid against the schema.", jsonFilePath);
            } else if (!result.errors().isEmpty()) {
                logger.error("Validation failed! ✗");
                logger.error("The file '{}' is not valid against the schema.", jsonFilePath);
                logger.error("Number of validation errors: {}", result.errors().size());
                
                // Log error types without showing the actual JSON content
                if (verbose) {
                    logger.error("Error details:");
                    
                    // Safe way to log error information without exposing JSON content
                    result.errors().stream()
                        .map(msg -> {
                            // Get message details without exposing values
                            String msgString = msg.toString();
                            // Extract schema path and error type, but not values
                            int valueStart = msgString.indexOf(": ");
                            if (valueStart > 0) {
                                return msgString.substring(0, valueStart);
                            }
                            return "schema validation error";
                        })
                        .distinct()
                        .forEach(info -> logger.error(" - {}", info));
                }
            }
            
            return result.exitCode();
        };
        
        // Check schema file
        Optional<Path> schemaPathOpt = fileExists.apply(schemaFilePath);
        if (schemaPathOpt.isEmpty()) {
            logger.error("Schema file not found: {}", schemaFilePath);
            return 1;
        }
        
        // Check JSON file 
        Optional<Path> jsonPathOpt = fileExists.apply(jsonFilePath);
        if (jsonPathOpt.isEmpty()) {
            logger.error("JSON file not found: {}", jsonFilePath);
            return 1;
        }
        
        // Read schema file
        Optional<JsonNode> schemaNodeOpt = readJson.apply(schemaPathOpt.get());
        if (schemaNodeOpt.isEmpty()) {
            return 3; // File I/O error
        }
        
        // Read JSON file
        Optional<JsonNode> jsonNodeOpt = readJson.apply(jsonPathOpt.get());
        if (jsonNodeOpt.isEmpty()) {
            return 3; // File I/O error
        }
        
        // Validate and return exit code
        return logResult.apply(validate.apply(schemaNodeOpt.get(), jsonNodeOpt.get()));
    }
}
