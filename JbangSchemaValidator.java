///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS com.networknt:json-schema-validator:1.3.2
//DEPS com.fasterxml.jackson.core:jackson-databind:2.15.3
//DEPS info.picocli:picocli:4.7.5

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
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
            log("Starting validation");
            log("Schema file: " + schemaFile);
            log("JSON file: " + jsonFile);
            
            // Define file paths
            Path schemaPath = Paths.get(schemaFile);
            Path jsonPath = Paths.get(jsonFile);
            
            // Check if files exist
            if (!Files.exists(schemaPath)) {
                System.err.println("Error: Schema file not found: " + schemaPath.toAbsolutePath());
                return 1;
            }
            
            if (!Files.exists(jsonPath)) {
                System.err.println("Error: JSON file not found: " + jsonPath.toAbsolutePath());
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
                System.out.println("Validation successful! ✓");
                System.out.println("The file '" + jsonFile + "' is valid against the schema.");
                return 0;
            } else {
                System.err.println("Validation failed! ✗");
                System.err.println("The file '" + jsonFile + "' is not valid against the schema.");
                System.err.println("\nValidation errors:");
                
                // Print all validation errors
                validationResult.forEach(msg -> System.err.println(" - " + msg));
                
                return 2; // Specific error code for validation failure
            }
        } catch (IOException e) {
            System.err.println("Error reading files: " + e.getMessage());
            e.printStackTrace();
            return 3; // File I/O error
        } catch (Exception e) {
            System.err.println("Error during validation: " + e.getMessage());
            e.printStackTrace();
            return 4; // General error
        }
    }
    
    private void log(String message) {
        if (verbose) {
            System.out.println("[INFO] " + message);
        }
    }
}
