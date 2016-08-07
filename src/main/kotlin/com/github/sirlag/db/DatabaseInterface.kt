package com.github.sirlag.db

import com.github.sirlag.Dependency
import com.github.sirlag.DependencyVersion

interface RepositoryDatabase {
    fun getDependency(identifier: String): Dependency?
    fun addDependency(dependency: Dependency)
    fun addVersionToDependency(identifier: String, version: DependencyVersion)
    fun getDependencyVersion(identifier: String, version: Double): DependencyVersion
    fun getLatestDependencyVersion(identifier: String): DependencyVersion
    fun searchForDependencies(identifier: String): List<Dependency>
}