package com.github.sirlag.db

import com.github.sirlag.Dependency
import com.github.sirlag.DependencyVersion
import com.github.sirlag.User
import com.google.gson.Gson
import com.mongodb.MongoClient
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.regex
import org.bson.Document
import java.util.regex.Pattern

class MongoWrapper(address: String, database: String, private val gson: Gson): RepositoryDatabase {
    private val db: MongoDatabase = MongoClient(address).getDatabase(database)

    override fun addUser(user:User){
        val doc = Document()
            .append("name", user.name)
            .append("email", user.email)
        db.getCollection("users").insertOne(doc)
    }

    override fun getUser(email: String): User? {
        val user = db.getCollection("users").find(Document("email", email))?.first()?.toJson()
        return if(user != null) gson.fromJson(user, User::class.java) else null
    }

    override fun getUserByDisplayName(name: String): User? {
        val user = db.getCollection("users").find(Document("display_name", name))?.first()?.toJson()
        return if(user != null) gson.fromJson(user, User::class.java) else null
    }

    override fun setUserDisplayName(email:String, displayName: String): Boolean{
        if (db.getCollection("users").find(Document("display_name", displayName)).first() == null){
            db.getCollection("users").updateOne(Document("email", email), Document("\$set", Document("display_name", displayName)))
            return true
        }
        return false
    }

    override fun getDependency(identifier:String): Dependency? {
        val dependency = db.getCollection("dependencies").find(Document("identifier", identifier))?.first()?.toJson()
        return if(dependency != null) gson.fromJson(dependency, Dependency::class.java) else null
    }

    override infix fun addDependency(dependency: Dependency){

        if (getDependency(dependency.identifier) != null)
            throw DependencyAlreadyExistsException("The dependency ${dependency.identifier} already exists. You are" +
                    " probably looking to update ${dependency.identifier} instead")

        val doc = Document()
            .append("identifier", dependency.identifier)
            .append("versions", dependency.versions.map {
                Document("version", it.version)
                    .append("dependencies", it.dependencies.toList())
                    .append("repositories", it.repositories.toList())
            })
        db.getCollection("dependencies").insertOne(doc)
    }

    override fun addVersionToDependency(identifier: String, version: DependencyVersion){
        val dependency = getDependency(identifier)?: throw DependencyNotFoundException("Unable to find $identifier to add version to")
        if (dependency.versions.asList().contains(version))
            throw VersionAlreadyExistsExceptions("Unable to add version $version, it already exists")
        val versions = gson.toJson(dependency.versions + version)
        db.getCollection("dependencies")
                .updateOne(Document("identifier", identifier), Document("\$set", Document("versions",versions)))
    }

    override fun getDependencyVersion(identifier: String, version: Double) : DependencyVersion{
        val dependency = getDependency(identifier)
             ?: throw DependencyNotFoundException("Could not locate $identifier")
        val versionsList = dependency.versions.filter { it.version == version }
        if (versionsList.size == 0)
            throw DependencyNotFoundException("Unable to find $identifier")
        else if (versionsList.size > 1)
            throw ConflictingDependencyVersionFound("$identifier:$version")
        else
            return versionsList.first()
    }

    override fun getLatestDependencyVersion(identifier: String): DependencyVersion{
        val dependency = getDependency(identifier) ?: throw DependencyNotFoundException("Could not locate $identifier")
        return dependency.versions.sortedByDescending { it.version }.first()
    }

    override fun searchForDependencies(identifier: String): List<Dependency>{
        return db.getCollection("dependencies")
                .find(regex("identifier", Pattern.compile(Pattern.quote(identifier), Pattern.CASE_INSENSITIVE)))
                .map { gson.fromJson(it.toJson(), Dependency::class.java) }.toList()

    }

}
