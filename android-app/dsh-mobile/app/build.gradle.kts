plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.self.dshmobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.self.dshmobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // dsh Web 服务默认地址与端口（随容器内 `dsh web` 启动）
        buildConfigField("String", "DSH_WEB_HOST", "\"127.0.0.1\"")
        buildConfigField("int", "DSH_WEB_PORT", "3080")
        // 容器数据目录（root 可访问、非易失分区，见方案 7.1）
        buildConfigField("String", "CONTAINER_ROOT", "\"/data/local/dsh-container\"")
        // 内置容器归档（APK assets 内嵌，首次部署解压，无需下载）
        buildConfigField("String", "CONTAINER_ASSET", "\"dsh-container-arm64.tar.gz\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // 内置容器归档已是 gzip，禁止再 deflate 一次（避免大文件二次压缩的内存/CPU 峰值）
    androidResources {
        noCompress += "gz"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // Material 3（原生壳）
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
