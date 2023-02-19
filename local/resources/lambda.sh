awslocal lambda create-function \
    --function-name TestSuccess \
    --runtime nodejs \
    --zip-file fileb:///docker-entrypoint-initaws.d/lambda.zip \
    --handler lambda/lambda-success.handler \
    --role lambda

awslocal lambda create-function \
    --function-name TestFail \
    --runtime nodejs \
    --zip-file fileb:///docker-entrypoint-initaws.d/lambda.zip \
    --handler lambda/lambda-fail.handler \
    --role lambda