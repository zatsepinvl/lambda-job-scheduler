package org.luartz.deployer

class LambdaJobDeploymentException(message: String?, throwable: Throwable? = null) :
    RuntimeException(message, throwable)