package com.example.gradecalculator

data class GradeResult(
    val name: String,
    val marks: Int,
    val grade: String,
    val gpa: Double
)

object GradeCalculator {

    fun calculateGrade(marks: Int): String {
        return when {
            marks in 80..100 -> "A"
            marks in 70..79 -> "B+"
            marks in 60..69 -> "B"
            marks in 55..59 -> "C+"
            marks in 50..54 -> "C"
            marks in 45..49 -> "D+"
            marks in 40..44 -> "D"
            else -> "F"
        }
    }

    fun calculateGPA(marks: Int): Double {
        return when {
            marks in 80..100 -> 4.0
            marks in 70..79 -> 3.5
            marks in 60..69 -> 3.0
            marks in 55..59 -> 2.5
            marks in 50..54 -> 2.0
            marks in 45..49 -> 1.5
            marks in 40..44 -> 1.0
            else -> 0.0
        }
    }

    fun processStudent(name: String, marks: Int): GradeResult {
        return GradeResult(
            name = name,
            marks = marks,
            grade = calculateGrade(marks),
            gpa = calculateGPA(marks)
        )
    }

    fun processAllStudents(students: List<Pair<String, Int>>): List<GradeResult> {
        return students.map { (name, marks) -> processStudent(name, marks) }
    }
}