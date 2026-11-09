#!/usr/bin/env bash
# Ensures SonarQube is running, creates a token, runs the Maven scanner, and dumps results to reports/.
# Run ./run_analysis.sh first so Sonar can import coverage, test results, SpotBugs, PMD and Checkstyle.
# Usage: ./run_sonar.sh   (uses admin/admin on first run, then $SONAR_ADMIN_PASS)
set -euo pipefail
cd "$(dirname "$0")"
HOST=http://localhost:9000
PROJECT=buggy-java-app
ADMIN_PASS=${SONAR_ADMIN_PASS:-Admin123!Admin}

# Reuse an existing `sonarqube` container (e.g. the one from PythonTestProject) if there is one.
if ! curl -s "$HOST/api/system/status" | grep -q '"status":"UP"'; then
  if docker ps -a --format '{{.Names}}' | grep -qx sonarqube; then
    docker start sonarqube >/dev/null
  else
    docker compose up -d sonarqube
  fi
fi
echo "Waiting for SonarQube to be UP..."
until curl -s "$HOST/api/system/status" | grep -q '"status":"UP"'; do sleep 5; done

# First run: change default admin password (SonarQube forces this).
curl -s -u admin:admin -X POST "$HOST/api/users/change_password" \
  -d "login=admin&previousPassword=admin&password=$ADMIN_PASS" >/dev/null || true

TOKEN=$(curl -s -u "admin:$ADMIN_PASS" -X POST "$HOST/api/user_tokens/generate" \
  -d "name=java-scan-$(date +%s)&type=GLOBAL_ANALYSIS_TOKEN" | sed -E 's/.*"token":"([^"]+)".*/\1/')

# Create the project up front and use the broadest built-in Java profile (override with $SONAR_PROFILE).
PROFILE=${SONAR_PROFILE:-Sonar way comprehensive}
curl -s -u "admin:$ADMIN_PASS" -X POST "$HOST/api/projects/create" \
  --data-urlencode "project=$PROJECT" --data-urlencode "name=Buggy Java App" >/dev/null || true
curl -s -u "admin:$ADMIN_PASS" -X POST "$HOST/api/qualityprofiles/add_project" \
  --data-urlencode "language=java" --data-urlencode "qualityProfile=$PROFILE" \
  --data-urlencode "project=$PROJECT" >/dev/null || true

mkdir -p reports
# No `clean`: keep target/ so the scanner finds compiled classes and the other tools' reports.
mvn -B sonar:sonar -Dsonar.host.url="$HOST" -Dsonar.token="$TOKEN" | tee reports/sonar-run.log

echo "Waiting for background analysis task..."
sleep 5
until curl -s -u "admin:$ADMIN_PASS" "$HOST/api/ce/component?component=$PROJECT" \
      | grep -q '"queue":\[\]'; do sleep 3; done

A=(-s -u "admin:$ADMIN_PASS")
curl "${A[@]}" "$HOST/api/issues/search?componentKeys=$PROJECT&ps=500" > reports/sonar-issues.json
curl "${A[@]}" "$HOST/api/hotspots/search?project=$PROJECT&ps=500" > reports/sonar-hotspots.json
curl "${A[@]}" "$HOST/api/measures/component?component=$PROJECT&metricKeys=bugs,vulnerabilities,code_smells,security_hotspots,coverage,line_coverage,tests,test_failures,test_errors,duplicated_lines_density,ncloc,sqale_rating,reliability_rating,security_rating,cognitive_complexity,complexity" > reports/sonar-measures.json
curl "${A[@]}" "$HOST/api/qualitygates/project_status?projectKey=$PROJECT" > reports/sonar-qualitygate.json
echo "Dashboard: $HOST/dashboard?id=$PROJECT  (login admin / $ADMIN_PASS)"
