# SIMATS ENGINEERING — PROGRAMMING IN JAVA (CSA09)
## Assignment 5 – CO5: Data Persistence Engineering – Files and Databases

* **Course:** Programming in Java (CSA09)
* **Course Outcome:** CO5 – Java I/O (byte, character, buffered streams, `File`, serialization) & JDBC (`Statement`, `PreparedStatement`, `CallableStatement`, transactions, batch processing, metadata)
* **Student Name:** `Surya Narayanan Muthu Kumar`
* **Register Number:** `192372007`
* **GitHub Repository:** `https://github.com/<your-username>/CSA09-192372007-Assignment5`
* **Java Version:** JDK 17+[cite: 10]

---

## Table of Contents
* **Part A – Persistence Design & Pseudocode (25 Marks)**
  * [Task A1: Design a File Format](#task-a1-design-a-file-format)
  * [Task A2: Don't Read What You Don't Need](#task-a2-dont-read-what-you-dont-need)
  * [Task A3: Discover the Data-Access Design Pattern](#task-a3-discover-the-data-access-design-pattern)
  * [Task A4: What Should Be Serialized?](#task-a4-what-should-be-serialized)
  * [Task A5: Design for Correctness and Safety](#task-a5-design-for-correctness-and-safety)
* **Part B – Implementation & Experimental Investigation (25 Marks)**
  * [Task B1: Implement and Instrument the File Store](#task-b1-implement-and-instrument-the-file-store)
  * [Task B2: Engineer an Adversarial Input](#task-b2-engineer-an-adversarial-input)
  * [Task B3: Statement vs PreparedStatement vs Batch](#task-b3-statement-vs-preparedstatement-vs-batch)
  * [Task B4: Final Engineering Decision](#task-b4-final-engineering-decision)

---

# PART A — PERSISTENCE DESIGN & PSEUDOCODE (25 Marks)

> **Note:** Per submission instructions, Part A provides record layouts, algorithmic pseudocode, diagrams, and design reasoning[cite: 18].

---

## Task A1: Design a File Format

### A1.1 – Record Representation

#### (a) Delimited Text File (`passes.txt`)
* **Field Order:** `passId | regNo | studentName | routeNo | boardingStop | validityDate | amountPaid`
* **Delimiter:** Pipe (`|`) or Tab (`\t`)—preferred over comma (`,`) because names or boarding stops rarely contain `|`.
* **Handling Delimiters Inside Names:**
  1. **Escaping:** Replace any literal `|` inside `studentName` or `boardingStop` with `\|` (and `\` with `\\`) on write, and unescape on read.
  2. **RFC-4180 Quoting (if CSV `,` is used):** Enclose fields containing commas in double quotes (`"Kumar, Arun"`) and double any internal quotes (`""`).
* **Example Line:**
  `P1001|192311001|Arun Kumar|12|Tambaram West|2026-12-31|4500.00`

#### (b) Binary File (`passes.bin` via `DataOutputStream`)
Each record is written sequentially in fixed binary field order using `DataOutputStream` primitive/UTF methods:

| Field | Java Type | `DataOutputStream` Method | Size / Encoding |
| :--- | :--- | :--- | :--- |
| `passId` | `String` | `writeUTF(passId)` | 2-byte length prefix + modified UTF-8 bytes |
| `regNo` | `String` | `writeUTF(regNo)` | 2-byte length prefix + modified UTF-8 bytes |
| `studentName` | `String` | `writeUTF(studentName)` | 2-byte length prefix + modified UTF-8 bytes |
| `routeNo` | `int` | `writeInt(routeNo)` | 4 bytes (big-endian 32-bit integer) |
| `boardingStop` | `String` | `writeUTF(boardingStop)` | 2-byte length prefix + modified UTF-8 bytes |
| `validityDate` | `String` (or `long` epoch) | `writeUTF(validityDate)` | 2-byte prefix + 10 bytes (`YYYY-MM-DD`) |
| `amountPaid` | `double` | `writeDouble(amountPaid)` | 8 bytes (IEEE 754 floating-point) |

* **Handling Delimiters in Binary Format:** `writeUTF()` prefixes each string with its exact 2-byte byte-length, so any character (`|`, `,`, `\n`) inside `studentName` is treated strictly as raw character data without ambiguity.

---

### A1.2 – Text-File Operations (Pseudocode & Stream Justification)

**Stream Choices & Justification:**
* **`BufferedWriter` wrapping `FileWriter(..., true)`** (or `Files.newBufferedWriter(..., CREATE, APPEND)`) for adding passes: `FileWriter` handles character-to-byte encoding, while `BufferedWriter` batches characters in an 8 KB memory buffer and provides `newLine()` for platform-independent line endings.
* **`BufferedReader` wrapping `FileReader`** for searching and listing: Reads large 8 KB chunks from disk into memory and exposes `readLine()`, avoiding per-character OS system calls. (`FileInputStream`/`FileOutputStream` are raw byte streams and are unsuitable for character-encoded lines without explicit `InputStreamReader`/`OutputStreamWriter` wrappers.)

#### (i) Add a New Pass
```text
ALGORITHM AddBusPass(filePath, passRecord)
INPUT:  filePath (String), passRecord (BusPass object)
OUTPUT: Appends one formatted pass line to filePath

BEGIN
    OPEN writer AS BufferedWriter(FileWriter(filePath, APPEND = TRUE))
    TRY
        safeName <- REPLACE(passRecord.studentName, "|", "\|")
        safeStop <- REPLACE(passRecord.boardingStop, "|", "\|")
        line <- passRecord.passId + "|" + passRecord.regNo + "|" + safeName + "|"
                + passRecord.routeNo + "|" + safeStop + "|"
                + passRecord.validityDate + "|" + passRecord.amountPaid
        writer.write(line)
        writer.newLine()
        writer.flush()
    FINALLY
        writer.close()
    END TRY
END
