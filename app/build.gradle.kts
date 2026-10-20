import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.equipe1.aurora"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.equipe1.aurora"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            excludes.add("META-INF/INDEX.LIST")
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/io.netty.versions.properties")
        }
    }

    buildFeatures {
        viewBinding = true
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
}

dependencies {

    // =========================================================================
    // 1. INTERFACE DE USUÁRIO (UI) E RECURSOS CORE
    // =========================================================================
    // Suporte para compatibilidade de componentes visuais em versões antigas do Android
    implementation(libs.appcompat)
    // Componentes visuais do Material Design (Botões, Cards, Modais, etc.)
    implementation(libs.material)
    // Layout flexível para construção de telas responsivas
    implementation(libs.constraintlayout)
    // Extensões KTX para gerenciamento do ciclo de vida da Activity
    implementation(libs.activity.ktx)
    // Suporte para gerenciamento da tela de abertura (Splash Screen)
    implementation(libs.core.splashscreen)
    // Gerenciamento das telas de preferências e configurações do usuário
    implementation(libs.preference)


    // =========================================================================
    // 2. NAVEGAÇÃO ENTRE TELAS (JETPACK NAVIGATION)
    // =========================================================================
    // Suporte para gerenciamento do fluxo e transições entre Fragments
    implementation(libs.navigation.fragment)
    // Integração entre a navegação e componentes como Toolbar/BottomNavigation
    implementation(libs.navigation.ui)


    // =========================================================================
    // 3. PERSISTÊNCIA LOCAL DE DADOS (ROOM DATABASE - SUPORTE OFFLINE)
    // =========================================================================
    // Biblioteca de abstração do SQLite para armazenamento local no dispositivo
    implementation(libs.room.runtime)
    // Processador de anotações do Room para geração de código (Java/Kapt)
    annotationProcessor(libs.room.compiler)


    // =========================================================================
    // 4. COMUNICAÇÃO DE REDE / API REST (RETROFIT & GSON)
    // =========================================================================
    // Cliente HTTP para realizar chamadas REST para a API (MySQL no Servidor)
    implementation(libs.retrofit)
    // Conversor automático de respostas JSON para objetos Java/Kotlin
    implementation(libs.converter.gson)


    // =========================================================================
    // 5. GEOLOCALIZAÇÃO E SERVIÇOS EXTERNOS
    // =========================================================================
    // Biblioteca para exibição de mapas do OpenStreetMap (Mapeamento de Áreas de Risco)
    implementation(libs.osmdroid.android)
    // Google Play Services Auth para suporte ao Login com a conta Google
    implementation(libs.play.services.auth)


    // =========================================================================
    // 6. SERVIÇOS EM NUVEM (FIREBASE)
    // =========================================================================
    // Firebase BoM (Bill of Materials): Centraliza e alinha as versões dos serviços Firebase
    implementation(platform(libs.firebase.bom))
    // Coleta de métricas e telemetria de uso do aplicativo
    implementation(libs.firebase.analytics)


    // =========================================================================
    // 7. AMBIENTE DE TESTES
    // =========================================================================
    // Framework para testes unitários locais (JVM)
    testImplementation(libs.junit)
    // Extensão JUnit para testes de instrumentação no Android
    androidTestImplementation(libs.ext.junit)
    // Framework para testes automatizados de interface visual (UI)
    androidTestImplementation(libs.espresso.core)
}