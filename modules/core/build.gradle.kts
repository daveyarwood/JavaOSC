plugins {
  `java-library`
  `maven`
  `maven-publish`
}

val sourcesJar by tasks.creating(Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets.getByName("main").allSource)
}

group = "io.djy"
version = "0.8"

publishing {
  publications {
    register("MyPublication", MavenPublication::class) {
      from(components["java"])
      groupId = "io.djy"
      artifactId = "javaosc"
      artifact(sourcesJar)
    }
  }

  repositories {
    maven {
      name = "clojars"
      url = uri("https://repo.clojars.org")
      credentials {
        username = System.getenv("CLOJARS_USER")
        password = System.getenv("CLOJARS_PASSWORD")
      }
    }
  }
}

repositories {
  jcenter()
  mavenCentral()
}

tasks.test {
  useJUnitPlatform()

  testLogging {
    showStandardStreams = true
  }
}

dependencies {
  api("org.slf4j:slf4j-api:1.7.25")
  api("org.slf4j:slf4j-ext:1.7.25")
  api("org.slf4j:slf4j-log4j12:1.7.25")
  runtimeOnly("log4j:log4j:1.2.17")
  testImplementation("junit:junit:4.12")
}

