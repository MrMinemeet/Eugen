
plugins {
    kotlin("jvm") version "1.9.21"
}


group = "org.example"
version = "0.1-inDev"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.slf4j:slf4j-api:2.0.10")
    implementation("org.mnode.ical4j:ical4j:4.0.0-rc2")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module = "opus-java")
    }
}

tasks.jar {
    manifest.attributes["Main-Class"] = "MainKt"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}


