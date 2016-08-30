package com.github.sirlag

import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.ProfileManager
import org.pac4j.sparkjava.SparkWebContext
import spark.Request
import spark.Response
import java.util.*

data class DependencyVersion(val version: Double, val dependencies: Array<String>, val repositories: Array<String>)
data class Dependency(val identifier: String, val versions: Array<DependencyVersion>)
data class User(val name: String, val email: String)

fun Request.getAuthentication(response: Response): HashMap<String, Any> {
    val context = SparkWebContext(this, response)
    val manager = ProfileManager<CommonProfile>(context)
    val auth = manager.isAuthenticated
    val user = if(auth) {
        val profile = manager.get(true)
        profile.get()
    } else {
        CommonProfile()
    }
    val model = HashMap<String, Any>()
    model.put("isAuthenticated", auth)
    if (auth) {
        model.put("name", user.displayName.split(' ').first())
    }
    return model
}