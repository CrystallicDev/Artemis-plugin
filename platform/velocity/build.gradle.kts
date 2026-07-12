plugins {
    id("apollo.shadow-conventions")
}

base {
    archivesName.set("Artemis-velocity")
}

java {
    javaTarget(17)
}

dependencies {
    compileOnly(libs.velocity)

    api(project(path = ":apollo-api", configuration = "shadow"))
    api(project(path = ":apollo-common", configuration = "shadow"))
    api(project(":extra:apollo-extra-adventure4"))

    annotationProcessor(libs.velocity)
}
