package impl

import model.GradeLevel
import model.GradeResult
import model.ProcessingReport
import model.Student
import service.GradeCalculatorService

/**
 * Default implementation of [GradeCalculatorService].
 *
 * Grading scale:
 * | Range      | Grade | GPA |
 * |------------|-------|-----|
 * | 95 – 100   |  A+   | 4.0 |
 * | 90 – 94.9  |  A    | 4.0 |
 * | 85 – 89.9  |  A-   | 3.7 |
 * | 80 – 84.9  |  B+   | 3.3 |
 * | 75 – 79.9  |  B    | 3.0 |
 * | 70 – 74.9  |  B-   | 2.7 |
 * | 65 – 69.9  |  C+   | 2.3 |
 * | 60 – 64.9  |  C    | 2.0 |
 * | 55 – 59.9  |  C-   | 1.7 |
 * | 50 – 54.9  |  D+   | 1.3 |
 * | 45 – 49.9  |  D    | 1.0 |
 * |  0 – 44.9  |  F    | 0.0 |
 */
class GradeCalculatorServiceImpl : GradeCalculatorService {

    override fun calculateGrade(student: Student): GradeResult =
        GradeResult(
            student    = student,
            gradeLevel = GradeLevel.fromMark(student.marks)
        )

    override fun processClass(students: List<Student>): ProcessingReport {
        require(students.isNotEmpty()) { "Student list must not be empty." }
        val results = students.map { calculateGrade(it) }
        return ProcessingReport.from(results)
    }
}
