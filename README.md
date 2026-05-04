# Horizon QA

Horizon QA is an end-to-end testing framework for GTNH. It implements the modern Minecraft GameTest API on 1.7.10, providing a specialized environment to validate multiblocks, machine logic, and logistics.

## Features
* **Java API**: Define tests using standard `@GameTest` annotations and the GameTestHelper class for assertions.
* **GTNH Integration**: Built-in support for handling EU, maintenance states, and time-warping for machine processing.
* **Structure Support**: Tools to export, place, and verify multiblock structures via JSON or NBT.
* **Visual Feedback**: In-game overlays, beacons, and ghost blocks to identify test failures visually.
* **CI/CD Integration**: Generates JUnit-compatible XML reports for automated build pipelines.
* **GameTest Wand**: An in-game tool to help with area selection and structure management.

## Usage

1. Build your test structure in-game.
2. Select the area with the **Horizon Wand** and use `/gametest export <name>` to create a template.
3. Write a Java test class using `@GameTest` to define the logic and assertions.
4. Run tests using the `/gametest runall` command and view results in-game or in the build logs.

The project maintains the `com.gtnewhorizons.gametest` package and standard naming conventions to stay compatible with existing GameTest knowledge.
