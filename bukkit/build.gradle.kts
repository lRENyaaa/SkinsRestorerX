dependencies {
    implementation(projects.skinsrestorerApi)
    implementation(projects.skinsrestorerShared)
    implementation(projects.mappings.shared)
    implementation(projects.multiver.spigot)
    implementation(projects.multiver.paper)
    implementation(projects.multiver.v17)
    setOf("1-18", "1-18-2", "1-19", "1-19-3", "1-19-4").forEach {
        implementation(project(":mappings:mc-$it", "remapped"))
    }

    compileOnly("org.spigotmc:spigot-api:1.19.3-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
    }

    implementation("io.papermc:paperlib:1.0.7")
    implementation("org.bstats:bstats-bukkit:3.0.1")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.github.cryptomorin:XSeries:9.3.0")

    compileOnly("com.viaversion:viabackwards-common:4.5.1")
    compileOnly("com.viaversion:viaversion:4.4.1")
    compileOnly("com.mojang:authlib:1.11")
}
