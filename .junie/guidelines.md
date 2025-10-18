This repository is a Minecraft (PaperMC) plugin. All of the mechanics are played in Minecraft. it has nothing to do with real life.

Important import rule for readability
- Always import classes and enums you use. Do not reference fully qualified names inline. For example, prefer:
  - import java.util.List; then use List
  - import me.mxndarijn.weerwolven.game.timer.TimerScope; then use TimerScope.GROUP

Build and configuration (project-specific)
- Java version: 21. The Gradle build uses toolchains; Gradle will provision a matching JDK if your current JDK is older. You can still invoke Gradle with any modern JDK; the toolchain ensures bytecode level.
- Primary tasks
  - Build: gradlew build
  - Shaded jar for deployment: gradlew shadowJar
    - The shadow plugin adds the -all classifier to the artifact.
  - Run a local Paper server (auto-downloads matching server): gradlew runServer
    - Your plugin’s jar (shadow jar if present) is injected automatically.
  - Clean: gradlew clean
- Dependencies (from build.gradle)
  - Paper API is compileOnly; runtime provided by the server. Avoid depending on CraftBukkit internals directly unless guarded.
  - JUnit 5 for tests via junit-jupiter BOM.
  - Lombok is configured for both main and test sources (compileOnly + annotationProcessor).
  - Additional libs: MxLib, HikariCP, Hibernate ORM, json-simple, etc.
- Resource processing
  - processResources expands paper-plugin.yml with the project version.
- Deployment (SFTP)
  - gradle.properties keys: deploy.host, deploy.port, deploy.user, deploy.password, deploy.remoteDir
  - Task: gradlew deployToVenex
    - Builds the shadow jar, connects via SFTP, deletes any existing Weerwolven*.jar in the target directory, and uploads the new artifact.
    - Prefer SSH key auth in production; current script uses password if provided.

Testing: configuring, running, and adding tests
- Framework: JUnit 5 (Jupiter). Gradle’s test task is set to useJUnitPlatform() and prints passed/skipped/failed.
- Run tests
  - CLI: gradlew test
  - IDE: use IntelliJ IDEA’s JUnit runner. The project uses standard Java test layout and JUnit 5 discovery.
- Add tests
  - Location: src/test/java
  - Package tests to mirror src/main/java packages of the units you test.
  - Keep unit tests pure-Java when possible. Avoid direct Bukkit/Paper dependencies in unit tests. If you need Bukkit integration:
    - Either wrap Bukkit calls behind small adapters and unit-test the logic behind those seams, or
    - Mark such tests with @Disabled and run them manually in a server harness.
- Verified example
  - We temporarily added a trivial JUnit test under src/test/java/me/mxndarijn/weerwolven that asserted 2 + 3 == 5 and confirmed the test pipeline passes via gradlew test. The file was removed to keep the repository clean, but the configuration is ready for your own tests.

Development notes (project-specific)
- Code style
  - English for identifiers and comments.
  - Import classes and enums instead of using fully qualified names in code bodies (see rule at the top). This especially applies to project-local enums such as TimerScope.
  - Use Javadoc for public APIs and non-trivial classes; keep line comments focused on intent, constraints, and caveats.
- TimerScope import rule
  - Always import TimerScope and reference as TimerScope.GROUP or TimerScope.PER_PLAYER. Do not write me.mxndarijn.weerwolven.game.timer.TimerScope.GROUP inline.
- Timers and ActionBar rendering
  - ActionTimerService ticks every 10L (0.5s) and only pushes per-player action bar lines when the mm:ss key changes. Host action bar rotates one timer per second.
  - When adding timers, prefer deterministic ids and stable audiences. Use TimerFormats utilities for mm:ss and progress bars for consistent UI and to avoid duplication.
  - Keep onTick lambdas light; compute only values needed for the message and guard expensive calls with a coarser cadence.
- Voting orchestration
  - DayOrchestrator and DuskOrchestrator illustrate the pattern: start vote/phase work; start a TimerSpec with TimerScope.GROUP and the alive-set as audience; cancel the timer in the completion callback; call game.getGameVoteManager().forceResolve() on timeout.
  - Weighting is resolved via GameEventBus events (e.g., DayVoteWeightEvent). Keep new weighting logic in listeners; avoid coupling orchestrators to specific weights.
- Event bus listeners
  - Keep listeners small and stateless. Use Priority appropriately, exit early if irrelevant to current game state, and keep side effects explicit.
- Bukkit threading
  - Interactions with the world or Player must occur on the main thread. From timer callbacks that need Bukkit APIs, dispatch via Bukkit.getScheduler().runTask(plugin, runnable) as shown in DuskOrchestrator.
- Nullability and Optionals
  - GamePlayer exposes Optional<UUID>. Always check presence before resolving Player from Bukkit. Avoid holding Player references; resolve per tick/operation.
- Collections and concurrency
  - Many collections use ConcurrentHashMap for tick-safety. Keep per-tick allocations low; reuse buffers in services like ActionTimerService if extended.
- Debugging
  - Use minimal, structured logging at orchestration boundaries (phase transitions, vote resolution, timer timeouts). Prefer consistent prefixes so hosts and devs can filter quickly. Use [DEBUG_LOG] when temporarily probing logic; remove after verification.
- Project-specific gotchas
  - Resource strings: LanguageManager keys map to src/main/resources/languages. Ensure new keys are added for both player and host messages where relevant.
  - Visibility and houses: Dusk orchestration toggles GameVisibilityManager and GameHouseManager. Always clean up door/window permissions when completing a phase (see cleanupDuskHome).

How to verify your changes quickly
- Add a focused unit test for any pure logic you touch and run gradlew test.
- For gameplay flows, spin a server with gradlew runServer and run through a short phase (e.g., Dusk) to validate ActionBar and teleport behavior. Keep temporary logs guarded and remove them before committing.

Housekeeping
- Do not commit temporary tests or scratch files. If you verify locally, place throwaway tests under src/test/java and remove them before pushing. The repository is test-ready; keep only value-adding tests checked in.
