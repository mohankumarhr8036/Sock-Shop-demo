variable "aws_access_key" {
  # set aws access key
  default = "AKIAWDWEOKZEEK5MUXSU"
}

variable "aws_secret_key" {
  # set aws secret key
  default = "jlrlu1zu+ypjxGNSzGTtobqNM5B7RkBwZDpRmalO"
}

variable "region" {
  # set aws region
  default = "us-east-1"
}

variable "lambda_payload_filename" {
  default = "C:/Users/ADMIN/Desktop/javaproject/build/libs/javaproject-1.0.jar"
}

variable "lambda_function_handler" {
  default = "example.Hello.LambdaHandler"
}

variable "lambda_runtime" {
  default = "java8"
}