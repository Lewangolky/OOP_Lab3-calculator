package com.oop.calculator.engine

/** Unit in which trigonometric functions take and return angles. */
enum class AngleUnit {
    DEGREES,
    RADIANS;

    fun toRadians(angle: Double): Double =
        if (this == DEGREES) Math.toRadians(angle % 360.0) else angle

    fun fromRadians(angle: Double): Double =
        if (this == DEGREES) Math.toDegrees(angle) else angle

    fun toggled(): AngleUnit = if (this == DEGREES) RADIANS else DEGREES
}
