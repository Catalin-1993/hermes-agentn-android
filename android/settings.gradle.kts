pluginManagement {
    repositories {
        run {
            val configured = providers.gradleProperty("hermesChaquopyLab").orNull
                ?: providers.gradleProperty("hermesPythonBundle").orNull
            val lab = configured?.let { file(it).canonicalFile }
                ?: gradle.gradleUserHomeDir.resolve("hermes-python-runtime").canonicalFile
            require(lab.resolve("consumer.json").isFile && lab.resolve("maven").isDirectory) {
                "Prepare the source-built Hermes Python bundle before Gradle: see android/PYTHON_RUNTIME.md"
            }
            exclusiveContent {
                forRepository { maven { url = uri(lab.resolve("maven")) } }
                filter { includeModule("com.chaquo.python.runtime", "bootstrap") }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // sherpa-onnx, the on-device speech recognizer, is published only through JitPack. Nothing
        // else may come from there, and the app's build checks the AAR's SHA-256.
        exclusiveContent {
            forRepository { maven { url = uri("https://jitpack.io") } }
            filter { includeGroup("com.github.k2-fsa.sherpa-onnx") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "HermesAgentAndroid"
include(":app")
include(":macrobenchmark")
