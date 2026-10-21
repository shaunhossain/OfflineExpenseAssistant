package com.example.expenseassistant.ai

import com.example.expenseassistant.domain.TransactionValidator
import com.example.expenseassistant.mcp.SingleItemConfirmation
import java.util.regex.Pattern

data class ExtractedDate(
    val cleanedText: String,
    val timestamp: Long?
)

data class QuickParseResult(
    val matched: Boolean,
    val intent: String,
    val amountMinor: Long? = null,
    val category: String? = null,
    val note: String? = null,
    val occurredAt: Long? = null,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val searchQuery: String? = null,
    val multipleItems: List<SingleItemConfirmation> = emptyList(),
    val id: Long? = null,
    val limit: Int? = null,
    val isActive: Boolean? = null
)

data class ParsedItemTotal(
    val item: String,
    val period: String? = null
)

object BanglishNormalizer {

    private val FINANCIAL_ANCHOR_KEYWORDS = setOf(
        // Transport, Vehicle Upkeep, Traffic Fines, Flights & BRTA
        "vara", "bhara", "bada", "vaada", "bus", "rickshaw", "riksa", "riksha", "rikshaw", "cng", "auto", "train", "metro", "transport", "uber", "pathao", "gari", "tempo",
        "laguna", "leguna", "launch", "nouka", "ferry", "van", "octane", "petrol", "diesel", "fuel", "toll", "parking", "garage",
        "brta", "license", "licence", "tax token", "fitness", "servicing", "puncture", "mobil", "engine oil", "hawa", "grease", "filter", "plug", "helmet", "raincoat", "learner",
        "pickup", "truck", "coolie", "labour", "traffic", "fine", "mamla", "sergeant", "biman", "flight", "wash", "bike", "motorcycle",
        // Food & Dining
        "lunch", "dinner", "breakfast", "nasta", "nashta", "cha", "tea", "coffee", "food", "khabar", "biryani", "biriyani", "biriani",
        "kacchi", "tehari", "khichuri", "halim", "haleem", "chotpoti", "misti", "mishti", "doi", "jhalmuri", "bakorkhani", "jilapi", "roshogolla", "chomchom", "nihari", "paya", "polao",
        "ruti", "roti", "parata", "porota", "singara", "shingara", "somosa", "fuchka", "fuska", "bhat", "vaat", "churmur", "velpuri", "bhelpuri", "panipuri", "chanachur", "badam", "chop", "beguni", "roll",
        "burger", "pizza", "shawarma", "grill", "nan", "naan", "kabab", "kebab", "biscuit", "biskut", "cake", "pauruti", "bread", "juice", "coke", "pepsi", "lassi", "soup", "momo", "chowmein", "pasta", "paan", "zorda", "kulfi",
        "canteen", "dining", "meal", "tong", "biri", "cigarette", "smoke", "treat", "adda", "borhani", "daab", "dab",
        // Groceries, Farming & Animal Feed
        "bazar", "bajar", "bazaar", "grocery", "chal", "chaal", "dal", "daal", "tel", "murgi", "mach", "maach", "dim", "deem", "dudh", "sobji", "shobji",
        "mangsho", "mangso", "meat", "beef", "mutton", "ilish", "chingri", "torkari", "aloo", "alu", "peyaj", "piyaj", "roshun", "rosun", "morich", "moris",
        "chini", "lobon", "moyda", "suji", "moshla", "masala", "holud", "ada", "ghee", "butter", "shaban", "saban", "soap", "shampoo", "toothpaste", "detergent", "tissue",
        "fol", "fruits", "kola", "aam", "apel", "komola", "malta", "begun", "potol", "tomato", "gajor", "lichu", "kathal", "pepe", "peyara",
        "harpic", "vim", "diaper", "pampers", "cerelac", "feeder", "sar", "keetnashok", "beej", "bhushi", "khor", "feed",
        // Utilities, Rent, Construction, Legal, Repairs, Laundry & Banking
        "bill", "current", "karent", "gas", "electricity", "biddut", "bidyut", "pani", "paani", "wifi", "internet", "net", "broadband", "rent", "recharge", "reacharge", "load", "flexi", "utility",
        "dish", "kisti", "emi", "cylinder", "bua", "maid", "khala", "advance", "dps", "credit card", "bank", "charge", "fee", "penalty", "interest", "shudh",
        "mistri", "electrician", "plumber", "repair", "servicing", "chula", "tanki", "septic", "moyla", "daroan", "inverter", "generator", "capacitor", "pump",
        "ukil", "advocate", "court", "notary", "stamp", "passport", "laundry", "dhobi", "iron", "istri", "samity", "somiti",
        "rajmistri", "cement", "rod", "bali", "eet", "brick", "manpower", "bmet", "koshai", "kosai",
        // Shopping, Winter Wear, Gadgets, Tailoring, Healthcare, Education & Entertainment
        "shopping", "cloth", "kapor", "kapod", "jama", "shirt", "pant", "juta", "juto", "shoe", "sari", "sharee", "panjabi", "punjabi", "salwar", "kamiz", "borka", "hijab", "lungi", "tshirt", "ghori", "watch", "choshma", "cosmetics", "daraz", "aarong", "apex", "bata",
        "darji", "tailor", "selai", "piko", "polish", "chador", "balish", "toshok", "porda", "curtain", "lep", "kambal", "blanket", "sweater", "hoodie", "jacket", "chabi", "chata", "shave", "daari", "facial", "mehedi", "mehendi", "rebonding", "multiplug", "bulb", "khelna", "tiffin", "blender", "korai", "khorai",
        "delivery", "courier", "sundarban", "steadfast", "redx", "cod", "display", "battery", "charger", "earphone", "headphone", "glass", "tempered", "cover", "esim",
        "medicine", "doctor", "daktar", "hospital", "haspatal", "osudh", "oshudh", "napa", "seclo", "sergel", "monas", "orsaline", "saline", "vitamin", "antibiotic", "syrup", "tablet", "pathology", "diagnostic", "xray", "pharmacy",
        "ecg", "ultrasono", "physio", "physiotherapy", "therapy", "dental", "scaling", "lens", "gamca", "wafid", "homeo", "homeopathy", "ayurvedic", "kabiraji", "savlon", "dettol", "hexisol", "inhaler", "glucometer", "cbc", "creatinine", "lipid", "urine", "stool", "thyroid", "bandage", "gauze", "cotton",
        "tuition", "tution", "coaching", "school", "college", "university", "varsity", "semester", "admission", "boi", "book", "khata", "kolom", "pen", "stationery",
        "form fillup", "admit card", "registration", "photocopy", "sheet", "binding", "lamination", "recheck", "reunion", "convocation", "transcript", "attestation", "dope",
        "movie", "cinema", "cineplex", "netflix", "spotify", "hoichoi", "chorki", "picnic", "tour", "gym", "fitness",
        "turf", "futsal", "diamond", "topup", "pubg", "badminton",
        "domain", "hosting", "copilot", "canva", "storage", "vpn", "cat food", "whiskas", "drools", "vet",
        "biye", "holud", "stage", "catering", "photographer", "convention",
        // Religious, Charitable, Tipping, Slang & Cultural
        "eid", "qurbani", "sadakah", "sadaqah", "fitra", "zakat", "chanda", "milad", "hashil", "salami", "masjid", "madrasa", "dan", "bokshish", "baksheesh", "tips", "chillar", "bhangti", "uthkho", "ghaat",
        // People, Family & Contacts
        "ma", "ammu", "baba", "abbu", "bhai", "vai", "bhaiya", "vaiya", "bon", "apu", "apa", "dost", "bondhu", "chele", "chhele", "meye", "mama", "kaka", "chacha", "khala", "fupu",
        // Transaction Verbs & Indicators
        "khoroch", "khoros", "koroch", "khoroj", "kharach", "kharos", "kharcha", "cost", "expense", "spending", "dilam", "delam", "disi", "dici", "diasilam", "disilam", "dislam", "diyechi", "dilo", "dise", "disen", "kinlam", "kinechi", "korsi", "khelam", "khaisi", "pathalam", "pathaisi", "pathaylam", "pathaise", "paisi", "pelam", "dhar", "loan", "karjo", "dena", "paona",
        // MFS, Banking & Digital Services
        "bkash", "bekash", "bikash", "nagad", "nogod", "rocket", "roket", "upay", "cellfin", "foodpanda", "chaldal", "shwapno", "desco", "wasa", "titas", "nesco", "dpdc"
    )

    fun hasPhoneticFinancialAnchor(text: String, note: String = ""): Boolean {
        val combined = "$text $note".lowercase()
        if (FINANCIAL_ANCHOR_KEYWORDS.any { combined.contains(it) }) return true
        val words = combined.split(Regex("[^a-zA-Z0-9\\u0980-\\u09FF]+")).filter { it.isNotBlank() }
        return words.any { w ->
            val canon = BanglishTransliterator.resolveCanonicalBanglishWord(w)
            canon in FINANCIAL_ANCHOR_KEYWORDS
        }
    }

    fun normalize(input: String): String {
        var text = input.trim().lowercase()

        // Replace Bengali digits with ASCII digits
        val bengaliDigits = "০১২৩৪৫৬৭৮৯"
        bengaliDigits.forEachIndexed { index, char ->
            text = text.replace(char.toString(), index.toString())
        }

        // Normalize compound Bengali balance/money phrases before single-word currency substitution
        text = text.replace("টাকা পয়সার হিসাব", "taka poyshar hisab")
        text = text.replace("টাকা পয়সা", "taka poysha")
        text = text.replace("টাকা পয়সার হিসাব", "taka poyshar hisab")
        text = text.replace("টাকা পয়সা", "taka poysha")

        // Normalize currency terms (including regional dialect terms like teha, tyaha, taeha)
        text = text.replace("টাকায়", "tk")
        text = text.replace("টাকার", "tk")
        text = text.replace("টাকা", "tk")
        text = text.replace("৳", "tk ")
        text = text.replace("takar", "tk")
        text = text.replace("taka", "tk")
        text = text.replace("bdt", "tk")
        text = text.replace(Regex("(?:tehar|tyahar|teha|tyaha|taeha|tekha|tegar|tega)\\b", RegexOption.IGNORE_CASE), "tk")

        // Remove formatting commas within numbers (e.g. 50,000 -> 50000, 1,50,000 -> 150000)
        text = text.replace(Regex("(\\d+),(?=\\d{3})"), "$1")

        // Suffix magnitude 'k' with digits (e.g. "50k" -> "50000", "2.5k" -> "2500")
        text = text.replace(Regex("\\b(\\d+(?:\\.\\d+)?)\\s*k\\b", RegexOption.IGNORE_CASE)) { matchResult ->
            val num = matchResult.groupValues[1].toDoubleOrNull()
            if (num != null) {
                val total = (num * 1000).toLong()
                total.toString()
            } else {
                matchResult.value
            }
        }

        // Separate joined numbers and currency terms: e.g. "100tk" -> "100 tk", "500taka" -> "500 tk", "50৳" -> "50 tk"
        text = text.replace(Regex("(\\d+(?:\\.\\d+)?)(tk)\\b", RegexOption.IGNORE_CASE), "$1 tk")
        text = text.replace(Regex("\\b(tk)(\\d+(?:\\.\\d+)?)", RegexOption.IGNORE_CASE), "tk $2")

        // Normalize spoken number words (der, arai, sare sotero, barosho, etc.)
        text = normalizeSpokenNumbers(text)

        // Normalize common Banglish variations using word boundaries (mined from 245k parallel corpus)
        text = text.replace(Regex("\\b(?:khoros|khorosh|koroch|koros|khoroc|koroc|khoroj|kharach|kharos|kharcha|khorch|khorcha)\\b", RegexOption.IGNORE_CASE), "khoroch")
        text = text.replace(Regex("\\b(?:hoddur|hoddun|hoddok)\\b", RegexOption.IGNORE_CASE), "koto")
        text = text.replace(Regex("\\b(?:khoroser|korocher|koroser|khorocer|korocer)\\b", RegexOption.IGNORE_CASE), "khorocher")
        text = text.replace(Regex("\\b(?:bhara|bada|vaada)\\b", RegexOption.IGNORE_CASE), "vara")
        text = text.replace(Regex("\\b(?:bajar|bazaar|baazar)\\b", RegexOption.IGNORE_CASE), "bazar")
        text = text.replace(Regex("\\b(?:paise|paichi|peyechi|peyesi|pelam|payci|payesi|pailam)\\b", RegexOption.IGNORE_CASE), "paisi")
        text = text.replace(Regex("\\b(?:bekash|bikash|bkas|bksh)\\b", RegexOption.IGNORE_CASE), "bkash")
        text = text.replace(Regex("\\b(?:nogod|nogode|nagade)\\b", RegexOption.IGNORE_CASE), "nagad")
        text = text.replace(Regex("\\b(?:roket|rockt)\\b", RegexOption.IGNORE_CASE), "rocket")
        text = text.replace(Regex("\\bupai\\b", RegexOption.IGNORE_CASE), "upay")
        text = text.replace(Regex("\\b(?:delam|dilm|dilem|diasilam|diasilm|disilam|dislam)\\b", RegexOption.IGNORE_CASE), "dilam")
        text = text.replace(Regex("\\b(?:gesoi|gelse|giasilo)\\b", RegexOption.IGNORE_CASE), "gese")
        text = text.replace(Regex("\\b(?:pathaylam|pathayechi|pathiyechi|pathechi)\\b", RegexOption.IGNORE_CASE), "pathalam")
        text = text.replace(Regex("\\b(?:kinesilam|kinlem)\\b", RegexOption.IGNORE_CASE), "kinlam")
        text = text.replace(Regex("\\b(?:koresi|koreci|korci)\\b", RegexOption.IGNORE_CASE), "korsi")
        text = text.replace(Regex("\\b(?:shobji|shobjee)\\b", RegexOption.IGNORE_CASE), "sobji")
        text = text.replace(Regex("\\b(?:porota|porata)\\b", RegexOption.IGNORE_CASE), "parata")
        text = text.replace(Regex("\\bshingara\\b", RegexOption.IGNORE_CASE), "singara")
        text = text.replace(Regex("\\bfuska\\b", RegexOption.IGNORE_CASE), "fuchka")
        text = text.replace(Regex("\\bdeem\\b", RegexOption.IGNORE_CASE), "dim")
        text = text.replace(Regex("\\bdaal\\b", RegexOption.IGNORE_CASE), "dal")
        text = text.replace(Regex("\\bchaal\\b", RegexOption.IGNORE_CASE), "chal")
        text = text.replace(Regex("\\bmangso\\b", RegexOption.IGNORE_CASE), "mangsho")
        text = text.replace(Regex("\\b(?:karent|biddut|bidyut|bidut)\\b", RegexOption.IGNORE_CASE), "current")
        text = text.replace(Regex("\\b(?:piyaj|peaj)\\b", RegexOption.IGNORE_CASE), "peyaj")
        text = text.replace(Regex("\\brosun\\b", RegexOption.IGNORE_CASE), "roshun")
        text = text.replace(Regex("\\bdaktar\\b", RegexOption.IGNORE_CASE), "doctor")
        text = text.replace(Regex("\\bhaspatal\\b", RegexOption.IGNORE_CASE), "hospital")
        text = text.replace(Regex("\\b(?:oshudh|oushodh|ousodh)\\b", RegexOption.IGNORE_CASE), "osudh")
        text = text.replace(Regex("\\b(?:salay|salari)\\b", RegexOption.IGNORE_CASE), "salary")
        text = text.replace(Regex("\\bveton\\b", RegexOption.IGNORE_CASE), "beton")
        text = text.replace(Regex("\\bhishab\\b", RegexOption.IGNORE_CASE), "hisab")
        text = text.replace(Regex("\\b(?:sobcheye|sobceye|shobche|sobche)\\b", RegexOption.IGNORE_CASE), "shobcheye")
        text = text.replace(Regex("\\b(?:hoiche|hoice|hoyese|hoyeche|hoiyeche)\\b", RegexOption.IGNORE_CASE), "hoise")
        text = text.replace(Regex("\\b(?:geche|gache|giese|giyese)\\b", RegexOption.IGNORE_CASE), "gese")
        text = text.replace(Regex("\\b(?:kto|ktoo|kotoo)\\b", RegexOption.IGNORE_CASE), "koto")
        text = text.replace(Regex("\\b(?:porsu|porso)\\b", RegexOption.IGNORE_CASE), "porshu")
        text = text.replace(Regex("\\b(?:gotokl|gottkal)\\b", RegexOption.IGNORE_CASE), "gotokal")
        text = text.replace(Regex("\\b(?:balence|blance|blnc|balens)\\b", RegexOption.IGNORE_CASE), "balance")
        text = text.replace(Regex("\\b(?:shoptaho|soptah|soptaho)\\b", RegexOption.IGNORE_CASE), "shoptah")

        // Normalize common Bengali script query/action keywords for hybrid script support
        // (Longer compound phrases must precede single tokens to avoid partial shadowing)
        text = text.replace("লেনদেনের ইতিহাস", "transaction history")
        text = text.replace("সাম্প্রতিক লেনদেন", "recent transactions")
        text = text.replace("সাম্প্রতিক খরচ", "recent transactions")
        text = text.replace("সাম্প্রতিক", "recent")
        text = text.replace("দৈনিক হিসাব", "daily log")
        text = text.replace("দৈনিক লগ", "daily log")
        text = text.replace("দৈনিক", "daily")
        text = text.replace("হিসাব নিকাশ", "hisab")
        text = text.replace("সবচেয়ে বেশি", "shobcheye beshi")
        text = text.replace("সবচেয়ে বড়", "shobcheye boro")
        text = text.replace("সর্বোচ্চ খরচ", "maximum khoroch")
        text = text.replace("সর্বোচ্চ", "highest")
        text = text.replace("খুঁজে বের করো", "khuje ber koro")
        text = text.replace("খুঁজে বের করুন", "khuje ber korun")
        text = text.replace("খোঁজ করো", "khoj koro")
        text = text.replace("খোঁজ করুন", "khoj korun")
        text = text.replace("খুঁজে দিন", "khuje den")
        text = text.replace("খুঁজে দাও", "khuje dao")
        text = text.replace("খুঁজুন", "khujo")
        text = text.replace("খুঁজো", "khujo")
        text = text.replace("সার্চ করো", "search koro")
        text = text.replace("সার্চ করুন", "search korun")
        text = text.replace("সার্চ", "search")
        text = text.replace("সেভ হলো", "save holo")
        text = text.replace("সেভ হইছে", "save hoise")
        text = text.replace("সেভ করো", "save koro")
        text = text.replace("সেভ করুন", "save korun")
        text = text.replace("সেভ", "save")
        text = text.replace("লিখে রাখো", "likhe rakho")
        text = text.replace("লিখে রাখুন", "likhe rakhun")
        text = text.replace("খাতায় তোলো", "khatai tolo")
        text = text.replace("তুলে রাখো", "tule rakho")
        text = text.replace("টুকে রাখো", "tuke rakho")
        text = text.replace("সঞ্চয়", "savings")
        text = text.replace("জমলো", "jomlo")
        text = text.replace("জমসে", "jomse")
        text = text.replace("বাঁচলো", "bachlo")
        text = text.replace("রিমাইন্ডার", "reminder")
        text = text.replace("সব রিমাইন্ডার", "shob reminder")
        text = text.replace("সব খরচ", "shob khoroch")
        text = text.replace("সব হিসাব", "shob hisab")
        text = text.replace("সব লেনদেন", "shob transaction")
        text = text.replace("বাতিল করো", "batil koro")
        text = text.replace("বাতিল করুন", "batil koro")
        text = text.replace("বাতিল", "batil")
        text = text.replace("শেষ", "shesh")
        text = text.replace("কত", "koto")
        text = text.replace("খরচের", "khorocher")
        text = text.replace("খরচ", "khoroch")
        text = text.replace("হিসাব", "hisab")
        text = text.replace("ব্যালেন্স", "balance")
        text = text.replace("বাকি", "baki")
        text = text.replace("আজকে", "ajke")
        text = text.replace("গতকাল", "gotokal")
        text = text.replace("পরশু", "porshu")
        text = text.replace("তারিখ", "tarikh")
        text = text.replace("বেতন", "beton")
        text = text.replace("বোনাস", "bonus")
        text = text.replace("পাইছি", "paisi")
        text = text.replace("পেয়েছি", "paisi")
        text = text.replace("পেলাম", "paisi")
        text = text.replace("দিলাম", "dilam")
        text = text.replace("দিয়েছি", "dilam")
        text = text.replace("দিছি", "disi")
        text = text.replace("পাঠালাম", "pathalam")
        text = text.replace("পাঠিয়েছি", "pathalam")
        text = text.replace("হইছে", "hoise")
        text = text.replace("হয়েছে", "hoise")
        text = text.replace("গেছে", "gese")
        text = text.replace("গেলাম", "gelam")
        text = text.replace("কিনেছি", "kinlam")
        text = text.replace("কিনলাম", "kinlam")
        text = text.replace("কিনসি", "kinlam")
        text = text.replace("কিনছি", "kinlam")
        text = text.replace("খেয়েছি", "khelam")
        text = text.replace("খেলাম", "khelam")
        text = text.replace("খাইছি", "khelam")
        text = text.replace("মুছে ফেলুন", "muche felo")
        text = text.replace("মুছে ফেল", "muche felo")
        text = text.replace("মুছুন", "muche felo")
        text = text.replace("ডিলিট করুন", "delete koro")
        text = text.replace("ডিলিট করো", "delete koro")
        text = text.replace("ডিলিট কর", "delete koro")
        text = text.replace("আপডেট করুন", "update koro")
        text = text.replace("আপডেট করো", "update koro")
        text = text.replace("আপডেট কর", "update koro")
        text = text.replace("পরিবর্তন করুন", "update koro")
        text = text.replace("পরিবর্তন করো", "update koro")
        text = text.replace("পরিবর্তন কর", "update koro")
        text = text.replace("ব্যয়ের", "byayer")
        text = text.replace("ব্যয়ের", "byayer")
        text = text.replace("ব্যয়", "byay")
        text = text.replace("প্যাটার্ন", "pattern")
        text = text.replace("ধরন", "dhoron")
        text = text.replace("বিশ্লেষণ", "analysis")
        text = text.replace("হিসাব কিতাব", "hisab kitab")
        text = text.replace("পয়সার", "poyshar")
        text = text.replace("পয়সা", "poysha")
        text = text.replace("পয়সার", "poyshar")
        text = text.replace("পয়সা", "poysha")
        text = text.replace("পকেটে", "pocket e")
        text = text.replace("পকেট", "pocket")
        text = text.replace("হাতে", "haate")
        text = text.replace("আছে", "ache")
        text = text.replace("রইলো", "roilo")
        text = text.replace("রইল", "roilo")
        text = text.replace("ক্যাশ", "cash")
        text = text.replace("দেখান", "dekhao")
        text = text.replace("দেখাও", "dekhao")
        text = text.replace("বলুন", "bolo")
        text = text.replace("বলো", "bolo")
        text = text.replace("লিস্ট", "list")
        text = text.replace("ইতিহাস", "history")
        text = text.replace("লগ", "log")
        text = text.replace("স্টেটমেন্ট", "statement")
        text = text.replace("তালিকা", "talika")
        text = text.replace("লেজার", "ledger")
        text = text.replace(Regex("(?<=[a-zA-Z\u0980-\u09FF0-9])টা\\b"), " ta")
        text = text.replace(Regex("(?<=[a-zA-Z\u0980-\u09FF0-9])টি\\b"), " ti")
        text = text.replace("খতিয়ান", "khatiyan")

        // Bengali script category keywords (longer phrases first to avoid partial token shadowing)
        text = text.replace("বাড়ি ভাড়া", "basha vara")
        text = text.replace("বাসা ভাড়া", "basha vara")
        text = text.replace("মেস ভাড়া", "mess vara")
        text = text.replace("দোকান ভাড়া", "dokan vara")
        text = text.replace("গ্যারেজ ভাড়া", "garage vara")
        text = text.replace("রিকশা ভাড়া", "rickshaw vara")
        text = text.replace("বাস ভাড়া", "bus vara")
        text = text.replace("ট্রেন ভাড়া", "train vara")
        text = text.replace("গাড়ি ভাড়া", "gari vara")
        text = text.replace("বাসা", "basha")
        text = text.replace("বাড়ি", "bari")
        text = text.replace("রিকশা", "rickshaw")
        text = text.replace("বাস", "bus")
        text = text.replace("ভাড়া", "vara")
        text = text.replace("সিএনজি", "cng")
        text = text.replace("অটো", "auto")
        text = text.replace("মেট্রোরেল", "metro")
        text = text.replace("মেট্রো", "metro")
        text = text.replace("ট্রেন", "train")
        text = text.replace("উবার", "uber")
        text = text.replace("পাঠাও", "pathao")
        text = text.replace("গাড়ি", "gari")
        text = text.replace("বিদ্যুৎ বিল", "current bill")
        text = text.replace("কারেন্ট বিল", "current bill")
        text = text.replace("বিদ্যুৎ", "biddut")
        text = text.replace("কারেন্ট", "current")
        text = text.replace("পানি বিল", "pani bill")
        text = text.replace("পানি", "pani")
        text = text.replace("গ্যাস বিল", "gas bill")
        text = text.replace("গ্যাস", "gas")
        text = text.replace("ইন্টারনেট বিল", "net bill")
        text = text.replace("ইন্টারনেট", "internet")
        text = text.replace("ওয়াইফাই", "wifi")
        text = text.replace("রিচার্জ", "recharge")
        text = text.replace("ক্রেডিট কার্ড", "credit card")
        text = text.replace("ডিশ বিল", "dish bill")
        text = text.replace("কাঁচাবাজার", "kachabazar")
        text = text.replace("বাজার", "bazar")
        text = text.replace("মুদি", "mudi")
        text = text.replace("চাল", "chal")
        text = text.replace("ডাল", "dal")
        text = text.replace("তেল", "tel")
        text = text.replace("আলু", "aloo")
        text = text.replace("পেঁয়াজ", "peyaj")
        text = text.replace("রসুন", "roshun")
        text = text.replace("মরিচ", "morich")
        text = text.replace("আদা", "ada")
        text = text.replace("হলুদ", "holud")
        text = text.replace("লবণ", "lobon")
        text = text.replace("চিনি", "chini")
        text = text.replace("মসলা", "moshla")
        text = text.replace("মাছ", "mach")
        text = text.replace("মাংস", "mangsho")
        text = text.replace("মুরগি", "murgi")
        text = text.replace("ডিম", "dim")
        text = text.replace("দুধ", "dudh")
        text = text.replace("সবজি", "sobji")
        text = text.replace("নাস্তা", "nasta")
        text = text.replace("চা", "cha")
        text = text.replace("কফি", "coffee")
        text = text.replace("খাবার", "khabar")
        text = text.replace("লাঞ্চ", "lunch")
        text = text.replace("ডিনার", "dinner")
        text = text.replace("বিরিয়ানি", "biryani")
        text = text.replace("কাচ্চি", "kacchi")
        text = text.replace("তেহারি", "tehari")
        text = text.replace("খিচুড়ি", "khichuri")
        text = text.replace("ভাত", "bhat")
        text = text.replace("রুটি", "ruti")
        text = text.replace("পরোটা", "porota")
        text = text.replace("সিঙ্গারা", "singara")
        text = text.replace("ফুচকা", "fuchka")
        text = text.replace("ওষুধ", "osudh")
        text = text.replace("ঔষধ", "osudh")
        text = text.replace("ডাক্তার", "daktar")
        text = text.replace("হাসপাতাল", "hospital")
        text = text.replace("ফার্মেসি", "pharmacy")
        text = text.replace("জামা", "jama")
        text = text.replace("কাপড়", "kapor")
        text = text.replace("শার্ট", "shirt")
        text = text.replace("প্যান্ট", "pant")
        text = text.replace("জুতা", "juta")
        text = text.replace("শাড়ি", "saree")
        text = text.replace("পাঞ্জাবি", "panjabi")
        text = text.replace("শপিং", "shopping")
        text = text.replace("টিউশন", "tuition")
        text = text.replace("কোচিং", "coaching")
        text = text.replace("বই", "boi")
        text = text.replace("খাতা", "khata")
        text = text.replace("কলম", "kolom")
        text = text.replace("দান", "dan")
        text = text.replace("চাঁদা", "chanda")
        text = text.replace("যাকাত", "zakat")
        text = text.replace("সালামি", "salami")
        text = text.replace("বখশিশ", "bokshish")
        text = text.replace("টিপস", "tips")
        text = text.replace("যাতায়াত", "transport")
        text = text.replace("যাতায়াত", "transport")
        text = text.replace("কেনাকাটা", "shopping")
        text = text.replace("চিকিৎসা", "medicine")
        text = text.replace("বিনোদন", "entertainment")
        text = text.replace("শিক্ষা", "education")
        text = text.replace("মুদিবাজার", "grocery")
        text = text.replace("মুদি বাজার", "grocery")
        text = text.replace("কাঁচা বাজার", "bazar")
        text = text.replace("কাঁচাবাজার", "bazar")

        return text.trim()
    }

    /**
     * Converts spoken Bengali/Banglish numerical expressions and traditional fractions
     * into standard ASCII numbers.
     * Handles:
     * - "der" (1.5), "der hajar" (1500), "dersho" (150)
     * - "arai" (2.5), "arai hajar" (2500), "araisho" (250)
     * - "sare <num>" (num + 0.5), e.g. "sare sotero" -> 17.5, "sare tin hajar" -> 3500, "sare barosho" -> 1250
     * - Hundred-words: "barosho" (1200), "choddosho" (1400), "ponerosho" (1500), "satsho" (700), etc.
     * - "sowa" (+0.25) and "poune" (-0.25)
     * - Native Bengali script forms: দেড়শ, আড়াইশ, বারোশ, সাড়ে সতেরো, etc.
     */
    fun normalizeSpokenNumbers(raw: String): String {
        var s = raw

        // 1. Bengali script hundreds and fractions
        val bnScriptHundreds = listOf(
            "দেড়শো" to "150", "দেড়শ" to "150",
            "আড়াইশো" to "250", "আড়াইশ" to "250",
            "একশো" to "100", "একশ" to "100",
            "দুইশো" to "200", "দুইশ" to "200",
            "তিনশো" to "300", "তিনশ" to "300",
            "চারশো" to "400", "চারশ" to "400",
            "পাঁচশো" to "500", "পাঁচশ" to "500",
            "ছয়শো" to "600", "ছয়শ" to "600",
            "সাতশো" to "700", "সাতশ" to "700",
            "আটশো" to "800", "আটশ" to "800",
            "নয়শো" to "900", "নয়শ" to "900",
            "এগারোশো" to "1100", "এগারোশ" to "1100",
            "বারোশো" to "1200", "বারোশ" to "1200",
            "তেরোশো" to "1300", "তেরোশ" to "1300",
            "চৌদ্দশো" to "1400", "চৌদ্দশ" to "1400",
            "পনেরোশো" to "1500", "পনেরোশ" to "1500",
            "ষোলশো" to "1600", "ষোলশ" to "1600",
            "সতেরোশো" to "1700", "সতেরোশ" to "1700",
            "আঠারোশো" to "1800", "আঠারোশ" to "1800",
            "উনিশশো" to "1900", "উনিশশ" to "1900",
            "বিশশো" to "2000", "বিশশ" to "2000"
        )
        s = s.replace(Regex("দেড়\\s*(?:শত|শো|শ)"), "150")
        s = s.replace(Regex("আড়াই\\s*(?:শত|শো|শ)"), "250")
        s = s.replace(Regex("দেড়\\s*হাজার"), "1500")
        s = s.replace(Regex("আড়াই\\s*হাজার"), "2500")
        s = s.replace(Regex("দেড়\\s*লাখ"), "150000")
        s = s.replace(Regex("আড়াই\\s*লাখ"), "250000")
        s = s.replace(Regex("দেড়\\s*(?:টাকা|tk\\b)"), "1.5 tk")
        s = s.replace(Regex("আড়াই\\s*(?:টাকা|tk\\b)"), "2.5 tk")
        for ((bnWord, numVal) in bnScriptHundreds) {
            s = s.replace(bnWord, numVal)
        }

        // 2. Banglish Hundreds map
        val hundredsMap = listOf(
            "eksho" to 100, "ek sho" to 100, "ek so" to 100, "ek soto" to 100, "ek shoto" to 100,
            "dersho" to 150, "der sho" to 150, "derhsho" to 150, "derso" to 150, "der so" to 150, "der soto" to 150, "der shoto" to 150,
            "duisho" to 200, "dushho" to 200, "duso" to 200, "dui sho" to 200, "dui so" to 200, "dui soto" to 200,
            "araisho" to 250, "arai sho" to 250, "arhaisho" to 250, "araiso" to 250, "arai so" to 250, "arai soto" to 250, "arai shoto" to 250,
            "tinsho" to 300, "tin sho" to 300, "tin so" to 300, "tin soto" to 300,
            "charsho" to 400, "char sho" to 400, "char so" to 400, "char soto" to 400,
            "pachsho" to 500, "pasho" to 500, "pach sho" to 500, "pach so" to 500, "pach soto" to 500,
            "choysho" to 600, "choy sho" to 600, "choy so" to 600, "choy soto" to 600,
            "satsho" to 700, "shatsho" to 700, "sat sho" to 700, "sat so" to 700, "sat soto" to 700,
            "atsho" to 800, "aatsho" to 800, "aat sho" to 800, "at sho" to 800, "aat so" to 800, "at so" to 800, "aat soto" to 800,
            "noysho" to 900, "noy sho" to 900, "noy so" to 900, "noy soto" to 900,
            "egarosho" to 1100, "egaro sho" to 1100, "egaro so" to 1100, "egaro soto" to 1100,
            "barosho" to 1200, "baro sho" to 1200, "baro so" to 1200, "baro soto" to 1200, "barosoto" to 1200, "baroshoto" to 1200,
            "terosho" to 1300, "tero sho" to 1300, "tero so" to 1300, "tero soto" to 1300,
            "choddosho" to 1400, "chouddosho" to 1400, "choddo sho" to 1400, "choddo so" to 1400, "chouddo soto" to 1400,
            "ponerosho" to 1500, "ponero sho" to 1500, "ponero so" to 1500, "ponero soto" to 1500,
            "sholosho" to 1600, "solosho" to 1600, "solo sho" to 1600, "solo so" to 1600, "solo soto" to 1600,
            "soterosho" to 1700, "shoterosho" to 1700, "sotero sho" to 1700, "sotero so" to 1700, "sotero soto" to 1700,
            "atharsho" to 1800, "atharosho" to 1800, "atharo sho" to 1800, "atharo so" to 1800, "atharo soto" to 1800,
            "unishsho" to 1900, "unish sho" to 1900, "unish so" to 1900, "unish soto" to 1900,
            "bishsho" to 2000, "bis sho" to 2000, "bis so" to 2000, "bish soto" to 2000
        )

        val bengaliNumberWords = mapOf(
            "shunno" to 0, "ek" to 1, "dui" to 2, "tin" to 3, "char" to 4,
            "pach" to 5, "paach" to 5, "choy" to 6, "sat" to 7, "shat" to 7,
            "aat" to 8, "at" to 8, "noy" to 9, "dosh" to 10,
            "egaro" to 11, "baro" to 12, "tero" to 13, "choddo" to 14, "chouddo" to 14,
            "ponero" to 15, "sholo" to 16, "solo" to 16, "sotero" to 17, "shotero" to 17,
            "atharo" to 18, "athara" to 18, "unish" to 19, "bish" to 20, "bis" to 20,
            "kuri" to 20, "ekrish" to 21, "baish" to 22, "teish" to 23, "chobbish" to 24,
            "pocchish" to 25, "pochish" to 25, "chabbish" to 26, "sathash" to 27,
            "athash" to 28, "unirish" to 29, "trish" to 30, "tirish" to 30,
            "poytirish" to 35, "chollish" to 40, "ponchash" to 50,
            "shaat" to 60, "shottor" to 70, "sottor" to 70, "ashi" to 80, "nobboi" to 90
        )

        // 3. "sare" with hundred-words e.g. "sare barosho" -> 1250, "sare tinsho" -> 350
        val hundredWordsRegex = hundredsMap.map { it.first.replace(" ", "\\s+") }.joinToString("|")
        val sareHundredPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+($hundredWordsRegex)\\b", Pattern.CASE_INSENSITIVE)
        val mSareH = sareHundredPattern.matcher(s)
        val sbSareH = StringBuffer()
        while (mSareH.find()) {
            val matchedWord = mSareH.group(1)?.lowercase()?.replace(Regex("\\s+"), " ") ?: ""
            val valH = hundredsMap.firstOrNull { it.first == matchedWord }?.second
            if (valH != null) {
                mSareH.appendReplacement(sbSareH, (valH + 50).toString())
            } else {
                mSareH.appendReplacement(sbSareH, mSareH.group(0) ?: "")
            }
        }
        mSareH.appendTail(sbSareH)
        s = sbSareH.toString()

        // 4. "sare <num> (hajar|hazar|k)" -> (num + 0.5) * 1000
        val sareHajarPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+([a-zA-Z]+|\\d+)\\s*(?:hajar|hazar|k)\\b", Pattern.CASE_INSENSITIVE)
        val mSareHajar = sareHajarPattern.matcher(s)
        val sbSareHajar = StringBuffer()
        while (mSareHajar.find()) {
            val numStr = mSareHajar.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal + 0.5) * 1000
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSareHajar.appendReplacement(sbSareHajar, formatted)
            } else {
                mSareHajar.appendReplacement(sbSareHajar, mSareHajar.group(0) ?: "")
            }
        }
        mSareHajar.appendTail(sbSareHajar)
        s = sbSareHajar.toString()

        // 5. "sare <num> sho" -> (num + 0.5) * 100
        val sareShoPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+([a-zA-Z]+|\\d+)\\s*sho\\b", Pattern.CASE_INSENSITIVE)
        val mSareSho = sareShoPattern.matcher(s)
        val sbSareSho = StringBuffer()
        while (mSareSho.find()) {
            val numStr = mSareSho.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal + 0.5) * 100
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSareSho.appendReplacement(sbSareSho, formatted)
            } else {
                mSareSho.appendReplacement(sbSareSho, mSareSho.group(0) ?: "")
            }
        }
        mSareSho.appendTail(sbSareSho)
        s = sbSareSho.toString()

        // 6. "sare <num>" e.g. "sare sotero" -> 17.5, "sare tin" -> 3.5
        val numWordsRegex = bengaliNumberWords.keys.joinToString("|")
        val sarePlainPattern = Pattern.compile("\\b(?:sare|share|sade)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mSarePlain = sarePlainPattern.matcher(s)
        val sbSarePlain = StringBuffer()
        while (mSarePlain.find()) {
            val numStr = mSarePlain.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal + 0.5
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSarePlain.appendReplacement(sbSarePlain, formatted)
            } else {
                mSarePlain.appendReplacement(sbSarePlain, mSarePlain.group(0) ?: "")
            }
        }
        mSarePlain.appendTail(sbSarePlain)
        s = sbSarePlain.toString()

        // 7. "der hajar" / "arai hajar" / "lakh"
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*(?:hajar|hazar|k)\\b", RegexOption.IGNORE_CASE), "1500")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*(?:hajar|hazar|k)\\b", RegexOption.IGNORE_CASE), "2500")
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*lakh\\b", RegexOption.IGNORE_CASE), "150000")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*lakh\\b", RegexOption.IGNORE_CASE), "250000")

        // 7b. General magnitude words: hajar / hazar / হাজার, lakh / lac / লাখ / লক্ষ, crore / koti / কোটি
        val magnitudePattern = Pattern.compile(
            "(?<![a-zA-Z0-9\\u0980-\\u09FF])($numWordsRegex|\\d+(?:\\.\\d+)?)\\s*(hajar|hazar|হাজার|lakh|lac|লাখ|লক্ষ|crore|koti|কোটি)(?![a-zA-Z0-9\\u0980-\\u09FF])",
            Pattern.CASE_INSENSITIVE
        )
        val mMag = magnitudePattern.matcher(s)
        val sbMag = StringBuffer()
        while (mMag.find()) {
            val numStr = mMag.group(1)?.lowercase() ?: ""
            val unitStr = mMag.group(2)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val multiplier = when (unitStr) {
                    "crore", "koti", "কোটি" -> 10_000_000.0
                    "lakh", "lac", "লাখ", "লক্ষ" -> 100_000.0
                    else -> 1_000.0 // hajar, hazar, হাজার
                }
                val total = (numVal * multiplier).toLong()
                mMag.appendReplacement(sbMag, total.toString())
            } else {
                mMag.appendReplacement(sbMag, mMag.group(0) ?: "")
            }
        }
        mMag.appendTail(sbMag)
        s = sbMag.toString()

        // 8. "der sho" / "der so" / "der soto" / "der shoto" / "derso"
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\s*(?:sho|so|shoto|soto)\\b", RegexOption.IGNORE_CASE), "150")
        s = s.replace(Regex("\\b(?:derso|dedso|derhso)\\b", RegexOption.IGNORE_CASE), "150")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\s*(?:sho|so|shoto|soto)\\b", RegexOption.IGNORE_CASE), "250")
        s = s.replace(Regex("\\b(?:araiso|arhaiso|adhaiso)\\b", RegexOption.IGNORE_CASE), "250")

        // General <num> soto / <num> shoto (e.g. tin soto -> 300, baro soto -> 1200)
        val sotoPattern = Pattern.compile("\\b($numWordsRegex|\\d+)\\s*(?:shoto|soto)\\b", Pattern.CASE_INSENSITIVE)
        val mSoto = sotoPattern.matcher(s)
        val sbSoto = StringBuffer()
        while (mSoto.find()) {
            val numStr = mSoto.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal * 100).toLong()
                mSoto.appendReplacement(sbSoto, total.toString())
            } else {
                mSoto.appendReplacement(sbSoto, mSoto.group(0) ?: "")
            }
        }
        mSoto.appendTail(sbSoto)
        s = sbSoto.toString()

        // General <num> so (e.g. tin so -> 300, baro so -> 1200)
        val soPattern = Pattern.compile("\\b($numWordsRegex|\\d+)\\s+so\\b", Pattern.CASE_INSENSITIVE)
        val mSo = soPattern.matcher(s)
        val sbSo = StringBuffer()
        while (mSo.find()) {
            val numStr = mSo.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = (numVal * 100).toLong()
                mSo.appendReplacement(sbSo, total.toString())
            } else {
                mSo.appendReplacement(sbSo, mSo.group(0) ?: "")
            }
        }
        mSo.appendTail(sbSo)
        s = sbSo.toString()

        // 9. Replace standalone hundreds: "barosho", "dersho", "araisho", "satsho", etc.
        for ((w, v) in hundredsMap) {
            val p = Pattern.compile("\\b" + Pattern.quote(w) + "\\b", Pattern.CASE_INSENSITIVE)
            s = p.matcher(s).replaceAll(v.toString())
        }

        // 10. Standalone "der" / "arai" followed by currency (tk/taka)
        s = s.replace(Regex("\\b(?:der|derh|dedh)\\b(?=\\s*(?:tk|taka\\b))", RegexOption.IGNORE_CASE), "1.5")
        s = s.replace(Regex("\\b(?:arai|arhai|adhai)\\b(?=\\s*(?:tk|taka\\b))", RegexOption.IGNORE_CASE), "2.5")

        // 11. "sowa" (+0.25) and "poune" (-0.25)
        s = s.replace(Regex("\\b(?:sowa|showa)\\s+hajar\\b", RegexOption.IGNORE_CASE), "1250")
        s = s.replace(Regex("\\b(?:poune|pone|powne)\\s+hajar\\b", RegexOption.IGNORE_CASE), "750")
        val sowaPattern = Pattern.compile("\\b(?:sowa|showa)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mSowa = sowaPattern.matcher(s)
        val sbSowa = StringBuffer()
        while (mSowa.find()) {
            val numStr = mSowa.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal + 0.25
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mSowa.appendReplacement(sbSowa, formatted)
            } else {
                mSowa.appendReplacement(sbSowa, mSowa.group(0) ?: "")
            }
        }
        mSowa.appendTail(sbSowa)
        s = sbSowa.toString()

        val pounePattern = Pattern.compile("\\b(?:poune|pone|powne)\\s+($numWordsRegex|\\d+)\\b", Pattern.CASE_INSENSITIVE)
        val mPoune = pounePattern.matcher(s)
        val sbPoune = StringBuffer()
        while (mPoune.find()) {
            val numStr = mPoune.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]?.toDouble() ?: numStr.toDoubleOrNull()
            if (numVal != null) {
                val total = numVal - 0.25
                val formatted = if (total % 1.0 == 0.0) total.toLong().toString() else total.toString()
                mPoune.appendReplacement(sbPoune, formatted)
            } else {
                mPoune.appendReplacement(sbPoune, mPoune.group(0) ?: "")
            }
        }
        mPoune.appendTail(sbPoune)
        s = sbPoune.toString()

        // 12. Standalone spoken number words followed by currency (e.g. "ponchash tk cha" -> "50 tk cha", "bish tk bus" -> "20 tk bus")
        val standaloneWordsRegex = bengaliNumberWords.keys.filter { it.length >= 3 && it != "at" }.joinToString("|")
        val standaloneNumPattern = Pattern.compile("\\b($standaloneWordsRegex)\\b(?=\\s*(?:tk|taka)\\b)", Pattern.CASE_INSENSITIVE)
        val mStandalone = standaloneNumPattern.matcher(s)
        val sbStandalone = StringBuffer()
        while (mStandalone.find()) {
            val numStr = mStandalone.group(1)?.lowercase() ?: ""
            val numVal = bengaliNumberWords[numStr]
            if (numVal != null && numVal > 0) {
                mStandalone.appendReplacement(sbStandalone, numVal.toString())
            } else {
                mStandalone.appendReplacement(sbStandalone, mStandalone.group(0) ?: "")
            }
        }
        mStandalone.appendTail(sbStandalone)
        s = sbStandalone.toString()

        return s
    }

    fun isDateWord(w: String): Boolean {
        val lower = w.lowercase().trim()
        val dateWords = setOf("tarikh", "tarik", "tarek", "tarike", "tarikhe", "date", "tarikher", "tarikhor", "tareker")
        return dateWords.contains(lower)
    }

    // =========================================================================
    // Pre-compiled Regex Patterns for Query Matching & Parameter Extraction
    // =========================================================================

    // Explicit Search / Find Query Patterns
    private val PATTERN_PREFIX_SEARCH = Pattern.compile(
        "^(?:search(?:\\s+(?:for|koro|korun))?|find|khujo|khujun|khoj\\s+(?:koro|korun)|khuje\\s+(?:dao|den|dekhao|dekhaw|ber\\s+koro|ber\\s+korun))\\s+(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_SUFFIX_SEARCH = Pattern.compile(
        "^(.+?)\\s+(?:search(?:\\s+(?:koro|korun))?|khujo|khujun|khoj\\s+(?:koro|korun)|khuje\\s+(?:dao|den|dekhao|dekhaw|ber\\s+koro|ber\\s+korun))$",
        Pattern.CASE_INSENSITIVE
    )

    // History / Recent Log / Statement Query Patterns
    private val PATTERN_RECENT_LOG = Pattern.compile(
        ".*\\b(?:history|log|statement|ledger|khatiyan|talika|hisab\\s+nikash)\\b.*|" +
        "(?:transaction|expense|khoroch|entry|hisab|hishab)(?:s|er|ar)?\\s+(?:er\\s+)?(?:list|talika)\\b.*|" +
        "(?:recent|shesh|sesh|last)\\s+(?:(\\d+)\\s*(?:ta|din|diner|days?)?\\s+)?(?:transaction|expense|khoroch|entry|hisab|hishab)(?:s|gula|gulo)?|" +
        "(?:show|list|dekhao|dekhaw)\\s+(?:all\\s+)?(?:recent\\s+)?(?:(\\d+)\\s+)?(?:transactions?|expenses?|khoroch|entries|hisab|hishab)|" +
        "(?:(\\d+)\\s*(?:din|diner|days?)\\s+(?:er\\s+)?(?:khoroch|hisab|statement|log)(?:\\s+(?:dekhao|show|list))?)|" +
        "(?:shob|all)\\s+(?:transaction|expense|khoroch|hisab)(?:s|gula|gulo)?\\s+(?:dekhao|dekhaw|list|show)|" +
        "^(?:history|transaction history|expense history|statement|ledger|khatiyan|log)$",
        Pattern.CASE_INSENSITIVE
    )

    // Deletion & Clear Patterns
    private val PATTERN_DELETE_LAST_TX = Pattern.compile(
        ".*\\b(?:last|shesh|sesh)\\s+(?:expense|transaction|khoroch|entry|hisab|hishab)(?:ta|\\s*ta|\\s*টা|টা)?\\s+(?:delete|muche felo|batil).*|" +
        ".*\\b(?:delete|muche felo|batil koro)\\s+(?:the\\s+)?(?:last|shesh|sesh)\\s+(?:expense|transaction|khoroch|entry|hisab|hishab)(?:ta|\\s*ta|\\s*টা|টা)?.*|" +
        "^(?:delete last expense|delete last transaction)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_DELETE_TX_BY_ID = Pattern.compile(
        "(?:delete|muche felo|batil koro|remove)\\s+(?:transaction|expense|entry)\\s+(?:id\\s+)?#?(\\d+)|(?:transaction|expense|entry)\\s+#?(\\d+)\\s+(?:delete|muche felo|batil)",
        Pattern.CASE_INSENSITIVE
    )

    // Update Transaction Pattern
    private val PATTERN_UPDATE_TX = Pattern.compile(
        "(?:update|change|edit|shongshodhon|bodlao)\\s+(?:the\\s+)?(?:transaction|expense|entry|hisab|khoroch)\\s+(?:id\\s+|no\\s+)?#?(\\d+)(?:\\s+(?:to|koro|korun|e|te))?(?:\\s+(\\d+(?:\\.\\d+)?)\\s*(?:tk|taka)?)?(?:\\s+(?:note\\s+)?(.+))?|" +
        "(?:(?:id\\s+|no\\s+)?#?(\\d+)\\s+(?:no\\s+)?(?:transaction|expense|entry|hisab|khoroch)|(?:transaction|expense|entry|hisab|khoroch)\\s+(?:id\\s+|no\\s+)?#?(\\d+))\\s+(?:update|change|edit|shongshodhon|bodlao)(?:\\s+koro|\\s+korun)?(?:\\s+(?:to|e|te))?(?:\\s+(\\d+(?:\\.\\d+)?)\\s*(?:tk|taka)?)?(?:\\s+(?:note\\s+)?(.+))?",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_CLEAR_ALL_TX = Pattern.compile(
        ".*\\b(?:clear|delete)\\s+all\\s+(?:transactions|expenses).*|" +
        ".*\\bshob\\s+(?:transaction|expense|khoroch|hisab)(?:s)?\\s+(?:clear|delete|muche felo|batil).*",
        Pattern.CASE_INSENSITIVE
    )

    // Cancellation & Negative Guard Pattern (Prevents LLM False Expense Creation)
    private val PATTERN_CANCEL_OR_NEGATIVE = Pattern.compile(
        "^(?:cancel|never\\s*mind|nothing|kisu\\s+na|kichu\\s+na|bad\\s+dao|bad\\s+den|thak|thak\\s+dorkar\\s+nai|dorkar\\s+nai|dorkar\\s+nei|lagbe\\s+na|দরকার\\s+নেই|দরকার\\s+নাই|থাক|কিছু\\s+না|বাতিল)$|" +
        ".*\\b(?:kono|kono\\s+ekta)?\\s*(?:khoroch|expense|transaction|entry)\\s*(?:kori\\s+nai|hoy\\s+nai|honi|kori\\s+ni|kore\\s+nai|nai)\\b.*|" +
        ".*(?:কোনো\\s+খরচ\\s+(?:হয়নি|হয়\\s+নাই|করি\\s+নাই|করিনি|নেই|নাই)).*",
        Pattern.CASE_INSENSITIVE
    )

    // Spending Patterns & Trends Pattern
    private val PATTERN_SPENDING_PATTERNS = Pattern.compile(
        ".*(?:spending\\s+patterns?|expense\\s+patterns?|khorocher\\s+patterns?|spending\\s+analysis|khorocher\\s+dhoron|byayer\\s+dhoron|byayer\\s+analysis|byay\\s+analysis|khorocher\\s+trend|spending\\s+trend|expense\\s+trend|khorocher\\s+analysis|khorocher\\s+bishleshon).*" +
        "|.*(?:খরচের\\s+ধরন|ব্যয়ের\\s+ধরন|ব্যয়ের\\s+ধরন|খরচের\\s+প্যাটার্ন|প্যাটার্ন\\s+দেখাও|ব্যয়ের\\s+বিশ্লেষণ|ব্যয়ের\\s+বিশ্লেষণ|খরচের\\s+বিশ্লেষণ).*",
        Pattern.CASE_INSENSITIVE
    )

    // Reminder Query & Management Patterns
    private val PATTERN_LIST_REMINDERS = Pattern.compile(
        ".*\\b(?:shob|all|list|show)?\\s*reminders?(?:\\s*(?:list|gula|gulo|dekhao|show|dekhaw)).*|" +
        "^(?:reminders|show reminders|list reminders)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_DELETE_REMINDER = Pattern.compile(
        "(?:delete|muche felo|batil koro|remove)\\s+reminder\\s+(?:id\\s+)?#?(\\d+)|reminder\\s+#?(\\d+)\\s+(?:delete|muche felo|batil)",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_TOGGLE_OFF_REMINDER = Pattern.compile(
        "(?:pause|bondho koro|off koro)\\s+reminder\\s+#?(\\d+)|reminder\\s+#?(\\d+)\\s+(?:pause|bondho|off)",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_TOGGLE_ON_REMINDER = Pattern.compile(
        "(?:activate|chalu koro|on koro|unpause)\\s+reminder\\s+#?(\\d+)|reminder\\s+#?(\\d+)\\s+(?:activate|chalu|on)",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_PROPOSE_REMINDER = Pattern.compile(
        "(?:shokal|bikel|shondha|raat)?\\s*(\\d{1,2})(?:\\s*(?:tay|ta|am|pm|:(\\d{2})))?.*?reminder",
        Pattern.CASE_INSENSITIVE
    )

    // Help & Greeting Patterns
    private val PATTERN_HELP = Pattern.compile(
        "^(?:help|commands|options|features|capabilities|instructions)$|" +
        ".*\\b(?:ki\\s+korte\\s+paro|tumi\\s+ki\\s+korte\\s+paro|ki\\s+ki\\s+korte\\s+paro|tumi\\s+ki\\s+ki\\s+paro|what\\s+can\\s+you\\s+do|how\\s+to\\s+use|how\\s+it\\s+works|kivabe\\s+use|kivabe\\s+chalabo|rules|usage|ki\\s+vabe\\s+use)\\b.*|" +
        ".*(?:কীভাবে\\s+ব্যবহার|কিভাবে\\s+ব্যবহার|কি\\s+করতে\\s+পারো|কী\\s+করতে\\s+পারো|কি\\s+কি\\s+করতে\\s+পারো|সাহায্য|কমান্ড).*|^(?:help\\s+me|help)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_GREETING = Pattern.compile(
        "^(?:hi|hello|hey|salam|assalamu\\s+alaikum|kemon\\s+acho|good\\s+morning|good\\s+evening|good\\s+afternoon|good\\s+night|halo|ola|sup|yo|hy)(?:\\s+.*)?$|" +
        "^(?:কেমন\\s+আছো|কেমন\\s+আছেন|সালাম|হ্যালো|হাই)$",
        Pattern.CASE_INSENSITIVE
    )

    // Telemetry & DB Stats Pattern
    private val PATTERN_DATABASE_STATS = Pattern.compile(
        ".*\\b(?:db|database)\\s+(?:stats|telemetry|info|status|summary).*|" +
        ".*\\b(?:stats|telemetry)\\s+(?:dekhao|dekhaw|show).*|" +
        "^(?:db stats|database stats)$",
        Pattern.CASE_INSENSITIVE
    )

    // Financial Analytics Patterns (Balance, Highest Expense, General Summary)
    private val PATTERN_BALANCE_QUERY = Pattern.compile(
        "\\bbalan[cs]e\\b|ব্যালেন্স|\\bbaki\\b|বাকি|\\bsavings?\\b|(?:shonchoy|sonchoy)|সঞ্চয়|oboshishto|অবশিষ্ট|" +
        "save\\s+(?:holo|hoise|hoilo|korlam|koto)|koto\\s+save|jomse|jomlo|jomalam|" +
        "joma\\s+(?:holo|hoise|ache)|bachlo|bachlam|বাঁচলো|জমলো|সেভ\\s*(?:হলো|হইছে)|জমা\\s*আছে|" +
        "\\bpocket(?:\\s*e)?\\s+koto|\\bha?te\\s+koto|\\bcash\\s+(?:taka|koto)|\\bkache\\s+koto|\\bbaki\\s+ache|" +
        "(?:(?:tk|taka)\\s*)?(?:poysha|poisha|poysa|poisa|paisa|peysa)(?:r|er)?(?:\\s+(?:hisab|hishab))?|টাকা\\s*প[য়য়]সা(?:র)?(?:\\s+হিসাব)?|পকেটে|হাতে\\s*কত|ক্যাশ\\s*কত|" +
        "\\bkoto.*(?:ache|ase|ace|roilo)\\b|" +
        "কত.*(?:আছে|রইলো|রইল)",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_HIGHEST_EXPENSE = Pattern.compile(
        "highest|(?:shobcheye|sobcheye)\\s+(?:beshi|boro)|biggest\\s+expense|max(?:imum)?\\s+(?:expense|khoroch)|top\\s+expense|" +
        "beshi\\s+taka.*(?:kothay|khoroch|khoros|koroch|gese|gelo|hoise)",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_GENERAL_SUMMARY = Pattern.compile(
        "(?:full|this|ei|current)\\s+month(?:er)?|ei\\s+mash(?:er)?|মাস(?:ের)?|সারামাসের|পুরো\\s+মাসের|" +
        "month(?:ly)?\\s+data|monthly\\s+summary|monthly\\s+expense|monthly\\s+report|" +
        "all\\s+data|shob\\s+data|full\\s+data|all\\s+transactions?|show\\s+data|" +
        "data\\s+dekhao|all\\s+expense|shob\\s+khoroch|total\\s+khoroch|shob\\s+hisab|report|koto\\s+khoroch",
        Pattern.CASE_INSENSITIVE
    )

    // Period Summary Patterns
    private val PATTERN_PERIOD_TODAY = Pattern.compile(
        "today|ajke|ajker|ajk|aaj|saradin|saradine|all\\s+day|আজ|আজকে|আজকের|সারাদিন|সারাদিনের",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_PERIOD_YESTERDAY = Pattern.compile(
        "yesterday|gotokal|gotokaler|gotokalke|goto\\s+kal|কালকের|গতকালের|গতকাল",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_PERIOD_WEEK = Pattern.compile(
        "(?:this|ai|ei|current)\\s+(?:week|shoptah|shoptaho)(?:er)?|shoptahir|এই\\s*সপ্তাহ(?:ের)?",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_SPEND_INDICATOR = Pattern.compile(
        "khoroch|khoros|koroch|expense|summary|hisab|hishab|gese|gelo|holo|hoise|data|cost|koto",
        Pattern.CASE_INSENSITIVE
    )

    // Item Total Query Patterns
    private val PATTERN_AMOUNT_GUARD = Pattern.compile("\\d+\\s*(?:tk|taka)", Pattern.CASE_INSENSITIVE)

    private val PATTERN_PERIOD_TIMEFRAME = Pattern.compile(
        "\\b(?:(?:ai|ei|this)\\s+(?:week|shoptah|shoptaho|mash|month|bochor|year)(?:\\s*[ae])?|today|ajke|ajk|gotokal|yesterday)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_EN = Pattern.compile(
        "^(?:how much(?: did i spend)?(?: spent)?(?: on)?|total spent on)\\s+(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_TOTAL_PREFIX = Pattern.compile(
        "^(?:total|mot|moth)\\s+(.+?)(?:\\s+(?:vara|bhara|khoroch|cost|bill))?(?:\\s+koto(?:\\s*(?:tk|taka))?)?(?:\\s+(?:gese|gelo|hoise|holo))?$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_INFLECTED = Pattern.compile(
        "^(.+?)(?:\\s+(?:khate|khat|babod|babode|category(?:\\s+te)?|kategori(?:\\s+te)?|sector|sectore|a|e|te|ta|tay|er|ar|ye|ke|k|re)|(?:e|te|er|ye))?(?:\\s+(?:cost|khoroch|vara|bhara|bill|mot|total))?\\s+(?:koto|total)(?:\\s*(?:tk|taka))?(?:\\s+(?:cost|khoroch|khoros|koroch|gese|gelo|galo|giyese|geche|gache|giese|lagse|lagce|laglo|lagsilo|holo|hoise|disi|dilam|pathaisi|pathalam|send korsi)(?:\\s*(?:hoise|holo|hoyeche|gese|gelo))?)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_VARA = Pattern.compile(
        "^(.+?)(?:\\s+(?:ar|er))?(?:\\s+(?:vara|bhara|bada|khoroch|khoros|koroch|cost|bill))\\s+(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?(?:\\s+(?:gese|gelo|geche|gache|giese|lagse|lagce|laglo|lagsilo|holo|hoise|disi|dilam))?$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_KOTO_PREFIX = Pattern.compile(
        "^(?:mot\\s+)?koto(?:\\s*(?:tk|taka))?\\s+(.+?)(?:\\s+(?:gese|gelo|galo|geche|gache|giese|hoise|holo|lagse|lagce|laglo|lagsilo|disi|dilam|pathaisi|pathalam))?$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_ITEM_TOTAL_HISAB_SUFFIX = Pattern.compile(
        "^(.+?)(?:\\s+(?:er|ar))?\\s+(?:hisab|hishab)(?:\\s+(?:dao|den|dekhao|dekhaw|bolo|bolen|show|list))?$",
        Pattern.CASE_INSENSITIVE
    )

    // Transaction & Date Parsing Patterns
    private val PATTERN_DATE_EXPLICIT_DAY = Pattern.compile(
        "\\b(?:(?:tarikh|tarik|tarek|date)\\s*(\\d{1,2})|(\\d{1,2})\\s*(?:tarikh|tarik|tarek|tarike|tarikhe|tarikher|date|th\\s*e|th\\b|no\\b|number\\b))\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_AMOUNT_FIRST_TX = Pattern.compile(
        "^(\\d+(?:\\.\\d+)?)\\s*(?:tk)?\\s+(.+)$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_AMOUNT_LAST_TX = Pattern.compile(
        "^(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(?:tk)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val PATTERN_CONSECUTIVE_EXPENSES = Pattern.compile(
        "(\\d+(?:\\.\\d+)?)\\s*(?:tk)?\\s+([a-zA-Z\u0980-\u09FF]+(?:\\s+[a-zA-Z\u0980-\u09FF]+)*)"
    )

    private val PATTERN_AMOUNT_EXTRACT = Pattern.compile(
        "(?:(?:tk|taka)\\s*)?(\\d+(?:\\.\\d+)?)(?:\\s*(?:tk|taka))?",
        Pattern.CASE_INSENSITIVE
    )

    fun parseSingleExpensePart(rawPart: String, defaultOccurredAt: Long? = null): SingleItemConfirmation? {
        val part = rawPart.trim().removePrefix("and").removePrefix("ar").removePrefix("ebong").removePrefix("এবং").removePrefix("আর").trim()
        if (part.isBlank()) return null

        // Pattern A: "<amount> tk <note>" or "<amount> <note>"
        val m1 = PATTERN_AMOUNT_FIRST_TX.matcher(part)
        if (m1.find()) {
            val amount = m1.group(1)?.toDoubleOrNull() ?: return null
            var note = m1.group(2)?.trim() ?: ""
            note = cleanExpenseNote(note.removePrefix("tk").trim())
            if (note.isNotBlank() && !isDateWord(note) && amount > 0) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return SingleItemConfirmation(
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = defaultOccurredAt
                )
            }
        }

        // Pattern B: "<note> <amount> tk" or "<note> <amount>"
        val m2 = PATTERN_AMOUNT_LAST_TX.matcher(part)
        if (m2.find()) {
            var note = m2.group(1)?.trim() ?: ""
            val amount = m2.group(2)?.toDoubleOrNull() ?: return null
            note = note.removePrefix("ajke").removePrefix("aj").trim()
            note = cleanExpenseNote(note)
            if (note.isNotBlank() && !isDateWord(note) && amount > 0) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                return SingleItemConfirmation(
                    amountMinor = (amount * 100).toLong(),
                    category = category,
                    note = note,
                    occurredAt = defaultOccurredAt
                )
            }
        }

        // Pattern C: Natural Language clause fallback: e.g. "60 tk diye nasta korsi", "dupure 180 tk lunch"
        val natural = parseNaturalLanguageExpense(part, defaultOccurredAt)
        if (natural != null && natural.amountMinor != null && !natural.note.isNullOrBlank() && !isDateWord(natural.note)) {
            return SingleItemConfirmation(
                amountMinor = natural.amountMinor,
                category = natural.category ?: "Other",
                note = natural.note,
                occurredAt = defaultOccurredAt
            )
        }

        return null
    }

    fun parseMultipleExpenses(text: String, defaultOccurredAt: Long? = null): List<SingleItemConfirmation> {
        // Try splitting by comma, semicolon, newline, or conjunction words
        val parts = text.split(Regex("[,;\\n+]|\\s+(?:and|ar|ebong|plus|&|এবং|আর)\\s+"))
        if (parts.size >= 2) {
            val parsedList = parts.mapNotNull { parseSingleExpensePart(it, defaultOccurredAt) }
            if (parsedList.size >= 2) {
                return parsedList
            }
        }

        // Fallback: match consecutive pattern without explicit commas: "50 tk sosage 320 tk murgi 420 tk bazar"
        val matcher = PATTERN_CONSECUTIVE_EXPENSES.matcher(text)
        val foundItems = mutableListOf<SingleItemConfirmation>()
        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            val nt = matcher.group(2)?.trim()
            if (amt != null && amt > 0 && !nt.isNullOrBlank()) {
                val cleanNote = cleanExpenseNote(nt)
                if (cleanNote.isNotBlank() && !isDateWord(cleanNote)) {
                    val cat = TransactionValidator.mapToValidCategory(null, cleanNote, com.example.expenseassistant.data.TransactionType.EXPENSE)
                    foundItems.add(
                        SingleItemConfirmation(
                            amountMinor = (amt * 100).toLong(),
                            category = cat,
                            note = cleanNote,
                            occurredAt = defaultOccurredAt
                        )
                    )
                }
            }
        }
        if (foundItems.size >= 2) {
            return foundItems
        }

        return emptyList()
    }

    fun isGreetingOrHelp(text: String): Pair<Boolean, Boolean>? {
        val t = text.lowercase().trim()
        if (PATTERN_HELP.matcher(t).find()) {
            return Pair(true, true)
        }
        val greetings = setOf("hi", "hello", "hey", "salam", "assalamu alaikum", "kemon acho", "good morning", "good evening", "good afternoon", "halo", "ola")
        if (greetings.contains(t) || t.startsWith("hi ") || t.startsWith("hello ") || t.startsWith("hey ") || PATTERN_GREETING.matcher(t).matches()) {
            return Pair(true, false)
        }
        return null
    }

    fun isMockDataRequest(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("mock") || t.contains("dummy data") || t.contains("seed data") ||
                (t.contains("push") && t.contains("transaction")) ||
                (t.contains("add") && t.contains("transaction") && (t.contains("1000") || t.contains("100")))
    }

    fun isBalanceQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        return PATTERN_BALANCE_QUERY.matcher(t).find()
    }

    fun isHighestExpenseQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        return PATTERN_HIGHEST_EXPENSE.matcher(t).find()
    }

    fun parsePeriodSummary(text: String): String? {
        val t = text.lowercase().trim()
        val specificItems = listOf("bus", "vara", "bhara", "bazar", "cha", "tea", "coffee", "rickshaw", "food", "lunch", "dinner", "nasta", "medicine", "bill", "recharge", "wifi", "rent", "shopping", "cloth", "uber", "pathao")
        if (specificItems.any { t.contains(it) }) {
            return null
        }

        val hasSpendIndicator = PATTERN_SPEND_INDICATOR.matcher(t).find()
        val isToday = PATTERN_PERIOD_TODAY.matcher(t).find() ||
                ((t.startsWith("aj ") || t.startsWith("aaj ")) && (t.contains("khoroch") || t.contains("khoros") || t.contains("koroch") || t.contains("cost")))

        if (isToday && hasSpendIndicator) {
            return "today"
        }
        if (PATTERN_PERIOD_YESTERDAY.matcher(t).find() && hasSpendIndicator) {
            return "yesterday"
        }
        if (PATTERN_PERIOD_WEEK.matcher(t).find() && hasSpendIndicator) {
            return "week"
        }
        return null
    }

    fun cleanQueryKeyword(raw: String): String {
        var k = raw.trim().lowercase()
        k = k.removePrefix("total").removePrefix("shob").removePrefix("mot").removePrefix("moth").trim()
        k = k.removePrefix("amar ").removePrefix("amader ").trim()
        k = k.removeSuffix("koto").removeSuffix("koto taka").trim()

        val genericWords = setOf(
            "vara", "bhara", "bada", "khoroch", "khoros", "koroch", "cost", "bill", "expense", "spending",
            "taka", "tk", "gese", "gelo", "galo", "giyese", "geche", "gache", "giese", "gesilo",
            "hoise", "hoiche", "holo", "lagse", "lagce", "laglo", "lagsilo",
            "disi", "dilam", "korsi", "kinlam", "paisi", "pelam",
            "category", "kategori", "khate", "khat", "sector", "sectore", "babod", "babode", "bapari", "bapare",
            "খাতে", "বাবদ", "ক্যাটাগরি"
        )
        val parts = k.split(Regex("\\s+")).filter { it.isNotBlank() }
        val nonGeneric = parts.filter { it !in genericWords }

        val base = if (nonGeneric.isNotEmpty()) nonGeneric.joinToString(" ") else k

        var trimmed = base.trim()
        val caseSuffixes = listOf(" ke", " k", " re", " te", " er", " ar", " e", " ye", " a", " ta", " tay", " babod", " khate")
        for (suffix in caseSuffixes) {
            if (trimmed.endsWith(suffix, ignoreCase = true)) {
                trimmed = trimmed.substring(0, trimmed.length - suffix.length).trim()
                break
            }
        }
        return if (trimmed.isNotBlank()) trimmed else raw.trim()
    }

    private fun isStopWord(word: String): Boolean {
        val w = word.lowercase().trim()
        val stops = setOf(
            "khoroch", "khoros", "koroch", "hisab", "hishab", "summary", "balance", "balace",
            "data", "expense", "taka", "tk", "all", "shob", "sob", "total", "report",
            "poysha", "poyshar", "poisha", "poishar", "cash",
            "mash", "mashe", "masher", "ei mash", "ei mashe", "ei masher", "month", "this month",
            "week", "ai week", "ei week", "this week", "shoptah", "ei shoptah", "ei shoptahir",
            "today", "ajke", "ajker", "gotokal", "gotokaler", "yesterday"
        )
        return stops.contains(w)
    }

    fun parseItemTotalQueryDetails(text: String): ParsedItemTotal? {
        val clean = text.replace("?", "").trim()
        if (clean.isBlank()) return null
        if (PATTERN_AMOUNT_GUARD.matcher(clean).find()) return null

        // Detect and extract any timeframe expressions
        val pMatcher = PATTERN_PERIOD_TIMEFRAME.matcher(clean)
        var foundPeriod: String? = null
        var textWithoutPeriod = clean
        if (pMatcher.find()) {
            val pStr = pMatcher.group().lowercase()
            foundPeriod = when {
                pStr.contains("week") || pStr.contains("shoptah") -> "week"
                pStr.contains("mash") || pStr.contains("month") -> "month"
                pStr.contains("today") || pStr.contains("aj") -> "today"
                pStr.contains("gotokal") || pStr.contains("yesterday") -> "yesterday"
                pStr.contains("bochor") || pStr.contains("year") -> "year"
                else -> null
            }
            textWithoutPeriod = clean.removeRange(pMatcher.start(), pMatcher.end()).replace(Regex("\\s+"), " ").trim()
        }

        // 1. English: "how much (did i spend )?(spent )?(on )?<item>"
        val mEn = PATTERN_ITEM_TOTAL_EN.matcher(textWithoutPeriod)
        if (mEn.find()) {
            val target = mEn.group(1)?.trim()
            if (!target.isNullOrBlank() && !isStopWord(target)) {
                return ParsedItemTotal(cleanQueryKeyword(target), foundPeriod)
            }
        }

        // 2. "(total|mot) <item> [vara|khoroch|cost]? [koto|koto taka|koto tk]?"
        val mTotal = PATTERN_ITEM_TOTAL_TOTAL_PREFIX.matcher(textWithoutPeriod)
        if (mTotal.find()) {
            val raw = mTotal.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return ParsedItemTotal(cleanQueryKeyword(raw), foundPeriod)
            }
        }

        // 3. "<item> [a/e/te/ta/tay/er/ar/ye/ke/k/re] [cost/khoroch/vara/bill/mot/total]? koto [tk/taka]? [cost/khoroch/gese/gelo/hoise/holo/disi/dilam/pathaisi]?"
        val mInf = PATTERN_ITEM_TOTAL_INFLECTED.matcher(textWithoutPeriod)
        if (mInf.find()) {
            val raw = mInf.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return ParsedItemTotal(cleanQueryKeyword(raw), foundPeriod)
            }
        }

        // 4. "<item> [vara/khoroch/cost/bill] [mot]? koto [tk/taka]?"
        val mVara = PATTERN_ITEM_TOTAL_VARA.matcher(textWithoutPeriod)
        if (mVara.find()) {
            val raw = mVara.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return ParsedItemTotal(cleanQueryKeyword(raw), foundPeriod)
            }
        }

        // 5. "[mot]? koto [tk/taka] <item> [vara/khoroch/e/te/ke/k] [gese/gelo/geche/hoise/holo/lagse/lagce/laglo/lagsilo/disi/dilam/pathaisi/pathalam))?$"
        val mKoto = PATTERN_ITEM_TOTAL_KOTO_PREFIX.matcher(textWithoutPeriod)
        if (mKoto.find()) {
            val raw = mKoto.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return ParsedItemTotal(cleanQueryKeyword(raw), foundPeriod)
            }
        }

        // 6. "<item/person> [er/ar]? hisab [dao/dekhao/bolo]?" -> e.g. "rahim er hisab", "bus er hisab", "bazar er hisab dao", "bazar er hisab dekhao"
        val mHisab = PATTERN_ITEM_TOTAL_HISAB_SUFFIX.matcher(textWithoutPeriod)
        if (mHisab.find()) {
            val raw = mHisab.group(1)?.trim()
            if (!raw.isNullOrBlank() && !isStopWord(raw)) {
                return ParsedItemTotal(cleanQueryKeyword(raw), foundPeriod)
            }
        }

        return null
    }

    fun parseItemTotalQuery(text: String): String? = parseItemTotalQueryDetails(text)?.item

    fun isGeneralSummaryQuery(text: String): Boolean {
        val t = text.lowercase().trim()
        val specificItems = listOf("bus", "vara", "bhara", "bazar", "cha", "tea", "coffee", "rickshaw", "food", "lunch", "dinner", "nasta", "medicine", "bill", "recharge", "wifi", "rent", "shopping", "cloth", "uber", "pathao")
        if (specificItems.any { t.contains(it) }) {
            return false
        }

        return PATTERN_GENERAL_SUMMARY.matcher(t).find()
    }

    fun cleanConversationalPrefix(raw: String): String {
        var s = raw.trim()
        val prefixes = listOf(
            "can you please tell me ", "could you please tell me ",
            "can you tell me ", "could you tell me ", "please tell me ", "tell me ",
            "can you show me ", "could you show me ", "please show me ", "show me ",
            "i want to know ", "can you check ", "could you check ", "please check ", "check ",
            "bolo to ", "bolo ", "janano jabe ", "dekhao to ", "dekhao ",
            "ami jante chai ", "ektu bolo to ", "ektu bolben ", "ektu janan ",
            "please save this expense: ", "please save this expense ", "please save this transaction: ", "please save this transaction ", "please save this: ", "please save this ", "please save: ", "please save ",
            "save this expense: ", "save this expense ", "save this transaction: ", "save this transaction ", "save this: ", "save this ", "save it: ", "save it ", "save expense: ", "save expense ", "save: ",
            "save koro: ", "save koro ", "save korun: ", "save korun ", "save kore rakho: ", "save kore rakho ", "save kore rakhba: ", "save kore rakhba ",
            "save kore dao: ", "save kore dao ", "save koro to: ", "save koro to ",
            "likhe rakho to: ", "likhe rakho to ", "likhe rakho: ", "likhe rakho ",
            "likhe rakhun: ", "likhe rakhun ", "likhe rakhba: ", "likhe rakhba ",
            "khatai tolo: ", "khatai tolo ", "khatai tule rakho: ", "khatai tule rakho ",
            "hishab e tolo: ", "hishab e tolo ", "hishabe tolo: ", "hishabe tolo ", "hishab rakho: ", "hishab rakho ",
            "hishab e jog koro: ", "hishab e jog koro ",
            "note kore rakho: ", "note kore rakho ", "note down: ", "note down ", "note koro: ", "note koro ",
            "tukiye rakho: ", "tukiye rakho ", "tuke rakho: ", "tuke rakho ",
            "entry koro: ", "entry koro ", "entry korun: ", "entry korun ",
            "record this expense: ", "record this: ", "record this ", "record koro: ", "record koro ", "record korun: ", "record korun ",
            "add this expense: ", "add this: ", "add this ", "add koro: ", "add koro ", "add korun: ", "add korun ",
            "log koro: ", "log koro ", "log this: ", "log this ",
            "লিখে রাখো: ", "লিখে রাখো ", "লিখে রাখুন: ", "লিখে রাখুন ",
            "সেভ করো: ", "সেভ করো ", "সেভ করে রাখো: ", "সেভ করে রাখো ", "সেভ করুন: ", "সেভ করুন ", "সেভ করে দিন: ", "সেভ করে দিন ", "সেভ করে দাও: ", "সেভ করে দাও ",
            "খাতায় তোলো: ", "খাতায় তোলো ", "হিসাবে তোলো: ", "হিসাবে তোলো ", "হিসাব রাখো: ", "হিসাব রাখো ",
            "তুলে রাখো: ", "তুলে রাখো ", "টুকে রাখো: ", "টুকে রাখো ",
            "নোট করে রাখো: ", "নোট করে রাখো ", "এন্ট্রি করো: ", "এন্ট্রি করো ", "লগ করো: ", "লগ করো "
        )
        for (p in prefixes) {
            if (s.startsWith(p, ignoreCase = true)) {
                s = s.substring(p.length).trim().removePrefix(":").trim()
                break
            }
        }
        return s
    }

    fun cleanExpenseNote(rawNote: String): String {
        val verbs = setOf(
            "dilam", "delam", "dilm", "disi", "dici", "diasilam", "disilam", "dise", "disen", "diyechi", "dieci", "ditesi", "dese", "dite", "dilo", "delo",
            "korsi", "korci", "korechi", "koreci", "koresi", "korlam", "korsilam", "korchilam", "kore", "kora",
            "kinlam", "kinci", "kinechi", "kinesilam", "kinsi", "kinbo", "kina", "kinte",
            "kheyechi", "khelam", "khailam", "khaisi", "khawa",
            "peyechi", "peyesi", "pelam", "paisi", "paise", "paici", "payci", "pailam", "payesi",
            "pathalam", "pathaisi", "pathaylam", "pathayechi", "pathiyechi", "pathano", "pathate", "pathaise",
            "send", "sent", "transfer", "pathechi", "patheyasi",
            "laglo", "lagse", "lagce", "lagbe", "lagsilo",
            "gese", "gelo", "galo", "giyese", "gache", "giese", "gesilo", "holo", "hoise",
            "diye", "diya", "dia", "dya", "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "theke", "thika", "theikha", "theikei",
            "jonno", "jnne", "jonne",
            "tarikh", "tarik", "tarek", "tarike", "tarikhe", "date",
            "spent", "paid", "bought", "had", "ate", "cost", "add", "record", "log", "please", "enter",
            "save", "saved", "saving", "rakho", "rakhun", "rakhba", "rekhecho", "likhe", "likhun", "likho", "likhlam",
            "khatai", "khata", "tolo", "tulun", "tullo", "tukiye", "tuke", "tule", "hishab", "hishabe", "entry", "note", "jog", "dao", "den", "batil",
            "সেভ", "করো", "করুন", "রাখো", "রাখুন", "লিখে", "খাতায়", "তোলো", "তুলে", "টুকে", "হিসাব", "হিসাবে", "নোট", "এন্ট্রি", "দাও", "দিন", "যোগ", "বাতিল"
        )
        val words = rawNote.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val filtered = words.filter { it.lowercase() !in verbs }
        var result = if (filtered.isNotEmpty()) filtered.joinToString(" ") else rawNote.trim()
        if (result.contains(" ")) {
            val parts = result.split(Regex("\\s+"))
            if (parts.size == 2 && (parts[1].lowercase() in setOf("ke", "re") || (parts[1].lowercase() == "k" && BanglishTransliterator.isKnownNameOrMerchant(parts[0])))) {
                result = parts[0]
            }
        }
        if (result.endsWith("ke", ignoreCase = true) && result.length > 4) {
            val stem = result.substring(0, result.length - 2)
            if (BanglishTransliterator.isKnownNameOrMerchant(stem)) {
                result = stem
            }
        }
        return result
    }

    fun extractDateAndClean(rawText: String): ExtractedDate {
        var text = rawText
        var targetTime: Long? = null

        // 1. Explicit Day of Month: e.g. "4 tarikh", "4 tarikhe", "tarikh 4", "4 date", "15 tarikh", "4 tarik", "4 tarek"
        val matcher = PATTERN_DATE_EXPLICIT_DAY.matcher(text)
        if (matcher.find()) {
            val dayStr = matcher.group(1) ?: matcher.group(2)
            val day = dayStr?.toIntOrNull()
            if (day != null && day in 1..31) {
                val cal = java.util.Calendar.getInstance()
                val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                cal.set(java.util.Calendar.DAY_OF_MONTH, day)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                // If day is greater than today's day of month, user likely meant last month
                if (day > currentDay) {
                    cal.add(java.util.Calendar.MONTH, -1)
                }
                targetTime = cal.timeInMillis
                text = text.removeRange(matcher.start(), matcher.end()).trim()
            }
        }

        // 2. Relative Days: "gotokal", "yesterday", "porshu", "ajke", "today"
        if (targetTime == null) {
            val lower = text.lowercase()
            if (lower.contains("gotokal") || lower.contains("yesterday")) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                targetTime = cal.timeInMillis
                text = text.replace(Regex("\\b(?:gotokal|gotokalke|gotokaler|yesterday)\\b", RegexOption.IGNORE_CASE), " ").trim()
            } else if (lower.contains("porshu") || lower.contains("gotoporshu")) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -2)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                targetTime = cal.timeInMillis
                text = text.replace(Regex("\\b(?:gotoporshu|porshu)\\b", RegexOption.IGNORE_CASE), " ").trim()
            }
        }

        return ExtractedDate(
            cleanedText = text.replace(Regex("\\s+"), " ").trim(),
            timestamp = targetTime
        )
    }

    fun parseIncomeTransaction(text: String, defaultOccurredAt: Long? = null): QuickParseResult? {
        val t = text.lowercase().trim()
        val isStaffWageExpense = t.contains("daroan") || t.contains("darowan") ||
                t.contains("bua") || t.contains("driver") || t.contains("kormochari") ||
                t.contains("dilam") || t.contains("disi") || t.contains("diasilam")
        if (isStaffWageExpense) return null

        val incomeKeywords = listOf("salary", "salay", "salari", "beton", "veton", "income", "bonus", "cashback", "remittance", "credited", "deposit")
        val receiveVerbs = listOf("paisi", "peyechi", "paise", "paichi", "pelam", "peyesi", "payci", "received", "got", "dhukse", "dhuklo", "ashlo", "aslo")

        val hasIncomeKeyword = incomeKeywords.any { t.contains(it) }
        val hasReceiveVerb = receiveVerbs.any { t.contains(it) }

        // Person giving money to user: e.g. "rahim 500 taka dilo", "karim 1000 tk dise", "abbu 5000 pathaise"
        // Must NOT match giving money to others: "rahim ke dilam", "rahim k dilam", "dilam", "disi", "pathalam"
        val isGivenToUser = (t.contains("dilo") || t.contains("delo") || t.contains("dise") || t.contains("pathaise") || t.contains("pathalo")) &&
                !t.contains(Regex("\\b(?:ke|k|re|dilam|delam|disi|dici|pathalam|pathaisi)\\b"))

        if (!hasIncomeKeyword && !hasReceiveVerb && !isGivenToUser) return null

        // Ignore if this is an interrogative or check query
        if (t.contains("koto") || t.contains("how much") || t.contains("check") || t.contains("bolo")) return null

        // Extract amount
        val matcher = PATTERN_AMOUNT_EXTRACT.matcher(t)
        var foundAmount: Double? = null
        var matchStart = -1
        var matchEnd = -1

        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            if (amt != null && amt > 0) {
                foundAmount = amt
                matchStart = matcher.start()
                matchEnd = matcher.end()
                break
            }
        }

        if (foundAmount == null || matchStart == -1) return null

        val beforeAmt = t.substring(0, matchStart)
        val afterAmt = t.substring(matchEnd)
        val textWithoutAmt = "$beforeAmt $afterAmt".trim()

        val stopWords = setOf(
            "ami", "amra", "amar", "tumi", "apni", "amader",
            "aj", "ajke", "kalke", "gotokal", "tarikh", "tarik", "tarike", "tarikhe", "date",
            "tk", "taka", "takar", "tkr", "theke", "thika", "theika", "er", "ar", "diye", "kach", "kache", "kachtheke",
            "paisi", "peyechi", "paise", "paichi", "pelam", "received", "got", "dhukse", "dhuklo",
            "dilo", "delo", "dise", "pathaise", "pathalo",
            "save", "rakho", "likhe", "note", "entry", "khatai", "tolo", "tuke", "hishab",
            "সেভ", "করো", "রাখো", "লিখে", "নোট", "এন্ট্রি"
        )
        val words = textWithoutAmt.split(Regex("[^a-zA-Z\u0980-\u09FF]+")).filter { it.isNotBlank() }
        val filtered = words.filter { it !in stopWords }

        var note = if (filtered.isNotEmpty()) {
            filtered.joinToString(" ").replaceFirstChar { it.uppercase() }
        } else {
            "Salary"
        }
        if (note.equals("salay", ignoreCase = true)) note = "Salary"

        return QuickParseResult(
            matched = true,
            intent = "prepare_income",
            amountMinor = (foundAmount * 100).toLong(),
            category = "Salary",
            note = note,
            occurredAt = defaultOccurredAt
        )
    }

    fun parseNaturalLanguageExpense(text: String, defaultOccurredAt: Long? = null): QuickParseResult? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val matcher = PATTERN_AMOUNT_EXTRACT.matcher(clean)
        var foundAmount: Double? = null
        var matchStart = -1
        var matchEnd = -1

        while (matcher.find()) {
            val amt = matcher.group(1)?.toDoubleOrNull()
            if (amt != null && amt > 0) {
                foundAmount = amt
                matchStart = matcher.start()
                matchEnd = matcher.end()
                break
            }
        }

        if (foundAmount == null || matchStart == -1) return null

        val beforeAmt = clean.substring(0, matchStart)
        val afterAmt = clean.substring(matchEnd)
        val textWithoutAmt = "$beforeAmt $afterAmt".lowercase()

        val stopWords = setOf(
            "ami", "amra", "amar", "tumi", "apni", "amader", "tar", "tader", "ke", "der",
            "aj", "ajke", "ajk", "aaj", "kalke", "gotokal", "dupure", "shokale", "raate", "bikale", "shondhay",
            "tarikh", "tarik", "tarek", "tarike", "tarikhe", "date",
            "diye", "diya", "dia", "dite", "diasilam", "disilam", "dilam", "delam", "dilm", "disi", "dici", "dise", "disen", "diyechi", "dieci",
            "korsi", "korci", "korechi", "koreci", "koresi", "korlam", "korsilam", "korchilam", "kore", "kora",
            "kheyechi", "khelam", "khailam", "khaisi", "khawa",
            "kinlam", "kinci", "kinechi", "kinesilam", "kinsi", "kinbo", "kina", "kinte",
            "pathalam", "pathaisi", "pathaylam", "pathayechi", "pathiyechi", "pathano", "pathate",
            "send", "sent", "transfer", "pathechi", "patheyasi",
            "laglo", "lagse", "lagce", "lagbe", "lagsilo",
            "gese", "gelo", "galo", "giyese", "gache", "giese", "gesilo", "holo", "hoise",
            "theke", "thika", "theikha", "theikei", "er", "ar", "ebong", "jonno", "jnne", "jonne",
            "ashte", "ashar", "ashlam", "jete", "jawar", "jawyar", "gelam", "jaite", "jaoar",
            "pothe", "shomoy", "hoye", "nilam", "nisi", "ferar",
            "expense", "khoroch", "khoros", "koroch", "tk", "taka", "takar", "tkr", "te", "e", "ye",
            "in", "at", "for", "on", "a", "an", "the", "i", "we", "my",
            "spent", "paid", "bought", "had", "ate", "cost",
            "can", "could", "you", "please", "add", "record", "log", "enter", "track", "save", "write",
            "rakho", "rakhun", "rakhba", "rekhecho", "likhe", "likhun", "likho", "likhlam",
            "khatai", "khata", "tolo", "tulun", "tullo", "tukiye", "tuke", "tule", "hishab", "hishabe", "entry", "note", "jog", "dao", "den", "batil",
            "সেভ", "করো", "করুন", "রাখো", "রাখুন", "লিখে", "খাতায়", "তোলো", "তুলে", "টুকে", "হিসাব", "হিসাবে", "নোট", "এন্ট্রি", "দাও", "দিন", "যোগ", "বাতিল"
        )

        val words = textWithoutAmt.split(Regex("[^a-zA-Z\u0980-\u09FF]+")).filter { it.isNotBlank() }
        val filteredWords = words.filter { it !in stopWords }

        var note = if (filteredWords.isNotEmpty()) {
            filteredWords.joinToString(" ")
        } else if (words.isNotEmpty()) {
            words.joinToString(" ")
        } else {
            "Expense"
        }

        val isStaffWageExpense = textWithoutAmt.contains("daroan") || textWithoutAmt.contains("darowan") ||
                textWithoutAmt.contains("bua") || textWithoutAmt.contains("driver") ||
                clean.contains("dilam") || clean.contains("disi") || clean.contains("diasilam")

        val isIncome = !isStaffWageExpense && (textWithoutAmt.contains("salary") || textWithoutAmt.contains("salay") ||
                textWithoutAmt.contains("salari") || textWithoutAmt.contains("beton") ||
                textWithoutAmt.contains("veton") || textWithoutAmt.contains("bonus") ||
                textWithoutAmt.contains("income") || textWithoutAmt.contains("cashback") ||
                textWithoutAmt.contains("paisi") || textWithoutAmt.contains("peyechi") ||
                textWithoutAmt.contains("pelam") || textWithoutAmt.contains("payci"))

        val intent = if (isIncome) "prepare_income" else "prepare_expense"
        val category = if (isIncome) "Salary" else TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
        if (isIncome && (note.equals("salay", ignoreCase = true) || note.isBlank())) {
            note = "Salary"
        }

        val hasCurrency = clean.contains("tk") || clean.contains("taka")
        val hasTransferMarker = clean.contains(Regex("\\b(?:ke|k|re|dilam|delam|disi|dici|dise|dilo|pathalam|pathaisi)\\b", RegexOption.IGNORE_CASE))
        val isPersonOrMerchant = BanglishTransliterator.isKnownNameOrMerchant(note)
        val hasFinancialAnchor = category != "Other" || isIncome || hasPhoneticFinancialAnchor(clean, note) || hasTransferMarker || isPersonOrMerchant
        if (!hasCurrency && !hasFinancialAnchor) {
            return null
        }

        return QuickParseResult(
            matched = true,
            intent = intent,
            amountMinor = (foundAmount * 100).toLong(),
            category = category,
            note = note,
            occurredAt = defaultOccurredAt
        )
    }

    /**
     * High-speed deterministic regex parser for common Bangladeshi expense patterns and queries.
     * Yields instant response times for natural language questions, summaries, and expense sentences.
     */
    fun tryFastDeterministicParse(rawInput: String): QuickParseResult {
        val unwrap = cleanConversationalPrefix(rawInput)
        val text = normalize(unwrap)

        // 1. Check for greetings / help
        val greetingHelp = isGreetingOrHelp(text)
        if (greetingHelp != null) {
            val isHelp = greetingHelp.second
            return QuickParseResult(
                matched = true,
                intent = "get_help_or_greeting",
                searchQuery = if (isHelp) "help" else "greeting"
            )
        }

        // 1.2 Check for cancel / negative / no-op (Anti-Hallucination Guard)
        if (PATTERN_CANCEL_OR_NEGATIVE.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "get_help_or_greeting",
                searchQuery = "cancel"
            )
        }

        // 1.5 Check for mock data seeding
        if (isMockDataRequest(text)) {
            return QuickParseResult(
                matched = true,
                intent = "seed_mock_transactions"
            )
        }

        // 1.6 Check for delete last transaction: "last expense delete koro", "delete last transaction", "shesh khoroch muche felo", "shesh hisab ta muche felo"
        if (PATTERN_DELETE_LAST_TX.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "delete_last_transaction"
            )
        }

        // 1.7 Check for delete specific transaction by ID: "delete transaction 5", "transaction 5 delete koro", "delete expense 5"
        val deleteTxMatcher = PATTERN_DELETE_TX_BY_ID.matcher(text)
        if (deleteTxMatcher.find()) {
            val idStr = deleteTxMatcher.group(1) ?: deleteTxMatcher.group(2)
            val txId = idStr?.toLongOrNull()
            if (txId != null) {
                return QuickParseResult(
                    matched = true,
                    intent = "delete_transaction",
                    id = txId
                )
            }
        }

        // 1.75 Check for update transaction by ID: "update transaction 5 to 500 tk", "transaction 5 change koro 250 tk"
        val updateTxMatcher = PATTERN_UPDATE_TX.matcher(text)
        if (updateTxMatcher.find()) {
            val idStr = updateTxMatcher.group(1) ?: updateTxMatcher.group(4) ?: updateTxMatcher.group(5)
            val txId = idStr?.toLongOrNull()
            if (txId != null) {
                val amtStr = updateTxMatcher.group(2) ?: updateTxMatcher.group(6)
                val amount = amtStr?.toDoubleOrNull()
                val rawNote = (updateTxMatcher.group(3) ?: updateTxMatcher.group(7))?.trim()
                val note = if (!rawNote.isNullOrBlank()) cleanExpenseNote(rawNote.removePrefix("note").trim()) else null
                val category = if (note != null) TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE) else null
                return QuickParseResult(
                    matched = true,
                    intent = "update_transaction",
                    id = txId,
                    amountMinor = if (amount != null && amount > 0) (amount * 100).toLong() else null,
                    note = note,
                    category = category
                )
            }
        }

        // 1.8 Check for clear all transactions: "clear all transactions", "shob transaction clear koro", "shob expense delete koro"
        if (PATTERN_CLEAR_ALL_TX.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "clear_all_transactions"
            )
        }

        // 1.9 Check for history / log / recent transactions / statements:
        // "history dekhao", "amar history dekhao", "transaction history", "expense history",
        // "log dekhao", "expense log", "daily log", "statement dekhao", "ledger dekhao", "khatiyan dekhao",
        // "shob transaction dekhao", "all transactions", "list transactions", "khorocher list dekhao", "khorocher talika",
        // "recent transactions dekhao", "shesh 5 ta transaction", "last 10 expenses"
        val historyLogMatcher = PATTERN_RECENT_LOG.matcher(text)
        if (historyLogMatcher.find()) {
            val limitStr = historyLogMatcher.group(1) ?: historyLogMatcher.group(2) ?: historyLogMatcher.group(3)
            val limit = limitStr?.toIntOrNull() ?: 10
            return QuickParseResult(
                matched = true,
                intent = "get_recent_transactions",
                limit = limit
            )
        }

        // 1.95 Check for explicit search / find queries:
        // Prefix: "search biryani", "find napa", "khujo rickshaw", "khuje dao medicine", "search for uber",
        //         "search koro medicine", "khuje ber koro napa", "khoj koro doctor"
        // Suffix: "napa search koro", "rickshaw khuje dao", "medicine khujo", "bazar khuje ber koro"
        val prefixSearchMatcher = PATTERN_PREFIX_SEARCH.matcher(text)
        if (prefixSearchMatcher.find()) {
            val q = prefixSearchMatcher.group(1)?.trim()
            if (!q.isNullOrBlank()) {
                val cleanedQ = cleanQueryKeyword(q)
                return QuickParseResult(
                    matched = true,
                    intent = "search_transactions",
                    searchQuery = cleanedQ
                )
            }
        }

        val suffixSearchMatcher = PATTERN_SUFFIX_SEARCH.matcher(text)
        if (suffixSearchMatcher.find()) {
            val q = suffixSearchMatcher.group(1)?.trim()
            if (!q.isNullOrBlank()) {
                val cleanedQ = cleanQueryKeyword(q)
                return QuickParseResult(
                    matched = true,
                    intent = "search_transactions",
                    searchQuery = cleanedQ
                )
            }
        }

        // 1.10 Check for list reminders: "reminder gula dekhao", "shob reminder dekhao", "reminders dekhao", "show reminders"
        if (PATTERN_LIST_REMINDERS.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "list_reminders"
            )
        }

        // 1.11 Check for delete reminder by ID: "delete reminder 2", "reminder 2 delete koro", "reminder 2 muche felo"
        val deleteRemMatcher = PATTERN_DELETE_REMINDER.matcher(text)
        if (deleteRemMatcher.find()) {
            val idStr = deleteRemMatcher.group(1) ?: deleteRemMatcher.group(2)
            val remId = idStr?.toLongOrNull()
            if (remId != null) {
                return QuickParseResult(
                    matched = true,
                    intent = "delete_reminder",
                    id = remId
                )
            }
        }

        // 1.12 Check for toggle reminder: "reminder 1 bondho koro", "pause reminder 1", "reminder 1 chalu koro"
        val toggleOffMatcher = PATTERN_TOGGLE_OFF_REMINDER.matcher(text)
        if (toggleOffMatcher.find()) {
            val idStr = toggleOffMatcher.group(1) ?: toggleOffMatcher.group(2)
            val remId = idStr?.toLongOrNull()
            if (remId != null) {
                return QuickParseResult(
                    matched = true,
                    intent = "toggle_reminder",
                    id = remId,
                    isActive = false
                )
            }
        }
        val toggleOnMatcher = PATTERN_TOGGLE_ON_REMINDER.matcher(text)
        if (toggleOnMatcher.find()) {
            val idStr = toggleOnMatcher.group(1) ?: toggleOnMatcher.group(2)
            val remId = idStr?.toLongOrNull()
            if (remId != null) {
                return QuickParseResult(
                    matched = true,
                    intent = "toggle_reminder",
                    id = remId,
                    isActive = true
                )
            }
        }

        // 1.13 Check for database stats / telemetry: "db stats dekhao", "database stats", "telemetry dekhao", "database telemetry"
        if (PATTERN_DATABASE_STATS.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "get_database_stats"
            )
        }

        // 1.14 Check for spending patterns / trends / analysis: "spending patterns dekhao", "khorocher pattern", "kharach pattern"
        if (PATTERN_SPENDING_PATTERNS.matcher(text).matches()) {
            return QuickParseResult(
                matched = true,
                intent = "get_spending_patterns"
            )
        }

        // 2. Check for balance queries: "amar balance koto", "koto taka baki", etc.
        if (isBalanceQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_financial_balance"
            )
        }

        // 3. Check for highest expense: "highest expense", "shobcheye beshi khoroch", etc.
        if (isHighestExpenseQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_highest_expense"
            )
        }

        // 4. Check for period summary (today / yesterday)
        val period = parsePeriodSummary(text)
        if (period != null) {
            return QuickParseResult(
                matched = true,
                intent = "get_period_summary",
                searchQuery = period
            )
        }

        // 5. Check for general monthly summary intent
        if (isGeneralSummaryQuery(text)) {
            return QuickParseResult(
                matched = true,
                intent = "get_spending_summary"
            )
        }

        // 6. Check for item / category specific queries: e.g. "total bus vara koto", "bazar e koto khoroch hoise", "lunch a total koto cost hoiche ai week a"
        val itemDetails = parseItemTotalQueryDetails(text)
        if (itemDetails != null) {
            return QuickParseResult(
                matched = true,
                intent = "get_item_total",
                searchQuery = itemDetails.item,
                note = itemDetails.period
            )
        }

        // 7. Check for reminder intent: "raat 9 tay reminder", "9 pm reminder dio", etc.
        val reminderMatcher = PATTERN_PROPOSE_REMINDER.matcher(text)
        if (reminderMatcher.find()) {
            var hour = reminderMatcher.group(1)?.toIntOrNull() ?: 21
            val minute = reminderMatcher.group(2)?.toIntOrNull() ?: 0

            if (text.contains("raat") || text.contains("shondha") || text.contains("pm")) {
                if (hour < 12) hour += 12
            } else if (text.contains("shokal") || text.contains("am")) {
                if (hour == 12) hour = 0
            }

            return QuickParseResult(
                matched = true,
                intent = "propose_reminder",
                reminderHour = hour,
                reminderMinute = minute,
                note = "Daily expense entry reminder"
            )
        }

        // Date and Day Extraction (e.g. "4 tarikh", "4 tarikhe", "15 tarikh", "gotokal", etc.)
        val dateInfo = extractDateAndClean(text)
        val textWithoutDate = dateInfo.cleanedText
        val occurredAt = dateInfo.timestamp

        // 8. Check for income: "salary paisi 30000 tk", "100 tk salay peyechi", "5000 tk bonus pelam", etc.
        val incomeParsed = parseIncomeTransaction(textWithoutDate, occurredAt)
        if (incomeParsed != null) {
            return incomeParsed
        }

        // 9. Check for MULTIPLE expenses: e.g. "50 tk sosage , 320 tk murgi , 420 tk bazar"
        val multipleItems = parseMultipleExpenses(textWithoutDate, occurredAt)
        if (multipleItems.size >= 2) {
            return QuickParseResult(
                matched = true,
                intent = "prepare_multiple_expenses",
                multipleItems = multipleItems
            )
        }

        // 10. Strict Pattern: "<amount> tk <note>" -> e.g. "50 tk bus vara", "100 tk cha ar bill"
        val amountFirstMatcher = PATTERN_AMOUNT_FIRST_TX.matcher(textWithoutDate)
        if (amountFirstMatcher.find()) {
            val amount = amountFirstMatcher.group(1)?.toDoubleOrNull()
            var note = amountFirstMatcher.group(2)?.trim() ?: ""
            note = cleanExpenseNote(note.removePrefix("tk").trim())
            if (amount != null && amount > 0 && note.isNotBlank() && !isDateWord(note)) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                val hasCurrency = textWithoutDate.contains("tk") || textWithoutDate.contains("taka")
                val hasTransferMarker = textWithoutDate.contains(Regex("\\b(?:ke|k|re|dilam|delam|disi|dici|dise|dilo|pathalam|pathaisi)\\b", RegexOption.IGNORE_CASE))
                val isPersonOrMerchant = BanglishTransliterator.isKnownNameOrMerchant(note)
                val hasFinancialAnchor = category != "Other" || hasPhoneticFinancialAnchor(textWithoutDate, note) || hasTransferMarker || isPersonOrMerchant
                if (hasCurrency || hasFinancialAnchor) {
                    return QuickParseResult(
                        matched = true,
                        intent = "prepare_expense",
                        amountMinor = (amount * 100).toLong(),
                        category = category,
                        note = note,
                        occurredAt = occurredAt
                    )
                }
            }
        }

        // 11. Strict Pattern: "<note> <amount> tk" -> e.g. "ajke lunch 250 tk", "cha ar bill 100 tk", "rickshaw vara dilam 60 tk"
        val amountLastMatcher = PATTERN_AMOUNT_LAST_TX.matcher(textWithoutDate)
        if (amountLastMatcher.find()) {
            var note = amountLastMatcher.group(1)?.trim() ?: ""
            val amount = amountLastMatcher.group(2)?.toDoubleOrNull()
            note = note.removePrefix("ajke").removePrefix("aj").trim()
            note = cleanExpenseNote(note)
            if (amount != null && amount > 0 && note.isNotBlank() && !isDateWord(note)) {
                val category = TransactionValidator.mapToValidCategory(null, note, com.example.expenseassistant.data.TransactionType.EXPENSE)
                val hasCurrency = textWithoutDate.contains("tk") || textWithoutDate.contains("taka")
                val hasTransferMarker = textWithoutDate.contains(Regex("\\b(?:ke|k|re|dilam|delam|disi|dici|dise|dilo|pathalam|pathaisi)\\b", RegexOption.IGNORE_CASE))
                val isPersonOrMerchant = BanglishTransliterator.isKnownNameOrMerchant(note)
                val hasFinancialAnchor = category != "Other" || hasPhoneticFinancialAnchor(textWithoutDate, note) || hasTransferMarker || isPersonOrMerchant
                if (hasCurrency || hasFinancialAnchor) {
                    return QuickParseResult(
                        matched = true,
                        intent = "prepare_expense",
                        amountMinor = (amount * 100).toLong(),
                        category = category,
                        note = note,
                        occurredAt = occurredAt
                    )
                }
            }
        }

        // 12. Natural Language Sentences with filler words/verbs: e.g. "ami 200 tk diye lunch korsi", "rickshaw vara dilam 60 tk"
        val naturalExpense = parseNaturalLanguageExpense(textWithoutDate, occurredAt)
        if (naturalExpense != null) {
            return naturalExpense
        }

        return QuickParseResult(matched = false, intent = "")
    }
}
