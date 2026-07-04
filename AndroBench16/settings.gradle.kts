//which module in the project, where to download plugin

pluginManagement{//where to download AGP
    repositories{
        google{//google playstore
            content{
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()//apple app store
        gradlePluginPortal()//gradle official plugin
    }
}

dependencyResolutionManagement{ //where to download library
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories{
        google()
        mavenCentral()
    }
}

rootProject.name="AndroBench16"
include(":app") //apk 1개 단위