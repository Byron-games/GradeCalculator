package model

/**
 * Aggregate report produced after processing an entire class.
 *
 * @property results     Ordered list of individual [GradeResult] records.
 * @property classAvgGpa The mean GPA across all students.
 * @property highestMark The single highest raw mark in the class.
 * @property lowestMark  The single lowest raw mark in the class.
 * @property passCount   Number of students who achieved grade D or higher.
 * @property failCount   Number of students who received grade F.
 */
data class ProcessingReport(
    val results: List<GradeResult>,
    val classAvgGpa: Double,
    val highestMark: Double,
    val lowestMark: Double,
    val passCount: Int,
    val failCount: Int
) {
    val totalStudents: Int get() = results.size

    companion object {
        /**
         * Factory function – builds a [ProcessingReport] from a list of [GradeResult]s
         * using lambda-based aggregation.
         */
        fun from(results: List<GradeResult>): ProcessingReport {
            require(results.isNotEmpty()) { "Cannot build a report from an empty result list." }
            return ProcessingReport(
                results      = results,
                classAvgGpa  = results.map { it.gpa }.average(),
                highestMark  = results.maxOf { it.marks },
                lowestMark   = results.minOf { it.marks },
                passCount    = results.count { it.gradeLevel != GradeLevel.F },
                failCount    = results.count { it.gradeLevel == GradeLevel.F }
            )
        }
    }
}
