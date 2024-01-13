
plugins {
    kotlin("jvm") version "1.9.21"
}


group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.slf4j:slf4j-api:2.0.10")
    implementation("org.mnode.ical4j:ical4j:4.0.0-rc2")
    implementation("org.jsoup:jsoup:1.17.2")
}

