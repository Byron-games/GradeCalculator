import impl.GradeCalculatorServiceImpl
import model.GradeLevel
import model.Student
import kotlin.test.*

class GradeCalculatorServiceTest {

    private val service = GradeCalculatorServiceImpl()

    // ── calculateGrade ────────────────────────────────────────────────────────

    @Test fun `100 marks should give A+ grade`() {
        val result = service.calculateGrade(Student("Alice", 100.0))
        assertEquals(GradeLevel.A_PLUS, result.gradeLevel)
        assertEquals(4.0, result.gpa)
    }

    @Test fun `90 marks should give A grade`() {
        val result = service.calculateGrade(Student("Bob", 90.0))
        assertEquals(GradeLevel.A, result.gradeLevel)
    }

    @Test fun `75 marks should give B grade`() {
        val result = service.calculateGrade(Student("Carol", 75.0))
        assertEquals(GradeLevel.B, result.gradeLevel)
        assertEquals(3.0, result.gpa)
    }

    @Test fun `60 marks should give C grade`() {
        val result = service.calculateGrade(Student("Dave", 60.0))
        assertEquals(GradeLevel.C, result.gradeLevel)
    }

    @Test fun `44 marks should give F grade`() {
        val result = service.calculateGrade(Student("Eve", 44.0))
        assertEquals(GradeLevel.F, result.gradeLevel)
        assertEquals(0.0, result.gpa)
    }

    @Test fun `0 marks should give F grade`() {
        val result = service.calculateGrade(Student("Frank", 0.0))
        assertEquals(GradeLevel.F, result.gradeLevel)
    }

    // ── Boundary checks ───────────────────────────────────────────────────────

    @Test fun `44_9 is still F`() {
        assertEquals(GradeLevel.F, service.calculateGrade(Student("G", 44.9)).gradeLevel)
    }

    @Test fun `45_0 is D`() {
        assertEquals(GradeLevel.D, service.calculateGrade(Student("H", 45.0)).gradeLevel)
    }

    @Test fun `95_0 is A_PLUS`() {
        assertEquals(GradeLevel.A_PLUS, service.calculateGrade(Student("I", 95.0)).gradeLevel)
    }

    @Test fun `94_9 is A`() {
        assertEquals(GradeLevel.A, service.calculateGrade(Student("J", 94.9)).gradeLevel)
    }

    // ── processClass ─────────────────────────────────────────────────────────

    @Test fun `processClass produces correct statistics`() {
        val students = listOf(
            Student("Alice", 95.0),
            Student("Bob",   75.0),
            Student("Carol", 40.0)
        )
        val report = service.processClass(students)

        assertEquals(3, report.totalStudents)
        assertEquals(2, report.passCount)
        assertEquals(1, report.failCount)
        assertEquals(95.0, report.highestMark)
        assertEquals(40.0, report.lowestMark)
    }

    @Test fun `processClass throws on empty list`() {
        assertFailsWith<IllegalArgumentException> { service.processClass(emptyList()) }
    }

    // ── Student validation ────────────────────────────────────────────────────

    @Test fun `Student rejects blank name`() {
        assertFailsWith<IllegalArgumentException> { Student("  ", 80.0) }
    }

    @Test fun `Student rejects marks above 100`() {
        assertFailsWith<IllegalArgumentException> { Student("X", 101.0) }
    }

    @Test fun `Student rejects negative marks`() {
        assertFailsWith<IllegalArgumentException> { Student("X", -1.0) }
    }
}
