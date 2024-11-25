plugins {
    java
}

abstract class Transform : TransformAction<TransformParameters.None> {
    abstract val input: Provider<FileSystemLocation>
        @InputArtifact get

    abstract val dependencies: FileCollection
        @InputArtifactDependencies get

    override fun transform(outputs: TransformOutputs) {
        for (dependency in dependencies) {
            println("Dependency: $dependency")
        }

        outputs.file(input)
    }
}

val transformationState = Attribute.of("transformationState", String::class.java)

val feature by sourceSets.creating

val generated = configurations.consumable("generated") {
    outgoing {
        capability("test:generated:1.0")
    }

    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.STANDARD_JVM))
    }
}

val dependencyHolder = configurations.dependencyScope("generatedDependencyHolder")

val generatedDependency = dependencies.project(path).apply {
    capabilities {
        requireCapability("test:generated")
    }
}

java {
    registerFeature(feature.name) {
        usingSourceSet(feature)
    }
}

configurations.compileClasspath {
    extendsFrom(dependencyHolder.get())

    attributes {
        attribute(transformationState, "transformed")
    }
}

configurations.named(feature.compileClasspathConfigurationName) {
    extendsFrom(dependencyHolder.get())

    attributes {
        attribute(transformationState, "transformed")
    }
}

artifacts {
    add(generated.name, file("generated.jar")) {
        type = "generated"
        extension = "generated"
    }
}

dependencies {
    artifactTypes {
        register("generated") {
            attributes.attribute(transformationState, "none")
        }
    }

    registerTransform(Transform::class.java) {
        from.attribute(transformationState, "none")
        to.attribute(transformationState, "transformed")
    }

    dependencyHolder(generatedDependency)

    feature.implementationConfigurationName(project(path)) {
        capabilities {
            requireCapability("${project.group}:${project.name}")
        }
    }
}
