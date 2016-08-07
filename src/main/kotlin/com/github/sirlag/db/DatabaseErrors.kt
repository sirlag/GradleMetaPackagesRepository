package com.github.sirlag.db

class DependencyAlreadyExistsException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class DependencyNotFoundException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class VersionAlreadyExistsExceptions(msg:String? = null, cause: Throwable? = null) : Exception(msg, cause)
class ConflictingDependencyVersionFound(versionInfo:String,
                                        msg:String = "Conflicting versioning info at $versionInfo",
                                        cause:Throwable? = null): Exception(msg, cause)