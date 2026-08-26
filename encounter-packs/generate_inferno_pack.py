"""Generate the Inferno encounter pack (69 waves + basic Zuk).

Produces encounter-packs/inferno.pack/encounter.json from the canonical
wave table (OSRS Wiki) and per-archetype attack callouts.

Data sources: osrs.runescape.wiki/w/Inferno (wave list), OpenOSRS/InfernoTrainer
+ boss-cooldown community data (NPC ids, attack animation ids).
Animation IDs are best-known values — see inferno.pack/README.md checklist.
"""
import json
from pathlib import Path

PACK_DIR = Path(__file__).parent / "inferno.pack"

# --- NPC ids (flagged for live verification; see README) ---
NPC_IDS = {
    "nibbler": [7690, 7691, 7692, 7693],   # Jal-Nib variants
    "bat": 7694,          # Jal-MejRah
    "blob": 7695,         # Jal-Ak
    "blob_split_ranged": 7704,  # Jal-AkRek-Xil
    "blob_split_magic": 7705,   # Jal-AkRek-Mej
    "blob_split_melee": 7703,   # Jal-AkRek-Ket
    "meleer": 7696,       # Jal-ImKot
    "ranger": 7698,       # Jal-Xil
    "mager": 7699,        # Jal-Zek
    "jad_healer": 7701,   # Yt-HurKot
    "jad": 7700,          # JalTok-Jad
    "zuk_healer": 7702,   # Jal-MejJak
    "zuk": 7706,          # TzKal-Zuk
}

# --- Attack animations (best-known; flagged for verification) ---
ANIMS = {
    "bat_ranged": (7578, 3),
    "blob_magic": (7581, 6),
    "blob_melee": (7582, 9),
    "blob_ranged": (7583, 6),
    "meleer_attack": (7597, 4),
    "ranger_ranged": (7605, 4),
    "ranger_melee": (7604, 4),
    "mager_magic": (7610, 4),
    "mager_melee": (7612, 4),
    "jad_magic": (7592, None),
    "jad_ranged": (7593, None),
    "jad_melee": (7594, None),
    "zuk_attack": (7566, None),
}

# --- Wave table: archetype -> count. N=nibbler B=bat K=blob M=meleer R=ranger Z=mager ---
WAVES = {
    1: {"N": 3, "B": 1}, 2: {"N": 3, "B": 2}, 3: {"N": 6},
    4: {"N": 3, "K": 1}, 5: {"N": 3, "K": 1, "B": 1}, 6: {"N": 3, "B": 2, "K": 1},
    7: {"N": 3, "K": 2}, 8: {"N": 6},
    9: {"N": 3, "M": 1}, 10: {"N": 3, "M": 1, "B": 1}, 11: {"N": 3, "M": 1, "B": 2},
    12: {"N": 3, "M": 1, "K": 1}, 13: {"N": 3, "M": 1, "K": 1, "B": 1},
    14: {"N": 3, "M": 1, "K": 1, "B": 2}, 15: {"N": 3, "M": 1, "K": 2},
    16: {"N": 3, "M": 2}, 17: {"N": 6},
    18: {"N": 3, "R": 1}, 19: {"N": 3, "R": 1, "B": 1}, 20: {"N": 3, "R": 1, "B": 2},
    21: {"N": 3, "R": 1, "K": 1}, 22: {"N": 3, "R": 1, "K": 1, "B": 1},
    23: {"N": 3, "R": 1, "K": 1, "B": 2}, 24: {"N": 3, "R": 1, "K": 2},
    25: {"N": 3, "R": 1, "M": 1}, 26: {"N": 3, "R": 1, "M": 1, "B": 1},
    27: {"N": 3, "R": 1, "M": 1, "B": 2}, 28: {"N": 3, "R": 1, "M": 1, "K": 1},
    29: {"N": 3, "R": 1, "M": 1, "K": 1, "B": 1}, 30: {"N": 3, "R": 1, "M": 1, "K": 1, "B": 2},
    31: {"N": 3, "R": 1, "M": 1, "K": 2}, 32: {"N": 3, "R": 1, "M": 2},
    33: {"N": 3, "R": 2}, 34: {"N": 6},
    35: {"N": 3, "Z": 1}, 36: {"N": 3, "Z": 1, "B": 1}, 37: {"N": 3, "Z": 1, "B": 2},
    38: {"N": 3, "Z": 1, "K": 1}, 39: {"N": 3, "Z": 1, "K": 1, "B": 1},
    40: {"N": 3, "Z": 1, "K": 1, "B": 2}, 41: {"N": 3, "Z": 1, "K": 2},
    42: {"N": 3, "Z": 1, "M": 1}, 43: {"N": 3, "Z": 1, "M": 1, "B": 1},
    44: {"N": 3, "Z": 1, "M": 1, "B": 2}, 45: {"N": 3, "Z": 1, "M": 1, "K": 1},
    46: {"N": 3, "Z": 1, "M": 1, "K": 1, "B": 1}, 47: {"N": 3, "Z": 1, "M": 1, "K": 1, "B": 2},
    48: {"N": 3, "Z": 1, "M": 1, "K": 2}, 49: {"N": 3, "Z": 1, "M": 2},
    50: {"N": 3, "Z": 1, "R": 1}, 51: {"N": 3, "Z": 1, "R": 1, "B": 1},
    52: {"N": 3, "Z": 1, "R": 1, "B": 2}, 53: {"N": 3, "Z": 1, "R": 1, "K": 1},
    54: {"N": 3, "Z": 1, "R": 1, "K": 1, "B": 1}, 55: {"N": 3, "Z": 1, "R": 1, "K": 1, "B": 2},
    56: {"N": 3, "Z": 1, "R": 1, "K": 2}, 57: {"N": 3, "Z": 1, "R": 1, "M": 1},
    58: {"N": 3, "Z": 1, "R": 1, "M": 1, "B": 1}, 59: {"N": 3, "Z": 1, "R": 1, "M": 1, "B": 2},
    60: {"N": 3, "Z": 1, "R": 1, "M": 1, "K": 1},
    61: {"N": 3, "Z": 1, "R": 1, "M": 1, "K": 1, "B": 1},
    62: {"N": 3, "Z": 1, "R": 1, "M": 1, "K": 1, "B": 2},
    63: {"N": 3, "Z": 1, "R": 1, "M": 1, "K": 2}, 64: {"N": 3, "Z": 1, "R": 1, "M": 2},
    65: {"N": 3, "Z": 1, "R": 2}, 66: {"N": 3, "Z": 2},
    67: {"JAD": 1}, 68: {"JAD": 3},
    69: {"ZUK": 1},
}

ARCHETYPE_NPC = {
    "N": NPC_IDS["nibbler"], "B": [NPC_IDS["bat"]], "K": [NPC_IDS["blob"]],
    "M": [NPC_IDS["meleer"]], "R": [NPC_IDS["ranger"]], "Z": [NPC_IDS["mager"]],
    "JAD": [NPC_IDS["jad"]], "ZUK": [NPC_IDS["zuk"]],
}
SPLIT_IDS = [NPC_IDS["blob_split_ranged"], NPC_IDS["blob_split_magic"], NPC_IDS["blob_split_melee"]]


def callout(callout_id, text, audio, category, priority, color, duration=3):
    return {
        "calloutId": callout_id, "text": text, "audioFile": audio,
        "category": category, "priority": priority,
        "audioOffset": -1 if category == "critical" else 0,
        "visualOffset": 0,
        "visual": {"type": "text", "color": color, "durationTicks": duration},
    }


# Shared attack-callout mechanics injected into every phase that has the mob
ATTACK_MECHANICS = {
    "M": {
        "mechanicId": "meleer_attack", "name": "Jal-ImKot attacking",
        "triggers": [{"triggerId": "meleer_anim", "type": "animation",
                      "npcIds": [NPC_IDS["meleer"]], "animationId": ANIMS["meleer_attack"][0]}],
        "callouts": [callout("pray_melee_meleer", "Pray Melee!", "pray_melee.ogg",
                             "critical", 90, "#FF0000")],
        "cooldown": 1,
    },
    "R": {
        "mechanicId": "ranger_attack", "name": "Jal-Xil attacking",
        "triggers": [
            {"triggerId": "ranger_range", "type": "animation",
             "npcIds": [NPC_IDS["ranger"]], "animationId": ANIMS["ranger_ranged"][0]},
            {"triggerId": "ranger_melee", "type": "animation",
             "npcIds": [NPC_IDS["ranger"]], "animationId": ANIMS["ranger_melee"][0]},
        ],
        "callouts": [
            callout("pray_ranged_ranger", "Pray Ranged!", "pray_ranged.ogg",
                    "critical", 90, "#FF0000"),
            callout("ranger_melee_warn", "Ranger melee!", "ranger_melee.ogg",
                    "warning", 60, "#FF9800"),
        ],
        "cooldown": 1,
    },
    "Z": {
        "mechanicId": "mager_attack", "name": "Jal-Zek attacking",
        "triggers": [
            {"triggerId": "mager_magic", "type": "animation",
             "npcIds": [NPC_IDS["mager"]], "animationId": ANIMS["mager_magic"][0]},
            {"triggerId": "mager_melee", "type": "animation",
             "npcIds": [NPC_IDS["mager"]], "animationId": ANIMS["mager_melee"][0]},
        ],
        "callouts": [
            callout("pray_magic_mager", "Pray Magic!", "pray_magic.ogg",
                    "critical", 90, "#FF0000"),
            callout("mager_melee_warn", "Mager melee!", "mager_melee.ogg",
                    "warning", 60, "#FF9800"),
        ],
        "cooldown": 1,
    },
    "K": {
        "mechanicId": "blob_attack", "name": "Jal-Ak attacking",
        "triggers": [
            {"triggerId": "blob_a", "type": "animation",
             "npcIds": [NPC_IDS["blob"]], "animationId": ANIMS["blob_magic"][0]},
            {"triggerId": "blob_b", "type": "animation",
             "npcIds": [NPC_IDS["blob"]], "animationId": ANIMS["blob_ranged"][0]},
            {"triggerId": "blob_c", "type": "animation",
             "npcIds": [NPC_IDS["blob"]], "animationId": ANIMS["blob_melee"][0]},
        ],
        "callouts": [callout("blob_attacking", "Blob attacking — switch prayer!",
                             "blob_attack.ogg", "warning", 65, "#FF9800")],
        "cooldown": 1,
    },
    "JAD": {
        "mechanicId": "jad_attack", "name": "JalTok-Jad attacking",
        "triggers": [
            {"triggerId": "jad_magic", "type": "animation",
             "npcIds": [NPC_IDS["jad"]], "animationId": ANIMS["jad_magic"][0]},
            {"triggerId": "jad_ranged", "type": "animation",
             "npcIds": [NPC_IDS["jad"]], "animationId": ANIMS["jad_ranged"][0]},
            {"triggerId": "jad_melee", "type": "animation",
             "npcIds": [NPC_IDS["jad"]], "animationId": ANIMS["jad_melee"][0]},
        ],
        "callouts": [
            callout("jad_pray_magic", "PRAY MAGIC!", "pray_magic.ogg", "critical", 99, "#FF0000"),
            callout("jad_pray_ranged", "PRAY RANGED!", "pray_ranged.ogg", "critical", 99, "#FF0000"),
            callout("jad_pray_melee", "PRAY MELEE!", "pray_melee.ogg", "critical", 99, "#FF0000"),
        ],
        "cooldown": 1,
    },
    "HEALERS_JAD": {
        "mechanicId": "jad_healers", "name": "Yt-HurKot healers spawned",
        "triggers": [{"triggerId": "healer_spawn", "type": "npc_spawn",
                      "npcIds": [NPC_IDS["jad_healer"]]}],
        "callouts": [callout("healers_callout", "Healers! Tag them off the Jad",
                             "healers.ogg", "warning", 75, "#FF9800", 4)],
        "cooldown": 8,
    },
}


def wave_npc_ids(composition):
    ids = []
    for archetype, count in composition.items():
        ids.extend(ARCHETYPE_NPC[archetype] * count)
    return ids


def build_wave_phase(number, composition):
    spawn_ids = sorted(set(wave_npc_ids(composition)))
    cleared_ids = list(spawn_ids)
    if "K" in composition:
        cleared_ids.extend(SPLIT_IDS)  # blob splits must die before the wave ends

    mechanics = []
    for archetype in ("M", "R", "Z", "K", "JAD", "HEALERS_JAD"):
        if archetype in composition or (
            archetype == "HEALERS_JAD" and "JAD" in composition
        ):
            mechanics.append(ATTACK_MECHANICS[archetype])

    return {
        "phaseId": f"wave_{number}",
        "name": f"Wave {number}",
        "entryTrigger": {
            "triggerId": f"wave{number}_entry",
            "type": "npc_spawn",
            "npcIds": spawn_ids,
        },
        "exitTriggers": [{
            "triggerId": f"wave{number}_clear",
            "type": "wave_cleared",
            "npcIds": sorted(set(cleared_ids)),
        }],
        "mechanics": mechanics,
    }


def build_zuk_phase():
    return {
        "phaseId": "zuk",
        "name": "TzKal-Zuk",
        "entryTrigger": {
            "triggerId": "zuk_entry", "type": "npc_spawn", "npcIds": [NPC_IDS["zuk"]],
        },
        "exitTriggers": [],
        "mechanics": [
            {
                "mechanicId": "zuk_attack", "name": "Zuk attacking",
                "triggers": [{"triggerId": "zuk_anim", "type": "animation",
                              "npcIds": [NPC_IDS["zuk"]], "animationId": ANIMS["zuk_attack"][0]}],
                "callouts": [callout("zuk_attacking", "Zuk attacking — check shield spot!",
                                     "zuk_attack.ogg", "warning", 80, "#FF5722", 3)],
                "cooldown": 1,
            },
            {
                "mechanicId": "zuk_jad_spawn", "name": "Jad spawned in Zuk",
                "triggers": [{"triggerId": "zuk_jad", "type": "npc_spawn",
                              "npcIds": [NPC_IDS["jad"]]}],
                "callouts": [callout("zuk_jad_callout", "JAD spawned behind you!",
                                     "jad_spawned.ogg", "critical", 97, "#FF0000", 4)],
                "cooldown": 20,
            },
            {
                "mechanicId": "zuk_healers_spawn", "name": "Zuk healers spawned",
                "triggers": [{"triggerId": "zuk_healers", "type": "npc_spawn",
                              "npcIds": [NPC_IDS["zuk_healer"]]}],
                "callouts": [callout("zuk_healers_callout", "Zuk healers! Attack them fast",
                                     "healers.ogg", "critical", 95, "#FF5722", 4)],
                "cooldown": 20,
            },
        ],
    }


def main():
    phases = []
    for number in range(1, 68):
        phases.append(build_wave_phase(number, WAVES[number]))
    phases.append(build_wave_phase(68, WAVES[68]))
    phases.append(build_zuk_phase())

    pack = {
        "schemaVersion": "1.0",
        "metadata": {
            "packId": "inferno",
            "name": "The Inferno",
            "description": (
                "All 69 waves + basic TzKal-Zuk. Wave transitions detected via "
                "wave_cleared triggers (all wave NPCs dead); prayer callouts via "
                "attack animations. Animation/NPC ids are best-known community "
                "values — see README verification checklist."
            ),
            "author": "Project Coach",
            "version": "1.0.0",
            "gameVersion": "230",
        },
        "bosses": [{
            "bossId": "inferno",
            "name": "The Inferno",
            "npcId": NPC_IDS["zuk"],
            "phases": phases,
        }],
    }

    PACK_DIR.mkdir(parents=True, exist_ok=True)
    out = PACK_DIR / "encounter.json"
    out.write_text(json.dumps(pack, indent=1), encoding="utf-8")

    total_mechanics = sum(len(p["mechanics"]) for p in phases)
    print(f"wrote {out} ({out.stat().st_size} bytes)")
    print(f"phases: {len(phases)} (waves 1-68 + zuk), mechanics: {total_mechanics}")


if __name__ == "__main__":
    main()
