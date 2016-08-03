package com.github.sirlag

import com.github.sirlag.db.DependencyNotFoundException
import com.github.sirlag.db.getDependencyVersion
import com.github.sirlag.db.getLatestDependencyVersion
import spark.Request
import spark.Response
import spark.Spark.*

fun main(args: Array<String>) {

    get("repo/:identifier/") { request, response -> response.redirect("repo/${request.params("identifier")}/latest")}
    get("repo/:identifier/:version") { request, response -> HandleDependencyRequest(request, response) }

    after { request, response -> response.header("Content-Encoding", "gzip") }
}

fun HandleDependencyRequest(request: Request, response: Response): String {
    val identifier = request.params(":identifier")
    val version = request.params(":version")

    var responseMessage = ""

    try {
        if (version == "latest")
            responseMessage = getLatestDependencyVersion(identifier).toJson()
        else
            responseMessage = getDependencyVersion(identifier, version.toDouble()).toJson()
    } catch (ex: DependencyNotFoundException) {
        //TODO log the exception
        halt(404)
    }

    return responseMessage
}


