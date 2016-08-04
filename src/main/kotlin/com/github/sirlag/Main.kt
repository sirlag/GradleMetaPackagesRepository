package com.github.sirlag

import com.github.sirlag.db.*
import com.google.gson.Gson
import com.google.gson.JsonParseException
import spark.Request
import spark.Response
import spark.Spark.*

val gson = Gson()

fun main(args: Array<String>) {

    get("repo/:identifier") { request, response -> response.redirect("repo/${request.params("identifier")}/latest")}
    get("repo/:identifier/:version") { request, response -> HandleDependencyRequest(request, response) }

    post("repo/:identifier") {request, response -> HandleNewDependency(request, response)}
    post("repo/:identifier/:version") {request, response -> HandleNewDependencyVersion(request, response)}

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

fun HandleNewDependencyVersion(request: Request, response: Response): String{
    try {
        val version = gson.fromJson(request.body(), DependencyVersion::class.java)
        addVersionToDependency(request.params(":identifier"), version)
        return version.toString()
    } catch (ex: JsonParseException){
        response.status(400)
        return "Malformed JSON Request"
    } catch (ex: VersionAlreadyExistsExceptions){
        response.status(500)
        return "Version already exists in repository"
    }
}

fun HandleNewDependency(request: Request, response: Response): String{
    try {
        val dependency = gson.fromJson(request.body(), Dependency::class.java)
        addDependency(dependency)
        return dependency.toString()
    } catch (ex: JsonParseException){
        response.status(400)
        return "Malformed JSON Request"
    } catch (ex: DependencyAlreadyExistsException){
        response.status(500)
        return "Dependency already exists in repository"
    }
}

data class DependencyVersion(val version: Double, val dependencies: Array<String>, val repositories: Array<String>)
data class Dependency(val identifier: String, val versions: Array<DependencyVersion>)