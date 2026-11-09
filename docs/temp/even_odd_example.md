# Even/Odd Example: How Adapters Work in CS102 Auto-Grader

## Scenario
Student must implement: `public static String checkEvenOdd(int n)` that returns "even" or "odd"

## Current Files in Repository

### 1. Student Implementation File
`Project/RenameToYourUsername/Q1/Q1.java`
```java
/*
 * Name: 
 * Email ID: 
 */

public class Q1 {
    // STUDENT'S METHOD TO IMPLEMENT
    public static String checkEvenOdd(int n) {
        // Your logic goes here
        // Example correct implementation:
        // return (n % 2 == 0) ? "even" : "odd";
        
        // Starter code provided (to make it compile):
        return ""; 
    }

    // Student's personal test main (IGNORED by grader)
    public static void main(String[] args) {
        // You can test here, but auto-grader does NOT use this
        System.out.println(checkEvenOdd(4)); // Just for your testing
    }
}
```

### 2. Instructor Adapter File  
`src/main/resources/autograder/adapters/Q1Adapter.java`
```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Q1Adapter extends Q1 { // ← KEY: Inherits from student's Q1 class
    public static void main(String[] args) {
        // Grader calls: java Q1Adapter <case_number> <result_file_path>
        // Example: java Q1Adapter 1 /tmp/result_12345.txt
        
        Path resultPath = args.length == 2 ? Path.of(args[1]) : null;
        String status = "ERROR";
        
        try {
            // Validate arguments
            if (args.length != 2) {
                throw new IllegalArgumentException("Expected case number and result path");
            }
            
            int testCase = Integer.parseInt(args[0]);
            int inputNumber;
            String expectedResult;
            
            // INSTRUCTOR'S PRE-DEFINED TEST CASES
            switch (testCase) {
                case 1: 
                    inputNumber = 4; 
                    expectedResult = "even"; 
                    break;
                case 2: 
                    inputNumber = 7; 
                    expectedResult = "odd"; 
                    break;
                case 3: 
                    inputNumber = 0; 
                    expectedResult = "even"; 
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported case number: " + testCase);
            }
            
            // ← THIS IS THE MAGIC: CALLS STUDENT'S METHOD
            // Because Q1Adapter extends Q1, it inherits checkEvenOdd()
            // When we call checkEvenOdd(), we're executing THE STUDENT'S CODE
            String studentResult = checkEvenOdd(inputNumber);  
            
            // Compare student's output vs expected output
            status = expectedResult.equals(studentResult) ? "PASSED" : "FAILED";
            
        } catch (Exception exception) {
            // Any exception (including student's crashes) becomes ERROR
            exception.printStackTrace(System.err);
            status = "ERROR";
        }
        
        // Write result to file for grader to read
        if (resultPath != null) {
            try {
                Files.writeString(resultPath, status, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
    }
}
```

## Execution Flow Step-by-Step

### Step 1: Student writes their solution
```java
// In Q1.java
public static String checkEvenOdd(int n) {
    return (n % 2 == 0) ? "even" : "odd"; // CORRECT implementation
}
```

### Step 2: Auto-grader prepares to test
- Compiles: `Q1.java` (student's code) + `Q1Adapter.java` (instructor's test harness)
- Creates fresh directory for this test run
- Copies only the necessary files

### Step 3: Grader executes test case #1
Command: `java Q1Adapter 1 /tmp/result_1.txt`

Inside Q1Adapter.main():
1. `args[0] = "1"` → `testCase = 1`
2. Sets `inputNumber = 4`, `expectedResult = "even"`
3. Calls `checkEvenOdd(4)` 
   - → This executes the STUDENT'S code from Q1.java!
   - → Student's method returns `"even"` (because 4 % 2 == 0)
4. Compares: `"even".equals("even")` → `true`
5. Sets `status = "PASSED"`
6. Writes "PASSED" to `/tmp/result_1.txt`

### Step 4: Grader repeats for all test cases
- Test case 2: input=7 → expects "odd" → student returns "odd" → PASSED
- Test case 3: input=0 → expects "even" → student returns "even" → PASSED

### Step 5: Grader computes final score
- Reads all result files
- Counts PASSED cases (3 out of 3)
- Awards full points for Q1

## What Happens with Incorrect Student Code?

### Case A: Student forgets modulus (always returns "odd")
```java
public static String checkEvenOdd(int n) {
    return "odd"; // WRONG
}
```

Execution:
- Test 1 (input=4): student returns "odd", expected "even" → FAILED
- Test 2 (input=7): student returns "odd", expected "odd" → PASSED  
- Test 3 (input=0): student returns "odd", expected "even" → FAILED
- Score: 1/3 points

### Case B: Student hard-codes only the test cases (CHEATING)
```java
public static String checkEvenOdd(int n) {
    if (n == 4) return "even";
    if (n == 7) return "odd";
    if (n == 0) return "even";
    return "unknown"; // Fails on any other input
}
```

Execution with ORIGINAL adapter:
- Test 1 (4): returns "even" → PASSED
- Test 2 (7): returns "odd" → PASSED  
- Test 3 (0): returns "even" → PASSED
- Score: 3/3 points (FALSE PASS!)

## How Feature #4 Fixes This (Dynamic Test Generation)

William's Dynamic Parameterized Test Ingestion would:

1. **Generate NEW random inputs** (e.g., n=15, n=-2, n=100)
2. **Run them through the SAME adapter** (which still calls STUDENT'S method)
3. **Use adapter output as expected result** (since adapter executes correct logic via student's inherited method)

Example dynamic test:
- Generate input = 15
- Run through Q1Adapter with input 15:
  - Adapter calls student's checkEvenOdd(15)
  - Hard-coded student returns "unknown" 
  - Adapter compares "unknown" to expected result
  - BUT WAIT - how does it know expected result?

### The Key Insight:
For Feature #4 to work, you would need to:
1. Generate random input (e.g., 15)
2. Run that input **through a verified correct solution** to get expected output
3. Use that (input, expected) pair as a test case

In the current architecture, the **adapter IS the verified correct solution** when combined with student code, because:
- Adapter extends student's class
- When adapter calls checkEvenOdd(), it executes STUDENT'S method
- BUT we don't yet know if student's method is correct!

### Correct Approach for Feature #4:
Actually, looking more carefully at the architecture, you would need to either:
1. Have access to the instructor's reference solution (separate from adapters), OR
2. Generate test cases in pairs by varying inputs and having some oracle determine expected output

From examining `docs/ARCHITECTURE.md`:
> "Five explicit Java adapter resources preserve the original inputs, expected values and comparisons."

This suggests the adapters contain BOTH the test inputs AND the expected outputs (as we saw in the code).

For your dynamic test feature, you would likely:
1. Create a new provider (e.g., `DynamicTestCaseProvider`) 
2. That generates randomized test inputs
3. For each input, you would need to determine the expected output
   - Option A: If you have access to instructor's reference implementation
   - Option B: By somehow validating the input/output pair is correct

The adapter files show us exactly what the instructor considers correct test cases - you would be generating similar (input, expected output) pairs but with varied inputs.

## Summary of Current Flow

```
[Student Q1.java] 
       ↓ defines checkEvenOdd()
[Adapter Q1Adapter.java extends Q1]
       ↓ inherits student's method via extends
[Grader: java Q1Adapter <case> <result_file>]
       ↓ Adapter sets fixed test input
       ↓ Adapter calls checkEvenOdd(input) 
       ↓ Executes STUDENT'S CODE
       ↓ Gets student's output
       ↓ Compares to adapter's fixed expected output
       ↓ Writes PASSED/FAILED/ERROR to result file
       ↓ Grader reads result and scores
```

## For Your Feature #4 Work
You'll be working in: `/root/cs102-group3/`

Your dynamic test provider would likely go in:
`src/main/java/autograder/grading/` (possibly as `DynamicTestCaseProvider.java`)

It would need to:
1. Generate varied test inputs (instead of using fixed ones from adapter)
2. For each input, determine expected output (this is the key challenge)
3. Create standard TestCaseDefinition objects that the existing GradingPipeline can use
4. Handle invalid inputs gracefully (produce useful errors, not crashes)

The adapter files show you the format and validation approach to follow.