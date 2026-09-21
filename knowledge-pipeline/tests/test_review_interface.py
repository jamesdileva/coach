"""Sprint 26: human review interface tests (Flask test client, no browser)."""
import copy
import json
import shutil
import sys
from pathlib import Path

import pytest

SRC = Path(__file__).resolve().parents[1] / "src"
if str(SRC) not in sys.path:
    sys.path.insert(0, str(SRC))

from human_review_interface.app import create_app  # noqa: E402

FIXTURES = Path(__file__).resolve().parents[1] / "fixtures"
DRAFT = FIXTURES / "drafts" / "nex_draft.json"


@pytest.fixture()
def draft_file(tmp_path: Path) -> Path:
    target = tmp_path / "nex_draft.json"
    shutil.copy(DRAFT, target)
    return target


@pytest.fixture()
def client(draft_file: Path):
    app = create_app(draft_file, tmp_path_report(draft_file))
    app.config["TESTING"] = True
    return app.test_client()


def tmp_path_report(draft_file: Path) -> Path:
    return draft_file.with_name("validation_report.json")


def read_draft(draft_file: Path) -> dict:
    return json.loads(draft_file.read_text(encoding="utf-8"))


def test_review_page_renders_draft_and_report(client, draft_file):
    response = client.get("/review")
    assert response.status_code == 200
    html = response.get_data(as_text=True)
    assert "Nex" in html
    assert "Smoke phase" in html
    assert "Choke" in html
    assert "Validation report" in html
    assert "agent diff" in html


def test_root_redirects_to_review(client):
    response = client.get("/")
    assert response.status_code in (301, 302, 308)
    assert response.headers["Location"].endswith("/review")


def test_approve_stamps_metadata_and_report(client, draft_file):
    response = client.post("/review",
                           data={"action": "approved",
                                 "notes": "timing verified in-game"})
    assert response.status_code in (301, 302, 308)

    metadata = read_draft(draft_file)["metadata"]
    assert metadata["review_status"] == "approved"
    assert metadata["review_notes"] == "timing verified in-game"
    assert metadata["reviewed_at"]

    report = json.loads(tmp_path_report(draft_file).read_text(encoding="utf-8"))
    assert report["review"]["status"] == "approved"
    assert "summary" in report and "issues" in report


def test_reject_and_needs_work_statuses(client, draft_file):
    client.post("/review", data={"action": "rejected", "notes": "wrong ids"})
    assert read_draft(draft_file)["metadata"]["review_status"] == "rejected"

    client.post("/review", data={"action": "needs_work", "notes": "check hp"})
    assert read_draft(draft_file)["metadata"]["review_status"] == "needs_work"


def test_invalid_action_rejected_with_400(client, draft_file):
    response = client.post("/review", data={"action": "maybe", "notes": ""})
    assert response.status_code == 400
    assert "review_status" not in read_draft(draft_file)["metadata"]


def test_review_page_shows_current_status_after_decision(client):
    client.post("/review", data={"action": "approved", "notes": "ok"})
    html = client.get("/review").get_data(as_text=True)
    assert "review: approved" in html


def test_diff_page_renders(client):
    response = client.get("/diff")
    assert response.status_code == 200
    html = response.get_data(as_text=True)
    assert "ai-draft" in html or "no changes" in html


def test_diff_shows_agent_fixes():
    pytest.importorskip("json_generator")
    import json_generator

    draft = json.loads(DRAFT.read_text(encoding="utf-8"))
    broken = copy.deepcopy(draft)
    callout = broken["bosses"][0]["phases"][0]["mechanics"][1]["callouts"][0]
    callout["priority"] = 900  # force an auto-fixable issue

    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        target = Path(tmp) / "broken.json"
        target.write_text(json.dumps(broken), encoding="utf-8")
        app = create_app(target)
        app.config["TESTING"] = True
        html = app.test_client().get("/diff").get_data(as_text=True)
    assert "agent-fixed" in html or "agent fixed" in html.lower()
    assert "priority" in html


def test_api_status_reports_review_state(client):
    payload = client.get("/api/status").get_json()
    assert payload["reviewStatus"] is None
    assert "passed" in payload

    client.post("/review", data={"action": "needs_work", "notes": "x"})
    payload = client.get("/api/status").get_json()
    assert payload["reviewStatus"] == "needs_work"


def test_apply_review_rejects_unknown_status():
    from ai_validator_agent import apply_review
    with pytest.raises(ValueError):
        apply_review({}, "maybe")
