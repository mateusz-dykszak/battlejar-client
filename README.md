# BattleJar Client

BattleJar Client is the client implementation for the BattleJar Universe game engine, built with Java 25.

## Release

Current published version: **0.2.3** (GitHub Packages). The repository may track a newer **0.2.4-SNAPSHOT** between releases; dependency examples below use **0.2.3** unless you deliberately depend on snapshots from `mavenLocal()`.

## Project Structure

- `api`: Domain models (`Entity`, orders, registration, **`GameRecord` / `Frame`**) and serializers, including **JSON Lines** replay support (`GameRecordJsonlCodec`).
- `client`: HTTP + WebSocket client (`BattleJarClient`, `BattleJarContinuous`). **Registration is handled by the library**: you call `BattleJarClient.register(player)` (single game) or `BattleJarContinuous.run()` (multi-game loop) and do not need any additional registration steps. After a finished game it can optionally **download server history** (ZIP) into a directory named by **`BATTLEJAR_HISTORY_DIR`**, so you can capture JSON Lines replays locally. Behaviour is unchanged if that variable is unset. Till version 1.0.0, treat the API as unstable; minor bumps may introduce breaking changes.
- `math`: Math utilities. It's an almost exact copy of the [math package from the libgdx repository](https://github.com/libgdx/libgdx/tree/master/gdx/src/com/badlogic/gdx/math) (TODO: check if it can be replaced with [gdx-math](https://github.com/mini2Dx/gdx-math)). The project was originally a libgdx project.

### Replay files and testing

The API exposes **`GameRecord`** (participants plus ordered **`Frame`** snapshots with entities and orders) and **`GameRecordJsonlCodec`** in `it.battlejar.api.serialization` to read/write the server’s JSON Lines format.

Set the environment variable **`BATTLEJAR_HISTORY_DIR`** to an existing or creatable writable directory before launching your commander through **`BattleJarClient`**. After the WebSocket session ends, the client may download the game history archive from the API and unzip JSONL replay files **into that directory** (skipped if unset). Those files are handy as **frozen inputs** when unit-testing tactical logic outside a live connection.

See [HOWTO.md](HOWTO.md#15-game-history-and-testing) for a one-screen summary aimed at commander authors.

## Building and Installation

### Build the project
To build the project and run tests, use:
```bash
./gradlew build
```

### Install to mavenLocal
To install the artifacts to your local Maven repository, run:
```bash
./gradlew publishToMavenLocal
```

### Using as a dependency (GitHub Packages)

To depend on published artifacts (replace the version with the tag you target, e.g. **0.2.3**), add the GitHub Packages repository and the dependency coordinates below. Choose Gradle or Maven and follow that section from top to bottom.

---

#### Gradle

**Repository setup** — Add the GitHub Packages repository to your `build.gradle` or `settings.gradle` (e.g. inside `dependencyResolutionManagement.repositories` or root `repositories`):

```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/mateusz-dykszak/battlejar-client")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

**Credentials (choose one):**

- **In project / env:** The block above reads `gpr.user` and `gpr.key` from project properties or environment variables `USERNAME` and `TOKEN`.
- **Global (recommended if you don’t want credentials in the project):** Put credentials in `~/.gradle/gradle.properties` so the project never contains them. You still need the `repositories { ... }` block in the project (or in `~/.gradle/init.gradle` for all projects).

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_TOKEN
```

Use a [GitHub Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope for `gpr.key`.

**Dependencies** — Add to your `dependencies` block. The **math** module is optional (only if you need `Vector2` or other types from the math package).

Kotlin DSL:

```kotlin
dependencies {
    implementation("it.battlejar:battlejar-api:0.2.3")
    implementation("it.battlejar:battlejar-client:0.2.3")
    // optional: only if you need Vector2 and other math utilities
    implementation("it.battlejar:battlejar-math:0.2.3")
}
```

Groovy:

```groovy
dependencies {
    implementation 'it.battlejar:battlejar-api:0.2.3'
    implementation 'it.battlejar:battlejar-client:0.2.3'
    // optional: only if you need Vector2 and other math utilities
    implementation 'it.battlejar:battlejar-math:0.2.3'
}
```

---

#### Maven

**Repository setup** — Add the repository in your project’s `pom.xml`. Do not put credentials in `pom.xml`; use `~/.m2/settings.xml` (see below).

```xml
<repositories>
    <repository>
        <id>github-battlejar-client</id>
        <url>https://maven.pkg.github.com/mateusz-dykszak/battlejar-client</url>
        <snapshots><enabled>false</enabled></snapshots>
    </repository>
</repositories>
```

**Credentials** — Add a `<server>` with the same `id` in `~/.m2/settings.xml` so Maven can authenticate:

```xml
<servers>
    <server>
        <id>github-battlejar-client</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

Use a [GitHub Personal Access Token](https://github.com/settings/tokens) with `read:packages` scope as the password.

**Dependencies** — Add to your `pom.xml`. The **math** module is optional (only if you need `Vector2` or other types from the math package).

```xml
<dependencies>
    <dependency>
        <groupId>it.battlejar</groupId>
        <artifactId>battlejar-api</artifactId>
        <version>0.2.3</version>
    </dependency>
    <dependency>
        <groupId>it.battlejar</groupId>
        <artifactId>battlejar-client</artifactId>
        <version>0.2.3</version>
    </dependency>
    <!-- optional: only if you need Vector2 and other math utilities -->
    <dependency>
        <groupId>it.battlejar</groupId>
        <artifactId>battlejar-math</artifactId>
        <version>0.2.3</version>
    </dependency>
</dependencies>
```

---

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every subproject.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `test`: runs unit tests.
- `publishToMavenLocal`: installs artifacts to your local Maven repository.

Note that most tasks that are not specific to a single subproject can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific subproject.
For example, `client:clean` removes `build` folder only from the `client` subproject.

