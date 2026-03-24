package model

/**
 * Represents a student record as read from the input Excel file.
 *
 * @property name  The student's full name.
 * @property marks The raw numeric score (0–100).
 */
data class Student(
    val name: String,
    val marks: Double
) {
    init {
        require(name.isNotBlank()) { "Student name must not be blank." }
        require(marks in 0.0..100.0) { "Marks must be between 0 and 100, got $marks for '$name'." }
    }
}
