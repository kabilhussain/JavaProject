# Planted Bug Catalogue (Java)

This Maven project is **intentionally broken**. Use it to check what SonarQube and other Java analyzers catch.
Each bug is tagged in the source with a comment like `// BUG S4:` (or `<!-- BUG D1: -->` in `pom.xml`).

| Prefix | File | Category | Count |
|---|---|---|---|
| S | `src/main/java/.../SecurityIssues.java` | Security vulnerabilities / hotspots | 30 |
| L | `src/main/java/.../LogicBugs.java` | Reliability / runtime bugs | 36 |
| C | `src/main/java/.../CodeSmells.java` | Maintainability / code smells | 27 |
| I | `src/main/java/.../Inventory.java` | Subtle business-logic bugs | 11 |
| D | `pom.xml` | Vulnerable dependencies (SCA) | 6 |

Highlights:
- **S**: hard-coded password / AWS key / IP, SQL / OS command / XPath injection, path traversal, MD5 / SHA-1, `java.util.Random` tokens, seeded `SecureRandom`, DES/ECB, static IV, XXE, `ObjectInputStream` deserialization, trust-all `TrustManager` and `HostnameVerifier`, TLSv1, log injection (Log4Shell), reflected XSS, open redirect, insecure cookie, ReDoS, `ScriptEngine.eval`, unsafe SnakeYAML, Jackson default typing, Text4Shell, world-writable files, `createTempFile`, stack-trace leak.
- **L**: `==` on strings / boxed Integers, integer division, null deref, unrelated `equals`, resource leak, ignored `trim()` / `delete()`, off-by-one, empty catch, catching NPE, `new BigDecimal(double)`, float equality, self-assignment, switch fall-through, `Thread.run()`, sync on `Boolean`, `wait()` outside loop, double-checked locking, int overflow, `Math.abs(nextInt())`, subtraction `compareTo`, CME, `Optional.get()`, `return` in `finally`, identical branches, format-arg mismatch, infinite recursion / loop, odd check, `List<String>.contains(Integer)`, swallowed `InterruptedException`, dead store, static `SimpleDateFormat`.
- **C**: wildcard / unused imports, public mutable static, naming, too many params, deep nesting, triplicated code, magic numbers, TODO/FIXME, commented-out code, long line, `System.out` / `printStackTrace`, catch `Exception`, raw types, `== true`, string concat in loop, deprecated `new Integer`, empty method, unused private method / field / local / param, too many returns, missing switch default, collapsible `if`.
- **I**: broken `equals`/`hashCode`, `compareTo` inconsistent with `equals`, overwrite instead of accumulate, negative stock, percent vs fraction, NaN average, wrong boundary, exposed internal list, truncating integer percent, price², mutable map key.
- **D**: log4j-core 2.14.1, jackson-databind 2.9.8, snakeyaml 1.26, commons-text 1.9, commons-collections 3.2.1, h2 1.4.197.

24 JUnit 5 tests; 18 fail on purpose (they expose L/I bugs), so Sonar shows test failures and partial coverage.

## Tools
| Tool | What it checks | Run via |
|---|---|---|
| SonarQube (Sonar way comprehensive profile) | bugs, vulns, smells, duplication, coverage, tests; also imports SpotBugs/PMD/Checkstyle | `./run_sonar.sh` |
| SpotBugs + FindSecBugs | bytecode bug patterns + security | `./run_analysis.sh` |
| PMD | best practices, error-prone, design, multithreading, performance | `./run_analysis.sh` |
| CPD | copy-paste detection | `./run_analysis.sh` |
| Checkstyle (`config/checkstyle.xml`) | naming, complexity, suspicious constructs | `./run_analysis.sh` |
| Error Prone | compile-time bug patterns (javac plugin, `-Perrorprone`) | `./run_analysis.sh` |
| JUnit 5 + JaCoCo | failing tests + coverage | `./run_analysis.sh` |
| Trivy (Docker) | known CVEs in `pom.xml` dependencies | `./run_analysis.sh` |

## Usage
Requires JDK 17, Maven, Docker, Python 3.
```bash
./run_analysis.sh          # local tools -> reports/*  (~2-3 min)
./run_sonar.sh             # SonarQube scan (reuses the running `sonarqube` container), imports the reports above
python3 generate_report.py # -> reports/REPORT.md + reports/report.html (detection matrix)
```
Run `./run_analysis.sh` **first**: the Sonar scan needs the compiled classes and the coverage / test / linter reports in `target/`.
Dashboard: http://localhost:9000/dashboard?id=buggy-java-app (admin password from `$SONAR_ADMIN_PASS`, default `Admin123!Admin`).
Set `SONAR_PROFILE="Sonar way core"` to scan with the server's slimmer default profile instead.
