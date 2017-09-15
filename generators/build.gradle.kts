
apply { plugin("kotlin") }

dependencies {
    compile(protobufFull())
    compile(project(":core"))
    compile(project(":idea"))
    compile(project(":j2k"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:backend"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":idea:idea-test-framework"))
    compile(projectDist(":kotlin-test:kotlin-test-jvm"))
    compile(projectTests(":kotlin-build-common"))
    compile(projectTests(":compiler"))
    compile(projectTests(":compiler:tests-java8"))
    compile(projectTests(":compiler:container"))
    compile(projectTests(":compiler:incremental-compilation-impl"))
    compile(projectTests(":idea"))
    compile(projectTests(":idea:idea-gradle"))
    compile(projectTests(":idea:idea-maven"))
    compile(projectTests(":j2k"))
    compile(projectTests(":idea:idea-android"))
    compile(projectTests(":jps-plugin"))
    compile(projectTests(":plugins:plugins-tests"))
    compile(projectTests(":plugins:android-extensions-ide"))
    compile(projectTests(":kotlin-annotation-processing"))
    compile(projectTests(":plugins:uast-kotlin"))
    compile(projectTests(":js:js.tests"))
    //compile(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":idea:idea-test-framework")) { isTransitive = false }
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(ideaSdkDeps("openapi", "idea"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("idea-junit", "resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("IntelliLang", plugin = "IntelliLang"))
    testRuntime(ideaPluginDeps("jcommander", "testng", "testng-plugin", "resources_en", plugin = "testng"))
    testRuntime(ideaPluginDeps("copyright", plugin = "copyright"))
    testRuntime(ideaPluginDeps("properties", /*"resources_en",*/ plugin = "properties"))
    testRuntime(ideaPluginDeps("java-i18n", plugin = "java-i18n"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "Groovy"))
    testRuntime(ideaPluginDeps("coverage", "jacocoant", plugin = "coverage"))
    testRuntime(ideaPluginDeps("java-decompiler", plugin = "java-decompiler"))
    //testRuntime(ideaPluginDeps("*.jar", plugin = "maven"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

fun generator(fqName: String) = task<JavaExec> {
    classpath = the<JavaPluginConvention>().sourceSets["test"].runtimeClasspath
    main = fqName
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt")

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt")
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare")

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
