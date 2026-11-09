"""Build a consolidated report (reports/REPORT.md + reports/report.html) from all tool outputs.

For every planted `// BUG Xn:` tag it checks which tools flagged a line in that bug's region,
giving a detection matrix: which analyzer catches which kind of bug.
Run after ./run_analysis.sh and ./run_sonar.sh.
"""
import html
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).parent
REP = ROOT / "reports"
SRC = ROOT / "src/main/java"
TOOLS = ["SonarQube", "SpotBugs", "PMD", "CPD", "Checkstyle", "ErrorProne", "Trivy"]
BUG_RE = re.compile(r"(?://|<!--)\s*BUG ([A-Z]\d+):\s*(.*?)\s*(?:-->)?$")
# Findings that fire regardless of the planted bug (don't credit them).
NOISE = re.compile(r"DesignForExtension|FinalParameters|MissingJavadoc|CommentRequired|"
                   r"MethodArgumentCouldBeFinal|LocalVariableCouldBeFinal|AtLeastOneConstructor")
CATS = {"S": "Security", "L": "Logic / reliability", "C": "Code smell",
        "I": "Inventory (subtle)", "D": "Vulnerable dependency"}


def rel(path):
    p = str(path).replace("\\", "/")
    for marker in ("src/main/java/", "src/test/java/"):
        i = p.find(marker)
        if i >= 0:
            return p[i + len(marker):]
    i = p.find("com/example/")
    if i >= 0:
        return p[i:]
    return p.rsplit("/", 1)[-1]


def load_text(name):
    f = REP / name
    return f.read_text(errors="replace") if f.exists() else ""


def load_json(name):
    t = load_text(name)
    return json.loads(t) if t.strip() else {}


def load_xml(name):
    f = REP / name
    try:
        return ET.parse(f).getroot() if f.exists() else None
    except ET.ParseError:
        return None


# ---------- planted bugs and their line windows ----------
def statement_end(lines, start):
    """Last line (1-based) of the declaration/statement that begins after `start`."""
    depth, seen_brace = 0, False
    for i in range(start - 1, len(lines)):  # include the tag line (inline tags)
        code = re.sub(r'"([^"\\]|\\.)*"', '""', lines[i].split("//")[0])
        depth += code.count("{") - code.count("}")
        seen_brace |= "{" in code
        if (seen_brace and depth <= 0) or (not seen_brace and code.rstrip().endswith(";")):
            return i + 1
    return len(lines)


def planted_bugs():
    bugs = []
    for f in sorted(SRC.rglob("*.java")):
        lines = f.read_text().splitlines()
        tags = [(n, BUG_RE.search(l)) for n, l in enumerate(lines, 1)]
        tags = [(n, m) for n, m in tags if m]
        for k, (ln, m) in enumerate(tags):
            next_tag = tags[k + 1][0] - 1 if k + 1 < len(tags) else len(lines)
            end = min(next_tag, statement_end(lines, ln))
            bugs.append({"id": m.group(1), "desc": m.group(2), "file": rel(f),
                         "line": ln, "start": ln, "end": end})
    pom = (ROOT / "pom.xml").read_text().splitlines()
    for ln, line in enumerate(pom, 1):
        m = BUG_RE.search(line)
        if m:
            art = next((re.search(r"<artifactId>(.*?)</artifactId>", x).group(1)
                        for x in pom[ln:ln + 4] if "<artifactId>" in x), "?")
            ver = next((re.search(r"<version>(.*?)</version>", x).group(1)
                        for x in pom[ln:ln + 5] if "<version>" in x), "?")
            bugs.append({"id": m.group(1), "desc": m.group(2), "file": "pom.xml", "line": ln,
                         "start": ln, "end": ln + 5, "artifact": art, "version": ver})
    return sorted(bugs, key=lambda b: ("SLCID".index(b["id"][0]), int(b["id"][1:])))


# ---------- findings per tool: list of (file, line, rule, message) ----------
def findings():
    out = defaultdict(list)
    sonar = load_json("sonar-issues.json")
    for i in sonar.get("issues", []):
        if i["rule"].startswith("java:"):  # native Sonar rules only
            sev = i.get("severity") or "-"
            out["SonarQube"].append((rel(i["component"].split(":", 1)[1]), i.get("line", 0),
                                     i["rule"], f'[{i.get("type", "?")}/{sev}] {i["message"]}'))
    for h in load_json("sonar-hotspots.json").get("hotspots", []):
        out["SonarQube"].append((rel(h["component"].split(":", 1)[1]), h.get("line", 0),
                                 h["ruleKey"], "[HOTSPOT] " + h["message"]))

    root = load_xml("spotbugs.xml")
    for bi in (root.iter("BugInstance") if root is not None else []):
        msg = (bi.findtext("ShortMessage") or bi.get("type"))
        # Prefer the bug's own SourceLine, then the method's, then the field's.
        cands = bi.findall("SourceLine") + bi.findall("Method/SourceLine") + bi.findall("Field/SourceLine")
        sl = next((c for c in cands if c.get("start") and c.get("sourcepath")), None)
        if sl is not None:
            out["SpotBugs"].append((rel(sl.get("sourcepath")), int(sl.get("start")), bi.get("type"),
                                    f'[{bi.get("category")}] {msg}'))

    root = load_xml("pmd.xml")
    for fe in (root.iterfind(".//{*}file") if root is not None else []):
        for v in fe.iterfind(".//{*}violation"):
            out["PMD"].append((rel(fe.get("name")), int(v.get("beginline")), v.get("rule"),
                               (v.text or "").strip()))

    root = load_xml("cpd.xml")
    for i, dup in enumerate(root.iterfind(".//{*}duplication") if root is not None else []):
        locs = list(dup.iterfind(".//{*}file"))
        for fe in locs:
            s, e = int(fe.get("line")), int(fe.get("endline") or fe.get("line"))
            for ln in range(s, e + 1):
                out["CPD"].append((rel(fe.get("path")), ln, f"dup#{i + 1}",
                                   f'{dup.get("lines")} lines duplicated {len(locs)}x'))

    root = load_xml("checkstyle.xml")
    for fe in (root.iter("file") if root is not None else []):
        for e in fe.iter("error"):
            out["Checkstyle"].append((rel(fe.get("name")), int(e.get("line", 0)),
                                      e.get("source", "").rsplit(".", 1)[-1], e.get("message")))

    ep = re.compile(r"^\[WARNING\] (\S+\.java):\[(\d+),\d+\] \[(\w+)\] (.*)")
    for line in load_text("errorprone.txt").splitlines():
        m = ep.match(line)
        if m:
            out["ErrorProne"].append((rel(m.group(1)), int(m.group(2)), m.group(3), m.group(4)))
    return out


def trivy_deps():
    deps = defaultdict(lambda: {"version": "", "ids": set(), "sev": Counter()})
    for res in load_json("trivy.json").get("Results", []) or []:
        for v in res.get("Vulnerabilities", []) or []:
            d = deps[v["PkgName"]]
            d["version"] = v.get("InstalledVersion", "")
            if v["VulnerabilityID"] not in d["ids"]:
                d["ids"].add(v["VulnerabilityID"])
                d["sev"][v.get("Severity", "UNKNOWN")] += 1
    return deps


def test_results():
    total = Counter()
    failed = []
    for f in sorted((REP / "surefire").glob("TEST-*.xml")):
        ts = ET.parse(f).getroot()
        for k in ("tests", "failures", "errors", "skipped"):
            total[k] += int(ts.get(k, 0))
        for tc in ts.iter("testcase"):
            bad = tc.find("failure") if tc.find("failure") is not None else tc.find("error")
            if bad is not None:
                msg = (bad.get("message") or bad.get("type") or "").splitlines()[0][:140]
                failed.append((f'{tc.get("classname").rsplit(".", 1)[-1]}.{tc.get("name")}', msg))
    return total, failed


def jacoco_coverage():
    root = load_xml("jacoco.xml")
    if root is None:
        return "n/a"
    for c in root.findall("counter"):
        if c.get("type") == "LINE":
            cov, mis = int(c.get("covered")), int(c.get("missed"))
            return f"{cov * 100 / (cov + mis):.1f}%"
    return "n/a"


def main():
    bugs = planted_bugs()
    found = findings()
    deps = trivy_deps()
    matrix = {}
    for b in bugs:
        row = {t: [] for t in TOOLS}
        if b["file"] == "pom.xml":
            d = deps.get(b["artifact"]) or next((v for k, v in deps.items() if k.endswith(":" + b["artifact"])), None)
            if d and d["ids"]:
                row["Trivy"] = [("pom.xml", b["line"], "CVE", f'{len(d["ids"])} advisories: ' +
                                 ", ".join(sorted(d["ids"])[:5]))]
        else:
            for t in TOOLS:
                hits = [f for f in found.get(t, []) if f[0] == b["file"] and b["start"] <= f[1] <= b["end"]]
                row[t] = [h for h in hits if not NOISE.search(f"{h[2]} {h[3]}")]
        matrix[b["id"]] = row

    per_tool = {t: sum(1 for b in bugs if matrix[b["id"]][t]) for t in TOOLS}
    per_cat_tool = {c: {t: sum(1 for b in bugs if b["id"][0] == c and matrix[b["id"]][t]) for t in TOOLS}
                    for c in CATS}
    cat_totals = Counter(b["id"][0] for b in bugs)
    caught_any = sum(1 for b in bugs if any(matrix[b["id"]].values()))
    missed = [b for b in bugs if not any(matrix[b["id"]].values())]
    pct = caught_any * 100 // max(len(bugs), 1)

    measures = {m["metric"]: m.get("value") for m in
                load_json("sonar-measures.json").get("component", {}).get("measures", [])}
    gate = load_json("sonar-qualitygate.json").get("projectStatus", {}).get("status", "n/a")
    tests, failed = test_results()
    test_summary = (f'{tests["tests"]} run, {tests["failures"]} failed, {tests["errors"]} errors, '
                    f'{tests["skipped"]} skipped')
    coverage = jacoco_coverage()
    sonar_keys = ["bugs", "vulnerabilities", "code_smells", "security_hotspots", "coverage",
                  "duplicated_lines_density", "cognitive_complexity", "complexity", "ncloc",
                  "reliability_rating", "security_rating", "sqale_rating"]
    dep_rows_data = sorted(((k, v["version"], len(v["ids"]), v["sev"]) for k, v in deps.items()),
                           key=lambda r: -r[2])

    # ---------- Markdown ----------
    md = ["# Static Analysis Report: buggy-java-app", ""]
    md += [f"**Planted bugs:** {len(bugs)}  |  **Caught by at least one tool:** {caught_any} ({pct}%)  "
           f"|  **Missed by all:** {len(missed)}  |  **Tests:** {test_summary}  |  "
           f"**Line coverage (JaCoCo):** {coverage}", ""]
    md += ["## SonarQube metrics", "", "| Metric | Value |", "|---|---|"]
    md += [f"| {k} | {measures.get(k, 'n/a')} |" for k in sonar_keys]
    md += [f"| quality gate | {gate} |", "",
           "_Sonar headline counts include issues imported from SpotBugs/FindSecBugs/PMD/Checkstyle; "
           "the matrix below credits only Sonar's own `java:` rules._", ""]
    md += ["## Detection rate per tool (planted bugs caught)", "",
           "| Category | Total | " + " | ".join(TOOLS) + " |", "|---|---|" + "---|" * len(TOOLS)]
    for c, name in CATS.items():
        md.append(f"| {name} | {cat_totals[c]} | " + " | ".join(str(per_cat_tool[c][t]) for t in TOOLS) + " |")
    md.append(f"| **All** | **{len(bugs)}** | " + " | ".join(f"**{per_tool[t]}**" for t in TOOLS) + " |")
    md += ["", "## Bug-by-bug matrix", "", "| Bug | Description | " + " | ".join(TOOLS) + " |",
           "|---|---|" + "---|" * len(TOOLS)]
    for b in bugs:
        md.append(f"| {b['id']} | {b['desc']} | " +
                  " | ".join("✔" if matrix[b["id"]][t] else "" for t in TOOLS) + " |")
    md += ["", "## Missed by every tool", ""]
    md += [f"- **{b['id']}** {b['desc']} (`{b['file']}:{b['line']}`)" for b in missed] or ["- None"]
    md += ["", "## Failing tests", "", f"`{test_summary}`", ""]
    md += [f"- `{n}` — {m}" for n, m in failed]
    md += ["", "## Vulnerable dependencies (Trivy)", "", "| Package | Version | Advisories | Severity |",
           "|---|---|---|---|"]
    md += [f"| {n} | {v} | {c} | {', '.join(f'{k}:{s[k]}' for k in ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') if s[k])} |"
           for n, v, c, s in dep_rows_data]
    md += ["", "## Raw finding counts per tool", ""]
    md += [f"- {t}: {len(found.get(t, []))}" for t in TOOLS if t not in ("Trivy", "CPD")]
    md += [f"- CPD duplicated lines: {len(found.get('CPD', []))}",
           f"- Trivy: {sum(r[2] for r in dep_rows_data)} advisories in {len(dep_rows_data)} packages"]
    (REP / "REPORT.md").write_text("\n".join(md) + "\n")

    # ---------- HTML ----------
    def cell(hits):
        if not hits:
            return '<td class="no">·</td>'
        tip = html.escape("\n".join(f"L{h[1]} {h[2]}: {h[3]}" for h in hits[:6]))
        return f'<td class="yes" title="{tip}">✔</td>'

    rows = "".join(
        f'<tr><td class="id">{b["id"]}</td><td class="desc">{html.escape(b["desc"])}'
        f'<span class="loc">{b["file"]}:{b["line"]}</span></td>'
        + "".join(cell(matrix[b["id"]][t]) for t in TOOLS) + "</tr>" for b in bugs)
    cat_rows = "".join(
        f"<tr><td>{n}</td><td>{cat_totals[c]}</td>" + "".join(
            f'<td style="--p:{per_cat_tool[c][t] / max(cat_totals[c], 1):.2f}" class="pct">{per_cat_tool[c][t]}</td>'
            for t in TOOLS) + "</tr>" for c, n in CATS.items())
    cards = [(k.replace("_", " "), measures.get(k, "n/a")) for k in
             ["bugs", "vulnerabilities", "code_smells", "coverage", "duplicated_lines_density", "cognitive_complexity"]]
    cards += [("tests failed", f'{tests["failures"] + tests["errors"]} / {tests["tests"]}')]
    metric_cards = "".join(f'<div class="m"><span>{html.escape(k)}</span><b>{html.escape(str(v))}</b></div>'
                           for k, v in cards)
    dep_rows = "".join(
        f"<tr><td>{html.escape(n)}</td><td>{html.escape(v)}</td><td>{c}</td>"
        f"<td>{s['CRITICAL']}</td><td>{s['HIGH']}</td><td>{s['MEDIUM']}</td><td>{s['LOW']}</td></tr>"
        for n, v, c, s in dep_rows_data)
    missed_li = "".join(f"<li><b>{b['id']}</b> {html.escape(b['desc'])} <span class='loc'>{b['file']}:{b['line']}</span></li>"
                        for b in missed)
    tests_li = "".join(f"<li><code>{html.escape(n)}</code> <span class='loc'>{html.escape(m)}</span></li>"
                       for n, m in failed)
    head = "".join(f"<th>{t}</th>" for t in TOOLS)
    page = f"""<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>buggy-java-app Analysis</title><style>
:root{{--bg:#f6f7f9;--fg:#1c2230;--mut:#5d6677;--card:#fff;--line:#dfe3ea;--ok:#1f8a4c;--okbg:#e3f4ea;--bad:#b4232a}}
@media (prefers-color-scheme:dark){{:root{{--bg:#12151c;--fg:#e4e8f0;--mut:#9aa3b5;--card:#1a1f29;--line:#2c3341;--ok:#5fd394;--okbg:#173527;--bad:#ff7b7f;color-scheme:dark}}}}
body{{margin:0;background:var(--bg);color:var(--fg);font:14px/1.5 system-ui,sans-serif;padding:24px max(16px,4vw)}}
h1{{margin:0 0 4px;font-size:24px}} h2{{font-size:17px;margin:32px 0 10px}} .sub{{color:var(--mut)}}
.ms{{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px;margin-top:16px}}
.m{{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:10px 12px}}
.m span{{display:block;color:var(--mut);font-size:12px;text-transform:uppercase;letter-spacing:.04em}} .m b{{font-size:22px;font-variant-numeric:tabular-nums}}
.wrap{{overflow-x:auto;background:var(--card);border:1px solid var(--line);border-radius:8px}}
table{{border-collapse:collapse;width:100%;font-variant-numeric:tabular-nums}} th,td{{padding:6px 10px;border-bottom:1px solid var(--line);text-align:center}}
th{{font-size:12px;color:var(--mut);position:sticky;top:0;background:var(--card)}} td.desc,td:first-child{{text-align:left}}
td.id{{font-weight:600;font-family:ui-monospace,monospace}} .loc{{display:block;color:var(--mut);font:11px ui-monospace,monospace}}
td.yes{{color:var(--ok);background:var(--okbg);font-weight:700;cursor:help}} td.no{{color:var(--line)}}
td.pct{{background:color-mix(in srgb,var(--ok) calc(var(--p)*45%),transparent)}}
.miss li{{margin:4px 0}} .miss b{{color:var(--bad)}}
</style></head><body>
<h1>buggy-java-app static analysis</h1>
<div class="sub">{len(bugs)} planted bugs · {caught_any} caught by at least one tool ({pct}%) · {len(missed)} missed by all ·
Sonar quality gate: <b>{gate}</b> · tests: {html.escape(test_summary)} · JaCoCo line coverage: {coverage}</div>
<div class="ms">{metric_cards}</div>
<h2>Detection by category</h2><div class="wrap"><table><tr><th>Category</th><th>Planted</th>{head}</tr>{cat_rows}
<tr><td><b>All</b></td><td><b>{len(bugs)}</b></td>{"".join(f"<td><b>{per_tool[t]}</b></td>" for t in TOOLS)}</tr></table></div>
<h2>Missed by every tool</h2><ul class="miss">{missed_li or "<li>None</li>"}</ul>
<h2>Bug-by-bug matrix <span class="sub">(hover a ✔ to see the finding)</span></h2>
<div class="wrap"><table><tr><th>Bug</th><th>Description</th>{head}</tr>{rows}</table></div>
<h2>Failing tests</h2><ul>{tests_li}</ul>
<h2>Vulnerable dependencies (Trivy)</h2><div class="wrap"><table><tr><th>Package</th><th>Version</th><th>Advisories</th>
<th>Critical</th><th>High</th><th>Medium</th><th>Low</th></tr>{dep_rows}</table></div>
<p class="sub">SonarQube column counts native <code>java:</code> rules only. SpotBugs includes the FindSecBugs plugin.
A tool is credited when it flags any line in the bug's declaration. SonarQube Community Build has no dependency (SCA) scanning, so D-bugs are Trivy-only.</p>
</body></html>"""
    (REP / "report.html").write_text(page)
    print(f"Planted: {len(bugs)}  caught: {caught_any}  missed: {len(missed)}")
    for t in TOOLS:
        print(f"  {t:11s} {per_tool[t]:3d} / {len(bugs)}")
    print("Wrote reports/REPORT.md and reports/report.html")


if __name__ == "__main__":
    main()
