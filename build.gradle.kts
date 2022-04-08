plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

repositories {
    mavenCentral()
    maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }

    maven { url = uri("https://nexus.illyria.io/repository/maven-public/") }

    maven { url = uri("https://jitpack.io") }

    maven { url = uri("https://maven.enginehub.org/repo/") }

    maven { url = uri("https://repo.maven.apache.org/maven2/") }

    maven { url = uri("https://repo.ajg0702.us") }
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")

    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.3-SNAPSHOT")

    implementation("net.kyori:adventure-api:4.10.0")
    implementation("net.kyori:adventure-text-minimessage:4.10.0")
    implementation("net.kyori:adventure-platform-bukkit:4.0.1")

    compileOnly("org.spongepowered:configurate-yaml:4.0.0")

    implementation("us.ajg0702:ajUtils:1.1.31")

    compileOnly(fileTree("dep"))
}

tasks.shadowJar {
    relocate("net.kyori", "us.ajg0702.antixray.libs.kyori")
    relocate("org.bstats", "us.ajg0702.antixray.libs.bstats")
    relocate("us.ajg0702.utils", "us.ajg0702.antixray.libs.utils")
    relocate("us.ajg0702.commands", "us.ajg0702.antixray.commands.base")
    relocate("com.zaxxer.hikari", "us.ajg0702.antixray.libs.hikari")
    relocate("org.spongepowered", "us.ajg0702.antixray.libs")
    relocate("org.yaml", "us.ajg0702.antixray.libs")
    relocate("io.leangen", "us.ajg0702.antixray.libs")
    archiveBaseName.set("ajAntiXray")
    archiveClassifier.set("")
    exclude("junit/**/*")
    exclude("org/junit/**/*")
    exclude("org/hamcrest/**/*")
    exclude("LICENSE-junit.txt")
}


group = "ajAntiXray"
version = "1.8.1"
description = "ajAntiXray"
java.sourceCompatibility = JavaVersion.VERSION_1_8

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}
