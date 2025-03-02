FROM eclipse-temurin:23-jre-alpine

# Install JBang
RUN apk add --no-cache curl bash && \
    curl -Ls https://sh.jbang.dev | bash -s - app setup
ENV PATH="${PATH}:/root/.jbang/bin"

# Create working directory
WORKDIR /app

# Copy the validator script
COPY JbangSchemaValidator.java .

# Set default schema and JSON paths
ENV SCHEMA_PATH=/app/jbang-catalog-schema.json
ENV JSON_PATH=/app/jbang-catalog.json

# Run the validation
ENTRYPOINT ["jbang", "JbangSchemaValidator.java"]
CMD ["--verbose"]