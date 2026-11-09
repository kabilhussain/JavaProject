#!/usr/bin/env bash
# Runs all local static-analysis tools and copies their output to reports/.
# Tools report findings without failing the build, but we avoid `set -e` so one broken tool
# doesn't stop the rest.
set -u
cd "$(dirname "$0")"
mkdir -p reports
MVN="mvn -B"

echo "==> Error Prone (javac plugin)"
$MVN -Perrorprone clean compile > reports/errorprone.txt 2>&1

echo "==> JUnit tests + JaCoCo coverage"
$MVN verify > reports/maven-test.txt 2>&1
cp target/site/jacoco/jacoco.xml reports/jacoco.xml 2>/dev/null
cat target/surefire-reports/*.txt > reports/junit-summary.txt 2>/dev/null
mkdir -p reports/surefire && cp target/surefire-reports/TEST-*.xml reports/surefire/ 2>/dev/null

echo "==> SpotBugs + FindSecBugs"
$MVN spotbugs:spotbugs > reports/spotbugs-run.txt 2>&1
cp target/spotbugsXml.xml reports/spotbugs.xml 2>/dev/null

echo "==> PMD + CPD (copy-paste detector)"
$MVN pmd:pmd pmd:cpd > reports/pmd-run.txt 2>&1
cp target/pmd.xml reports/pmd.xml 2>/dev/null
cp target/cpd.xml reports/cpd.xml 2>/dev/null

echo "==> Checkstyle"
$MVN checkstyle:checkstyle > reports/checkstyle-run.txt 2>&1
cp target/checkstyle-result.xml reports/checkstyle.xml 2>/dev/null

echo "==> Trivy (vulnerable dependencies in pom.xml)"
docker run --rm -v "$PWD/pom.xml:/src/pom.xml:ro" -v trivy-cache:/root/.cache/trivy \
  aquasec/trivy:0.56.2 fs --quiet --scanners vuln --format json /src > reports/trivy.json 2> reports/trivy-run.txt
docker run --rm -v "$PWD/pom.xml:/src/pom.xml:ro" -v trivy-cache:/root/.cache/trivy \
  aquasec/trivy:0.56.2 fs --quiet --scanners vuln --format table /src > reports/trivy.txt 2>>reports/trivy-run.txt

echo "Done. Reports in ./reports/ (raw tool output also in ./target/)"
