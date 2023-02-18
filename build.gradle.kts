plugins {
	`java-library`
	`maven-publish`
}

gradle.buildFinished {
	project.buildDir.deleteRecursively()
}

allprojects {
	group = rootProject.group
	version = rootProject.version
	description = rootProject.description
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "maven-publish")

	java {
		toolchain.languageVersion.set(JavaLanguageVersion.of(17))
		withJavadocJar()
		withSourcesJar()
	}

	tasks {
		compileJava {
			options.encoding = Charsets.UTF_8.name()
			options.release.set(17)
		}
		processResources {
			filteringCharset = Charsets.UTF_8.name()
			filesMatching("**/plugin.yml") {
				expand( project.properties )
			}
		}
	}

	repositories {
		mavenCentral()
		maven("https://repo.dmulloy2.net/repository/public/")
		maven("https://ci.frostcast.net/plugin/repository/everything")
		maven("https://jitpack.io")
		maven("https://nexus.civunion.com/repository/maven-public/")
	}

	publishing {
		repositories {
			maven {
				url = uri("https://nexus.civunion.com/repository/maven-releases/")
				credentials {
					username = System.getenv("REPO_USERNAME")
					password = System.getenv("REPO_PASSWORD")
				}
			}
		}
		publications {
			register<MavenPublication>("main") {
				from(components["java"])
			}
		}
	}
}
