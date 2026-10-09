plugins {
    java
}

group = providers.gradleProperty("maven_group").get()
version = providers.gradleProperty("twilight_version").get()

base {
    archivesName = "TwilightProxy"
}

dependencies {
    // Compiled against current APIs; only long-standing events and methods are used.
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.1")
    compileOnly("org.geysermc.geyser:api:2.11.2-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.14.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // The proxies provide Gson at runtime (item mappings).
    testImplementation("com.google.code.gson:gson:2.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}

// The plugin-message protocol shared with Twilight on the backends.
sourceSets.main {
    java.srcDir(rootProject.file("protocol/src/main/java"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Geyser, which twilight-proxy serves, requires Java 21.
    options.release = 21
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    val properties = mapOf("version" to project.version.toString())
    inputs.properties(properties)
    filesMatching(listOf("velocity-plugin.json", "bungee.yml")) {
        expand(properties)
    }
}

tasks.jar {
    archiveFileName = "TwilightProxy.jar"
    from(rootProject.file("LICENSE")) { rename { "LICENSE_Twilight" } }
    from(rootProject.file("LICENSE.LESSER")) { rename { "LICENSE.LESSER_Twilight" } }
    manifest.attributes(
        "Implementation-Title" to "twilight-proxy",
        "Implementation-Version" to project.version,
        "Implementation-Vendor" to "siberanka"
    )
}

tasks.test {
    useJUnitPlatform()
}
