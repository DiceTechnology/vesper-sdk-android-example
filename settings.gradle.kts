pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            // Mux repository
            url = uri("https://muxinc.jfrog.io/artifactory/default-maven-release-local")
            content {
                includeGroupByRegex("com\\.mux.*")
            }
        }
        maven {
            // Endeavor Streaming Maven repository
            url = uri("https://d1yvb7bfbv0w4t.cloudfront.net/")
            credentials {
                username = extra["authToken"] as String
            }
        }
    }
}

rootProject.name = "vesper-sdk-android-example"
include(":app")