#!/usr/bin/env python3
"""
BanglaTLit Parallel Corpus Parser & Transliteration Dictionary Builder
======================================================================
This script parses the complete 'BanglaTLit_train.csv' dataset (245,727 sentence pairs) to:
  1. Perform Unsupervised Financial Entity Mining (nouns, items, merchants, verbs).
  2. Build a full bidirectional Banglish <-> Bengali Transliteration Dictionary (37,000+ words).
  3. Synthesize boundary-aware Regex patterns for BanglishNormalizer.kt.
  4. Generate Kotlin code:
     - BanglishCorpusVocabulary.kt (Anchors, Action Verbs, Normalization rules)
     - BanglishTransliterator.kt (Offline word-by-word transliterator to Bengali script)

Usage:
  python3 scripts/parse_bangla_tlit.py --csv /path/to/BanglaTLit_train.csv
  python3 scripts/parse_bangla_tlit.py --query taka
  python3 scripts/parse_bangla_tlit.py --query খরচ
"""

import os
import sys
import csv
import re
import json
import argparse
from collections import Counter, defaultdict
from pathlib import Path

# ==============================================================================
# Domain Dictionary Seeds: Bengali Lemmas -> Canonical Banglish & Categories
# ==============================================================================

SEED_LEMMAS = {
    # Currency & Denominations
    "টাকা": {"canonical": "taka", "group": "currency", "special": ["tk", "tka", "takar", "takay", "taaka", "takaye"]},
    "টাকার": {"canonical": "takar", "group": "currency", "special": ["tkr", "tkar", "takare"]},
    "টাকায়": {"canonical": "takay", "group": "currency", "special": ["takaye", "takat"]},
    "পয়সা": {"canonical": "poisa", "group": "currency", "special": ["poysha", "poesha", "poisha"]},
    "হাজার": {"canonical": "hajar", "group": "magnitude", "special": ["hazar", "hajer", "hajaro", "hajare"]},
    "লাখ": {"canonical": "lakh", "group": "magnitude", "special": ["lac", "laakh", "lakhe"]},
    "লক্ষ": {"canonical": "lakh", "group": "magnitude", "special": ["lokkho", "lokkh"]},
    "কোটি": {"canonical": "koti", "group": "magnitude", "special": ["crore", "kotee", "kuti"]},
    "শত": {"canonical": "shoto", "group": "magnitude", "special": ["soto", "shoto"]},
    "শ": {"canonical": "sho", "group": "magnitude", "special": ["sho", "so"]},
    "দেড়": {"canonical": "der", "group": "fraction", "special": ["derh", "dedh"]},
    "আড়াই": {"canonical": "arai", "group": "fraction", "special": ["arhai", "adhai"]},
    "সাড়ে": {"canonical": "sare", "group": "fraction", "special": ["share", "sade"]},

    # Transaction Indicators & Action Verbs
    "খরচ": {"canonical": "khoroch", "group": "expense_keyword", "special": ["khoros", "koroch", "koros", "khoroc", "koroc", "khoroj", "khorche"]},
    "খরচের": {"canonical": "khorocher", "group": "expense_keyword", "special": ["khoroser", "korocher", "koroser", "khorocer"]},
    "দিলাম": {"canonical": "dilam", "group": "verb_give", "special": ["delam", "dilm", "dilem", "diasilam", "disilam"]},
    "দিয়েছি": {"canonical": "diyechi", "group": "verb_give", "special": ["diyechi", "dieci", "diyeche"]},
    "দিছি": {"canonical": "disi", "group": "verb_give", "special": ["dici", "dise", "disen", "ditesi", "dese"]},
    "দিল": {"canonical": "dilo", "group": "verb_give", "special": ["delo", "dilo"]},
    "দিবে": {"canonical": "dibe", "group": "verb_give", "special": ["debe", "diben", "dibo"]},
    "দিতে": {"canonical": "dite", "group": "verb_give", "special": ["dete", "dite"]},
    "করলাম": {"canonical": "korlam", "group": "verb_do", "special": ["krlam", "korlam", "korlem"]},
    "করেছি": {"canonical": "korechi", "group": "verb_do", "special": ["korechi", "koreci", "koresi", "koreche"]},
    "করছি": {"canonical": "korsi", "group": "verb_do", "special": ["korci", "korchi", "korsi", "korsilam"]},
    "কিনলাম": {"canonical": "kinlam", "group": "verb_buy", "special": ["kinlam", "kinlem"]},
    "কিনেছি": {"canonical": "kinechi", "group": "verb_buy", "special": ["kinechi", "kinesilam", "kineche"]},
    "কিনছি": {"canonical": "kinsi", "group": "verb_buy", "special": ["kinci", "kinsi", "kinchilam"]},
    "কিনব": {"canonical": "kinbo", "group": "verb_buy", "special": ["kinbo", "kinbe"]},
    "খেলাম": {"canonical": "khelam", "group": "verb_eat", "special": ["khailam", "khelam"]},
    "খেয়েছি": {"canonical": "kheyechi", "group": "verb_eat", "special": ["kheyechi", "khaisi", "khaici"]},
    "পাঠালাম": {"canonical": "pathalam", "group": "verb_send", "special": ["pathaylam", "pathalam"]},
    "পাঠিয়েছি": {"canonical": "pathiyechi", "group": "verb_send", "special": ["pathayechi", "pathiyechi", "pathechi"]},
    "পাঠাইছি": {"canonical": "pathaisi", "group": "verb_send", "special": ["pathaisi", "pathayci", "pathaysi"]},
    "পেলাম": {"canonical": "pelam", "group": "verb_receive", "special": ["pailam", "pelam"]},
    "পেয়েছি": {"canonical": "peyechi", "group": "verb_receive", "special": ["peyechi", "peyesi"]},
    "পাইছি": {"canonical": "paisi", "group": "verb_receive", "special": ["paisi", "paici", "paise", "payci"]},
    "লাগলো": {"canonical": "laglo", "group": "verb_cost", "special": ["laglo", "lagsilo"]},
    "লাগবে": {"canonical": "lagbe", "group": "verb_cost", "special": ["lagbe", "lagba"]},
    "গেল": {"canonical": "gelo", "group": "verb_cost", "special": ["gelo", "galo"]},
    "গেছে": {"canonical": "gese", "group": "verb_cost", "special": ["gese", "geche", "gesilo"]},
    "হলো": {"canonical": "holo", "group": "verb_cost", "special": ["holo", "holona"]},
    "হইছে": {"canonical": "hoise", "group": "verb_cost", "special": ["hoise", "hoice", "hoiche"]},

    # Transport
    "ভাড়া": {"canonical": "vara", "group": "transport", "special": ["bhara", "bada", "vaada", "varah", "varar", "bharar"]},
    "বাস": {"canonical": "bus", "group": "transport", "special": ["bus", "buse", "buser"]},
    "রিকশা": {"canonical": "rickshaw", "group": "transport", "special": ["riksa", "riksha", "ricksa", "rikshaw"]},
    "সিএনজি": {"canonical": "cng", "group": "transport", "special": ["cng", "c.n.g"]},
    "অটো": {"canonical": "auto", "group": "transport", "special": ["auto", "otou"]},
    "ট্রেন": {"canonical": "train", "group": "transport", "special": ["train", "trein"]},
    "মেট্রো": {"canonical": "metro", "group": "transport", "special": ["metro"]},
    "উবার": {"canonical": "uber", "group": "transport", "special": ["uber"]},
    "পাঠাও": {"canonical": "pathao", "group": "transport", "special": ["pathao"]},
    "গাড়ি": {"canonical": "gari", "group": "transport", "special": ["gari", "garir", "gadi"]},

    # Food & Dining
    "খাবার": {"canonical": "khabar", "group": "food", "special": ["khabar", "khabaar", "khabare"]},
    "নাস্তা": {"canonical": "nasta", "group": "food", "special": ["nashta", "nasta", "nastah"]},
    "লাঞ্চ": {"canonical": "lunch", "group": "food", "special": ["lunch", "luncher"]},
    "ডিনার": {"canonical": "dinner", "group": "food", "special": ["dinner", "dinar"]},
    "ভাত": {"canonical": "bhat", "group": "food", "special": ["bhat", "vaat"]},
    "বিরিয়ানি": {"canonical": "biryani", "group": "food", "special": ["biriani", "biriyani", "biryani"]},
    "চা": {"canonical": "cha", "group": "food", "special": ["cha", "tea"]},
    "কফি": {"canonical": "coffee", "group": "food", "special": ["coffee", "coffe"]},
    "রুটি": {"canonical": "ruti", "group": "food", "special": ["ruti", "roti"]},
    "পরোটা": {"canonical": "parata", "group": "food", "special": ["porota", "parata", "porata"]},
    "সিঙ্গারা": {"canonical": "singara", "group": "food", "special": ["shingara", "singara"]},
    "ফুচকা": {"canonical": "fuchka", "group": "food", "special": ["fuchka", "fuska"]},

    # Groceries
    "বাজার": {"canonical": "bazar", "group": "grocery", "special": ["bajar", "bazar", "baazar", "bajare", "bazare"]},
    "চাল": {"canonical": "chal", "group": "grocery", "special": ["chaal", "chal"]},
    "ডাল": {"canonical": "dal", "group": "grocery", "special": ["daal", "dal"]},
    "তেল": {"canonical": "tel", "group": "grocery", "special": ["tel", "toil", "oil"]},
    "ডিম": {"canonical": "dim", "group": "grocery", "special": ["dim", "deem"]},
    "দুধ": {"canonical": "dudh", "group": "grocery", "special": ["dudh", "dud"]},
    "মাছ": {"canonical": "mach", "group": "grocery", "special": ["maach", "mach", "macher"]},
    "মাংস": {"canonical": "mangsho", "group": "grocery", "special": ["mangsho", "mangso", "mangsho"]},
    "মুরগি": {"canonical": "murgi", "group": "grocery", "special": ["murgi", "moorgi", "morgi"]},
    "সবজি": {"canonical": "sobji", "group": "grocery", "special": ["shobji", "sobji", "shobjee"]},
    "তরকারি": {"canonical": "torkari", "group": "grocery", "special": ["torkari", "torkari"]},
    "আলু": {"canonical": "aloo", "group": "grocery", "special": ["alu", "aloo"]},
    "পেঁয়াজ": {"canonical": "peyaj", "group": "grocery", "special": ["peyaj", "piyaj", "peaj"]},
    "রসুন": {"canonical": "roshun", "group": "grocery", "special": ["roshun", "rosun"]},
    "মরিচ": {"canonical": "morich", "group": "grocery", "special": ["morich", "moris"]},

    # Bills & Utilities
    "বিল": {"canonical": "bill", "group": "bills", "special": ["bill", "bil"]},
    "বিদ্যুৎ": {"canonical": "biddut", "group": "bills", "special": ["biddut", "bidyut", "bidut"]},
    "কারেন্ট": {"canonical": "current", "group": "bills", "special": ["current", "karent"]},
    "গ্যাস": {"canonical": "gas", "group": "bills", "special": ["gas"]},
    "পানি": {"canonical": "pani", "group": "bills", "special": ["pani", "paani"]},
    "ওয়াইফাই": {"canonical": "wifi", "group": "bills", "special": ["wifi", "wi-fi"]},
    "ইন্টারনেট": {"canonical": "internet", "group": "bills", "special": ["internet", "net"]},
    "রিচার্জ": {"canonical": "recharge", "group": "bills", "special": ["recharge", "reacharge", "recharg"]},
    "লোড": {"canonical": "load", "group": "bills", "special": ["load", "flaxiload", "flexi"]},
    "বাসা": {"canonical": "basha", "group": "bills", "special": ["basha", "basa", "bashar"]},

    # MFS & Local Digital Platforms
    "বিকাশ": {"canonical": "bkash", "group": "mfs", "special": ["bkash", "bekash", "bikash", "bkas", "bkashe", "bikashe"]},
    "নগদ": {"canonical": "nagad", "group": "mfs", "special": ["nagad", "nogod", "nagade", "nogode"]},
    "রকেট": {"canonical": "rocket", "group": "mfs", "special": ["rocket", "roket"]},
    "উপায়": {"canonical": "upay", "group": "mfs", "special": ["upay", "upai"]},
    "সেলফিন": {"canonical": "cellfin", "group": "mfs", "special": ["cellfin", "cellfin"]},
    "ফুডপান্ডা": {"canonical": "foodpanda", "group": "services", "special": ["foodpanda"]},
    "চালডাল": {"canonical": "chaldal", "group": "services", "special": ["chaldal"]},
    "স্বপ্ন": {"canonical": "shwapno", "group": "services", "special": ["shwapno", "swapno", "shopno"]},
    "দারাজ": {"canonical": "daraz", "group": "shopping", "special": ["daraz"]},

    # Healthcare & Shopping
    "ওষুধ": {"canonical": "osudh", "group": "health", "special": ["osudh", "oshudh", "ousodh", "medicine"]},
    "ডাক্তার": {"canonical": "doctor", "group": "health", "special": ["doctor", "daktar"]},
    "হাসপাতাল": {"canonical": "hospital", "group": "health", "special": ["hospital", "haspatal"]},
    "শপিং": {"canonical": "shopping", "group": "shopping", "special": ["shopping", "shoping"]},
    "জামা": {"canonical": "jama", "group": "shopping", "special": ["jama"]},
    "কাপড়": {"canonical": "kapor", "group": "shopping", "special": ["kapor", "kaporer", "kapod"]},

    # Income & Accounts
    "বেতন": {"canonical": "beton", "group": "income", "special": ["beton", "veton"]},
    "ইনকাম": {"canonical": "income", "group": "income", "special": ["income"]},
    "বোনাস": {"canonical": "bonus", "group": "income", "special": ["bonus"]},
    "ক্যাশব্যাক": {"canonical": "cashback", "group": "income", "special": ["cashback"]},

    # Temporal & Questions
    "আজকে": {"canonical": "ajke", "group": "temporal", "special": ["ajke", "aajke", "ajk", "ajker"]},
    "আজ": {"canonical": "aj", "group": "temporal", "special": ["aj", "aaj"]},
    "কালকে": {"canonical": "kalke", "group": "temporal", "special": ["kalke", "kaalke", "kalk"]},
    "গতকাল": {"canonical": "gotokal", "group": "temporal", "special": ["gotokal", "gotokalke", "gotokaler"]},
    "পরশু": {"canonical": "porshu", "group": "temporal", "special": ["porshu", "porsu"]},
    "তারিখ": {"canonical": "tarikh", "group": "temporal", "special": ["tarikh", "tarik", "tarek", "tarike", "tarikhe"]},
    "কত": {"canonical": "koto", "group": "question", "special": ["koto", "kto"]},
    "হিসাব": {"canonical": "hisab", "group": "question", "special": ["hisab", "hishab"]},
    "ব্যালেন্স": {"canonical": "balance", "group": "question", "special": ["balance", "balace"]},
    "মোট": {"canonical": "mot", "group": "question", "special": ["mot", "moth", "mote"]},
    "সবচেয়ে": {"canonical": "shobcheye", "group": "question", "special": ["shobcheye", "sobcheye", "shobche", "sobche"]},
    "বাকি": {"canonical": "baki", "group": "question", "special": ["baki"]},
}

STOPWORDS = {
    "ami", "amra", "amar", "tumi", "apni", "amader", "apnar", "vai", "bro", "post", "na", "ki",
    "ta", "er", "kore", "e", "a", "to", "valo", "korte", "ar", "plz", "diye", "link", "kora",
    "ei", "ai", "korle", "hobe", "tai", "jodi", "eta", "je", "theke", "dia", "dekhi", "apne",
    "bhai", "vaiya", "kisu", "kichu", "kono", "ekta", "akta", "shob", "sob", "korben", "koren",
    "te", "ye", "jonno", "jnne", "jonne", "hoye", "hole", "parbo", "pari", "ache", "ase", "ace"
}

# ==============================================================================
# Phonetic Profiling & Distance Helpers
# ==============================================================================

def get_banglish_profile(w: str) -> str:
    """Produces a canonical phonetic consonant signature for a Banglish word."""
    w = w.lower().strip()
    w = re.sub(r'bh|v', 'b', w)
    w = re.sub(r'kh', 'k', w)
    w = re.sub(r'gh', 'g', w)
    w = re.sub(r'sh|ch|c|z', 's', w)
    w = re.sub(r'dh', 'd', w)
    w = re.sub(r'ph', 'f', w)
    w = re.sub(r'rh|d', 'r', w)
    w = re.sub(r'j', 's', w)
    w = re.sub(r'(.)\1+', r'\1', w)
    if len(w) > 1:
        first = w[0]
        rest = re.sub(r'[aeiouy]', '', w[1:])
        return first + rest
    return w

def levenshtein_dist(s1: str, s2: str) -> int:
    """Calculates classic Levenshtein distance between two strings."""
    if len(s1) < len(s2):
        return levenshtein_dist(s2, s1)
    if len(s2) == 0:
        return len(s1)
    prev = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        curr = [i + 1]
        for j, c2 in enumerate(s2):
            ins = prev[j + 1] + 1
            delete = curr[j] + 1
            sub = prev[j] + (0 if c1 == c2 else 1)
            curr.append(min(ins, delete, sub))
        prev = curr
    return prev[-1]

def build_regex_pattern(variants: list) -> str:
    """Synthesizes an optimal non-capturing regex with word boundaries."""
    if not variants:
        return ""
    sorted_vars = sorted(list(set(variants)), key=lambda x: len(x), reverse=True)
    escaped = [re.escape(v) for v in sorted_vars]
    if len(escaped) == 1:
        return f"\\b{escaped[0]}\\b"
    return f"\\b(?:{'|'.join(escaped)})\\b"


# ==============================================================================
# Core Parser & Full Transliteration Engine
# ==============================================================================

class BanglaTLitMiner:
    def __init__(self, csv_path: str, min_freq: int = 1, dict_min_count: int = 3):
        self.csv_path = csv_path
        self.min_freq = min_freq
        self.dict_min_count = dict_min_count
        self.total_rows = 0

        # Frequency counters
        self.tlit_token_freq = Counter()
        self.bn_lemma_freq = Counter()
        self.co_occurrences = defaultdict(Counter)

        # Full alignment dictionary: Banglish word -> Counter(Bengali words)
        self.tlit_to_bn_align = defaultdict(Counter)

        # Context around numbers & currency
        self.tokens_after_num = Counter()
        self.tokens_before_num = Counter()
        self.tokens_around_tk = Counter()
        self.unsupervised_entities = Counter()

        # Results
        self.lemma_variants = defaultdict(dict)
        self.confident_dict = {}

    def scan_dataset(self, limit: int = None):
        """Streams and parses the complete CSV parallel dataset (245k+ lines)."""
        print(f"[*] Scanning complete parallel dataset from: {self.csv_path}")
        if not os.path.exists(self.csv_path):
            raise FileNotFoundError(f"CSV file not found: {self.csv_path}")

        num_token_re = re.compile(r'\b(\d+(?:\.\d+)?)\s+([a-z0-9]+)\b', re.IGNORECASE)
        token_num_re = re.compile(r'\b([a-z0-9]+)\s+(\d+(?:\.\d+)?)\b', re.IGNORECASE)
        tk_context_re = re.compile(r'\b([a-z0-9]+)\s+(?:tk|taka|bdt)\b|\b(?:tk|taka|bdt)\s+([a-z0-9]+)\b', re.IGNORECASE)
        unsup_entity_re = re.compile(r'\b([a-z]+)\s+(?:kinlam|kinechi|kinesilam|kinci|khelam|khaisi|kheyechi|dilam|delam|disi|dici|lagse|laglo|lagce|khoroch)\b', re.IGNORECASE)

        clean_en_re = re.compile(r'[a-z0-9]+')
        clean_bn_re = re.compile(r'[\u0980-\u09FF0-9]+')

        with open(self.csv_path, mode='r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for i, row in enumerate(reader):
                if limit and i >= limit:
                    break
                self.total_rows += 1
                bn_text = row.get("text_bengali", "")
                tlit_text = row.get("text_transliterated", "")
                if not bn_text or not tlit_text:
                    continue

                tlit_lower = tlit_text.lower()
                en_tokens = clean_en_re.findall(tlit_lower)
                bn_tokens = clean_bn_re.findall(bn_text)
                len_en = len(en_tokens)
                len_bn = len(bn_tokens)

                if len_en == 0 or len_bn == 0:
                    continue

                # 1. Token frequencies
                for tok in en_tokens:
                    self.tlit_token_freq[tok] += 1

                # 2. Parallel co-occurrences with seed Bengali lemmas
                unique_en = set(en_tokens)
                for bn_lemma in SEED_LEMMAS:
                    if bn_lemma in bn_text:
                        self.bn_lemma_freq[bn_lemma] += 1
                        for tok in unique_en:
                            if len(tok) >= 2 and tok not in STOPWORDS:
                                self.co_occurrences[bn_lemma][tok] += 1

                # 3. Contextual n-grams & Unsupervised entity mining
                for num, tok in num_token_re.findall(tlit_lower):
                    if len(tok) >= 2 and tok not in STOPWORDS:
                        self.tokens_after_num[tok] += 1

                for tok, num in token_num_re.findall(tlit_lower):
                    if len(tok) >= 2 and tok not in STOPWORDS:
                        self.tokens_before_num[tok] += 1

                for pre, post in tk_context_re.findall(tlit_lower):
                    target = pre if pre else post
                    if len(target) >= 2 and target not in STOPWORDS:
                        self.tokens_around_tk[target] += 1

                for ent in unsup_entity_re.findall(tlit_lower):
                    if len(ent) >= 3 and ent not in STOPWORDS:
                        self.unsupervised_entities[ent] += 1

                # 4. Full Transliteration Alignment Mapping
                if len_en == len_bn:
                    for e, b in zip(en_tokens, bn_tokens):
                        self.tlit_to_bn_align[e][b] += 3
                elif abs(len_en - len_bn) <= 2 and len_en <= 14:
                    for idx, e in enumerate(en_tokens):
                        rel = int(round(idx * len_bn / len_en))
                        for offset in (-1, 0, 1):
                            bn_idx = rel + offset
                            if 0 <= bn_idx < len_bn:
                                self.tlit_to_bn_align[e][bn_tokens[bn_idx]] += 1

                if self.total_rows % 50000 == 0:
                    print(f"    ...processed {self.total_rows:,} rows")

        print(f"[✓] Finished scanning all {self.total_rows:,} rows.")

    def mine_variations(self):
        """Identifies and scores valid transliteration variations for each seed lemma."""
        print("[*] Mining phonetic transliteration variations...")

        for bn_lemma, info in SEED_LEMMAS.items():
            canonical = info["canonical"]
            bn_count = self.bn_lemma_freq[bn_lemma]
            specials = set(info.get("special", []))
            canon_profile = get_banglish_profile(canonical)

            candidates = {}

            # Add guaranteed domain variations
            for sp in specials:
                cnt = self.co_occurrences[bn_lemma].get(sp, 0)
                tot = self.tlit_token_freq.get(sp, 0)
                candidates[sp] = {
                    "count": cnt,
                    "total_freq": tot,
                    "score": 1.0,
                    "source": "domain_lexicon"
                }

            # Add canonical word
            candidates[canonical] = {
                "count": self.co_occurrences[bn_lemma].get(canonical, 0),
                "total_freq": self.tlit_token_freq.get(canonical, 0),
                "score": 1.0,
                "source": "canonical"
            }

            # Mine corpus variations
            if bn_count > 0:
                for tok, co_cnt in self.co_occurrences[bn_lemma].items():
                    if co_cnt < self.min_freq or tok in STOPWORDS:
                        continue

                    tok_profile = get_banglish_profile(tok)
                    is_profile_match = (tok_profile == canon_profile)
                    dist = levenshtein_dist(tok, canonical)
                    max_len = max(len(tok), len(canonical))
                    sim = 1.0 - (dist / max(max_len, 1))

                    valid = False
                    if is_profile_match and sim >= 0.40:
                        valid = True
                    elif dist <= 1 and len(tok) >= 3:
                        valid = True

                    if canonical == "taka" and tok in {"thake", "theke", "thik", "take", "tara", "tader"}:
                        valid = False

                    if valid:
                        tok_freq = self.tlit_token_freq[tok]
                        dice = (2.0 * co_cnt) / (bn_count + tok_freq)
                        score = (dice * 0.5) + (sim * 0.5)
                        candidates[tok] = {
                            "count": co_cnt,
                            "total_freq": tok_freq,
                            "score": round(score, 3),
                            "source": "corpus_mined"
                        }

            sorted_candidates = dict(sorted(candidates.items(), key=lambda item: (item[1]["count"], item[1]["score"]), reverse=True))
            self.lemma_variants[bn_lemma] = {
                "canonical": canonical,
                "group": info["group"],
                "variants": sorted_candidates,
                "regex": build_regex_pattern(list(sorted_candidates.keys()))
            }

        print(f"[✓] Successfully mined variations for {len(self.lemma_variants)} lemmas.")

    def build_transliteration_dictionary(self):
        """Constructs confident Banglish -> Bengali word translation table."""
        print("[*] Compiling full transliteration dictionary across 40k+ words...")
        self.confident_dict = {}

        for e, counts in self.tlit_to_bn_align.items():
            total_votes = sum(counts.values())
            if total_votes >= self.dict_min_count:
                best_bn, best_cnt = counts.most_common(1)[0]
                confidence = best_cnt / total_votes
                if confidence >= 0.35:
                    self.confident_dict[e] = {
                        "bengali": best_bn,
                        "confidence": round(confidence, 3),
                        "count": best_cnt,
                        "total_votes": total_votes
                    }

        print(f"[✓] Compiled {len(self.confident_dict):,} confident transliteration word pairs.")

    def lookup_word(self, query: str) -> dict:
        """Looks up a specific Bengali lemma or Banglish token."""
        q = query.strip().lower()

        # Check Bengali lemma
        if query in self.lemma_variants:
            return {
                "query": query,
                "type": "bengali_lemma",
                "data": self.lemma_variants[query]
            }

        # Check dictionary translation
        dict_trans = self.confident_dict.get(q)

        # Check if query matches canonical or variant in mined lemmas
        matched = []
        for bn_lemma, data in self.lemma_variants.items():
            if q == data["canonical"] or q in data["variants"]:
                matched.append((bn_lemma, data))

        return {
            "query": query,
            "transliteration_to_bengali": dict_trans,
            "financial_matches": matched,
            "frequency": self.tlit_token_freq.get(q, 0),
            "co_with_numbers": self.tokens_after_num.get(q, 0) + self.tokens_before_num.get(q, 0),
            "co_with_currency": self.tokens_around_tk.get(q, 0)
        }

    def generate_kotlin_code(self) -> str:
        """Generates clean, production-ready Kotlin code blocks for BanglishNormalizer.kt."""
        # 1. FINANCIAL_ANCHOR_KEYWORDS
        anchor_set = set()
        for bn_lemma, item in self.lemma_variants.items():
            if item["group"] in {"currency", "magnitude", "expense_keyword", "transport", "food", "grocery", "bills", "mfs", "services", "shopping", "health"}:
                for var_word in item["variants"]:
                    if len(var_word) >= 2:
                        anchor_set.add(var_word)

        # Include unsupervised mined entities
        for tok, cnt in self.unsupervised_entities.most_common(60):
            if len(tok) >= 3 and tok not in STOPWORDS and cnt >= 3:
                if tok.isalpha():
                    anchor_set.add(tok)

        sorted_anchors = sorted(list(anchor_set))

        # 2. Normalization Regex replacements
        norm_replacements = []
        for bn_lemma, item in self.lemma_variants.items():
            canonical = item["canonical"]
            variants = [v for v in item["variants"].keys() if v != canonical]
            if variants:
                rgx = build_regex_pattern(variants)
                norm_replacements.append((rgx, canonical, bn_lemma))

        # 3. Clean Expense Note Verbs
        verb_set = set()
        for bn_lemma, item in self.lemma_variants.items():
            if item["group"].startswith("verb_"):
                for var_word in item["variants"]:
                    if var_word.isalpha():
                        verb_set.add(var_word)
        sorted_verbs = sorted(list(verb_set))

        out = []
        out.append("// ==============================================================================")
        out.append("// Auto-generated from BanglaTLit Parallel Corpus (245,727 samples)")
        out.append("// For: BanglishNormalizer.kt & TransactionValidator.kt")
        out.append("// ==============================================================================\n")
        out.append("package com.example.expenseassistant.ai\n")
        out.append("object BanglishCorpusVocabulary {\n")

        # Set: FINANCIAL_ANCHOR_KEYWORDS
        out.append("    // 1. Extended FINANCIAL_ANCHOR_KEYWORDS (Unsupervised + Seeded)")
        out.append("    val EXTENDED_FINANCIAL_ANCHORS: Set<String> = setOf(")
        for i in range(0, len(sorted_anchors), 8):
            chunk = sorted_anchors[i:i+8]
            out.append('        ' + ', '.join(f'"{w}"' for w in chunk) + (',' if i+8 < len(sorted_anchors) else ''))
        out.append("    )\n")

        # Set: ACTION_VERBS
        out.append("    // 2. Comprehensive Banglish Transaction Verbs")
        out.append("    val ACTION_VERBS: Set<String> = setOf(")
        for i in range(0, len(sorted_verbs), 8):
            chunk = sorted_verbs[i:i+8]
            out.append('        ' + ', '.join(f'"{w}"' for w in chunk) + (',' if i+8 < len(sorted_verbs) else ''))
        out.append("    )\n")

        # Function: applyLearnedNormalizations
        out.append("    // 3. Normalization Replacements for normalize(input: String)")
        out.append("    fun applyLearnedNormalizations(raw: String): String {")
        out.append("        var text = raw")
        for rgx, canonical, bn in norm_replacements:
            escaped_rgx = rgx.replace('\\', '\\\\')
            out.append(f'        // Bengali: {bn} -> {canonical}')
            out.append(f'        text = text.replace(Regex("{escaped_rgx}", RegexOption.IGNORE_CASE), "{canonical}")')
        out.append("        return text")
        out.append("    }\n")
        out.append("}\n")
        return "\n".join(out)

    def generate_kotlin_transliterator(self, top_n: int = 4000) -> str:
        """Generates an offline Banglish -> Bengali word transliteration object in Kotlin."""
        final_pairs = {}

        # 1. First guarantee all domain financial words from SEED_LEMMAS are present
        for bn_lemma, info in SEED_LEMMAS.items():
            canon = info["canonical"]
            final_pairs[canon] = bn_lemma
            for sp in info.get("special", []):
                final_pairs[sp] = bn_lemma

        # 2. Add top confident dictionary entries up to top_n
        sorted_pairs = sorted(self.confident_dict.items(), key=lambda x: x[1]["total_votes"], reverse=True)
        for k, v in sorted_pairs:
            if k not in final_pairs and len(final_pairs) < top_n:
                final_pairs[k] = v["bengali"]

        out = []
        out.append("// ==============================================================================")
        out.append(f"// Auto-generated Offline Banglish -> Bengali Transliterator ({len(final_pairs)} vocabulary entries)")
        out.append("// Source: BanglaTLit 245k parallel corpus (prioritizing financial/daily domain)")
        out.append("// ==============================================================================\n")
        out.append("package com.example.expenseassistant.ai\n")
        out.append("object BanglishTransliterator {\n")
        out.append(f"    // Top {len(final_pairs)} confident transliterated word pairs")
        out.append("    private val DICTIONARY: Map<String, String> = mapOf(")
        
        pair_list = list(final_pairs.items())
        for i in range(0, len(pair_list), 4):
            chunk = pair_list[i:i+4]
            lines = [f'"{k}" to "{bn}"' for k, bn in chunk]
            out.append("        " + ", ".join(lines) + ("," if i+4 < len(pair_list) else ""))
        out.append("    )\n")

        out.append("""    /**
     * Translates a single Banglish token to its authentic Bengali script word.
     */
    fun transliterateWord(token: String): String {
        val lower = token.lowercase().trim()
        return DICTIONARY[lower] ?: token
    }

    /**
     * Translates a full Banglish sentence into Bengali script token by token.
     * Keeps ASCII digits as digits unless transliterateDigits is set to true.
     */
    fun transliterateSentence(sentence: String, transliterateDigits: Boolean = false): String {
        val regex = Regex("[a-zA-Z0-9]+|[^a-zA-Z0-9\\\\s]+|\\\\s+")
        return regex.replace(sentence) { matchResult ->
            val tok = matchResult.value
            if (tok.matches(Regex("[0-9]+"))) {
                if (transliterateDigits) DICTIONARY[tok] ?: tok else tok
            } else if (tok.matches(Regex("[a-zA-Z]+"))) {
                transliterateWord(tok)
            } else {
                tok
            }
        }
    }
}
""")
        return "\n".join(out)

    def export_artifacts(self, output_dir: str):
        """Exports all dictionaries, reports, and Kotlin code."""
        out_path = Path(output_dir)
        out_path.mkdir(parents=True, exist_ok=True)

        # 1. Full Transliteration Dictionary JSON (37,000+ words)
        dict_file = out_path / "banglish_to_bengali_full.json"
        with open(dict_file, "w", encoding="utf-8") as f:
            json.dump(self.confident_dict, f, ensure_ascii=False, indent=2)
        print(f"[✓] Saved full transliteration dictionary ({len(self.confident_dict):,} words): {dict_file}")

        # 2. Vocabulary & Regex JSON
        json_file = out_path / "banglish_vocabulary.json"
        with open(json_file, "w", encoding="utf-8") as f:
            json.dump({
                "total_rows_scanned": self.total_rows,
                "lemmas": self.lemma_variants,
                "unsupervised_entities": self.unsupervised_entities.most_common(100),
                "top_tokens_after_numbers": self.tokens_after_num.most_common(50),
                "top_tokens_around_currency": self.tokens_around_tk.most_common(50)
            }, f, ensure_ascii=False, indent=2)
        print(f"[✓] Saved JSON dictionary: {json_file}")

        # 3. Kotlin Normalizer Vocabulary
        kt_vocab_file = out_path / "BanglishCorpusVocabulary.kt"
        with open(kt_vocab_file, "w", encoding="utf-8") as f:
            f.write(self.generate_kotlin_code())
        print(f"[✓] Saved Kotlin vocabulary snippet: {kt_vocab_file}")

        # 4. Kotlin Transliterator Object
        kt_trans_file = out_path / "BanglishTransliterator.kt"
        with open(kt_trans_file, "w", encoding="utf-8") as f:
            f.write(self.generate_kotlin_transliterator(top_n=3500))
        print(f"[✓] Saved Kotlin Transliterator ({kt_trans_file})")

        # 5. Markdown Report
        md_file = out_path / "banglish_vocab_report.md"
        with open(md_file, "w", encoding="utf-8") as f:
            f.write(self._generate_markdown_report())
        print(f"[✓] Saved Markdown report: {md_file}")

    def _generate_markdown_report(self) -> str:
        lines = []
        lines.append("# BanglaTLit Transliteration & Vocabulary Analysis Report")
        lines.append(f"\n- **Total Parallel Rows Scanned:** {self.total_rows:,}")
        lines.append(f"- **Unique Transliterated Tokens Observed:** {len(self.tlit_token_freq):,}")
        lines.append(f"- **Aligned Confident Transliteration Dictionary:** {len(self.confident_dict):,} words")
        lines.append(f"- **Bengali Financial Lemmas Tracked:** {len(self.lemma_variants)}")

        lines.append("\n## 1. Mined Transliteration Variants & Synthesized Regexes\n")
        lines.append("| Bengali Lemma | Canonical | Group | Variants Discovered | Synthesized Regex |")
        lines.append("|---|---|---|---|---|")
        for bn_lemma, item in sorted(self.lemma_variants.items(), key=lambda x: (x[1]["group"], x[0])):
            vars_str = ", ".join([f"`{v}` ({d['count']})" for v, d in list(item["variants"].items())[:6]])
            regex_str = f"`{item['regex']}`"
            lines.append(f"| {bn_lemma} | **{item['canonical']}** | `{item['group']}` | {vars_str} | {regex_str} |")

        lines.append("\n## 2. Top Unsupervised Financial Entities\n")
        lines.append("Items frequently appearing with transaction verbs (`kinlam`, `khelam`, `dilam`, `lagse`, `khoroch`):")
        lines.append("\n| Entity / Item | Frequency |")
        lines.append("|---|---|")
        for ent, cnt in self.unsupervised_entities.most_common(30):
            lines.append(f"| `{ent}` | {cnt:,} |")

        lines.append("\n## 3. High Frequency Tokens Around Currency (`tk`, `taka`)\n")
        lines.append("\n| Token | Frequency Around `tk`/`taka` |")
        lines.append("|---|---|")
        for tok, cnt in self.tokens_around_tk.most_common(25):
            lines.append(f"| `{tok}` | {cnt:,} |")

        return "\n".join(lines)


# ==============================================================================
# CLI Entrypoint
# ==============================================================================

def main():
    parser = argparse.ArgumentParser(description="Mine Banglish vocabulary & build transliteration dictionary from BanglaTLit corpus.")
    parser.add_argument("--csv", type=str, default="/Users/shaunhossain/downloads/BanglaTLit_train.csv",
                        help="Path to BanglaTLit_train.csv")
    parser.add_argument("--output-dir", type=str, default="scripts/output",
                        help="Directory to save output artifacts")
    parser.add_argument("--limit", type=int, default=None,
                        help="Limit number of rows for testing (default: all)")
    parser.add_argument("--query", type=str, default=None,
                        help="Query a specific Bengali or Banglish word")
    parser.add_argument("--min-freq", type=int, default=1,
                        help="Minimum co-occurrence frequency to accept")
    parser.add_argument("--dict-min-count", type=int, default=3,
                        help="Minimum co-occurrence count for dictionary entries")

    args = parser.parse_args()

    miner = BanglaTLitMiner(csv_path=args.csv, min_freq=args.min_freq, dict_min_count=args.dict_min_count)
    miner.scan_dataset(limit=args.limit)
    miner.mine_variations()
    miner.build_transliteration_dictionary()

    if args.query:
        print(f"\n[?] Querying: {args.query}")
        result = miner.lookup_word(args.query)
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    # Export all artifacts
    miner.export_artifacts(args.output_dir)
    print("\n[✓] All analysis & dictionary compilation tasks complete!")

if __name__ == "__main__":
    main()
