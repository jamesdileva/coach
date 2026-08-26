"""Fetch and cache OSRS Wiki pages (roadmap Sprint 23).

Pages are cached verbatim under the cache directory so parsing is repeatable
offline; tests use committed fixtures instead of the network.
"""
import re
from pathlib import Path

import requests

WIKI_BASE = "https://oldschool.runescape.wiki"
USER_AGENT = "ProjectCoach/1.0 (encounter pack tooling)"


class WikiFetcher:
    def __init__(self, cache_dir: Path | str):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(parents=True, exist_ok=True)

    @staticmethod
    def _page_slug(page: str) -> str:
        slug = page.strip().replace(" ", "_")
        if "/" in slug:
            head, tail = slug.split("/", 1)
            slug = f"{head}__{tail}"
        return re.sub(r"[^A-Za-z0-9_-]", "_", slug)

    def _cache_path(self, page: str) -> Path:
        return self.cache_dir / f"{self._page_slug(page)}.html"

    def is_cached(self, page: str) -> bool:
        return self._cache_path(page).exists()

    def fetch(self, page: str, timeout: int = 30, force_refresh: bool = False) -> str:
        """Return the raw HTML of a wiki page, using the cache when present.

        Raises requests.HTTPError on bad responses; callers decide policy.
        """
        cache_path = self._cache_path(page)
        if cache_path.exists() and not force_refresh:
            return cache_path.read_text(encoding="utf-8")

        url = f"{WIKI_BASE}/w/{page.strip().replace(' ', '_')}"
        response = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=timeout)
        response.raise_for_status()
        html = response.text
        cache_path.write_text(html, encoding="utf-8")
        return html
