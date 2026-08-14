pluginManagement {
    repositories {
        // 沙箱网络拦截 maven.google.com，改用阿里云 google 镜像
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 沙箱网络拦截 maven.google.com，改用阿里云 google 镜像
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
    }
}

rootProject.name = "dsh-mobile"
include(":app")
