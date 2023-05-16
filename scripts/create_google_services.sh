#!/bin/bash

# Load environment variables from .env file
while IFS='=' read -r name value; do
    # Remove the trailing newline character
    value=${value%$'\n'}
    value2=${value%?}
    export "$name=$value2"
done < .env

# Create a temporary copy of the template
cp app/google-services.json.template app/temp.json

# Replace variables in the template using envsubst
envsubst < app/temp.json > app/google-services.json || true

# Remove the temporary file
rm app/temp.json || true

# Exit with a success status
exit 0
