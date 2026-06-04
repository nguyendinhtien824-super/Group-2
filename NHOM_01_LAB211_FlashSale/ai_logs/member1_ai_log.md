# AI Log & Reflection - Member 1

**Full Name:** Do Bui Quang Hung  
**Student ID:** QE190032  
**Assigned Tasks:** Designing the abstract Entity parent class (`BaseEntity`), the generic `CsvRepository<T>` class, building the mock data generator (`DataGeneratorService`), configuring and testing the concurrent order placement flow without locking (`NO_LOCK` flow) to demonstrate negative stock due to race conditions. Responsible for drawing the Data Generation Flowchart & overall UML Class Diagram for the project.

---

## 1. Raw Conversations

### Scenario 1: Designing Abstract Entity Structure and Generic CSV Repository
- **Prompt:**
  > "I need to design a CSV-based data storage system for a Java OOP Flash Sale simulation application. Please analyze the data tables structure and suggest how to organize the Entity classes inheriting from a common parent class to minimize code duplication, and write a generic CsvRepository<T> class using Reflection to serialize/parse files dynamically."
- **AI Output:**
  > AI suggested creating an abstract `BaseEntity` containing common fields (`id`, `createdAt`, `updatedAt`) and defining two abstract methods: `toCsvLine()` and `fromCsvLine(String csv)`. In `CsvRepository<T>`, AI proposed using Java Reflection (`Field[] fields = entity.getClass().getDeclaredFields()`) to iterate over subclass attributes dynamically to serialize/parse CSV records automatically.
- **Detected Error:**
  > 1. Compile Error: The static `fromCsvLine` method cannot be declared as `abstract` in the parent `BaseEntity` class because static methods cannot be overridden in Java.
  > 2. Performance Degradation: Using reflection at runtime for each line of data when handling large datasets (>10,000 lines of products and customers) creates a severe reflection overhead, significantly slowing down file I/O operations.
- **Corrective Action:**
  > 1. Removed the static `fromCsvLine` method from the abstract `BaseEntity` class, keeping only instance methods like `getId()` and `toCsvLine()`.
  > 2. Declared a protected abstract `T parseLine(String line)` method in `CsvRepository<T>`. Each concrete repository (e.g., `ProductRepository`, `FlashEventRepository`) inherits and implements its own optimized manual parsing logic.
  > 3. Called `entity.toCsvLine()` directly instead of using reflection in `CsvRepository` to optimize CSV write speeds.

### Scenario 2: File Corruption during Concurrent CSV Writing (Multi-threaded Write)
- **Prompt:**
  > "I ran a simulation with 100 concurrent threads writing orders to a CSV file using standard Java BufferedWriter, and the file got corrupted (overwritten lines, interleaved text, missing lines). Please explain why and how to fix it."
- **AI Output:**
  > AI explained that standard Java file I/O classes are not thread-safe when accessing the same physical resource concurrently, and proposed adding the `synchronized` keyword to the entire write method at the Repository or Service level.
- **Detected Error:**
  > Method-level `synchronized` lock serializes all file access, making read operations (queries) wait sequentially behind write operations, severely blocking throughput. Also, JVM-level synchronization cannot prevent conflicts if external processes write to the same file.
- **Corrective Action:**
  > 1. Implemented `ReentrantReadWriteLock` in the parent `CsvRepository`.
  > 2. Wrapped the `findAll()` query method with the read lock (`rwLock.readLock().lock()`) so hundreds of threads can read files in parallel at high speed.
  > 3. Wrapped writing and updating methods (like `save()`, `update()`, `deleteById()`) with the write lock (`rwLock.writeLock().lock()`) to guarantee mutual exclusion during physical I/O writes, keeping the file safe from corruption.

---

## 2. AI Reflection (Personal Assessment - ~650 words)

### a. AI Support Quality Assessment
During the LAB211 project, the AI assistant proved highly valuable in the initial development phases. It excelled at generating boilerplate code, setting up basic MVC class structures, and writing the initial mock data generation framework (`DataGeneratorService`) to synthesize thousands of lines of data. This significantly cut down raw coding time and allowed me to focus on core technical requirements.

However, for complex business rules and concurrency logic, AI lacked a holistic system perspective. It proposed invalid Java syntax (like static abstract methods) and hallucinated non-existent JDK libraries (like `java.nio.file.csv`). It was prone to oversimplifying the scope of the project, writing code that works in a simple single-threaded console but immediately breaks when subjected to parallel stress. Critical engineering knowledge is necessary to filter out these errors and direct the AI effectively.

### b. Concurrency Constraints in AI
Multi-threaded file I/O is notoriously complex, and this is where AI struggled most. It suggested locks that were either too restrictive (causing extreme performance bottlenecks by locking entire methods) or too weak (causing file corruption or negative stock due to non-atomic Check-then-Act pattern bugs). 

AI has no awareness of real-world hardware I/O latency or OS-level file locking issues. File I/O operations are orders of magnitude slower than in-memory operations, making the race window exceptionally large. If we had relied solely on raw AI output without writing a simulator and measuring metrics, the database files would have corrupted under concurrent checkout stress. We had to guide the AI to implement more advanced locking mechanisms like `ReentrantReadWriteLock` to balance safety and performance.

### c. Lessons Learned
1. **Prompt Engineering:** I learned to move away from vague prompts. Instead, I split complex issues into specific technical instructions: detailing data structures, relational constraints, and prescribing precise synchronization tools (like using `ReentrantReadWriteLock` instead of method-synchronized blocks).
2. **Verification:** AI-generated code is only a draft. Writing JUnit tests and running a stress-test concurrency simulator using `CountDownLatch` is essential to verify throughput (TPS) and thread-safety under load. Benchmarking numbers are the ultimate validation of code correctness.
3. **Reliance:** AI speeds up coding, but relying on it blindly blocks deep learning. Understanding OOP, MVC, and thread execution is mandatory to direct the AI and defend the project successfully. Developers must grasp the core architectural concepts to lead the development process rather than being led by generated code.
