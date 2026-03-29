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
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/mapconductor/android-for-maplibre")
            credentials {
                username = System.getenv("GPR_USER") ?: ""
                password = System.getenv("GPR_TOKEN") ?: ""
            }
        }
    }
}

rootProject.name = "mapconductor-for-maplibre"
include(":sample-app")
