#!/usr/bin/env bash
# Verifies JMH average scores against SEDNA performance targets (E2E-026–028).
# Run after: ./gradlew jmh
set -euo pipefail

RESULTS_JSON="${1:-benchmarks/build/results/jmh/results.json}"

if [[ ! -f "$RESULTS_JSON" ]]; then
  echo "Missing JMH JSON results: $RESULTS_JSON"
  echo "Run: ./gradlew jmh -Pjmh.include=io.sedna.benchmarks"
  exit 1
fi

python3 - "$RESULTS_JSON" <<'PY'
import json
import sys

path = sys.argv[1]
with open(path, encoding="utf-8") as f:
    data = json.load(f)

# JMH JSON: list of benchmarks with primaryMetric.score
limits_ms = {
    "io.sedna.benchmarks.DnaCodecBenchmark.decode": 100.0,
    "io.sedna.benchmarks.ForwardPipelineBenchmark.forwardReconstruction": 5000.0,
    "io.sedna.benchmarks.ReversePipelineBenchmark.reverseAnalysis": 30000.0,
}

failed = []
for entry in data:
    key = f"{entry['benchmark']}.{entry['benchmarkMode']}"
    for suffix, limit in limits_ms.items():
        if suffix in key or key.endswith(suffix.split(".")[-1]):
            score = entry["primaryMetric"]["score"]
            unit = entry["primaryMetric"].get("scoreUnit", "ms")
            score_ms = score * 1000 if unit == "ns/op" else score
            if score_ms > limit:
                failed.append((key, score_ms, limit))

if failed:
    for key, score, limit in failed:
        print(f"FAIL {key}: {score:.3f} ms > {limit} ms")
    sys.exit(1)

print("JMH thresholds OK:", ", ".join(limits_ms.keys()))
PY
