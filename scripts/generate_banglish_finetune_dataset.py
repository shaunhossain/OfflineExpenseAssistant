#!/usr/bin/env python3
"""
Banglish + Bengali + English Dataset Generator for Gemma 3 270M LoRA Fine-Tuning.

Generates instruction-tuning JSONL datasets tailored to OfflineExpenseAssistant's
exact system prompt and single-line JSON tool schemas:
- prepare_expense
- prepare_multiple_expenses
- prepare_income
- get_spending_summary
- get_item_total
- get_balance
- get_highest_expense
- set_reminder
- list_reminders
- delete_reminder
- toggle_reminder
- delete_last_transaction
- delete_transaction_by_id
- get_recent_transactions
- get_database_stats
- clear_all_transactions
- Conversational financial advice in Banglish / English

Usage:
    python3 scripts/generate_banglish_finetune_dataset.py \
        --output scripts/banglish_expense_finetune.jsonl \
        --num-samples 2500
"""

import argparse
import json
import random
from pathlib import Path
from typing import Dict, List, Tuple

SYSTEM_PROMPT = """You are an offline personal finance and expense assistant in Bangladesh.
Understand English, Bangla, and Banglish (e.g., tk, taka, khoroch, vara, bazar, nasta, bepari, dokan, koto, ajke, ei mashe).
If the user input includes a [Banglish Semantic Bridge] block, use both the original Banglish text and the English/Bengali bridge to accurately determine the intent, amount, category, and note.
Respond ONLY with a single-line JSON object for actions, or a short plain text answer for general advice.
Do NOT wrap JSON in markdown code blocks.

Valid categories: Food, Grocery, Transport, Shopping, Bills, Health, Entertainment, Education, Other.

Schemas:
1. Single Expense:
{"intent":"prepare_expense","amount":50.0,"category":"Transport","note":"bus vara"}
2. Multiple Expenses (when user lists 2+ items):
{"intent":"prepare_multiple_expenses","expenses":[{"amount":50.0,"category":"Food","note":"sosage"},{"amount":320.0,"category":"Grocery","note":"murgi"}]}
3. Income:
{"intent":"prepare_income","amount":30000.0,"category":"Salary","note":"monthly salary"}
4. Category/Period Summary:
{"intent":"get_spending_summary","period":"this_month","category":"Transport"}
(Valid periods: today, yesterday, this_week, this_month, all_time. Omit category or set null for all categories.)
5. Specific Item Total (when user asks how much spent on a specific item like bus vara, bazar, cha):
{"intent":"get_item_total","period":"this_month","query":"bus vara"}
6. Check Balance / Remaining Money:
{"intent":"get_balance","period":"this_month"}
7. Highest / Biggest Expense:
{"intent":"get_highest_expense","period":"this_month"}
8. Daily Reminder:
{"intent":"set_reminder","time":"21:00","message":"Log today's expenses"}
9. List Reminders:
{"intent":"list_reminders"}
10. Delete Reminder:
{"intent":"delete_reminder","id":1}
11. Toggle Reminder:
{"intent":"toggle_reminder","id":1,"active":false}
12. Delete Last Transaction:
{"intent":"delete_last_transaction"}
13. Delete Transaction By ID:
{"intent":"delete_transaction_by_id","id":12}
14. Recent Transactions:
{"intent":"get_recent_transactions","limit":5}
15. Database Stats & Overview:
{"intent":"get_database_stats"}
16. Clear All Transactions:
{"intent":"clear_all_transactions"}"""


# Category items with Banglish spelling variants and English glosses
CATEGORY_ITEMS: Dict[str, List[Tuple[str, str]]] = {
    "Transport": [
        ("bus vara", "bus fare"),
        ("bas bhara", "bus fare"),
        ("rickshaw vara", "rickshaw fare"),
        ("riksa vara", "rickshaw fare"),
        ("riksha bhara", "rickshaw fare"),
        ("cng vara", "CNG auto-rickshaw fare"),
        ("auto vara", "auto fare"),
        ("pathao", "Pathao ride"),
        ("uber", "Uber ride"),
        ("metro rail", "metro rail ticket"),
        ("train ticket", "train ticket"),
        ("launch vara", "launch fare"),
        ("nouka vara", "boat fare"),
        ("octen", "octane fuel"),
        ("petrol", "petrol fuel"),
        ("garage vara", "garage parking fee"),
        ("brta fee", "vehicle registration BRTA fee"),
        ("bridge toll", "highway bridge toll fee"),
        ("bike servicing", "motorcycle servicing and maintenance"),
        ("chaka puncture", "tyre puncture repair"),
        ("mobil change", "engine oil replacement"),
        ("pickup vara", "pickup truck transport fare"),
        ("truck vara", "truck transportation fare"),
        ("coolie charge", "porter coolie charge"),
        ("labour charge", "manual labour shifting charge"),
        ("traffic case", "traffic police fine"),
        ("sergeant fine", "traffic sergeant fine"),
        ("biman ticket", "airline flight ticket"),
        ("bike wash", "motorcycle foam wash"),
        ("bike e tel vorlam", "refueled motorcycle petrol"),
        ("chaka e hawa dilam", "tyre air inflation"),
        ("mobil filter bodlalam", "motorcycle oil filter replacement"),
        ("helmet kinlam", "bike safety helmet"),
    ],
    "Food": [
        ("cha nasta", "tea and snacks"),
        ("cha ar singara", "tea and samosa"),
        ("sokal er nasta", "morning breakfast"),
        ("dupur er khabar", "lunch meal"),
        ("raat er khabar", "dinner meal"),
        ("kacchi biryani", "kacchi biryani"),
        ("biriyani", "biryani"),
        ("tehari", "tehari"),
        ("khichuri", "khichuri"),
        ("fuchka", "fuchka street snack"),
        ("chotpoti", "chotpoti"),
        ("jhalmuri", "jhalmuri"),
        ("shingara", "samosa snack"),
        ("somucha", "samosa"),
        ("paratha ar dim", "paratha and egg"),
        ("burger", "burger"),
        ("pizza", "pizza"),
        ("coffee", "coffee"),
        ("misty", "sweet dessert"),
        ("doi", "yogurt"),
        ("ice cream", "ice cream"),
        ("borhani", "borhani yogurt drink"),
        ("daab er pani", "fresh green coconut water"),
        ("bakorkhani kinlam", "Old Dhaka bakorkhani bread"),
        ("jilapi ar cha", "jilapi sweet and tea"),
        ("panipuri khelam", "panipuri fuchka snack"),
        ("morog polao", "morog polao rice dish"),
        ("cholar boot ar badam", "fried chickpeas and peanuts"),
        ("foodpanda order", "Foodpanda delivery meal"),
        ("canteen bill", "campus canteen food"),
        ("dining bill", "mess dining hall meal"),
        ("meal charge", "bachelor mess meal cost"),
        ("tong er cha", "local tea stall tea"),
        ("biri ar cha", "cigarettes and tea"),
        ("treat dilam", "restaurant treat to friends"),
    ],
    "Grocery": [
        ("kacha bazar", "raw grocery market"),
        ("bazar", "grocery shopping"),
        ("murgi", "chicken"),
        ("gorur mangsho", "beef"),
        ("khashir mangsho", "mutton"),
        ("mach", "fish"),
        ("ilish mach", "hilsa fish"),
        ("rui mach", "rohu fish"),
        ("dim", "eggs"),
        ("dudh", "milk"),
        ("chal", "rice"),
        ("miniket chal", "miniket rice"),
        ("ata", "flour"),
        ("moyda", "refined flour"),
        ("soyabin tel", "soybean cooking oil"),
        ("shobji", "vegetables"),
        ("alu peyaj", "potato and onion"),
        ("rosun ada", "garlic and ginger"),
        ("chini ar lobon", "sugar and salt"),
        ("masala", "spices"),
        ("muri chanachur", "puffed rice and snacks"),
        ("harpic", "toilet cleaning liquid"),
        ("vim bar", "dishwashing soap bar"),
        ("sar ar beej", "agricultural fertilizer and seeds"),
        ("keetnashok", "agricultural pesticide"),
        ("gorur bhushi", "cattle wheat bran feed"),
        ("murgir feed", "poultry farm feed"),
        ("khejurer gur", "date palm winter jaggery"),
        ("lichu kinlam", "fresh seasonal litchi"),
    ],
    "Bills": [
        ("basha vara", "house rent"),
        ("room vara", "room rent"),
        ("mess vara", "mess rent"),
        ("current bill", "electricity bill"),
        ("bidyut bill", "electricity bill"),
        ("palli bidyut bill", "rural electricity bill"),
        ("desco bill", "DESCO electricity bill"),
        ("wifi bill", "WiFi internet bill"),
        ("net bill", "internet bill"),
        ("gas bill", "gas utility bill"),
        ("cylinder gas", "LPG cylinder gas"),
        ("pani bill", "water utility bill"),
        ("wasa bill", "WASA water bill"),
        ("mobile recharge", "mobile balance recharge"),
        ("flexiload", "mobile recharge"),
        ("grameenphone recharge", "GP mobile recharge"),
        ("bua bill", "housemaid salary"),
        ("khala bill", "cook salary"),
        ("moyla bill", "garbage collection bill"),
        ("darowan bill", "security guard fee"),
        ("kisti dilam", "loan installment / EMI"),
        ("dps er kisti", "DPS monthly savings deposit"),
        ("advance dilam", "advance rent deposit"),
        ("credit card bill", "credit card monthly bill"),
        ("domain renewal", "website domain renewal fee"),
        ("hosting bill", "web hosting subscription"),
        ("electrician bill", "electrician repair charge"),
        ("plumber fee", "plumber sanitary repair fee"),
        ("ukil fee", "lawyer advocate legal fee"),
        ("court fee", "court legal fee"),
        ("notary fee", "notary public attestation fee"),
        ("stamp paper", "judicial stamp paper"),
        ("passport fee", "e-passport government fee"),
        ("laundry bill", "laundry clothes washing bill"),
        ("dhobi bill", "washerman dhobi bill"),
        ("iron kora", "ironing clothes charges"),
        ("samity kisti", "cooperative society installment"),
        ("cement er bosta", "construction cement bag"),
        ("rajmistri hajira", "construction mason daily wage"),
        ("manpower fee", "overseas manpower clearance fee"),
        ("koshai hajira", "Eid butcher wage"),
        ("tanki saf koralam", "water tank cleaning charges"),
        ("daroan er beton", "security guard monthly wage"),
        ("ips battery servicing", "IPS backup battery servicing"),
    ],
    "Shopping": [
        ("jama kapor", "clothes shopping"),
        ("shirt", "shirt"),
        ("pant", "pants"),
        ("panjabi", "panjabi"),
        ("sharee", "saree"),
        ("salwar kameez", "salwar kameez"),
        ("juta", "shoes"),
        ("sandal", "sandals"),
        ("bag", "bag"),
        ("ghori", "watch"),
        ("choshma", "glasses"),
        ("cosmetics", "cosmetics"),
        ("shaban ar shampoo", "soap and shampoo"),
        ("daraz parcel", "Daraz online shopping order"),
        ("delivery charge", "home delivery charge"),
        ("courier charge", "courier parcel fee"),
        ("eid shopping", "Eid festive shopping"),
        ("saloon chul kata", "barber haircut and grooming"),
        ("mobile display change", "smartphone display screen replacement"),
        ("battery kinlam", "mobile phone battery"),
        ("cat food", "pet cat food"),
        ("diaper", "baby diapers"),
        ("pampers", "baby diapers"),
        ("cerelac", "baby cereal food"),
        ("feeder", "baby feeding bottle"),
        ("darji bill", "tailor stitching charge"),
        ("tailor majuri", "tailor wage for clothes"),
        ("saree fall piko", "saree border fall and piko"),
        ("shoe polish ar selai", "shoe polish and cobbler stitching"),
        ("tempered glass ar cover", "phone back cover and tempered glass"),
        ("chador ar balish", "bedsheet and pillows"),
        ("porda kinlam", "curtains for windows"),
        ("kambal kinlam", "warm winter blanket"),
        ("lep banalam", "handmade winter cotton quilt"),
        ("sweater kinlam", "warm winter sweater"),
        ("tala chabi banalam", "duplicate lock key making"),
        ("hair cut ar shave", "barber haircut and shave"),
        ("facial koralam", "beauty parlour facial"),
        ("mehedi dilam", "festive henna mehendi art"),
        ("multiplug kinlam", "electrical multiplug strip"),
        ("khelna kinlam", "children toys"),
        ("blender kinlam", "kitchen electric blender"),
    ],
    "Health": [
        ("oshudh", "medicine"),
        ("osud", "medicine"),
        ("pharmacy", "pharmacy medicine"),
        ("daktar fee", "doctor consultation fee"),
        ("doctor visit", "doctor visit"),
        ("blood test", "medical blood test"),
        ("hospital bill", "hospital bill"),
        ("dental checkup", "dental checkup"),
        ("chokher daktar", "eye doctor"),
        ("napa extra", "paracetamol medicine"),
        ("gastric er oshudh", "gastric medicine"),
        ("ecg test", "ECG medical diagnostic test"),
        ("ultrasono", "ultrasonography diagnostic test"),
        ("physiotherapy session", "physiotherapy treatment session"),
        ("gamca medical", "overseas GAMCA medical checkup"),
        ("homeopathy oshudh", "homeopathy medicine and clinic"),
        ("savlon ar hexisol", "antiseptic savlon and hand rub"),
        ("inhaler kinlam", "respiratory inhaler medicine"),
        ("cbc ar urine test", "CBC blood and urine medical test"),
        ("creatinine test", "kidney creatinine lab test"),
        ("bandage ar cotton", "wound bandage gauze and cotton"),
    ],
    "Entertainment": [
        ("cinema ticket", "movie ticket"),
        ("movie dekha", "watching movie"),
        ("star cineplex ticket", "Star Cineplex movie ticket"),
        ("netflix subscription", "Netflix subscription"),
        ("spotify subscription", "Spotify music streaming"),
        ("picnic chanda", "picnic contribution"),
        ("tour khoroch", "travel tour expense"),
        ("addar khoroch", "hangout expense"),
        ("khela dekha", "sports ticket"),
        ("gym fee", "monthly fitness gym membership"),
        ("spa massage", "wellness spa massage"),
        ("biye stage", "wedding stage decoration"),
        ("gaye holud stage", "gaye holud event stage"),
        ("catering bill", "wedding catering food service"),
        ("photographer fee", "event wedding photography fee"),
        ("turf vara", "sports football turf rental"),
        ("futsal booking", "futsal court hourly booking"),
        ("diamond topup", "Free Fire game diamond topup"),
        ("badminton court", "badminton court play fee"),
    ],
    "Education": [
        ("boi kena", "buying books"),
        ("khata kolom", "notebook and pen"),
        ("coaching fee", "coaching center fee"),
        ("varsity fee", "university tuition fee"),
        ("semester fee", "semester tuition fee"),
        ("exam fee", "examination fee"),
        ("photocopy ar print", "photocopy and printing"),
        ("admission fee", "admission fee"),
        ("form fillup", "exam form fillup fee"),
        ("admit card fee", "admit card processing fee"),
        ("registration fee", "student registration fee"),
        ("spiral binding", "thesis and document spiral binding"),
        ("lamination", "document plastic lamination"),
        ("board challenge fee", "board exam re-check challenge fee"),
        ("transcript fee", "university academic transcript fee"),
        ("convocation fee", "university convocation registration fee"),
    ],
    "Other": [
        ("ma ke bkash", "bKash transfer to mother"),
        ("baba ke nagad", "Nagad transfer to father"),
        ("choto bhai ke pocket money", "pocket money to younger brother"),
        ("bondhu ke dhar dilam", "lent money to friend"),
        ("dan korlam", "charity donation"),
        ("mosjid e chanda", "mosque donation"),
        ("masjid e dan", "mosque donation"),
        ("madrasa dan", "madrasa donation"),
        ("sadakah dilam", "Sadakah charity"),
        ("fitra dilam", "Fitra charity"),
        ("qurbani haat er hashil", "Qurbani market fee"),
        ("gift kena", "buying gift"),
        ("salami dilam", "Eid salami gift money"),
        ("bokshish dilam", "delivery tip and baksheesh"),
    ],
}

BANGLISH_EXPENSE_TEMPLATES = [
    "{amount} tk {item}",
    "{amount} taka {item}",
    "{amount}tk {item}",
    "{amount} teha {item}",
    "{amount} tyaha {item}",
    "{item} {amount} tk",
    "{item} {amount} taka",
    "{item} {amount} teha",
    "{item} e {amount} tk khoroch",
    "{item} te {amount} tk laglo",
    "{item} babod {amount} taka gelo",
    "ajke {item} {amount} tk",
    "gotokal {item} {amount} tk",
    "ami {amount} tk diye {item} kinechi",
    "{amount} tk {item} korlam",
    "{item} er jonno {amount} tk dilam",
    "{item} e {amount} tk diasilam",
    "aj {amount} taka {item} hoilo",
    "{item} lagse {amount} tk",
    "save this: {amount} tk {item}",
    "save {amount} tk {item}",
    "likhe rakho {amount} taka {item}",
    "khatai tolo {amount} taka {item}",
    "note kore rakho {amount} tk {item}",
    "entry koro {amount} taka {item}",
    "tukiye rakho {amount} tk {item}",
    "hishab e tolo {amount} tk {item}",
    "{amount} taka {item} khatai tolo",
    "{amount} taka {item} likhe rakho",
    "{amount} tk {item} note kore rakho",
    "{amount} taka {item} entry koro",
    "সেভ করো {amount} টাকা {item}",
    "খাতায় তোলো {amount} টাকা {item}",
    "লিখে রাখো {amount} টাকা {item}",
    "তুলে রাখো {amount} টাকা {item}",
    "হিসাব রাখো {amount} টাকা {item}",
]

SPOKEN_NUMBER_EXAMPLES = [
    ("der", 1.5),
    ("arai", 2.5),
    ("dersho", 150.0),
    ("araisho", 250.0),
    ("barosho", 1200.0),
    ("ponerosho", 1500.0),
    ("eksho", 100.0),
    ("duisho", 200.0),
    ("tinsho", 300.0),
    ("charsho", 400.0),
    ("pachsho", 500.0),
    ("ek hajar", 1000.0),
    ("dui hajar", 2000.0),
    ("pach hajar", 5000.0),
    ("der hajar", 1500.0),
    ("arai hajar", 2500.0),
    ("ponchash", 50.0),
    ("chollish", 40.0),
    ("trish", 30.0),
    ("bish", 20.0),
    ("shaat", 60.0),
    ("shottor", 70.0),
    ("ashi", 80.0),
    ("nobboi", 90.0),
]

INCOME_SOURCES = [
    ("salary", "Salary", "monthly salary"),
    ("beton", "Salary", "beton"),
    ("office theke beton", "Salary", "office theke beton"),
    ("freelancing", "Freelance", "freelancing income"),
    ("upwork theke", "Freelance", "upwork income"),
    ("fiverr theke", "Freelance", "fiverr income"),
    ("tuition fee", "Tuition", "tuition income"),
    ("tushoni", "Tuition", "tushoni"),
    ("bonus", "Bonus", "festival bonus"),
    ("eid bonus", "Bonus", "eid bonus"),
    ("baba dilo", "Allowance", "baba dilo"),
    ("ma dilo", "Allowance", "ma dilo"),
    ("pocket money", "Allowance", "pocket money"),
    ("dhar ferot", "Refund", "dhar ferot"),
    ("cashback", "Cashback", "cashback"),
    ("lav", "Business", "business profit"),
]

INCOME_TEMPLATES = [
    "{source} paisi {amount} tk",
    "{source} pelam {amount} taka",
    "ajke {amount} tk {source} asche",
    "{amount} tk {source} peyechi",
    "amar {source} {amount} tk dhukse",
    "{source} babod {amount} tk add koro",
]

PERIOD_PHRASES = [
    ("ajke", "today"),
    ("ajker", "today"),
    ("gotokal", "yesterday"),
    ("kal", "yesterday"),
    ("ei soptaho", "this_week"),
    ("ai week a", "this_week"),
    ("ei mashe", "this_month"),
    ("ai mase", "this_month"),
    ("mot", "all_time"),
    ("shob miliye", "all_time"),
]


def format_gemma3_turn(user_text: str, assistant_text: str, include_system: bool = True) -> Dict:
    """Formats a single training example in both messages list and Gemma 3 chat template format."""
    sys_prefix = f"{SYSTEM_PROMPT}\n\n" if include_system else ""
    full_prompt = (
        f"<start_of_turn>user\n"
        f"{sys_prefix}{user_text}<end_of_turn>\n"
        f"<start_of_turn>model\n"
        f"{assistant_text}<end_of_turn>"
    )
    return {
        "text": full_prompt,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_text},
            {"role": "assistant", "content": assistant_text},
        ],
    }


def generate_single_expense_samples(rng: random.Random, count: int) -> List[Dict]:
    samples = []
    categories = list(CATEGORY_ITEMS.keys())
    for _ in range(count):
        cat = rng.choice(categories)
        item, gloss = rng.choice(CATEGORY_ITEMS[cat])
        amount = float(rng.choice([10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 100, 120, 150, 180, 200, 250, 300, 350, 450, 500, 650, 800, 1000, 1200, 1500, 2500, 5000]))
        template = rng.choice(BANGLISH_EXPENSE_TEMPLATES)
        user_input = template.format(amount=int(amount), item=item)

        # Optionally include semantic bridge in 40% of examples so model learns both raw & bridged prompts
        if rng.random() < 0.4:
            user_input = (
                f"{user_input}\n"
                f'[Banglish Semantic Bridge: EnglishMeaning="{int(amount)} BDT {gloss}", '
                f'SuggestedIntent="prepare_expense", AmountBDT={amount}, Category="{cat}", Note="{item}"]'
            )

        target_json = json.dumps(
            {
                "intent": "prepare_expense",
                "amount": amount,
                "category": cat,
                "note": item,
            },
            separators=(",", ":"),
        )
        samples.append(format_gemma3_turn(user_input, target_json))

    # Add spoken number samples
    for spoken, val in SPOKEN_NUMBER_EXAMPLES:
        for cat in categories:
            item, _ = rng.choice(CATEGORY_ITEMS[cat])
            user_input = f"{spoken} tk {item}"
            target_json = json.dumps(
                {
                    "intent": "prepare_expense",
                    "amount": val,
                    "category": cat,
                    "note": item,
                },
                separators=(",", ":"),
            )
            samples.append(format_gemma3_turn(user_input, target_json))

    return samples


def generate_multi_expense_samples(rng: random.Random, count: int) -> List[Dict]:
    samples = []
    categories = list(CATEGORY_ITEMS.keys())
    connectors = [", ", " ar ", " ebong ", " + ", " এবং ", " আর "]
    for _ in range(count):
        num_items = rng.choice([2, 3])
        chosen = []
        parts = []
        for _ in range(num_items):
            cat = rng.choice(categories)
            item, _ = rng.choice(CATEGORY_ITEMS[cat])
            amount = float(rng.choice([20, 30, 50, 80, 100, 150, 200, 320, 420, 500]))
            if rng.random() < 0.5:
                parts.append(f"{int(amount)} tk {item}")
            else:
                parts.append(f"{item} {int(amount)} tk")
            chosen.append({"amount": amount, "category": cat, "note": item})

        conn = rng.choice(connectors)
        user_input = conn.join(parts)
        target_json = json.dumps(
            {
                "intent": "prepare_multiple_expenses",
                "expenses": chosen,
            },
            separators=(",", ":"),
        )
        samples.append(format_gemma3_turn(user_input, target_json))
    return samples


def generate_income_samples(rng: random.Random, count: int) -> List[Dict]:
    samples = []
    for _ in range(count):
        source, cat, note = rng.choice(INCOME_SOURCES)
        amount = float(rng.choice([500, 1000, 2000, 5000, 10000, 15000, 25000, 30000, 45000, 50000, 60000]))
        template = rng.choice(INCOME_TEMPLATES)
        user_input = template.format(source=source, amount=int(amount))
        target_json = json.dumps(
            {
                "intent": "prepare_income",
                "amount": amount,
                "category": cat,
                "note": note,
            },
            separators=(",", ":"),
        )
        samples.append(format_gemma3_turn(user_input, target_json))
    return samples


def generate_query_samples(rng: random.Random, count: int) -> List[Dict]:
    samples = []
    categories = list(CATEGORY_ITEMS.keys())

    query_templates = [
        "{period_bn} {item} te koto khoroch hoise",
        "{period_bn} total {item} koto",
        "{item} a total koto cost hoiche {period_bn}",
        "{item} babod koto geche {period_bn}",
        "{item} er hisab dao {period_bn}",
        "total {item} koto lagse",
    ]

    for _ in range(count // 2):
        cat = rng.choice(categories)
        item, _ = rng.choice(CATEGORY_ITEMS[cat])
        period_bn, period_en = rng.choice(PERIOD_PHRASES)
        tpl = rng.choice(query_templates)
        user_input = tpl.format(period_bn=period_bn, item=item)
        target_json = json.dumps(
            {
                "intent": "get_item_total",
                "period": period_en if "period_bn" in tpl else "this_month",
                "query": item,
            },
            separators=(",", ":"),
        )
        samples.append(format_gemma3_turn(user_input, target_json))

    summary_templates = [
        ("{period_bn} amar total khoroch koto", None),
        ("{period_bn} koto taka khoroch hoise", None),
        ("{period_bn} hisab dekhao", None),
        ("{period_bn} khabar e koto khoroch", "Food"),
        ("{period_bn} jatayat e koto geche", "Transport"),
        ("{period_bn} bazar e mot koto laglo", "Grocery"),
        ("{period_bn} bill babod koto gelo", "Bills"),
        ("{period_bn} chikitsha babod koto khoroch", "Health"),
    ]
    for _ in range(count // 4):
        period_bn, period_en = rng.choice(PERIOD_PHRASES)
        tpl, cat = rng.choice(summary_templates)
        user_input = tpl.format(period_bn=period_bn)
        payload = {"intent": "get_spending_summary", "period": period_en}
        if cat:
            payload["category"] = cat
        target_json = json.dumps(payload, separators=(",", ":"))
        samples.append(format_gemma3_turn(user_input, target_json))

    # Balance & Highest Expense & Reminders & DB management
    extra_pairs = [
        ("amar balance koto", {"intent": "get_balance", "period": "this_month"}),
        ("hate koto taka baki ache", {"intent": "get_balance", "period": "this_month"}),
        ("account e koto taka ache", {"intent": "get_balance", "period": "this_month"}),
        ("koto taka save holo", {"intent": "get_balance", "period": "this_month"}),
        ("koto save korlam ei mashe", {"intent": "get_balance", "period": "this_month"}),
        ("amar savings koto ache", {"intent": "get_balance", "period": "this_month"}),
        ("ei mashe koto taka jomse", {"intent": "get_balance", "period": "this_month"}),
        ("koto taka jomlo", {"intent": "get_balance", "period": "this_month"}),
        ("koto taka bachlo", {"intent": "get_balance", "period": "this_month"}),
        ("koto taka sonchoy holo", {"intent": "get_balance", "period": "this_month"}),
        ("কত টাকা সেভ হলো", {"intent": "get_balance", "period": "this_month"}),
        ("কত টাকা জমলো", {"intent": "get_balance", "period": "this_month"}),
        ("কত টাকা বাঁচলো", {"intent": "get_balance", "period": "this_month"}),
        ("কত টাকা সঞ্চয় হলো", {"intent": "get_balance", "period": "this_month"}),
        ("আমার সঞ্চয় কত আছে", {"intent": "get_balance", "period": "this_month"}),
        ("shobcheye beshi khoroch konta", {"intent": "get_highest_expense", "period": "this_month"}),
        ("ei mashe sobcheye boro khoroch ki chilo", {"intent": "get_highest_expense", "period": "this_month"}),
        ("highest expense konta", {"intent": "get_highest_expense", "period": "this_month"}),
        ("raat 9 tay reminder dio", {"intent": "set_reminder", "time": "21:00", "message": "Log today's expenses"}),
        ("raat 10 tay hisab lekhar reminder set koro", {"intent": "set_reminder", "time": "22:00", "message": "Log today's expenses"}),
        ("sokal 8 tay reminder dao", {"intent": "set_reminder", "time": "08:00", "message": "Log today's expenses"}),
        ("reminder gula dekhao", {"intent": "list_reminders"}),
        ("amar sob reminder list koro", {"intent": "list_reminders"}),
        ("delete reminder 2", {"intent": "delete_reminder", "id": 2}),
        ("reminder 1 muche felo", {"intent": "delete_reminder", "id": 1}),
        ("reminder 1 bondho koro", {"intent": "toggle_reminder", "id": 1, "active": False}),
        ("reminder 2 chalu koro", {"intent": "toggle_reminder", "id": 2, "active": True}),
        ("last expense delete koro", {"intent": "delete_last_transaction"}),
        ("ager khoroch ta muche felo", {"intent": "delete_last_transaction"}),
        ("sesh transaction ta bad dao", {"intent": "delete_last_transaction"}),
        ("delete transaction 7", {"intent": "delete_transaction_by_id", "id": 7}),
        ("transaction 12 delete koro", {"intent": "delete_transaction_by_id", "id": 12}),
        ("recent transactions dekhao", {"intent": "get_recent_transactions", "limit": 10}),
        ("history dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("transaction history dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("amar expense history dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("log dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("spending log dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("statement dekhao", {"intent": "get_recent_transactions", "limit": 20}),
        ("monthly statement dao", {"intent": "get_recent_transactions", "limit": 20}),
        ("khatiyan dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("ledger dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("shob transaction dekhao", {"intent": "get_recent_transactions", "limit": 20}),
        ("khoroch er list dekhao", {"intent": "get_recent_transactions", "limit": 15}),
        ("sorshes 5 ta khoroch dekhao", {"intent": "get_recent_transactions", "limit": 5}),
        ("shesh 10 ta expense dekhao", {"intent": "get_recent_transactions", "limit": 10}),
        ("search biryani", {"intent": "search_transactions", "query": "biryani"}),
        ("find napa", {"intent": "search_transactions", "query": "napa"}),
        ("khujo rickshaw", {"intent": "search_transactions", "query": "rickshaw"}),
        ("khuje dao medicine", {"intent": "search_transactions", "query": "medicine"}),
        ("search koro medicine", {"intent": "search_transactions", "query": "medicine"}),
        ("khuje ber koro napa", {"intent": "search_transactions", "query": "napa"}),
        ("khoj koro doctor", {"intent": "search_transactions", "query": "doctor"}),
        ("napa search koro", {"intent": "search_transactions", "query": "napa"}),
        ("rickshaw khuje dao", {"intent": "search_transactions", "query": "rickshaw"}),
        ("medicine khujo", {"intent": "search_transactions", "query": "medicine"}),
        ("bazar khuje ber koro", {"intent": "search_transactions", "query": "bazar"}),
        ("খুঁজে বের করো নাপা", {"intent": "search_transactions", "query": "napa"}),
        ("নাপা সার্চ করো", {"intent": "search_transactions", "query": "napa"}),
        ("রিকশা খুঁজে দাও", {"intent": "search_transactions", "query": "rickshaw"}),
        ("ওষুধ খোঁজ করো", {"intent": "search_transactions", "query": "osudh"}),
        ("bazar er hisab dao", {"intent": "get_item_total", "period": "this_month", "query": "bazar"}),
        ("rickshaw er hisab dekhao", {"intent": "get_item_total", "period": "this_month", "query": "rickshaw"}),
        ("bazar er hisab dekhao", {"intent": "get_item_total", "period": "this_month", "query": "bazar"}),
        ("shesh 5 ta hisab dekhao", {"intent": "get_recent_transactions", "limit": 5}),
        ("daily log dekhao", {"intent": "get_recent_transactions", "limit": 10}),
        ("দৈনিক হিসাব দেখাও", {"intent": "get_recent_transactions", "limit": 10}),
        ("দৈনিক লগ দেখাও", {"intent": "get_recent_transactions", "limit": 10}),
        ("সাম্প্রতিক খরচ দেখাও", {"intent": "get_recent_transactions", "limit": 10}),
        ("লেনদেনের ইতিহাস দেখাও", {"intent": "get_recent_transactions", "limit": 15}),
        ("স্টেটমেন্ট দেখাও", {"intent": "get_recent_transactions", "limit": 20}),
        ("খরচের তালিকা দেখাও", {"intent": "get_recent_transactions", "limit": 15}),
        ("সার্চ ওষুধ", {"intent": "search_transactions", "query": "osudh"}),
        ("db stats dekhao", {"intent": "get_database_stats"}),
        ("database e koto gula hisab ache", {"intent": "get_database_stats"}),
        ("shesh hisab ta muche felo", {"intent": "delete_last_transaction"}),
        ("shesh hisab batil koro", {"intent": "delete_last_transaction"}),
        ("শেষ খরচটা মুছে ফেলো", {"intent": "delete_last_transaction"}),
        ("শেষ হিসাবটা বাতিল করো", {"intent": "delete_last_transaction"}),
        ("সব রিমাইন্ডার দেখাও", {"intent": "list_reminders"}),
        ("রিমাইন্ডার তালিকা দেখাও", {"intent": "list_reminders"}),
        ("oboshishto koto ache", {"intent": "get_balance", "period": "this_month"}),
        ("অবশিষ্ট কত টাকা আছে", {"intent": "get_balance", "period": "this_month"}),
        ("clear all transactions", {"intent": "clear_all_transactions"}),
        ("sob hisab muche felo", {"intent": "clear_all_transactions"}),
        ("সব খরচ মুছে ফেলো", {"intent": "clear_all_transactions"}),
    ]
    for _ in range(count // 4):
        user_input, obj = rng.choice(extra_pairs)
        samples.append(format_gemma3_turn(user_input, json.dumps(obj, separators=(",", ":"))))

    return samples


def generate_advisory_samples() -> List[Dict]:
    advisory_pairs = [
        (
            "taka bachabo kivabe?",
            "Follow the 50/30/20 rule: allocate 50% of your income to essential needs (rent, bazar, transport), 30% to wants, and save 20% in an emergency fund or DPS. Tracking daily small spends like cha-nasta and rickshaw fares helps stop leaks!",
        ),
        (
            "khoroch komanor upay ki?",
            "To reduce monthly expenses: 1) Track every small spend (tea, snacks, ride-shares), 2) Plan weekly kacha bazar with a fixed list, 3) Pack home-cooked lunch instead of ordering out, and 4) Wait 24 hours before impulse shopping.",
        ),
        (
            "50/30/20 budget rule ta bujhiye bolo",
            "The 50/30/20 rule splits your monthly income into three buckets: 50% for Needs (basha vara, bazar, utility bills, commute), 30% for Wants (dining out, shopping, entertainment), and 20% for Savings & Emergency Fund.",
        ),
        (
            "tumi ke? ki korte paro?",
            "Ami tomar offline AI Expense Assistant! Ami 100% on-device choli, tai tomar hisab-nikash shob somoy private thake. Tumi Bangla, Banglish ba English-e khoroch likhte paro (jemon '50 tk bus vara') ebong hisab jante paro ('ei mashe koto khoroch hoise').",
        ),
    ]
    return [format_gemma3_turn(q, a) for q, a in advisory_pairs * 15]


def main():
    parser = argparse.ArgumentParser(description="Generate Banglish fine-tuning dataset for Gemma 3 270M.")
    parser.add_argument(
        "--output",
        type=str,
        default="scripts/banglish_expense_finetune.jsonl",
        help="Output JSONL file path",
    )
    parser.add_argument(
        "--num-samples",
        type=int,
        default=2000,
        help="Approximate total number of samples to generate",
    )
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    args = parser.parse_args()

    rng = random.Random(args.seed)
    samples: List[Dict] = []
    samples.extend(generate_single_expense_samples(rng, int(args.num_samples * 0.45)))
    samples.extend(generate_multi_expense_samples(rng, int(args.num_samples * 0.15)))
    samples.extend(generate_income_samples(rng, int(args.num_samples * 0.15)))
    samples.extend(generate_query_samples(rng, int(args.num_samples * 0.20)))
    samples.extend(generate_advisory_samples())

    rng.shuffle(samples)

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", encoding="utf-8") as f:
        for item in samples:
            f.write(json.dumps(item, ensure_ascii=False) + "\n")

    print(f"Generated {len(samples)} Banglish training examples -> {out_path}")


if __name__ == "__main__":
    main()
