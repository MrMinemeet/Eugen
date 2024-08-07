
plugins {
    kotlin("jvm") version "2.0.0"
}


group = "org.example"
version = "0.11.1-inDev"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.mnode.ical4j:ical4j:4.0.1")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("net.dv8tion:JDA:5.0.0") {
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


