package service

import model.GradeResult
import model.ProcessingReport
import model.Student

/**
 * Core grading API.
 *
 * All business logic for turning raw marks into grades lives behind this interface,
 * making it easy to swap grading schemes without touching the UI or export layers.
 */
interface GradeCalculatorService {

    /**
     * Calculates the grade for a single [Student].
     *
     * @param student The student whose marks should be graded.
     * @return A [GradeResult] containing the original student plus computed grade data.
     */
    fun calculateGrade(student: Student): GradeResult

    /**
     * Processes an entire list of students and returns a full class [ProcessingReport].
     *
     * @param students Non-empty list of students to process.
     * @return A [ProcessingReport] with individual results and class-level statistics.
     */
    fun processClass(students: List<Student>): ProcessingReport
}
