package com.github.sirlag.db

import com.github.sirlag.Dependency
import com.github.sirlag.DependencyVersion
import com.github.sirlag.User

interface RepositoryDatabase {
    //User Functions
    infix fun addUser(user: User)
    fun getUser(email: String): User?
    fun getUserByDisplayName(name: String): User?
    fun setUserDisplayName(email: String, displayName: String): Boolean

    //Dependency Functions
    fun getDependency(identifier: String): Dependency?
    infix fun addDependency(dependency: Dependency)
    fun addVersionToDependency(identifier: String, version: DependencyVersion)
    fun getDependencyVersion(identifier: String, version: Double): DependencyVersion
    fun getLatestDependencyVersion(identifier: String): DependencyVersion
    fun searchForDependencies(identifier: String): List<Dependency>
    fun testDependencyData(){
        val fake = Dependency("Fake", arrayOf(DependencyVersion(1.0, arrayOf("compile 'io.reactivex:rxkotlin:0.60.0'"), emptyArray<String>())))
        val rxKovenant = Dependency("RxKovenant", arrayOf(DependencyVersion(1.0, arrayOf("compile 'io.reactivex:rxkotlin:0.60.0'", "compile 'nl.komponents.kovenant:kovenant:3.3.0'"), emptyArray<String>())))
        val fakeDependency = Dependency("FakeDependency", arrayOf(DependencyVersion(1.0, arrayOf("compile 'io.reactivex:rxkotlin:0.60.0'"), arrayOf("mavenLocal()", "jcenter()"))))

        this addDependency fake
        this addDependency rxKovenant
        this addDependency fakeDependency
    }
}