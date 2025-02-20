// tag::artifact-views-lib[]
plugins {
    id 'java-library'
}

repositories {
    mavenCentral()
}

dependencies {
    // Define some dependencies here
}

// Define a task that produces a custom artifact
tasks.register('createProductionArtifact', Jar) {
    archiveBaseName.set('production')
    from(sourceSets.main.output)
    destinationDirectory.set(file('build/libs'))
}

configurations {
    // Define a custom configuration and extend from runtimeClasspath
    apiProductionElements {
        extendsFrom(configurations.apiElements)
        attributes {
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, 'production'))
        }
        artifacts {
            add('apiProductionElements', tasks.named('createProductionArtifact'))
        }
    }
}
// end::artifact-views-lib[]

tasks.register('checkProducerVariants') {
    def producerProject = project(':producer')

    // Check the outgoing variants for the producer
    producerProject.configurations.each { config ->
        println "Configuration: ${config.name}"
        config.outgoing.artifacts.each {
            println "  - Artifact: ${it.file}"
        }
    }
}

tasks.register('checkProducerAttributes') {
    configurations.each { config ->
        println "\nConfiguration: ${config.name}"
        println 'Attributes:'
        config.attributes.keySet().each { key ->
            println "  - ${key.name} -> ${config.attributes.getAttribute(key)}"
        }
        println 'Artifacts:'
        config.artifacts.each {
            println "${it.file}"
        }
    }
}
