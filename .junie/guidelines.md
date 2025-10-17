WeerWolven — Project guidelines for advanced contributors

Scope
- This file captures practical build, test, and development notes specific to this repository. It assumes familiarity with Gradle, Java 21, and PaperMC plugin development.

Build and configuration
- Java/Toolchains
  - Target Java: 21 (configured via Gradle toolchains). Ensure JDK 21 is available; Gradle will provision a matching toolchain if your current JDK is older.
- Gradle plugins of note
  - xyz.jpenilla.run-paper: run a Paper test server locally.
  - com.gradleup.shadow: produce a shaded jar for deployment.
  - org.hidetake.ssh: scripted SFTP deployment (see Deploy section).
- Dependencies
  - Paper API is compileOnly; runtime is provided by the server. Keep plugin classes from directly touching CraftBukkit/implementation types unless guarded by reflection/availability checks.
  - JUnit 5 is configured for unit tests (testImplementation). Tests run on JUnit Platform.
- Common tasks
  - Build (compile and package standard jar): gradlew build
  - Shaded artifact (recommended for deployment): gradlew shadowJar
    - Artifact path is the shadow jar produced by the shadow plugin (classifier -all by default).
  - Run a local Paper server (auto-downloads a matching server jar): gradlew runServer
    - The plugin’s built jar (or shadow jar if present) is injected automatically.
  - Clean: gradlew clean
- Resource processing
  - processResources expands paper-plugin.yml with the project version.

Deploy (SFTP via Gradle)
- gradle.properties controls deployment:
  - deploy.host, deploy.port, deploy.user, deploy.password, deploy.remoteDir
- Task: gradlew deployToVenex
  - Builds the shadow jar, connects via SFTP, removes any existing Weerwolven*.jar in the target directory, and uploads the new artifact.
  - Prefer SSH key auth in real environments; the script currently uses password if provided.

Testing
- Framework
  - JUnit 5 (Jupiter) is set up; Gradle test task uses useJUnitPlatform().
- Running tests
  - CLI: gradlew test (preferred; prints passed/skipped/failed events).
  - IDE: Use IntelliJ’s test runner; the project is standard Java with JUnit 5 discovery.
- Adding tests
  - Location: src/test/java
  - Package tests to mirror src/main/java packages of the units you test.
  - Avoid hard dependencies on Bukkit/Paper classes in pure unit tests. Isolate logic behind small adapters or keep tests purely on internal classes (e.g., vote logic, orchestration policies, formatting utilities like TimerFormats).
  - If you need integration with Bukkit APIs, either:
    - Mark tests @Disabled and run them manually inside a harness, or
    - Create thin seams around Bukkit calls and unit-test the logic behind those seams.
- Example process (verified locally during this update)
  - A trivial JUnit test in src/test/java executed successfully via the test task. The file was temporary and has been removed as requested in the issue; the configuration remains so you can add your own tests immediately.

Code style and development notes
- Language and comments
  - Use English for all comments and identifiers.
  - Favor concise, professional comments that explain intent and constraints, not obvious mechanics. Prefer Javadoc for public APIs and non-trivial classes; use line comments sparingly for algorithmic decisions or caveats.
- TimerScope import rule (readability)
  - Do not use fully qualified enum access like me.mxndarijn.weerwolven.game.timer.TimerScope.GROUP in code bodies.
  - Always add an import for TimerScope at the top and reference as TimerScope.GROUP (or PER_PLAYER). Apply the same principle to other project-local types when readability benefits.
- Timers and ActionBar rendering
  - ActionTimerService ticks every 10L (0.5s), pushes per-player action bar lines only when the mm:ss key changes. Host action bar rotates one timer per second. When adding timers, prefer deterministic ids and stable audiences. Use TimerFormats for mm:ss and progress bars to keep UI consistent and avoid duplication.
  - Minimize expensive work inside onTick lambdas. Prefer computing only values needed for the message and guard heavy calls with coarse cadence if necessary.
- Voting orchestration
  - DayOrchestrator and DuskOrchestrator demonstrate the pattern: start vote/phase work, start a TimerSpec with TimerScope.GROUP and the alive-set as audience, cancel the timer in the completion callback, and call game.getGameVoteManager().forceResolve() on timeout.
  - Weights are resolved via GameEventBus (e.g., DayVoteWeightEvent). Keep new weighting logic in listeners; avoid coupling to orchestrators.
- Event bus listeners
  - Listeners should be small and stateless where possible. Use Priority appropriately and keep side effects explicit. Prefer returning quickly if the event is irrelevant to the current game state.
- Bukkit threading
  - Any interaction with Bukkit world or Player must occur on the main thread. When a timer callback needs to touch Bukkit APIs, dispatch via Bukkit.getScheduler().runTask(plugin, runnable) as shown in DuskOrchestrator.
- Nullability and Optionals
  - GamePlayer exposes Optional<UUID>; always check presence before resolving Player from Bukkit. Avoid holding Player references; resolve per tick/operation.
- Collections
  - Many collections here are ConcurrentHashMap-backed for tick safety. Keep per-tick allocations low; reuse buffers if you expand ActionTimerService or related services.

Debugging tips
- Use structured, minimal logging near orchestration boundaries (phase transitions, vote resolution, timer timeouts). Prefer consistent prefixes so hosts and devs can filter logs quickly.
- When diagnosing vote tallies or weights, probe via temporary [DEBUG_LOG] prints inside the relevant code paths and remove them after verification.

Project-specific gotchas
- Resource strings
  - LanguageManager keys map to src/main/resources/languages; ensure new keys are added for both player and host messages where relevant.
- Visibility and houses
  - Dusk orchestration toggles GameVisibilityManager and GameHouseManager states. Always clean up door/window permissions when completing a phase (see cleanupDuskHome).

How to verify your change quickly
- Add a focused unit test for any pure logic you touch and run gradlew test.
- For gameplay flows, spin a server with gradlew runServer and run through a short phase (e.g., Dusk) to validate ActionBar and teleport behavior. Keep your temporary logs guarded and remove them before committing.

Housekeeping
- Do not commit temporary tests or scratch files. If you need to verify locally, place throwaway tests under src/test/java and remove them before pushing. The repository now has test infrastructure ready, but policy is to keep only persistent, value-adding tests.
