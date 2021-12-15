# ZIO Test runner support for the ZIO IntelliJ plugin

| Project Stage | CI | Release | Snapshot | Discord |
| --- | --- | --- | --- | --- |
| [![Project stage][Stage]][Stage-Page] | ![CI](https://github.com/zio/zio-test-intellij/workflows/CI/badge.svg) | [![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases] | [![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] | [![Badge-Discord]][Link-Discord] |

# Summary
This is an optional dependency module, providing enhanced test runner support in the [ZIO IntelliJ plugin](https://github.com/zio/zio-intellij) for ZIO 1.x.

<img src="https://user-images.githubusercontent.com/601206/74926840-37f77c80-53df-11ea-9991-ebd4b870d857.png" width="600" />

Once enabled, the test results will be rendered in the integrated IntelliJ test runner, allowing navigation, sorting, retrying failed tests and more!

# Usage

If you have the ZIO IntelliJ plugin installed, the first time you'll open a project with ZIO, the following prompt will appear in your IntelliJ:

![](https://user-images.githubusercontent.com/601206/104952866-019af680-59ce-11eb-9bd5-a2300691311d.png)

After clicking **Download ZIO Test runner** will automatically download the right version for your ZIO/Scala versions. The next time you'll run your tests from IntelliJ, they will run in the integrated runner.

**Please report any issues in the parent project: [ZIO IntelliJ plugin](https://github.com/zio/zio-intellij/issues).**

# Q&A

## What's the difference between this and ZIO IntelliJ plugin?

This is an optional library dependency, allowing the test runner to output results in a format that IntelliJ/TeamCity can understand.

When enabled, IntelliJ will use it to render the test output in the integrated runner. Otherwise, the output from the ZIO test runner will be displayed in the standard process output console.

## Why have a separate library at all?

Due to the way IntelliJ currently works, it makes it very difficult to include libraries that target a specific version of third-party libraries or different Scala versions. IntelliJ's own Scala plugin currently targets Scala 2.13.2, so all plugin developers that depend on it must target this version as well. The ZIO Plugin depends on the Scala plugin for Scala code analysis and other Scala-specific features.

Separating the library allows decoupling the plugin from any specific ZIO version, allowing separate and independent release cycles.

## I found a bug/have a feature request!

Great! Please report it to the [ZIO IntelliJ plugin](https://github.com/zio/zio-intellij/) repo.

# Contributing
[Documentation for contributors](https://zio.dev/docs/about/about_contributing)

## Code of Conduct

See the [Code of Conduct](https://zio.dev/docs/about/about_coc)

## Support

Come chat with us on [![Badge-Discord]][Link-Discord].

# License
[License](LICENSE)

[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/ziozio_2.12.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/dev.zio/ziozio_2.12.svg "Sonatype Snapshots"
[Badge-Discord]: https://img.shields.io/discord/629491597070827530?logo=discord "chat on discord"
[Badge-Circle]: https://circleci.com/gh/zio/zio-test-intellij.svg?style=svg "circleci"
[Link-Circle]: https://circleci.com/gh/zio/zio-test-intellij "circleci"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-test-intellij_2.12/ "Sonatype Releases"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/dev/zio/zio-test-intellij_2.12/ "Sonatype Snapshots"
[Link-Discord]: https://discord.gg/8fYmfG "Discord"
[Stage]: https://img.shields.io/badge/Project%20Stage-Production%20Ready-brightgreen.svg
[Stage-Page]: https://github.com/zio/zio/wiki/Project-Stages
