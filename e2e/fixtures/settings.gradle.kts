// Two tiny apps the emulator tests install next to Focus (see ../README.md). A build of their own,
// on purpose: nothing here touches the app's build, its release or F-Droid's build of it.
//   ./gradlew -p e2e/fixtures assembleDebug
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // The same plugin version as the app.
    versionCatalogs {
        create("libs") { from(files("../../gradle/libs.versions.toml")) }
    }
}

rootProject.name = "FocusTestFixtures"
include(":game", ":post")
