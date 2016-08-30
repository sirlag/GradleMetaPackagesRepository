package com.github.sirlag.Auth

import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.core.config.ConfigFactory
import org.pac4j.core.profile.CommonProfile

import org.pac4j.oauth.client.GitHubClient
import org.pac4j.sparkjava.DefaultHttpActionAdapter

class AppConfigFactory: ConfigFactory{
    override fun build(): Config {

        val clients = Clients("http://localhost:4567/callback", githubClient)

        val config = Config(clients)
        config.addAuthorizer("admin", RequireAnyRoleAuthorizer<CommonProfile>("ROLE_ADMIN"))
        config.httpActionAdapter = DefaultHttpActionAdapter()
        return config
    }
}