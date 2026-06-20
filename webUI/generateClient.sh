npx @openapitools/openapi-generator-cli generate -i http://localhost:8080/api/openapi.json -g typescript-fetch -o src/generated/client
npm run fmt
npm run verify