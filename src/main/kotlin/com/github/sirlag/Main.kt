package com.github.sirlag

import com.github.sirlag.db.*
import com.google.gson.Gson
import com.google.gson.JsonParseException
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.jade.JadeTemplateEngine
import java.util.*

val gson = Gson()

val database: RepositoryDatabase = MongoWrapper("192.168.99.100", "test", gson)

fun main(args: Array<String>) {

    get("/search", {request, response -> HandleDependencySearch(request, response)}, JadeTemplateEngine())

    //Repo end points.
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
            responseMessage = gson.toJson(database.getLatestDependencyVersion(identifier))
        else
            responseMessage = gson.toJson(database.getDependencyVersion(identifier, version.toDouble()))
    } catch (ex: DependencyNotFoundException) {
        //TODO log the exception
        halt(404)
    }

    return responseMessage
}

fun HandleNewDependencyVersion(request: Request, response: Response): String{
    try {
        val version = gson.fromJson(request.body(), DependencyVersion::class.java)
        database.addVersionToDependency(request.params(":identifier"), version)
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
        database.addDependency(dependency)
        return dependency.toString()
    } catch (ex: JsonParseException){
        response.status(400)
        return "Malformed JSON Request"
    } catch (ex: DependencyAlreadyExistsException){
        response.status(500)
        return "Dependency already exists in repository"
    }
}

fun HandleDependencySearch(request: Request, response: Response): ModelAndView{
    val model = HashMap<String, Any>()
    val query = request.queryParams("query")
    val queryResults = database.searchForDependencies(query)
    if (queryResults.size == 1)
        response.redirect("/package/${queryResults.first().identifier}")
    model.put("dependencies", queryResults)
    return ModelAndView(model, "SearchResults.jade")
}

data class DependencyVersion(val version: Double, val dependencies: Array<String>, val repositories: Array<String>)
data class Dependency(val identifier: String, val versions: Array<DependencyVersion>)