# Notes for Claude

- Bump the version in `gradle.properties` (`mod_version` for the backpack addon, `tweaks_version` for
  vanilla-tweaks/, `link_version` for storage-link/) with every change that ships a new jar. It becomes the jar file name and the
  version in `fabric.mod.json`. Minor version for new features, patch for fixes.
- Minecraft/Fabric servers are blocked in the cloud sandbox; GitHub Actions (`.github/workflows/build.yml`)
  builds the jar and boots a test server. Check that run instead of building locally.
