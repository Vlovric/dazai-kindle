- Use Swagger with the following annotations:
	- @Tag(name, description) - for controllers
	- @Operation(summary, description) - for endpoints
	- @ApiResponse(responseCode, description) - for endpoints

- `descriptions should be high level, the code is the documentation`
# API constraints
- Authorization rules: `(all endpoints JWT protected unless explicitly marked as PUBLIC)` 
- Error format:
# Endpoints
## path of endpoint

| **Purpose:**          |                    |
| --------------------- | ------------------ |
| **Authentication:**   | `PUBLIC/PROTECTED` |
| **Request payload:**  |                    |
| **Response payload:** |                    |
### Success response

| **Code:**             |     |
| --------------------- | --- |
| **Data:**             |     |
### Error response

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |

| **Scenario:** |     |
| ------------- | --- |
| **Code:**     |     |
| **Data:**     |     |
## path of endpoint
...