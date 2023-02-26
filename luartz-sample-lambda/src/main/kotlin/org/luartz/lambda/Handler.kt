package org.luartz.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class Handler : RequestHandler<Map<String, Any>, Any> {
    override fun handleRequest(input: Map<String, Any>, context: Context): Any {
        return input
    }
}