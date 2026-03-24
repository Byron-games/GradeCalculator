package model

/**
 * Represents all possible letter grades with their associated GPA points
 * and the mark range that produces them.
 */
enum class GradeLevel(
    val letter: String,
    val gpaPoints: Double,
    val minMark: Double,
    val maxMark: Double,
    val description: String
) {
    A_PLUS  ("A+", 4.0, 95.0, 100.0, "Exceptional"),
    A       ("A",  4.0, 90.0,  94.9, "Excellent"),
    A_MINUS ("A-", 3.7, 85.0,  89.9, "Very Good"),
    B_PLUS  ("B+", 3.3, 80.0,  84.9, "Good"),
    B       ("B",  3.0, 75.0,  79.9, "Above Average"),
    B_MINUS ("B-", 2.7, 70.0,  74.9, "Average"),
    C_PLUS  ("C+", 2.3, 65.0,  69.9, "Satisfactory"),
    C       ("C",  2.0, 60.0,  64.9, "Pass"),
    C_MINUS ("C-", 1.7, 55.0,  59.9, "Marginal Pass"),
    D_PLUS  ("D+", 1.3, 50.0,  54.9, "Below Average"),
    D       ("D",  1.0, 45.0,  49.9, "Poor"),
    F       ("F",  0.0,  0.0,  44.9, "Fail");

    companion object {
        /** Returns the [GradeLevel] whose range contains the given [mark]. */
        fun fromMark(mark: Double): GradeLevel =
            entries.first { grade -> mark >= grade.minMark && mark <= grade.maxMark }
    }
}
