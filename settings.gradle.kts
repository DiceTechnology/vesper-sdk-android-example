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
            url = uri("https://jitpack.io")
            credentials {
                username = extra["authToken"] as String
            }
        }
    }
}

rootProject.name = "vesper-sdk-android-example"
include(":app")