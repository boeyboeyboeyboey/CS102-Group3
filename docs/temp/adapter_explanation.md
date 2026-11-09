# Adapter File Explanation (CS102 Auto-Grader)

## Purpose
Adapter files are instructor-provided test harnesses that:
1. Extend the student's solution class (via inheritance)
2. Contain fixed test cases with inputs and expected outputs
3. Execute the student's method against those test cases
4. Write PASSED/FAILED/ERROR results for the auto-grader to read

## File Locations
- **Student implementation**: `Project/RenameToYourUsername/Q*/Q*.java`
- **Adapter files**: `src/main/resources/autograder/adapters/*Adapter.java`

## Simple Example: Even/Odd Number Check

### Student File (`Q1.java`)
```java
public class Q1 {
    // STUDENT IMPLEMENTS THIS METHOD
    public static String checkEvenOdd(int n) {
        // Your logic: return (n % 2 == 0) ? "even" : "odd";
        return ""; // Starter code
    }
}
```

### Adapter File (`Q1Adapter.java`)
```java
public class Q1Adapter extends Q1 { // Inherits from student's Q1 class
    public static void main(String[] args) {
        int testCase = Integer.parseInt(args[0]); // e.g., 1, 2, 3
        int input;
        String expected;
        
        // INSTRUCTOR'S FIXED TEST CASES
        if (testCase == 1) { input = 4;  expected = "even"; }
        else if (testCase == 2) { input = 7; expected = "odd"; }
        else if (testCase == 3) { input = 0; expected = "even"; }
        else { throw new IllegalArgumentException("Invalid case"); }

        try {
            // CALLS STUDENT'S METHOD (via inheritance!)
            String result = checkEvenOdd(input);  
            
            // COMPARE OUTPUT
            String status = expected.equals(result) ? "PASSED" : "FAILED";
            writeResult(args[1], status); // Write to result file
        } catch (Exception e) {
            writeResult(args[1], "ERROR"); // Handle exceptions
        }
    }
    
    private static void writeResult(String path, String status) { /* file I/O */ }
}
```

## Execution Flow
```
Student writes Q1.java
       ↓ (defines checkEvenOdd())
Adapter extends Q1.java
       ↓ (inherits student's method)
Grader runs: java Q1Adapter <case_num> <result_file>
       ↓
Adapter sets test input (e.g., 4)
       ↓
Adapter calls student's checkEvenOdd(4)
       ↓
Student's method returns "even" or "odd"
       ↓
Adapter compares to expected "even"
       ↓
Adapter writes PASSED/FAILED/ERROR to result file
       ↓
Grader reads result and computes score
```

## Key Points
- **Adapter ≠ Solution**: Only contains test cases; logic comes from student's file
- **Inheritance Bridge**: `extends Q1` gives adapter access to student's method
- **Why it catches hard-coding**: If student hard-codes only the 3 fixed inputs, William's Feature #4 (dynamic test generation) will create new inputs (e.g., 10, 15, -3) that run through the same adapter, exposing the incomplete solution
- **Instructor Control**: Adapters define what constitutes a correct answer; students cannot modify them

## For Feature #4 (Dynamic Parameterized Test Ingestion)
Your goal:
1. Generate novel/random test inputs (e.g., random integers)
2. Run them through the **existing adapter** (executes student's code)
3. Capture adapter's output as validated expected result
4. Create test cases: (your generated input, adapter's confirmed output)
This uses the instructor's adapter as the "answer key" while varying inputs to detect hard-coded solutions.