plugins {
    id("java")
    id("application")
}

version = "1.0.0"
group = "org.rwtodd"

sourceSets {
    main { 
        java { setSrcDirs(listOf("src")) }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.rwtodd:org.rwtodd.args:2.0.1")
}

application {
    mainModule = "asciipic"
    mainClass = "asciipic.App"
}

tasks.withType<JavaCompile>().configureEach {
   options.release = 21
}

tasks.register("runcmd") {
    dependsOn("assemble")
    doLast {
        var cp = sourceSets["main"].runtimeClasspath.filter { it.name.endsWith(".jar") }.getFiles().toList().map { it.getAbsolutePath() }
        cp += File(projectDir,"build/libs/asciipic-1.0.0.jar").getAbsolutePath()
        var joinChar = ":" 
        if ( (System.getProperties().get("os.name") as String).lowercase().contains("windows")) {
            joinChar = ";"
        }
        println("java -p '${cp.joinToString(joinChar)}' -m asciipic")
    }
}
