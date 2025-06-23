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
            url = uri("https://wdb-android-maven-844218222632.europe-west3.run.app")
            credentials {
                username = getLocalProperty("wdbuser")
                password = getLocalProperty("wdbpw")
            }
        }
    }
}

fun Settings.getLocalProperty(key: String): String? {
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (!localPropertiesFile.exists()) return null

    val props = java.util.Properties()
    localPropertiesFile.inputStream().use { props.load(it) }
    return props.getProperty(key)
}

rootProject.name = "WeDoBooks SDK sample app"
include(":app")
 