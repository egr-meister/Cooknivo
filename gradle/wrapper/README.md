# Gradle Wrapper

`gradle-wrapper.jar` is intentionally not committed in this snapshot.

Generate it once (either will do):

- **Android Studio**: open the project — it regenerates the wrapper on first sync.
- **Command line** (requires a local Gradle 8.9+):

  ```
  gradle wrapper --gradle-version 8.9 --distribution-type bin
  ```

The GitHub Actions workflow generates the wrapper automatically before building,
so CI does not require the jar to be present.
