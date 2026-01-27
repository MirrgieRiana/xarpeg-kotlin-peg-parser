# Old Distribution Method Deprecation Plan

## Current State

The project currently maintains two distribution methods:

1. **Maven Central** (New, Official) - `https://repo1.maven.org/maven2/`
   - Artifact: `io.github.mirrgieriana:xarpeg-kotlinMultiplatform:4.0.3`
   - Standard, reliable, widely used
   - Automatically synced via Sonatype

2. **GitHub Pages Maven Repository** (Old, Deprecated) - `https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven`
   - Artifact: `io.github.mirrgieriana.xarpite:xarpeg-kotlin-peg-parser:3.0.0`
   - Custom hosting via GitHub Pages
   - Still being updated by `.github/workflows/publish.yml`

## Why Deprecate the Old Method?

1. **Redundancy**: Maintaining two distribution channels is unnecessary and confusing
2. **Outdated naming**: The old artifact coordinates use `xarpite` prefix which is inconsistent
3. **Non-standard**: GitHub Pages is not a standard Maven repository host
4. **Maintenance burden**: Keeping the `maven` branch updated adds complexity
5. **User confusion**: Multiple installation methods can confuse users

## Recommended Deprecation Steps

### Phase 1: Documentation Update (Completed ✓)
- Update all documentation to use Maven Central
- Update all sample projects to use Maven Central
- Remove references to the old repository URL

### Phase 2: Announce Deprecation (Recommended)
- Add a deprecation notice to the README
- Create a GitHub Release note announcing the change
- Update the `maven` branch to include a deprecation notice
- Consider adding a migration guide for existing users

### Phase 3: Maintain Compatibility Period (3-6 months recommended)
- Continue publishing to both repositories for a transition period
- Monitor usage via download statistics if available
- Communicate the timeline clearly to users

### Phase 4: Remove Old Distribution
After the transition period:

1. **Update `.github/workflows/publish.yml`**:
   - Remove steps related to the `maven` branch (lines 27-32, 75-78, 93-104)
   - Remove the `Checkout maven branch`, `Prepare local maven repository`, `Sync artifacts to maven branch`, and `Commit and push artifacts` steps

2. **Archive or delete the `maven` branch**:
   ```bash
   # Option 1: Archive (keep history but prevent updates)
   git push origin :maven
   
   # Option 2: Add deprecation notice then archive
   # Create a README in maven branch explaining it's deprecated
   ```

3. **Update build.gradle.kts**:
   - Remove the local maven repository publication (lines 111-116 in build.gradle.kts)
   - This is currently used only for the old distribution method

4. **Clean up unused configuration**:
   - Review if any other configuration is specific to the old distribution method

## Migration Path for Users

Users currently using the old method should update their `build.gradle.kts`:

**Old (Deprecated):**
```kotlin
repositories {
    maven { url = uri("https://raw.githubusercontent.com/MirrgieRiana/xarpeg-kotlin-peg-parser/maven/maven") }
}
dependencies {
    implementation("io.github.mirrgieriana.xarpite:xarpeg-kotlin-peg-parser:3.0.0")
}
```

**New (Recommended):**
```kotlin
repositories {
    mavenCentral()
}
dependencies {
    implementation("io.github.mirrgieriana:xarpeg-kotlinMultiplatform:4.0.3")
}
```

## Risks and Mitigation

**Risk**: Breaking builds for users who haven't updated
**Mitigation**: 
- Clear communication with sufficient notice period
- Keep old repository read-only for extended period
- Provide clear migration documentation

**Risk**: Loss of historical artifacts
**Mitigation**:
- Archive the `maven` branch rather than deleting it
- Old versions remain available on Maven Central

## Recommendation

**Immediate**: Announce deprecation with a 6-month transition period

**After 6 months**: Remove the `maven` branch publishing from the workflow

**Timeline**:
- Month 0 (Now): Documentation updated, deprecation announced
- Month 1-5: Monitoring and user support for migration
- Month 6: Remove old distribution method from workflow
- Month 7+: Monitor for any issues, provide support as needed

This approach balances the need to simplify maintenance with user considerations.
