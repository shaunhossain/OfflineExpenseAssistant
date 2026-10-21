package com.example.expenseassistant.domain

import com.example.expenseassistant.ai.BanglishTransliterator
import com.example.expenseassistant.data.TransactionType

data class ValidationResult(
    val isValid: Boolean,
    val normalizedAmountMinor: Long,
    val normalizedCategory: String,
    val normalizedNote: String,
    val type: TransactionType,
    val needsReview: Boolean,
    val errorMessage: String? = null,
    val occurredAt: Long? = null
)

object TransactionValidator {

    val VALID_CATEGORIES = listOf(
        "Food",
        "Transport",
        "Grocery",
        "Bills",
        "Shopping",
        "Salary",
        "Health",
        "Entertainment",
        "Education",
        "Other"
    )

    private const val MAX_TRANSACTION_BDT = 10_000_000L // 1 Crore BDT upper boundary safety check

    fun validate(
        rawAmountMinor: Long?,
        rawCategory: String?,
        rawNote: String?,
        intentType: String?,
        occurredAt: Long? = null
    ): ValidationResult {
        if (rawAmountMinor == null || rawAmountMinor <= 0L) {
            return ValidationResult(
                isValid = false,
                normalizedAmountMinor = 0L,
                normalizedCategory = "Other",
                normalizedNote = rawNote ?: "",
                type = TransactionType.EXPENSE,
                needsReview = true,
                errorMessage = "Invalid or missing amount",
                occurredAt = occurredAt
            )
        }

        val taka = rawAmountMinor / 100L
        if (taka > MAX_TRANSACTION_BDT) {
            return ValidationResult(
                isValid = false,
                normalizedAmountMinor = rawAmountMinor,
                normalizedCategory = "Other",
                normalizedNote = rawNote ?: "",
                type = TransactionType.EXPENSE,
                needsReview = true,
                errorMessage = "Amount exceeds safety limit (৳$MAX_TRANSACTION_BDT)",
                occurredAt = occurredAt
            )
        }

        val text = "${rawCategory ?: ""} ${rawNote ?: ""}".lowercase()
        val isStaffWageExpense = text.contains("daroan") || text.contains("darowan") ||
                text.contains("bua") || text.contains("driver") || text.contains("kormochari") ||
                text.contains("dilam") || text.contains("disi") || text.contains("diasilam")

        val isIncome = !isStaffWageExpense && (intentType?.uppercase()?.contains("INCOME") == true ||
            rawCategory?.equals("Salary", ignoreCase = true) == true ||
            text.contains("salary") || text.contains("salay") || text.contains("salari") ||
            text.contains("beton") || text.contains("veton") ||
            text.contains("income") || text.contains("peyechi") || text.contains("paisi") ||
            text.contains("paici") || text.contains("payci") ||
            text.contains("pelam") || text.contains("credit") || text.contains("bonus"))

        val type = if (isIncome) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }

        val normalizedCategory = mapToValidCategory(rawCategory, rawNote, type)
        val cleanNote = (rawNote ?: normalizedCategory).trim()
        val needsReview = cleanNote.isBlank() || cleanNote.length < 2

        return ValidationResult(
            isValid = true,
            normalizedAmountMinor = rawAmountMinor,
            normalizedCategory = normalizedCategory,
            normalizedNote = if (cleanNote.isNotBlank()) cleanNote else normalizedCategory,
            type = type,
            needsReview = needsReview,
            errorMessage = null,
            occurredAt = occurredAt
        )
    }

    fun mapToValidCategory(rawCategory: String?, note: String?, type: TransactionType): String {
        if (type == TransactionType.INCOME) {
            return "Salary"
        }

        val rawText = "${rawCategory ?: ""} ${note ?: ""}".lowercase()
        val rawWords = rawText.split(Regex("[^a-zA-Z0-9\\u0980-\\u09FF]+")).filter { it.isNotBlank() }
        val canonicalWords = rawWords.map { BanglishTransliterator.resolveCanonicalBanglishWord(it) }
        val words = (rawWords + canonicalWords).toSet()
        val text = (rawText + " " + canonicalWords.joinToString(" ")).trim()

        return when {
            text.contains("basha vara") || text.contains("bari vara") || text.contains("flat vara") ||
                    text.contains("basa vara") || text.contains("mess vara") || text.contains("hostel vara") ||
                    text.contains("বাসা ভাড়া") || text.contains("বাড়ি ভাড়া") ||
                    text.contains("house rent") || text.contains("room rent") -> "Bills"

            words.contains("bus") || ((text.contains("vara") || text.contains("bhara") || text.contains("bada")) && !text.contains("turf") && !text.contains("futsal")) ||
                    text.contains("rickshaw") || text.contains("riksa") || text.contains("riksha") || text.contains("rikshaw") ||
                    words.contains("cng") || text.contains("uber") || (text.contains("pathao") && !text.contains("pathao food")) ||
                    text.contains("metro") || text.contains("train") || text.contains("transport") ||
                    text.contains("gari") || text.contains("auto") || text.contains("tempo") ||
                    text.contains("laguna") || text.contains("leguna") || text.contains("launch") ||
                    text.contains("nouka") || text.contains("ferry") || words.contains("van") ||
                    text.contains("octane") || text.contains("petrol") || text.contains("diesel") ||
                    text.contains("fuel") || text.contains("toll") || text.contains("parking") || text.contains("garage vara") ||
                    text.contains("brta") || text.contains("license") || text.contains("tax token") ||
                    text.contains("fitness") || text.contains("puncture") || text.contains("servicing") ||
                    words.contains("hawa") || text.contains("grease") || text.contains("filter") || text.contains("spark plug") || text.contains("brake shoe") ||
                    text.contains("helmet") || text.contains("raincoat") || text.contains("rain coat") || text.contains("learner fee") || text.contains("driving test") ||
                    text.contains("pickup") || text.contains("truck") || text.contains("coolie") || text.contains("cooli") ||
                    text.contains("labour") || text.contains("lebor") ||
                    text.contains("traffic") || text.contains("sergeant") || text.contains("mamla") || (words.contains("fine") && !text.contains("fitness")) ||
                    text.contains("biman") || text.contains("flight") || text.contains("air ticket") || text.contains("bike wash") || text.contains("car wash") || text.contains("gari wash") ||
                    words.contains("bike") || words.contains("motorcycle") || text.contains("tel vorlam") ||
                    ((words.contains("mobil") && !words.contains("mobile") && !text.contains("mobile")) || text.contains("engine oil")) ||
                    text.contains("বাস") || text.contains("ভাড়া") || text.contains("রিকশা") ||
                    text.contains("গাড়ি") || text.contains("সিএনজি") || text.contains("মেট্রো") ||
                    text.contains("ট্রেন") || text.contains("অটো") -> "Transport"

            text.contains("foodpanda") || text.contains("pathao food") || text.contains("hungrynaki") ||
                    text.contains("canteen") || text.contains("dining") || text.contains("meal") ||
                    text.contains("tong") || text.contains("biri") || text.contains("cigarette") || text.contains("smoke") ||
                    words.contains("cha") || words.contains("tea") || text.contains("coffee") ||
                    text.contains("lunch") || text.contains("dinner") || text.contains("breakfast") ||
                    text.contains("nasta") || text.contains("nashta") ||
                    text.contains("biryani") || text.contains("biriyani") || text.contains("biriani") ||
                    text.contains("kacchi") || text.contains("tehari") || text.contains("khichuri") ||
                    text.contains("halim") || text.contains("haleem") || text.contains("chotpoti") ||
                    text.contains("burger") || text.contains("pizza") || text.contains("kfc") ||
                    text.contains("shawarma") || text.contains("grill") || words.contains("nan") || words.contains("naan") ||
                    text.contains("kabab") || text.contains("kebab") || text.contains("biscuit") || text.contains("biskut") ||
                    text.contains("cake") || text.contains("pauruti") || text.contains("bread") ||
                    text.contains("lassi") || text.contains("juice") || text.contains("coke") || text.contains("pepsi") ||
                    text.contains("borhani") || text.contains("bakorkhani") || text.contains("jilapi") || text.contains("roshogolla") || text.contains("chomchom") || text.contains("kulfi") ||
                    text.contains("nihari") || text.contains("paya") || text.contains("morog polao") || words.contains("polao") ||
                    text.contains("daab") || words.contains("dab") || text.contains("dhab") ||
                    text.contains("sosage") || text.contains("sausage") ||
                    text.contains("treat") || text.contains("khabar") ||
                    text.contains("misti") || text.contains("mishti") || words.contains("doi") ||
                    text.contains("jhalmuri") || text.contains("singara") || text.contains("shingara") ||
                    text.contains("somosa") || text.contains("samucha") ||
                    text.contains("parata") || text.contains("porota") || text.contains("ruti") || text.contains("roti") ||
                    text.contains("fuchka") || text.contains("fuska") || words.contains("bhat") || words.contains("vaat") ||
                    text.contains("velpuri") || text.contains("bhelpuri") || text.contains("panipuri") || text.contains("churmur") ||
                    text.contains("chanachur") || text.contains("badam") ||
                    text.contains("alur chop") || text.contains("beguni") || text.contains("dimer chop") || text.contains("chicken roll") ||
                    words.contains("soup") || words.contains("momo") || text.contains("chowmein") || words.contains("pasta") || text.contains("paan") || text.contains("zorda") ||
                    text.contains("নাস্তা") || text.contains("চা") || text.contains("কফি") ||
                    text.contains("খাবার") || text.contains("লাঞ্চ") || text.contains("ডিনার") ||
                    text.contains("বিরিয়ানি") || text.contains("ভাত") || text.contains("রুটি") ||
                    text.contains("পরোটা") || text.contains("সিঙ্গারা") || text.contains("ফুচকা") ||
                    text.contains("food") || text.contains("restaurant") -> "Food"

            text.contains("bazar") || text.contains("bajar") || text.contains("bazaar") ||
                    text.contains("grocery") || text.contains("groceries") ||
                    words.contains("chal") || text.contains("chaal") || words.contains("dal") || text.contains("daal") ||
                    words.contains("oil") || words.contains("tel") ||
                    text.contains("shobji") || text.contains("sobji") || text.contains("soji") ||
                    text.contains("mach") || text.contains("maach") || text.contains("ilish") || text.contains("chingri") ||
                    text.contains("murgi") || text.contains("chicken") ||
                    text.contains("mangsho") || text.contains("meat") || text.contains("gosht") || text.contains("gostho") ||
                    text.contains("beef") || text.contains("mutton") ||
                    words.contains("dim") || words.contains("egg") ||
                    text.contains("dudh") || text.contains("dud") || words.contains("milk") ||
                    text.contains("aloo") || text.contains("alu") || text.contains("peyaj") || text.contains("piyaj") ||
                    text.contains("roshun") || text.contains("rosun") || text.contains("morich") || text.contains("moris") ||
                    text.contains("chini") || text.contains("sugar") || text.contains("lobon") || text.contains("salt") ||
                    text.contains("moyda") || text.contains("suji") || text.contains("moshla") || text.contains("masala") ||
                    text.contains("harpic") || words.contains("vim") || words.contains("sar") ||
                    text.contains("keetnashok") || words.contains("beej") || words.contains("kamla") ||
                    text.contains("bhushi") || text.contains("khor") || (words.contains("feed") && !words.contains("feeder")) || text.contains("gur") ||
                    ((text.contains("holud") || text.contains("halud")) && !text.contains("stage") && !text.contains("gaye holud") && !text.contains("biye")) ||
                    words.contains("ada") || text.contains("ghee") || text.contains("butter") ||
                    text.contains("shaban") || text.contains("saban") || text.contains("soap") || text.contains("shampoo") ||
                    text.contains("toothpaste") || text.contains("detergent") || text.contains("tissue") ||
                    words.contains("fol") || text.contains("fruits") || words.contains("kola") || words.contains("aam") ||
                    text.contains("lichu") || text.contains("litchi") || text.contains("kathal") || text.contains("pepe") || text.contains("peyara") ||
                    text.contains("apel") || text.contains("komola") || text.contains("malta") ||
                    text.contains("begun") || text.contains("potol") || text.contains("tomato") || text.contains("gajor") ||
                    text.contains("torkari") ||
                    text.contains("sawpno") || text.contains("swapno") || text.contains("shwapno") ||
                    text.contains("agora") || text.contains("meenabazar") || text.contains("unimart") ||
                    text.contains("chaldal") || text.contains("pandamart") || text.contains("kacha bazar") || text.contains("daily shopping") ||
                    text.contains("বাজার") || text.contains("সবজি") ||
                    text.contains("মাছ") || text.contains("মাংস") || text.contains("মুরগি") || text.contains("চাল") ||
                    text.contains("ডাল") || text.contains("তেল") || text.contains("ডিম") || text.contains("দুধ") ||
                    text.contains("আলু") || text.contains("পেঁয়াজ") || text.contains("রসুন") || text.contains("মরিচ") ||
                    text.contains("মুদি") -> "Grocery"

            text.contains("desco") || text.contains("wasa") || text.contains("titas") ||
                    text.contains("nesco") || text.contains("dpdc") || text.contains("palli bidyut") ||
                    text.contains("palli biddyut") || text.contains("bpdb") ||
                    (text.contains("bill") && !text.contains("darji") && !text.contains("tailor") && !text.contains("hospital") && !text.contains("doctor") && !text.contains("catering")) ||
                    text.contains("current") || text.contains("karent") ||
                    text.contains("electricity") || text.contains("biddut") || text.contains("bidyut") ||
                    words.contains("gas") || text.contains("wifi") || text.contains("internet") || text.contains("broadband") ||
                    words.contains("pani") || words.contains("paani") ||
                    text.contains("recharge") || text.contains("reacharge") || text.contains("rechrg") ||
                    words.contains("load") || text.contains("flexi") || text.contains("flaxiload") ||
                    text.contains("kisti") || words.contains("emi") || text.contains("cylinder") ||
                    text.contains("bua") || text.contains("maid") || text.contains("khala") ||
                    text.contains("dps") || text.contains("advance") || text.contains("credit card") ||
                    text.contains("mistri") || text.contains("electrician") || text.contains("plumber") ||
                    text.contains("tanki saf") || text.contains("tank saf") || text.contains("septic tank") ||
                    text.contains("moyla") || text.contains("garbage") || text.contains("dustbin bill") ||
                    text.contains("daroan") || text.contains("security guard") || text.contains("dokan vara") ||
                    text.contains("inverter") || text.contains("generator bill") || text.contains("ac service") || text.contains("gas refill ac") ||
                    text.contains("capacitor") || text.contains("condenser") || text.contains("coil badhai") || text.contains("water pump") || text.contains("pani pump") ||
                    text.contains("ukil") || text.contains("advocate") || text.contains("court") || text.contains("notary") || text.contains("stamp") ||
                    text.contains("passport") || text.contains("laundry") || text.contains("dhobi") || text.contains("iron") || text.contains("istri") || text.contains("dry clean") || text.contains("samity") || text.contains("somiti") ||
                    text.contains("rajmistri") || text.contains("rong mistri") || text.contains("jogali") || text.contains("cement") || words.contains("rod") || words.contains("rodd") || words.contains("bali") || text.contains("eet") || text.contains("brick") || text.contains("manpower") || text.contains("bmet") ||
                    text.contains("koshai") || text.contains("kosai") || text.contains("churi dhar") || text.contains("da dhar") ||
                    text.contains("domain") || text.contains("hosting") || text.contains("copilot") || text.contains("vpn") ||
                    text.contains("service charge") || text.contains("rent") || text.contains("utility") ||
                    text.contains("বিল") || text.contains("বিদ্যুৎ") || text.contains("কারেন্ট") || text.contains("গ্যাস") ||
                    text.contains("পানি") || text.contains("ওয়াইফাই") || text.contains("ইন্টারনেট") || text.contains("রিচার্জ") -> "Bills"

            text.contains("cloth") || text.contains("shirt") || text.contains("pant") ||
                    text.contains("shopping") || text.contains("shoe") || text.contains("daraz") ||
                    text.contains("delivery charge") || text.contains("courier") || text.contains("sundarban") ||
                    text.contains("display") || text.contains("battery") || text.contains("charger") ||
                    text.contains("earphone") || text.contains("headphone") ||
                    text.contains("tempered glass") || text.contains("back cover") || text.contains("phone cover") || text.contains("power bank") ||
                    text.contains("cat food") || text.contains("whiskas") || text.contains("drools") ||
                    text.contains("diaper") || text.contains("pampers") || text.contains("cerelac") || text.contains("feeder") || text.contains("wipes") ||
                    text.contains("darji") || text.contains("tailor") || text.contains("selai") || text.contains("fall piko") || text.contains("piko") || text.contains("button lagalam") || text.contains("pant chikon") ||
                    text.contains("chador") || text.contains("balish") || text.contains("toshok") || text.contains("porda") || text.contains("curtain") ||
                    words.contains("lep") || text.contains("kambal") || text.contains("blanket") || text.contains("sweater") || text.contains("hoodie") || text.contains("jacket") || text.contains("chabi") || text.contains("chata") ||
                    text.contains("shave") || text.contains("daari") || text.contains("facial") || text.contains("hair color") || text.contains("shoe polish") || text.contains("muchi") ||
                    text.contains("mehedi") || text.contains("mehendi") || text.contains("rebonding") || text.contains("keratin") ||
                    text.contains("multiplug") || text.contains("bulb") ||
                    text.contains("khelna") || text.contains("tiffin") || text.contains("school bag") || text.contains("blender") || text.contains("korai") || text.contains("khorai") || text.contains("pressure cooker") ||
                    text.contains("kapor") || text.contains("kapod") || text.contains("jama") ||
                    text.contains("juta") || text.contains("juto") || text.contains("sandal") ||
                    words.contains("sari") || text.contains("sharee") || text.contains("panjabi") || text.contains("punjabi") ||
                    text.contains("salwar") || text.contains("kamiz") || text.contains("borka") || text.contains("hijab") ||
                    text.contains("lungi") || text.contains("tshirt") || text.contains("t-shirt") ||
                    text.contains("ghori") || text.contains("watch") || text.contains("cosmetics") ||
                    text.contains("saloon") || text.contains("hair cut") || text.contains("parlour") ||
                    text.contains("aarong") || text.contains("apex") || text.contains("bata") ||
                    text.contains("eid shopping") ||
                    text.contains("জামা") || text.contains("কাপড়") || text.contains("শপিং") || text.contains("জুতা") -> "Shopping"

            text.contains("medicine") || text.contains("medicin") || text.contains("medical") ||
                    text.contains("osudh") || text.contains("oshudh") || text.contains("oushodh") ||
                    text.contains("osud") || text.contains("osod") ||
                    text.contains("napa") || text.contains("seclo") || text.contains("sergel") || text.contains("monas") ||
                    text.contains("orsaline") || text.contains("saline") || text.contains("vitamin") || text.contains("antibiotic") ||
                    text.contains("syrup") || text.contains("tablet") || text.contains("capsule") ||
                    text.contains("pathology") || text.contains("diagnostic") || text.contains("xray") || text.contains("x-ray") ||
                    text.contains("ecg") || text.contains("ultrasono") || text.contains("physio") || text.contains("physiotherapy") ||
                    text.contains("therapy") || text.contains("scaling") || text.contains("dental") || text.contains("lens") ||
                    text.contains("cbc test") || text.contains("creatinine") || text.contains("lipid profile") || text.contains("urine test") || text.contains("stool test") || text.contains("thyroid") || text.contains("dope test") ||
                    text.contains("gamca") || text.contains("wafid") ||
                    text.contains("homeo") || text.contains("homeopathy") || text.contains("ayurvedic") || text.contains("kabiraji") ||
                    text.contains("savlon") || text.contains("dettol") || text.contains("hexisol") || text.contains("inhaler") || text.contains("sugar strip") || text.contains("glucometer") || text.contains("pressure machine") || text.contains("bp machine") ||
                    text.contains("bandage") || text.contains("gauze") || text.contains("cotton") || text.contains("eye drop") ||
                    text.contains("doctor") || text.contains("daktar") || text.contains("hospital") || text.contains("haspatal") || text.contains("pharmacy") ||
                    text.contains("ঔষধ") || text.contains("ওষুধ") || text.contains("ডাক্তার") ||
                    text.contains("হাসপাতাল") ||
                    text.contains("health") -> "Health"

            text.contains("movie") || text.contains("cinema") || text.contains("cineplex") ||
                    text.contains("star cineplex") || text.contains("ticket") || text.contains("game") ||
                    text.contains("netflix") || text.contains("spotify") || text.contains("hoichoi") ||
                    text.contains("chorki") || text.contains("concert") || text.contains("picnic") ||
                    text.contains("gym") || text.contains("fitness") || text.contains("spa") ||
                    text.contains("biye") || text.contains("gaye holud") || text.contains("holud stage") ||
                    text.contains("catering") || text.contains("photographer") || text.contains("convention") ||
                    text.contains("turf") || text.contains("futsal") || text.contains("diamond") || text.contains("topup") || text.contains("pubg") || text.contains("badminton") ||
                    text.contains("tour") || text.contains("adda") ||
                    text.contains("সিনেপ্লেক্স") || text.contains("সিনেমা") || text.contains("টিকিট") -> "Entertainment"

            text.contains("tuition") || text.contains("tution") || text.contains("school") || text.contains("college") ||
                    text.contains("university") || text.contains("varsity") || text.contains("coaching") ||
                    text.contains("semester") || text.contains("admission") ||
                    text.contains("board challenge") || text.contains("re-check") || text.contains("recheck") ||
                    text.contains("transcript") || text.contains("convocation") || text.contains("reunion") || text.contains("attestation") ||
                    text.contains("form fillup") || text.contains("admit card") || text.contains("registration") || text.contains("photocopy") ||
                    text.contains("binding") || text.contains("lamination") || text.contains("passport size") ||
                    text.contains("book") || words.contains("boi") || text.contains("khata") ||
                    text.contains("kolom") || words.contains("pen") || text.contains("stationery") ||
                    text.contains("বই") || text.contains("খাতা") || text.contains("টিউশন") ||
                    text.contains("স্কুল") || text.contains("কলেজ") -> "Education"

            else -> {
                VALID_CATEGORIES.firstOrNull { it.equals(rawCategory, ignoreCase = true) } ?: "Other"
            }
        }
    }
}
