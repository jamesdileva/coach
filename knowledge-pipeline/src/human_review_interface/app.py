"""Human review web interface (roadmap Sprint 26).

Local-only Flask app for reviewing AI-generated encounter drafts:
view -> approve / reject / mark needs-work with free-text notes.

Run:  python -m human_review_interface.app <draft.json> [--port 8080]
"""
from __future__ import annotations

import argparse
import copy
import difflib
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SRC = HERE.parent
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

from ai_validator_agent import AiValidatorAgent, apply_review  # noqa: E402
from flask import Flask, jsonify, redirect, render_template, request, url_for  # noqa: E402

STATUSES = ("approved", "needs_work", "rejected")


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, data: dict) -> None:
    path.write_text(json.dumps(data, indent=1) + "\n", encoding="utf-8")


def unified_diff(original: dict, fixed: dict, context: int = 3) -> str:
    before = json.dumps(original, indent=1, sort_keys=True).splitlines()
    after = json.dumps(fixed, indent=1, sort_keys=True).splitlines()
    return "\n".join(difflib.unified_diff(
        before, after, fromfile="ai-draft", tofile="agent-fixed",
        lineterm="", n=context))


def create_app(draft_path: str | Path, report_path: str | Path | None = None) -> Flask:
    draft_file = Path(draft_path)
    app = Flask(
        __name__,
        template_folder=str(HERE / "templates"),
        static_folder=str(HERE / "static"),
    )
    app.config["DRAFT_FILE"] = draft_file
    app.config["REPORT_FILE"] = Path(report_path) if report_path else None

    # analyse once at startup; POST actions only stamp review metadata
    original = load_json(draft_file)
    agent = AiValidatorAgent()
    result = agent.run(copy.deepcopy(original))
    app.config["COACH_STATE"] = {
        "original": original,
        "fixed": result.fixed_draft,
        "report": result.report,
        "changes": result.changes,
    }

    @app.get("/")
    def index():
        return redirect(url_for("review"))

    @app.get("/review")
    def review():
        state = app.config["COACH_STATE"]
        draft = load_json(draft_file)  # re-read so review stamps show
        metadata = draft.get("metadata", {})
        return render_template(
            "review.html",
            metadata=metadata,
            bosses=draft.get("bosses", []),
            report=state["report"],
            report_lines=state["report"].lines(),
            changes=state["changes"],
            statuses=STATUSES,
        )

    @app.post("/review")
    def review_action():
        action = (request.form.get("action") or "").strip()
        notes = (request.form.get("notes") or "").strip()
        if action not in STATUSES:
            return jsonify({"error": f"action must be one of {list(STATUSES)}"}), 400
        draft = load_json(draft_file)
        apply_review(draft, action, notes)
        save_json(draft_file, draft)

        state = app.config["COACH_STATE"]
        state["report"].review = {
            "status": action,
            "notes": notes,
            "packId": draft.get("metadata", {}).get("packId"),
        }
        report_file = app.config["REPORT_FILE"]
        if report_file is not None:
            report_file.write_text(state["report"].to_json() + "\n",
                                   encoding="utf-8")
        return redirect(url_for("review"))

    @app.get("/diff")
    def diff():
        state = app.config["COACH_STATE"]
        diff_text = unified_diff(state["original"], state["fixed"])
        return render_template(
            "diff.html",
            diff_text=diff_text,
            has_changes=bool(diff_text.strip()),
            changes=state["changes"],
        )

    @app.get("/api/status")
    def api_status():
        state = app.config["COACH_STATE"]
        draft = load_json(draft_file)
        summary = state["report"].summary()
        summary["reviewStatus"] = draft.get("metadata", {}).get("review_status")
        return jsonify(summary)

    return app


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("draft", help="draft encounter JSON to review")
    parser.add_argument("--port", type=int, default=8080)
    parser.add_argument("--report", default=None,
                        help="where to write the validation report JSON")
    args = parser.parse_args(argv)

    app = create_app(args.draft, args.report)
    app.run(host="127.0.0.1", port=args.port, debug=False)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
