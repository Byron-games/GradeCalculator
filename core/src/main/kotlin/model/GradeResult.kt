package model

/**
 * Immutable result produced for a single student after grade processing.
 *
 * @property student    The original [Student] record.
 * @property gradeLevel The computed [GradeLevel].
 */
data class GradeResult(
    val student: Student,
    val gradeLevel: GradeLevel
) {
    /** Convenience accessor – student name. */
    val name: String get() = student.name

    /** Convenience accessor – raw marks. */
    val marks: Double get() = student.marks

    /** The letter grade string (e.g. "A+"). */
    val grade: String get() = gradeLevel.letter

    /** GPA on a 4.0 scale. */
    val gpa: Double get() = gradeLevel.gpaPoints

    /** Human-readable description of the grade (e.g. "Exceptional"). */
    val description: String get() = gradeLevel.description
}
