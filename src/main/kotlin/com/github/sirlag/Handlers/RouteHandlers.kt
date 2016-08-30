package com.github.sirlag.Handlers

import com.github.sirlag.*
import com.github.sirlag.Gravatar.Gravatar
import com.github.sirlag.db.DependencyAlreadyExistsException
import com.github.sirlag.db.DependencyNotFoundException
import com.github.sirlag.db.VersionAlreadyExistsExceptions
import com.google.gson.JsonParseException
import de.neuland.jade4j.template.JadeTemplate
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
    val model = request.getAuthentication(response)
    val query = request.queryParams("query")
    val queryResults = database.searchForDependencies(query)
    if (queryResults.size == 1)
        response.redirect("/package/${queryResults.first().identifier}")
    model.put("dependencies", queryResults)
    return ModelAndView(model, "SearchResults.jade")
}

fun HandleDependencyPage(request: Request, response: Response): ModelAndView {
    val model = request.getAuthentication(response)
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
    return ModelAndView(request.getAuthentication(response), "Index.jade")
}

fun HandleUpload(request: Request, response: Response): ModelAndView{
    return ModelAndView(request.getAuthentication(response), "Upload.jade")
}

fun HandleLogin(request: Request, response: Response): ModelAndView{
    return ModelAndView(request.getAuthentication(response), "Login.jade")
}

fun HandleCallback(request: Request, response: Response){
    val context = SparkWebContext(request, response)
    val manager = ProfileManager<CommonProfile>(context)
    if (manager.isAuthenticated){
        val user = manager.get(true).get()
        if(database.getUser(user.email) == null)
            database.addUser(User(user.displayName, user.email))
    } else{
        response.redirect("/404", 404)
    }
}

fun HandlePublicUserPage(request: Request, response: Response) : ModelAndView{

    val model = request.getAuthentication(response)
    val user = database.getUserByDisplayName(request.params(":displayName"))
    if (user == null) {
        response.redirect("/errorPage")
        response.status(404)
    } else {
        model.put("user", user)
        model.put("gravatarHash", Gravatar(user.email).getGravatarHash(250))
    }

    return ModelAndView(model ,"UserPage.jade")
}

fun HandleSignup(request: Request, response: Response): ModelAndView{
    val model = HashMap<String, Any>()
    val context = SparkWebContext(request, response)
    val manager = ProfileManager<CommonProfile>(context)
    val auth = manager.isAuthenticated
    if(auth) {
        model.put("isAuthenticated", auth)
        val profile = manager.get(true).get()
        model.put("user",User(profile.displayName, profile.email))
        model.put("gravatarHash", Gravatar(profile.email).getGravatarHash(128))
    } else {
        response.redirect("/errorPage", 404)
    }
    return ModelAndView(model, "FirstSignup.jade")
}