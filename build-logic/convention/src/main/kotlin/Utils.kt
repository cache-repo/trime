import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.Task
import java.io.ByteArrayOutputStream
import java.io.File

inline fun envOrDefault(
    env: String,
    default: () -> String,
) = System.getenv(env)?.takeIf { it.isNotBlank() } ?: default()

inline fun Project.propertyOrDefault(
    prop: String,
    default: () -> String,
) = runCatching { property(prop)!!.toString() }.getOrElse {
    default()
}

fun Project.runCmd(cmd: String): String =
    ByteArrayOutputStream().use {
        project.exec {
            commandLine = cmd.split(" ")
            standardOutput = it
        }
        it.toString().trim()
    }

val json = Json { prettyPrint = true }

internal inline fun Project.envOrProp(
    env: String,
    prop: String,
    block: () -> String,
) = envOrDefault(env) {
    propertyOrDefault(prop) {
        block()
    }
}

val Project.assetsDir: File
    get() = file("src/main/assets").also { it.mkdirs() }

val Project.cleanTask: Task
    get() = tasks.getByName("clean")

// Change default ABI here
val Project.buildABI
    get() =
        envOrProp("BUILD_ABI", "buildABI") {
//        "armeabi-v7a"
            "arm64-v8a"
//        "x86"
//        "x86_64"
        }

val Project.builder
    get() =
        envOrProp("CI_NAME", "ciName") {
            runCatching { runCmd("git config user.name").ifEmpty { "(Unknown)" } }.getOrElse { "(Unknown)" }
        }

val Project.buildGitRepo
    get() =
        envOrProp("BUILD_GIT_REPO", "buildGitRepo") {
            runCmd("git remote get-url origin")
                .replaceFirst("^git@github\\.com:", "https://github.com/")
                .replaceFirst("\\.git\$", "")
        }

val Project.buildVersionName
    get() =
        envOrProp("BUILD_VERSION_NAME", "buildVersionName") {
            // 构建正式版时过滤掉 nightly 标签
            val cmd =
                if (builder.contains("nightly", ignoreCase = true)) {
                    "git describe --tags --long --always --match nightly"
                } else {
                    "git describe --tags --long --always --match v*"
                }
            runCmd(cmd)
        }

val Project.buildCommitHash
    get() =
        envOrProp("BUILD_COMMIT_HASH", "buildCommitHash") {
            runCmd("git rev-parse HEAD")
        }

val Project.buildTimestamp
    get() =
        envOrProp("BUILD_TIMESTAMP", "buildTimestamp") {
            System.currentTimeMillis().toString()
        }
