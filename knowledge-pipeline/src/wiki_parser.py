"""Parse OSRS Wiki HTML into structured boss data (roadmap Sprint 23).

Two page kinds are understood:
- main boss pages (infobox: NPC ids, combat level, attack styles)
- /Strategies pages (phase sections with shout-announced special attacks)

Targets current oldschool.runescape.wiki MediaWiki markup: infobox rows as
th/td pairs, headings under #mw-content-text, shouts wrapped in
span.in-game-message.
"""
from __future__ import annotations

import re
from typing import Dict, List, Optional, Tuple

from bs4 import BeautifulSoup, Tag

from models import Boss, Mechanic, Phase

STYLE_WORDS = ("Melee", "Magic", "Ranged", "Crush", "Slash", "Stab")


def _text(element: Optional[Tag]) -> str:
    return element.get_text(" ", strip=True) if element else ""


def _page_title(soup: BeautifulSoup) -> str:
    title = soup.select_one("#firstHeading")
    return _text(title)


def parse_infobox(html: str) -> Dict[str, str]:
    """Flatten the first infobox table into {label: value} pairs."""
    soup = BeautifulSoup(html, "html.parser")
    infobox = soup.select_one("table.infobox")
    data: Dict[str, str] = {}
    if infobox is None:
        return data
    for row in infobox.select("tr"):
        th = row.find("th")
        td = row.find("td")
        if th is None or td is None:
            continue
        label = _text(th).strip().rstrip(":").strip()
        if label and label not in data:
            data[label] = _text(td)
    return data


def parse_npc_ids(infobox: Dict[str, str]) -> List[int]:
    for key in ("Monster ID", "NPC ID"):
        raw = infobox.get(key, "")
        ids = [int(part.strip()) for part in raw.split(",")
               if part.strip().isdigit()]
        if ids:
            return ids
    return []


def parse_attack_styles(infobox: Dict[str, str]) -> List[str]:
    styles = infobox.get("Attack style", "")
    if not styles:
        return []
    words: List[str] = []
    seen = set()
    for token in styles.replace(",", ", ").split(","):
        token = token.strip()
        if token in STYLE_WORDS and token.lower() not in seen:
            seen.add(token.lower())
            words.append(token)
    return words


def parse_combat_level(infobox: Dict[str, str]) -> str:
    level = infobox.get("Combat level", "")
    digits = "".join(ch for ch in level if ch.isdigit())
    return digits or level


def parse_max_hit(infobox: Dict[str, str]) -> str:
    return infobox.get("Max hit", "")


def _section_body_html(heading: Tag) -> str:
    """Everything between this heading and the next h2/h3/h4 in document
    order, keeping only content elements (paragraphs and lists)."""
    parts: List[str] = []
    for element in heading.find_all_next():
        if isinstance(element, Tag):
            if element.name in ("h2", "h3", "h4"):
                break
            if element.name in ("p", "ul"):
                parts.append(str(element))
    return "".join(parts)


def iter_sections(html: str,
                  content_selector: str = "#mw-content-text",
                  ) -> List[Tuple[int, str, str]]:
    """Return (level, title, body_html) for every section.

    A section's body runs from its heading until the next h2/h3/h4 anywhere
    after it in document order. Only paragraphs and lists are kept — enough
    for descriptions and mechanic bullet extraction.
    """
    soup = BeautifulSoup(html, "html.parser")
    if soup.select_one(content_selector) is None:
        return []

    sections: List[Tuple[int, str, str]] = []
    headings = [
        h for h in soup.find_all(["h2", "h3", "h4"])
        if h.get_text(strip=True)
    ]

    for heading in headings:
        level = int(heading.name[1])
        headline = heading.find(class_="mw-headline")
        title = (_text(headline) if headline else _text(heading))
        title = title.replace("[edit]", "").strip()

        sections.append((level, title, _section_body_html(heading)))
    return sections


def section_body(sections: List[Tuple[int, str, str]], title_substring: str,
                 ) -> Optional[str]:
    """Concatenated HTML of a section plus its sub-sections."""
    needle = title_substring.lower()
    index = next((i for i, (_, title, _) in enumerate(sections)
                  if needle in title.lower()), None)
    if index is None:
        return None

    match_level = sections[index][0]
    parts = [sections[index][2]]
    for level, _, body in sections[index + 1:]:
        if level <= match_level:
            break
        parts.append(body)
    return "".join(parts)


def extract_shouts(fragment: Tag) -> List[str]:
    """Boss telegraph shouts are wrapped in span.in-game-message."""
    shouts = [_text(span) for span in fragment.select("span.in-game-message")]
    seen = set()
    result = []
    for shout in shouts:
        if shout and shout.lower() not in seen:
            seen.add(shout.lower())
            result.append(shout)
    return result


def parse_mechanics_from_body(body_html: str) -> List[Mechanic]:
    """Bullet items whose lead element is bold become mechanics; embedded
    in-game-message spans become shout candidates."""
    soup = BeautifulSoup(body_html, "html.parser")
    mechanics: List[Mechanic] = []
    seen = set()
    for li in soup.select("ul > li"):
        bold = li.find("b")
        if bold is None:
            continue
        name = _text(bold).rstrip(":").strip()
        if not name or name.lower() in seen:
            continue
        seen.add(name.lower())

        # description = the bullet's full text minus the leading bold name
        description = _text(li)
        if description.startswith(name):
            description = description[len(name):]
        description = description.lstrip(" -–—:").strip()

        mechanics.append(Mechanic(
            name=name,
            description=description,
            shouts=extract_shouts(li),
        ))
    return mechanics


def parse_phases(strategies_html: str) -> List[Phase]:
    """Every h3 section titled '... phase' becomes a Phase."""
    sections = iter_sections(strategies_html)
    phases: List[Phase] = []
    for level, title, body in sections:
        if level != 3 or not title.lower().endswith("phase"):
            continue
        body_soup = BeautifulSoup(body, "html.parser")
        paragraphs = [_text(p) for p in body_soup.select("p")]
        phases.append(Phase(
            title=title,
            description=paragraphs[0] if paragraphs else "",
            mechanics=parse_mechanics_from_body(body),
            shouts=extract_shouts(body_soup),
        ))
    return phases


def parse_boss(main_html: str, strategies_html: str,
               source_pages: Optional[List[str]] = None) -> Boss:
    """Full extraction: infobox facts from the main page, phases + mechanics
    from the strategies page."""
    main_soup = BeautifulSoup(main_html, "html.parser")
    infobox = parse_infobox(main_html)

    strategies_soup = BeautifulSoup(strategies_html, "html.parser")
    base_name = _page_title(strategies_soup)
    if "/" in base_name:
        base_name = base_name.split("/")[0].strip()

    return Boss(
        name=_page_title(main_soup) or base_name,
        npc_ids=parse_npc_ids(infobox),
        combat_level=parse_combat_level(infobox),
        max_hit=parse_max_hit(infobox),
        attack_styles=parse_attack_styles(infobox),
        infobox=infobox,
        phases=parse_phases(strategies_html),
        source_pages=list(source_pages or []),
    )
