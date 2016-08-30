package com.github.sirlag

import com.github.sirlag.Auth.AppConfigFactory
import com.github.sirlag.db.*
import com.github.sirlag.Handlers.*
import com.google.gson.Gson
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.ProfileManager
import org.pac4j.sparkjava.ApplicationLogoutRoute
import org.pac4j.sparkjava.CallbackRoute
import org.pac4j.sparkjava.SecurityFilter
import org.pac4j.sparkjava.SparkWebContext
import spark.Request
import spark.Response
import spark.Spark.*
import spark.template.jade.JadeTemplateEngine
import spark.debug.DebugScreen.enableDebugScreen
import java.util.*

val gson = Gson()

val database: RepositoryDatabase = MongoWrapper("192.168.99.100", "GMD", gson)

fun main(args: Array<String>) {
    enableDebugScreen()

    staticFiles.location("/public")
    val config = AppConfigFactory().build()

    val callback = CallbackRoute(config, "/", true, true)
    get("/callback", callback)
    post("/callback", callback)
    after("/callback"){ request, response -> HandleCallback(request, response)}

    get("/logout", ApplicationLogoutRoute(config, "/"))

    before("/upload", SecurityFilter(config, "GithubClient"))
    get("/upload", {request, response -> HandleUpload(request, response)}, JadeTemplateEngine())

    get("/login", {request, response -> HandleLogin(request, response)}, JadeTemplateEngine())

    get("/", {request, response -> HandleIndex(request, response)}, JadeTemplateEngine())
    get("search", {request, response -> HandleDependencySearch(request, response)}, JadeTemplateEngine())
    get("package/:identifier", {request, response -> HandleDependencyPage(request, response)}, JadeTemplateEngine())

    //Repo end points.
    get("repo/:identifier") { request, response -> response.redirect("repo/${request.params("identifier")}/latest")}
    get("repo/:identifier/:version") { request, response -> HandleDependencyRequest(request, response) }

    post("repo/:identifier") {request, response -> HandleNewDependency(request, response)}
    post("repo/:identifier/:version") {request, response -> HandleNewDependencyVersion(request, response)}

    get("/user/me") {request, response -> "TODO"} //TODO make this work
    post("/user/me") { request, response -> "TODO"} //TODO change display name request
    get("/user/:displayName", {request, response -> HandlePublicUserPage(request, response)}, JadeTemplateEngine())
    get("/changedisplayname") {request, response -> database.setUserDisplayName("mnwranger@hotmail.com", "sirlag")}

    get("/signup", {request, response -> HandleSignup(request, response)}, JadeTemplateEngine())

    after { request, response -> response.header("Content-Encoding", "gzip") }
}
