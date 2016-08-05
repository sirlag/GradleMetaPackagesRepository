package com.github.sirlag.db

import com.github.sirlag.Dependency
import com.github.sirlag.DependencyVersion
import com.github.sirlag.gson
import com.mongodb.MongoClient
import com.mongodb.client.model.Filters.regex
import org.bson.Document
import java.time.LocalDateTime
import java.util.regex.Pattern

//This a local copy of mongodb running in a docker container. Don't worry about this too much for now
val mongoClient = MongoClient("192.168.99.100")
val db = mongoClient.getDatabase("test")

fun getDependency(identifier:String) : Document? {
    return db.getCollection("dependencies").find(Document("identifier", identifier)).firstOrNull()
}

fun addDependency(dependency: Dependency){

    if (getDependency(dependency.identifier) != null)
        throw DependencyAlreadyExistsException("The dependency ${dependency.identifier} already exists. You are probably looking to" +
                "update ${dependency.identifier} instead")

    val doc = Document()
        .append("identifier", dependency.identifier)
        .append("versions", dependency.versions.map {
            Document("version", it.version)
                .append("dependencies", it.dependencies.toList())
                .append("repositories", it.repositories.toList())
        })
    db.getCollection("dependencies").insertOne(doc)
}

fun addVersionToDependency(identifier: String, version: DependencyVersion){
    val doc = getDependency(identifier)!!
    val size = doc["versions", List::class.java].filter { (it as Document).getDouble("version") == version.version }.size
    if (size > 0)
        throw VersionAlreadyExistsExceptions("Unable to add version $version, it already exists")
    val versions = doc["versions", List::class.java] + Document.parse(gson.toJson(version))
    db.getCollection("dependencies").updateOne(doc, Document("\$set", Document("versions",versions)))
}

fun getDependencyVersion(identifier: String, version: Double) : Document{
     val doc = db.getCollection("dependencies")
                 .find(Document("identifier", identifier)
                 .append("versions.version", version))
                 .firstOrNull() ?: throw DependencyNotFoundException("Could not locate $identifier:$version")
     return doc.get("versions", List::class.java)
        .filter { (it as Document)
        .getDouble("version") == version }.firstOrNull() as Document
}

fun getLatestDependencyVersion(identifier: String): Document{
    val doc = getDependency(identifier) ?: throw DependencyNotFoundException("Could not locate $identifier:")
    return doc.get("versions", List::class.java)
        .sortedByDescending { (it as Document).getDouble("version") }.firstOrNull() as Document
}

fun searchForDependencies(identifier: String): List<Dependency>{
    return db.getCollection("dependencies")
            .find(regex("identifier", Pattern.compile(Pattern.quote(identifier), Pattern.CASE_INSENSITIVE)))
            .map { gson.fromJson(it.toJson(), Dependency::class.java) }.toList()

}

class DependencyAlreadyExistsException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class DependencyNotFoundException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class VersionAlreadyExistsExceptions(msg:String? = null, cause: Throwable? = null) : Exception(msg, cause)
