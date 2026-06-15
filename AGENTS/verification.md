## Verification Policy

- After implementation, only verify that the project builds.
- Do not launch Minecraft or perform interactive/runtime tests unless the user explicitly requests them.
- Use:

```sh
env LD_LIBRARY_PATH=/nix/store/zl6x30j3w9byijlj1x2nx1zavxvxaxv6-mob-life-native-libraries/lib ./gradlew build
```

- The Gradle client run also includes `/run/opengl-driver/lib` and the Nix linker path through `build.gradle`.
