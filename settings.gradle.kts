pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name="kstd-examples"

include("common-avros")
include("common-datagen")
include("user-flight-booking-notification-processor")
