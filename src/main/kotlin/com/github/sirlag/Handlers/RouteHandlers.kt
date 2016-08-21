package com.github.sirlag.Handlers

import com.github.sirlag.Dependency
import com.github.sirlag.DependencyVersion
import com.github.sirlag.database
import com.github.sirlag.db.DependencyAlreadyExistsException
import com.github.sirlag.db.DependencyNotFoundException
import com.github.sirlag.db.VersionAlreadyExistsExceptions
import com.github.sirlag.gson
import com.google.gson.JsonParseException
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.ProfileManager
import org.pac4j.sparkjava.SparkWebContext
import spark.ModelAndView
import spark.Request
import spark.Response
import spark.Spark
import java.util.*

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
        Spark.halt(404)
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

fun HandleDependencySearch(request: Request, response: Response): ModelAndView {
    val model = HashMap<String, Any>()
    val query = request.queryParams("query")
    val queryResults = database.searchForDependencies(query)
    if (queryResults.size == 1)
        response.redirect("/package/${queryResults.first().identifier}")
    model.put("dependencies", queryResults)
    return ModelAndView(model, "SearchResults.jade")
}

fun HandleDependencyPage(request: Request, response: Response): ModelAndView {
    val model = HashMap<String, Any>()
    val dependency = database.getDependency(request.params("identifier"))
    if (dependency == null){
        response.redirect("/errorPage")
        response.status(404)
    } else {
        model.put("dependency", dependency)
        model.put("latestVersion", dependency.versions.sortedByDescending { it.version }.first())
    }
    return ModelAndView(model, "PackagePage.jade")
}

fun HandleIndex(request: Request, response: Response): ModelAndView{
    val model = HashMap<String, Any>()
    val context = SparkWebContext(request, response)
    val manager = ProfileManager<CommonProfile>(context)
    model.put("isAuthenticated", manager.isAuthenticated)
    if(manager.isAuthenticated) {
        val profile = manager.get(true).get()
        model.put("name", profile.displayName.split(' ').first())
    }
    return ModelAndView(model, "Index.jade")
}

fun HandleUpload(request: Request, response: Response): ModelAndView{
    val model = HashMap<String, Any>()
    val context = SparkWebContext(request, response)
    val manager = ProfileManager<CommonProfile>(context)
    model.put("isAuthenticated", manager.isAuthenticated)
    println(manager.isAuthenticated)
    if(manager.isAuthenticated) {
        val profile = manager.get(true)
        model.put("user", profile.get())
    }
    return ModelAndView(model, "Upload.jade")
}

fun HandleLogin(request: Request, response: Response): ModelAndView{
    val model = HashMap<String, Any>()
    val context = SparkWebContext(request, response)
    val manager = ProfileManager<CommonProfile>(context)
    model.put("isAuthenticated", manager.isAuthenticated)
    if(manager.isAuthenticated) {
        val profile = manager.get(true).get()
        model.put("name", profile.displayName.split(' ').first())
    }
    return ModelAndView(model, "Login.jade")
}
