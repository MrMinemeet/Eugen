
plugins {
    kotlin("jvm") version "1.9.23"
}


group = "org.example"
version = "0.10.3-inDev"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.mnode.ical4j:ical4j:4.0.0-rc6")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("net.dv8tion:JDA:5.0.0-beta.23") {
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


