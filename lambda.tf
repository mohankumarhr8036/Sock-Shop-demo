resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_lambda_function" "java_lambda_function" {
  runtime          = "${var.lambda_runtime}"
  filename      = "${var.lambda_payload_filename}"
  #source_code_hash = "${base64sha256(file(var.lambda_payload_filename))}"
  function_name = "java_lambda_function"
  role          = aws_iam_role.iam_for_lambda.arn
  handler          = "${var.lambda_function_handler}"
  timeout = 60
  memory_size = 256

}