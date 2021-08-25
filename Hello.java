import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

// Handler value: example.Handler
public class Hello implements RequestHandler<Map<String,String>, String>{
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  @Override
  public String handleRequest(Map<String,String> event, Context context)
  {
    LambdaLogger logger = context.getLogger();
    String response = "200 OK";
    // log execution details
    logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
    logger.log("CONTEXT: " + gson.toJson(context));
    // process event
    logger.log("EVENT: " + gson.toJson(event));
    logger.log("EVENT TYPE: " + event.getClass());
    return response;
  }
}




{
  "swagger": "2.0",
  "info": {
    "description": "Terraform AWS API for Blockchain Subrogation Dashboard business interactions. Env ${branch}",
    "version": "1.0.0",
    "title": "bcsubro-aws-dashboard-api"
  },
  "basePath": "/v1/pc/claims/experience/internal/blockchain-subrogation-dashboard",
  "schemes": ["https"],
  "paths": {
    "/activities": {
      "get": {
        "summary": "Returns a set of activity data for the Blockchain on the given date.",
        "consumes": ["application/json"],
        "produces": ["application/json"],
        "parameters": [
          {
            "name": "date",
            "in": "query",
            "description": "The date to retrieve activities for. An example date is: 2021-07-16.",
            "required": true,
            "type": "string"
          },
          {
            "name": "cpCode",
            "in": "query",
            "description": "The counter party code to retreive data for. An example cpCode is: 73575",
            "required": false,
            "type": "string"
          }
        ],
        "responses": {
          "200": {
            "description": "200 response",
            "headers": {
              "Access-Control-Allow-Headers": {
                "type": "string"
              },
              "Access-Control-Allow-Methods": {
                "type": "string"
              },
              "Access-Control-Allow-Origin": {
                "type": "string"
              }
            },
            "schema": {
              "$ref": "#/definitions/Activity"
            },
            "responseSchema": {
              "$ref": "#/definitions/Activity"
            }
          }
        },
        "security": [
          {
            "aws-lambda-authorizer": []
          },
          {
            "usaa-oauth2": [
              "v1.bc-aws-business-api.read",
              "v1.bc-aws-business-api.write"
            ]
          },
          {
            "employeeOAuth": [
              "v1.bc-aws-business-api.read",
              "v1.bc-aws-business-api.write"
            ]
          }
        ],
        "x-amazon-apigateway-integration": {
          "type": "aws",
          "credentials": "${rolearn}",
          "uri": "arn:aws:apigateway:${region}:dynamodb:action/Query",
          "responses": {
            "default": {
              "statusCode": "200",
              "responseTemplates": {
                "application/json": "#set($inputRoot = $input.path('$'))\n#set($code = '00750')\n#set($items = [])\n#if($input.params()['querystring']['cpCode'] != '')\n#set($code = $input.params()['querystring']['cpCode'])\n#end\n#foreach($elem in $inputRoot.Items)\n#if($elem['type']['S'].split('[+]')[1] == $code)\n#set($bar = $items.add($elem))\n#end\n#end\n{\n  \"activities\": [\n    #foreach($elem in $items)\n {\n     #foreach($key in $elem.keySet())\n     #set($item = $elem[$key])\n     #foreach($inner in $item.keySet())\n\"$key\": \"$elem[$key][$inner]\"#end#if($foreach.hasNext),\n     #else\n  \n     }#end#end#if($foreach.hasNext),\n  #end\n#end\n\n  ]\n}"
              }
            }
          },
          "requestTemplates": {
            "application/json": "{\n    \"TableName\": \"${activity_table_name}\",\n    \"KeyConditionExpression\": \"#queryDate = :v1\",\n    \"ExpressionAttributeNames\": {\n        \"#queryDate\": \"date\"\n    }\n,\n    \"ExpressionAttributeValues\": {\n        \":v1\": {\n            \"S\": \"$input.params('date')\"\n        }\n    }\n}"
          },
          "passthroughBehavior": "when_no_templates",
          "httpMethod": "POST"
        }
      }
    },
    "/node-details": {
      "get": {
        "summary": "Returns health information and config details about the specified Blockchain Node.",
        "produces": ["application/json"],
        "parameters": [
          {
            "name": "color",
            "in": "query",
            "required": true,
            "description": "The color that identifies the node information is being returned for.",
            "type": "string",
            "enum": [
              "red",
              "green",
              "blue"
            ]
          }
        ],
        "responses": {
          "200": {
            "description": "200 OK",
            "headers": {
              "Access-Control-Allow-Headers": {
                "type": "string"
              },
              "Access-Control-Allow-Methods": {
                "type": "string"
              },
              "Access-Control-Allow-Origin": {
                "type": "string"
              }
            },
            "schema": {
              "$ref": "#/definitions/HealthData"
            },
            "responseSchema": {
              "$ref": "#/definitions/HealthData"
            }
          },
          "400": {
            "description": "400 Bad Request",
            "schema": {
              "$ref": "#/definitions/Error"
            },
            "responseSchema": {
              "$ref": "#/definitions/Error"
            }
          },
          "401": {
            "description": "401 Unauthorized",
            "schema": {
              "$ref": "#/definitions/Error"
            },
            "responseSchema": {
              "$ref": "#/definitions/Error"
            }
          },
          "403": {
            "description": "403 Forbidden",
            "schema": {
              "$ref": "#/definitions/Error"
            },
            "responseSchema": {
              "$ref": "#/definitions/Error"
            }
          },
          "404": {
            "description": "404 Not Found",
            "schema": {
              "$ref": "#/definitions/Error"
            },
            "responseSchema": {
              "$ref": "#/definitions/Error"
            }
          },
          "500": {
            "description": "500 Internal Server Error",
            "schema": {
              "$ref": "#/definitions/Error"
            },
            "responseSchema": {
              "$ref": "#/definitions/Error"
            }
          }
        },
        "security": [
          {
            "aws-lambda-authorizer": []
          },
          {
            "usaa-oauth2": [
              "v1.bc-aws-business-api.read",
              "v1.bc-aws-business-api.write"
            ]
          },
          {
            "employeeOAuth": [
              "v1.bc-aws-business-api.read",
              "v1.bc-aws-business-api.write"
            ]
          }
        ],
        "x-amazon-apigateway-integration": {
          "uri": "${lambda_health_check_arn}",
          "responses": {
            "default": {
              "statusCode": 204
            },
            "^\\[BadRequest\\].*": {
              "statusCode": 400,
              "responseTemplates": {
                "application/json": "#set($errorMessage = $input.path('$.errorMessage'))\n{\n    \"message\" : \"$errorMessage\"\n}"
              }
            },
            "^\\[NotFound\\].*": {
              "statusCode": 404,
              "responseTemplates": {
                "application/json": "#set($errorMessage = $input.path('$.errorMessage'))\n{\n    \"message\" : \"$errorMessage\"\n}"
              }
            },
            "^\\[InternalServerError\\].*": {
              "statusCode": 500,
              "responseTemplates": {
                "application/json": "#set($errorMessage = $input.path('$.errorMessage'))\n{\n    \"message\" : \"$errorMessage\"\n}"
              }
            }
          },
          "passthroughBehavior": "WHEN_NO_TEMPLATES",
          "httpMethod": "POST",
          "contentHandling": "CONVERT_TO_TEXT",
          "type": "aws_proxy",
          "requestTemplates": {
            "application/json": "{\n    \"color\": $input.querystring('color'),\n    }\n"
          }
        }
      }
    }
  },
  "securityDefinitions": {
    "aws-lambda-authorizer": {
      "type": "apiKey",
      "name": "api-key",
      "in": "header",
      "x-amazon-apigateway-authtype": "custom",
      "x-amazon-apigateway-authorizer": {
        "authorizerUri": "${auth_lambda_invoke_arn}",
        "authorizerCredentials": "${auth_invoke_role_arn}",
        "authorizerResultTtlInSeconds": 0,
        "identitySource": "method.request.header.id-claim",
        "type": "request"
      }
    },
    "usaa-oauth2": {
      "description": "OAuth2 Specification",
      "type": "oauth2",
      "authorizationUrl": "",
      "flow": "implicit",
      "scopes": {
        "v1.bc-aws-business-api.read": "BC Subro AWS API custom scope for read access to the API",
        "v1.bc-aws-business-api.write": "BC Subro AWS API custom scope for write access to the API"
      }
    },
    "employeeOAuth": {
      "type": "oauth2",
      "flow": "implicit",
      "scopes": {
        "v1.bc-aws-business-api.read": "BC Subro USAA API custom scope for read access to the API",
        "v1.bc-aws-business-api.write": "BC Subro USAA API custom scope for write access to the API"
      }
    }
  },
  "definitions": {
    "ActivityData": {
      "properties": {
        "data": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Activity"
          }
        }
      }
    },
    "Activity": {
      "type": "object",
      "properties": {
        "type": {
          "type": "string"
        },
        "date": {
          "type": "string"
        },
        "cpCode": {
          "type": "string"
        },
        "count": {
          "type": "integer",
          "format": "int32"
        }
      }
    },
    "Empty": {
      "properties": {
        "type": "object",
        "title": "Empty Schema"
      }
    },
    "Error": {
      "type": "object",
      "required": ["message"],
      "properties": {
        "message": {
          "type": "string",
          "description": "The error message."
        }
      },
      "title": "Default response for Error"
    },
    "HealthData": {
      "properties": {
        "blockNumber": {
          "type": "integer",
          "format": "int32"
        },
        "nodeInfo": {
          "type": "object",
          "properties": {
            "jsonrpc": {
              "type": "string"
            },
            "id": {
              "type": "integer",
              "format": "int32"
            },
            "result": {
              "type": "object",
              "properties": {
                "id": {
                  "type": "string"
                },
                "name": {
                  "type": "string"
                },
                "enode": {
                  "type": "string"
                },
                "enr": {
                  "type": "string"
                },
                "ip": {
                  "type": "string"
                },
                "ports": {
                  "type": "object",
                  "properties": {
                    "discovery": {
                      "type": "integer",
                      "format": "int32"
                    },
                    "listener": {
                      "type": "integer",
                      "format": "int32"
                    }
                  }
                },
                "listenAddr": {
                  "type": "string"
                },
                "protocols": {
                  "type": "object",
                  "properties": {
                    "eth": {
                      "type": "object",
                      "properties": {
                        "network": {
                          "type": "integer",
                          "format": "int32"
                        },
                        "difficulty": {
                          "type": "integer",
                          "format": "int32"
                        },
                        "genesis": {
                          "type": "string"
                        },
                        "config": {
                          "type": "object",
                          "properties": {
                            "chainId": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "homesteadBlock": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "eip150Block": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "eip150Hash": {
                              "type": "string"
                            },
                            "eip155Block": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "eip158Block": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "byzantiumBlock": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "constantinopleBlock": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "isQuorum": {
                              "type": "boolean"
                            },
                            "txnSizeLimit": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "maxCodeSize": {
                              "type": "integer",
                              "format": "int32"
                            },
                            "maxCodeSizeConfig": {
                              "type": "array",
                              "items": {
                                "type": "object",
                                "properties": {
                                  "block": {
                                    "type": "integer",
                                    "format": "int32"
                                  },
                                  "size": {
                                    "type": "integer",
                                    "format": "int32"
                                  }
                                }
                              }
                            }
                          }
                        },
                        "head": {
                          "type": "string"
                        },
                        "consensus": {
                          "type": "string"
                        }
                      }
                    }
                  }
                },
                "plugins": {
                  "type": "object",
                  "properties": {
                    "baseDir": {
                      "type": "string"
                    },
                    "security": {
                      "type": "object",
                      "properties": {
                        "config": {
                          "type": "string"
                        },
                        "executable": {
                          "type": "array",
                          "items": {
                            "type": "string"
                          }
                        },
                        "name": {
                          "type": "string"
                        },
                        "version": {
                          "type": "string"
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        },
        "raftCluster": {
          "type": "object",
          "properties": {
            "jsonrpc": {
              "type": "string"
            },
            "id": {
              "type": "integer",
              "format": "int32"
            },
            "result": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "raftId": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "nodeId": {
                    "type": "string"
                  },
                  "p2pPort": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "raftPort": {
                    "type": "integer",
                    "format": "int32"
                  },
                  "hostname": {
                    "type": "string"
                  },
                  "role": {
                    "type": "string"
                  },
                  "nodeActive": {
                    "type": "boolean"
                  }
                }
              }
            }
          }
        },
        "peers": {
          "type": "object",
          "properties": {
            "jsonrpc": {
              "type": "string"
            },
            "id": {
              "type": "integer",
              "format": "int32"
            },
            "result": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "enode": {
                    "type": "string"
                  },
                  "id": {
                    "type": "string"
                  },
                  "name": {
                    "type": "string"
                  },
                  "caps": {
                    "type": "array",
                    "items": {
                      "type": "string"
                    }
                  },
                  "network": {
                    "type": "object",
                    "properties": {
                      "localAddress": {
                        "type": "string"
                      },
                      "remoteAddress": {
                        "type": "string"
                      },
                      "inbound": {
                        "type": "boolean"
                      },
                      "trusted": {
                        "type": "boolean"
                      },
                      "static": {
                        "type": "boolean"
                      }
                    }
                  },
                  "protocols": {
                    "type": "object",
                    "properties": {
                      "eth": {
                        "type": "object",
                        "properties": {
                          "version": {
                            "type": "integer",
                            "format": "int32"
                          },
                          "difficulty": {
                            "type": "integer",
                            "format": "int32"
                          },
                          "head": {
                            "type": "string"
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        },
        "tessera": {
          "type": "boolean"
        },
        "quorumActive": {
          "type": "boolean"
        }
      }
    }
  },
  "responses": {
    "ErrorBadRequest": {
      "description": "400 Bad Request",
      "schema": {
        "$ref": "#/definitions/Error"
      },
      "responseSchema": {
        "$ref": "#/definitions/Error"
      }
    },
    "ErrorForbidden": {
      "description": "403 Forbidden",
      "schema": {
        "$ref": "#/definitions/Error"
      },
      "responseSchema": {
        "$ref": "#/definitions/Error"
      }
    },
    "ErrorInternalServerError": {
      "description": "500 Internal Server Error",
      "schema": {
        "$ref": "#/definitions/Error"
      },
      "responseSchema": {
        "$ref": "#/definitions/Error"
      }
    },
    "ErrorNotFound": {
      "description": "404 Not Found",
      "schema": {
        "$ref": "#/definitions/Error"
      },
      "responseSchema": {
        "$ref": "#/definitions/Error"
      }
    },
    "ErrorUnauthorized": {
      "description": "401 Unauthorized",
      "schema": {
        "$ref": "#/definitions/Error"
      },
      "responseSchema": {
        "$ref": "#/definitions/Error"
      }
    }
  },
  "x-amazon-apigateway-policy": {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "AWS": ["*"]
        },
        "Action": ["execute-api:Invoke", "execute-api:InvalidateCache"],
        "Resource": "arn:aws:execute-api:${region}:${account_id}:*/*/*/*",
        "Condition": {
          "StringEquals": {
            "aws:sourceVpce": "${vpce_id}",
            "aws:sourceVpc": "${vpc_id}"
          }
        }
      }
    ]
  },
  "x-amazon-apigateway-endpoint-configuration": {
    "vpcEndpointIds": ["${vpce_id}"]
  }
}
