package com.github.sirlag.db

import com.mongodb.MongoClient
import org.bson.Document

//This a local copy of mongodb running in a docker container. Don't worry about this too much for now
val mongoClient = MongoClient("192.168.99.100")
val db = mongoClient.getDatabase("test")

fun getDependency(identifier:String) : Document? {
    return db.getCollection("dependencies").find(Document("identifier", identifier)).firstOrNull()
}

fun addDependency(identifier: String, version: Double, dependencies: List<String>, repositories: List<String>){

    if (getDependency(identifier) != null)
        throw DependencyAlreadyExistsException("The dependency $identifier already exists. You are probably looking to" +
                "update $identifier instead")
    val doc = Document()
        .append("identifier", identifier)
        .append("versions", listOf(Document()
            .append("version", version)
            .append("dependencies", dependencies)
            .append("repositories", repositories)))

    db.getCollection("dependencies").insertOne(doc)
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

class DependencyAlreadyExistsException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class DependencyNotFoundException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
