# LootBeams (Fabric)

A client-side Fabric mod that renders colored beams of light and floating name tags above dropped items in the world.

This project is a multi-version Fabric port of the original Forge LootBeams mod by Elise (shiroroku).

## Supported Versions

| Minecraft Version | Loader | Subproject | Mappings | Language Level |
|---|---|---|---|---|
| 1.21.1 | Fabric | `:v1_21_1` | Yarn | Java 21 |
| 1.21.4 | Fabric | `:v1_21_4` | Yarn | Java 21 |
| 1.21.10 | Fabric | `:v1_21_10` | Yarn | Java 21 |
| 1.21.11 | Fabric | `:v1_21_11` | Yarn | Java 21 |
| 26.1.2 | Fabric | `:v26_1_2` | Mojmap | Java 25 |
| 26.2 | Fabric | `:v26_2` | Mojmap | Java 25 |
| 26.3 | Fabric | `:v26_3` | Mojmap | Java 25 |

The project architecture is structured under `versions/<version>` so future versions (such as 26.4) can be added cleanly as new subprojects.

## Project Structure

```
.
├── build.gradle
├── settings.gradle
├── gradle.properties
├── versions/
│   ├── 1.21.1/
│   ├── 1.21.4/
│   ├── 1.21.10/
│   ├── 1.21.11/
│   ├── 26.1.2/
│   ├── 26.2/
│   └── 26.3/
```

- **1.21.1**: Uses classic `ItemEntityRenderer` with MatrixStack and VertexConsumerProvider.
- **1.21.4**: Uses `ItemEntityRenderState` with MatrixStack and VertexConsumerProvider.
- **1.21.10 / 1.21.11**: Uses modern `ItemEntityRenderState` with `SubmitNodeCollector` / `OrderedRenderCommandQueue`.
- **26.x**: Uses official Mojang mappings and the modern deferred render submit pipeline.

## Build Requirements

- JDK 25 is required to run the Gradle build daemon because Fabric Loom parses Java 25 bytecode for the 26.x targets.
- If your system default JDK is not Java 25, ensure your `JAVA_HOME` points to a Java 25 installation or configure `org.gradle.java.home` in `~/.gradle/gradle.properties`.

## Compilation

Build all targets:

```bash
./gradlew build
```

Build a specific target:

```bash
./gradlew :v1_21_1:build
./gradlew :v1_21_4:build
./gradlew :v1_21_10:build
./gradlew :v1_21_11:build
./gradlew :v26_1_2:build
./gradlew :v26_2:build
./gradlew :v26_3:build
```

Compiled jar files are generated in `versions/<target>/build/libs/`.

## Configuration

Settings are saved in `config/lootbeams.json`. Key options include:

- `all_items`: Render beams on all items, or only specific rarities.
- `render_nametags`: Toggle floating name tags.
- `render_nametags_onlook`: Only display name tags when looking directly at items.
- `beam_radius`, `beam_height`, `beam_y_offset`: Geometry scaling.
- `color_overrides`: Custom hex colors mapped to item IDs or tags.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

Original mod created by Elise (shiroroku).
Fabric port maintained by ManorDev.
