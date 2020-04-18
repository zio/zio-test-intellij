# ZIO Test runner support for the ZIO IntelliJ plugin

| CI | Release | Snapshot | Discord |
| --- | --- | --- | --- |
| [![Build Status][Badge-Circle]][Link-Circle] | [![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases] | [![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] | [![Badge-Discord]][Link-Discord] |

# Summary
This is an optional dependency module, providing enhanced test runner support in the [ZIO IntelliJ plugin](https://github.com/zio/zio-intellij).

<img src="https://user-images.githubusercontent.com/601206/74926840-37f77c80-53df-11ea-9991-ebd4b870d857.png" width="600" />

# Usage

Add the following to your SBT file:

```
"dev.zio" %%% "zio-test-intellij" % zioVersion % "test"
```

The next time you run your tests from IntelliJ, the results will be rendered in the integrated test runner.

**Please report any issues in the parent project [ZIO IntelliJ plugin](https://github.com/zio/zio-intellij/issues).**

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

