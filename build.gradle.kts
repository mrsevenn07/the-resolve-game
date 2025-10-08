plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "com.platformer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    
    // Math and utilities
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // For potential future graphics integration
    // implementation("org.lwjgl:lwjgl:3.3.3")
    // implementation("org.lwjgl:lwjgl-opengl:3.3.3")
    // implementation("org.lwjgl:lwjgl-glfw:3.3.3")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

application {
    mainClass.set("MainKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

// Create executable JAR
tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    
    // Include all dependencies in the JAR
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Custom task to run the game
tasks.register("runGame") {
    group = "application"
    description = "Run the 2.5D Platformer Game"
    dependsOn("run")
}

// Custom task to build and package the game
tasks.register("packageGame") {
    group = "build"
    description = "Build and package the complete game"
    dependsOn("jar")
    
    doLast {
        println("Game packaged successfully!")
        println("Run with: java -jar build/libs/${project.name}-${project.version}.jar")
    }
}