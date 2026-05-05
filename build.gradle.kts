
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The java17Dependencies configuration in subprojects requires rfgDeobfuscatorTransformed=true when
// resolving variants. External JARs acquire this attribute through the RFG deobfuscator transform,
// but local project dependencies (devOnlyNonPublishable(project(":"))) bypass that transform.
// Declare the attribute on runtimeElements variants so Gradle can disambiguate them.
afterEvaluate {
    val rfgDeobfAttr = org.gradle.api.attributes.Attribute.of("rfgDeobfuscatorTransformed", Boolean::class.javaObjectType)
    configurations["runtimeElements"].attributes {
        attribute(rfgDeobfAttr, true)
    }
}
