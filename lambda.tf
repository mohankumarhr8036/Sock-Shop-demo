provider  {
  "aws"
  
}

resource "aws_iam_role" "lambdarole" {
    name = "lambdarole"
    managed_policy_arns = []
    assume_role_policy = jsonencode({
        Version = "2012-10-17"
        Statement = [{
            Action = "sts:AssumeRole"
            Effect = "Allow"
            Sid = ""
            Principal = {
                Service = "lambda.amazonaws.com"
            }
        },
        ]      
    })
}

resource "aws_lambda_function" "childlambda" {
  runtime          = "${var.lambda_runtime}"
  filename      = "${var.lambda_payload_filename}"
  source_code_hash = "${base64sha256(file(var.lambda_payload_filename))}"
  function_name = "java_lambda_function"
  role          = aws_iam_role.lambda.role.arn
  handler          = "${var.lambda_function_handler}"
  timeout = 60
  memory_size = 256

}

resource "aws_lambda_function" "parentlambda" {
  runtime          = "${var.lambda_runtime}"
  filename      = "${var.lambda_payload_filename}"
  source_code_hash = "${base64sha256(file(var.lambda_payload_filename))}"
  function_name = "java_lambda_function"
  role          = aws_iam_role.lambda.role.arn
  handler          = "${var.lambda_function_handler}"
  timeout = 60
  memory_size = 256

}

resource "aws_api_gateway_rest_api" "api" {
  name = "myapi"
}

resource "aws_api_gateway_resource" "resource" {
  path_part   = "resource"
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  rest_api_id = aws_api_gateway_rest_api.api.id
}

resource "aws_api_gateway_method" "method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.resource.id
  http_method   = "GET"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.resource.id
  http_method             = aws_api_gateway_method.method.http_method
  integration_http_method = "GET"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.parentlambda.invoke_arn
}

# Lambda
resource "aws_lambda_permission" "apigw_lambda" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.parentlambda.parentlambda
  principal     = "apigateway.amazonaws.com"

  source_arn = "arn:aws:execute-api:${var.myregion}:${var.accountId}:${aws_api_gateway_rest_api.api.id}/*/${aws_api_gateway_method.method.http_method}${aws_api_gateway_resource.resource.path}"
}

- create a role for child and parent lambda with managed policies
- create child lambda function and add the code or jar fileand atach the role created
- create parent lambda function and add the code or jar file and attach role created
	- inside code invoke the child lambda
- create API gaetway with new resource GET method to invoke parent lambda by passing input parameters to the code.

