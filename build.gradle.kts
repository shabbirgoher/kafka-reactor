import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitVersion = "5.5.2"

plugins {
    base
    kotlin("jvm") version "1.3.61"
    jacoco
}

group = "com.reactor.lib"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven { url = uri("http://packages.confluent.io/maven/") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.kafka:spring-kafka:2.4.1.RELEASE")
    implementation("io.projectreactor.kafka:reactor-kafka:1.2.2.RELEASE")
    implementation("io.confluent:kafka-avro-serializer:5.4.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("net.logstash.logback:logstash-logback-encoder:6.3")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation("org.apache.kafka:kafka-clients:2.4.0")
    testImplementation("org.apache.kafka:kafka_2.12:2.4.1")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.4.1.RELEASE")


}

buildscript {
    repositories {
        jcenter()
    }
}

apply(plugin = "kotlin")

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

val fileTree: FileTree = configurations.testRuntimeClasspath.get().filter {
    it.name.endsWith(".so") || it.name.endsWith(".dll") || it.name.endsWith(".dylib")
}.asFileTree

tasks.register<Copy>("copyNativeDeps") {

    from(fileTree.files)
    into("build/native-libs")

    doFirst {
        mkdir("build/native-libs")
    }
}

tasks.withType<Test> {
    dependsOn("copyNativeDeps", "cleanTest")
    finalizedBy("jacocoTestReport", "jacocoTestCoverageVerification")
}

tasks {
    "test"(Test::class) {
        useJUnitPlatform()

        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events("passed", "skipped", "failed")
        }
    }

    jacocoTestReport {
        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
    }

    jacocoTestCoverageVerification {
        violationRules {
            rule { limit { minimum = BigDecimal.valueOf(0.80) } }
        }
    }
}
