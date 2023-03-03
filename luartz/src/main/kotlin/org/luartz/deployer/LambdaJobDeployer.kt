package org.luartz.deployer

import org.luartz.util.ConcurrentHashLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.*
import java.io.FileInputStream
import kotlin.jvm.optionals.getOrNull

class LambdaJobDeployer(
    private val lambda: LambdaClient
) : JobDeployer {

    private val logger: Logger = LoggerFactory.getLogger(LambdaJobDeployer::class.java)
    private val lock = ConcurrentHashLock()

    override fun deploy(command: DeploymentCommand): DeploymentResult {
        // Put deployment for the same functionNames in order to prevent function creation race conditions
        return lock.whenAcquire(command.functionName) {
            doDeploy(command)
        }
    }

    private fun doDeploy(command: DeploymentCommand): DeploymentResult {
        val functionName = command.functionName
        val lambdaResponse = getLambda(functionName)
        if (lambdaResponse != null) {
            val currentRuntime = lambdaResponse.configuration().runtime().name
            if (currentRuntime.lowercase() != command.runtime.lowercase()) {
                throw LambdaJobDeploymentException(
                    "Existing lambda $functionName has different runtime: expected ${command.runtime}, but actual is $currentRuntime"
                )
            }

            val handler = lambdaResponse.configuration().handler()
            if (handler != command.handler) {
                throw LambdaJobDeploymentException(
                    "Existing lambda $functionName has different handler: expected ${command.handler}, but actual is $handler"
                )
            }
            logger.debug("Lambda function $functionName already exists. Skipping deployment.")
            return DeploymentResult(status = DeploymentStatus.SKIPPED)
        } else {
            logger.debug("Deploying lambda function $functionName...")

            createFunction(command)

            logger.debug("Lambda function $functionName deployed successfully")
            return DeploymentResult(status = DeploymentStatus.CREATED)
        }
    }


    private fun getLambda(name: String): GetFunctionResponse? {
        val getFunctionRequest = getFunctionRequest(name)
        return try {
            lambda.getFunction(getFunctionRequest)
        } catch (ex: ResourceNotFoundException) {
            // Lambda doesnt exists
            null
        }
    }

    private fun getFunctionRequest(functionName: String): GetFunctionRequest {
        return GetFunctionRequest.builder()
            .functionName(functionName)
            .build()
    }

    private fun createFunction(command: DeploymentCommand) {
        invokeFunctionCreation(command)
        waitForCreation(command.functionName)
    }

    private fun invokeFunctionCreation(command: DeploymentCommand) {
        val code = getFunctionCode(command.codeZipPath)
        val createRequest = CreateFunctionRequest.builder()
            .code(code)
            .functionName(command.functionName)
            .description(command.description)
            .handler(command.handler)
            .runtime(command.runtime)
            .role(command.roleArn)
            .build()

        lambda.createFunction(createRequest)
    }

    private fun getFunctionCode(codeZipPath: String): FunctionCode? {
        val fileToUpload = getCodeZipFile(codeZipPath)
        return FunctionCode.builder()
            .zipFile(fileToUpload)
            .build()
    }

    private fun getCodeZipFile(codeZipPath: String): SdkBytes? {
        val inputStream = FileInputStream(codeZipPath)
        return SdkBytes.fromInputStream(inputStream)
    }

    private fun waitForCreation(functionName: String) {
        val waiter = lambda.waiter()
        val getFunctionRequest = getFunctionRequest(functionName)

        val response = waiter.waitUntilFunctionExists(getFunctionRequest)

        val throwable = response.matched().exception().getOrNull()
        if (throwable != null) {
            throw throwable
        }
    }
}