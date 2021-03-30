import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
	kotlin("multiplatform") version "1.4.10"
	id("com.github.johnrengelman.shadow") version "6.1.0"
	application
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
	jcenter()
	mavenCentral()
	maven { url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
	maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
	maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
	maven("https://dl.bintray.com/kotlin/kotlin-eap")
}
dependencies {
	implementation("org.junit.jupiter:junit-jupiter:5.4.2")
	implementation(kotlin("stdlib-jdk8"))
}

kotlin {
	
	jvm {
		compilations.all {
			kotlinOptions.jvmTarget = "1.8"
		}
		testRuns["test"].executionTask.configure {
			useJUnitPlatform()
		}
		
		withJava()
	}
	js(LEGACY) {
		browser {
			binaries.executable()
			webpackTask {
				cssSupport.enabled = true
			}
			runTask {
				cssSupport.enabled = true
			}
			testTask {
				useKarma {
					useChromeHeadless()
					webpackConfig.cssSupport.enabled = true
				}
			}
		}
	}
	
	sourceSets.all {
		languageSettings.apply {
			useExperimentalAnnotation("kotlin.time.ExperimentalTime")
		}
	}
	
	sourceSets {
		val commonMain by getting {
			dependencies {
			}
		}
		val jsMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.0")
			}
		}
		val jvmMain by getting {
			dependencies {
				implementation("io.ktor:ktor-server-netty:1.4.0")
				implementation("io.ktor:ktor-websockets:1.4.0")
				implementation("io.ktor:ktor-html-builder:1.4.0")
				implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
				implementation("org.slf4j:slf4j-simple:1.6.1")
			}
		}
		val jvmTest by getting {
			dependencies {
				implementation(kotlin("test-junit"))
			}
		}
	}
}

application {
	applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
	mainClassName = "ServerKt"
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
	outputFileName = "main.js"
	
	doLast {
		File("$projectDir/build/distributions/main.js")
			.copyTo(File("$projectDir/src/jvmMain/resources/static/main.js"), overwrite = true)
		File("$projectDir/build/distributions/main.js.map")
			.copyTo(File("$projectDir/src/jvmMain/resources/static/main.js.map"), overwrite = true)
		println("Copied packed JS")
	}
}

tasks.getByName<ProcessResources>("processResources") {
	dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
}

tasks.getByName<Jar>("jvmJar") {
	dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
}

tasks.getByName<JavaExec>("run") {
	dependsOn(tasks.getByName<Jar>("jvmJar"))
	classpath(tasks.getByName<Jar>("jvmJar"))
}

tasks.shadowJar {
	dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
	
	archiveBaseName.set("build")
	archiveClassifier.set("")
	archiveVersion.set("")
}