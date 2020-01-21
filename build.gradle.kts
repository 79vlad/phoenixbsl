import java.net.URI

plugins {
    java
    maven
    jacoco
    id("org.openjfx.javafxplugin") version "0.0.8"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

repositories {
    flatDir {
        dirs("libs")
    }
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
}

group = "org.github.otymko.phoenixbsl"
version = "0.3.1"

dependencies {
    testImplementation("com.hynnet", "jacob", "1.18")
    testImplementation("junit", "junit", "4.12")
    implementation("net.java.dev.jna:jna-platform:5.4.0")
    implementation("org.eclipse.lsp4j", "org.eclipse.lsp4j", "0.8.1")
    implementation("org.slf4j", "slf4j-api", "1.8.0-beta4")
    implementation("org.slf4j", "slf4j-simple", "1.8.0-beta4")
    implementation("com.jfoenix","jfoenix", "9.0.9")
    implementation("lc.kra.system","system-hook", "3.5")
}

configure<JavaPluginConvention> {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

// TODO: путь к jar после build, можно сделать лучше?
var jarName = ""

tasks.jar {
    jarName = this.archiveFileName.get()
    manifest {
        attributes["Main-Class"] = "org.github.otymko.phoenixbsl.LauncherApp"
        attributes["Implementation-Version"] = "0.3.0"
    }
    enabled = false
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    project.configurations.implementation.get().isCanBeResolved = true
    configurations = listOf(project.configurations["implementation"])
    archiveClassifier.set("")
}

tasks.register<Exec>("jpackage") {
    var jpackage = System.getenv("JPACKAGE_HOME") + "/jpackage.exe"
    executable(jpackage)
    args(
            "--name", "phoenixbsl",
            "--type", "msi",
            "--input", "build/libs",
            "--main-jar", jarName,
            "--win-dir-chooser",
            "--win-shortcut",
            "--win-menu",
            "--app-version", project.version,
            "--vendor", "otymko"
    )
}

javafx {
    version = "11"
    modules("javafx.controls", "javafx.fxml")
}
