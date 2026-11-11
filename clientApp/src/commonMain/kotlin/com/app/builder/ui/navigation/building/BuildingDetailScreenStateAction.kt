package com.app.builder.ui.navigation.building

import com.app.builder.domain.Building

/** Actions supported by the Building Detail Screen. */
sealed interface BuildingDetailScreenAction {
    /**
     * Updates the building's name.
     *
     * @param name The new name value.
     */
    data class ChangeName(val name: String): BuildingDetailScreenAction
    /**
     * Updates the building's description.
     *
     * @param description The new description value.
     */
    data class ChangeDescription(val description: String): BuildingDetailScreenAction
    /**
     * Updates the building's address.
     *
     * @param address The new address value.
     */
    data class ChangeAddress(val address: String): BuildingDetailScreenAction
    /**
     * Updates the typed latitude.
     *
     * @param latitude The new latitude text.
     */
    data class ChangeLatitude(val latitude: String): BuildingDetailScreenAction
    /**
     * Updates the typed longitude.
     *
     * @param longitude The new longitude text.
     */
    data class ChangeLongitude(val longitude: String): BuildingDetailScreenAction
    /**
     * Fills both coordinates, such as with the device location.
     *
     * @param latitude The new latitude in degrees.
     * @param longitude The new longitude in degrees.
     */
    data class ChangeCoordinates(val latitude: Double, val longitude: Double): BuildingDetailScreenAction
    /** Start editing the current building. */
    data object Edit: BuildingDetailScreenAction
    /** Save the edited building and stop editing it. */
    data object Save: BuildingDetailScreenAction
    /** Discard the edits, then reload the stored building and stop editing it, or leave the screen when the building is new. */
    data object Cancel: BuildingDetailScreenAction
    /** Delete the current building. */
    data object Delete: BuildingDetailScreenAction
    /** Restore the current deleted building. */
    data object Restore: BuildingDetailScreenAction
}

/**
 * State of the Building Detail Screen.
 *
 * @property building Building being viewed or edited, or `null` while it's still loading.
 * @property editing Whether the building is being edited, which stops it from following the stored building.
 * @property new Whether the building is new and not stored yet.
 * @property latitude The latitude as typed, empty when the building has no coordinates.
 * @property longitude The longitude as typed, empty when the building has no coordinates.
 */
data class BuildingDetailScreenState(
    val building: Building? = null,
    val editing: Boolean = false,
    val new: Boolean = false,
    val latitude: String = "",
    val longitude: String = "",
) {
    /** Whether [latitude] is not a number within ±90, or is missing while [longitude] is set. */
    val latitudeError: Boolean
        get() = !latitude.isCoordinate(limit = 90.0) || (latitude.isBlank() && longitude.isNotBlank())

    /** Whether [longitude] is not a number within ±180, or is missing while [latitude] is set. */
    val longitudeError: Boolean
        get() = !longitude.isCoordinate(limit = 180.0) || (longitude.isBlank() && latitude.isNotBlank())

    /**
     * Shows [building], with its coordinates as text.
     *
     * @param building The building to show.
     * @return This state showing [building].
     */
    fun showing(building: Building?): BuildingDetailScreenState = copy(
        building = building,
        latitude = building?.latitude?.toString().orEmpty(),
        longitude = building?.longitude?.toString().orEmpty()
    )
}

/**
 * Checks whether this text is empty or a number within ±[limit].
 *
 * @param limit The largest absolute value allowed.
 * @return `true` if the text is blank or a valid coordinate.
 */
private fun String.isCoordinate(limit: Double): Boolean = isBlank() || toDoubleOrNull()?.let { it in -limit..limit } == true
