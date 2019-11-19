
import java.net.URI

plugins {
    java
    maven
    jacoco
    id("com.github.gradle-git-version-calculator") version "1.1.0"
    id("com.github.ben-manes.versions") version "0.22.0"
}

repositories {
    mavenCentral()
    maven { url = URI("https://jitpack.io") }
    flatDir {
        dirs("libs")
    }
}

group = "org.github.otymko.phoenixbsl"
version = "0.2.2"

dependencies {

    implementation("net.java.dev.jna:jna-platform:5.4.0")
    compile("net.java.dev.jna:jna-platform:5.4.0")
    compile("com.hynnet", "jacob", "1.18")
    compile("com.github.1c-syntax:bsl-language-server:ccad9e68b611d012b6523970cebdbe3b7a801770")
    testCompile("junit", "junit", "4.12")
    compile("lc.kra.system:system-hook:3.5")

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

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "org.github.otymko.phoenixbsl.Launcher"
        attributes["Implementation-Version"] = archiveVersion.get()
    }
    configurations["compile"].forEach {
        from(zipTree(it.absoluteFile)) {
            exclude("META-INF/MANIFEST.MF")
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
        }
    }
}