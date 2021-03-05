plugins {
  `java-library`
  `maven`
  `maven-publish`
  id("com.jfrog.bintray") version "1.8.4"
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
}

// To deploy, have environment variables set up and run `gradle bintrayUpload`
bintray {
  user = System.getenv("BINTRAY_USER")
  key = System.getenv("BINTRAY_KEY")

  setPublications("MyPublication")
  publish = true

  with(pkg) {
    repo = "maven"
    name = "javaosc"
    version.name = project.version.toString()
    vcsUrl = "https://github.com/daveyarwood/JavaOSC.git"
    setLicenses("BSD 3-Clause")
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

