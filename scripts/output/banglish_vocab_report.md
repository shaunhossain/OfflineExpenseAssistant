# BanglaTLit Transliteration & Vocabulary Analysis Report

- **Total Parallel Rows Scanned:** 245,727
- **Unique Transliterated Tokens Observed:** 46,530
- **Aligned Confident Transliteration Dictionary:** 37,853 words
- **Bengali Financial Lemmas Tracked:** 120

## 1. Mined Transliteration Variants & Synthesized Regexes

| Bengali Lemma | Canonical | Group | Variants Discovered | Synthesized Regex |
|---|---|---|---|---|
| ইন্টারনেট | **internet** | `bills` | `internet` (84), `net` (5), `intarnet` (2), `5internet` (1), `interet` (1), `intrnet` (1) | `\b(?:internate|5internet|intarnet|internet|intrnet|interet|nternet|net)\b` |
| ওয়াইফাই | **wifi** | `bills` | `wifi` (83), `wife` (1), `wi-fi` (0) | `\b(?:wi\-fi|wifi|wife)\b` |
| কারেন্ট | **current** | `bills` | `current` (6), `karent` (3) | `\b(?:current|karent)\b` |
| গ্যাস | **gas** | `bills` | `gass` (1), `gyasiyo` (1), `gas` (0) | `\b(?:gyasiyo|gass|gas)\b` |
| পানি | **pani** | `bills` | `paani` (8), `panir` (8), `pani` (7), `pan` (2), `panio` (1), `pane` (1) | `\b(?:panio|panno|ponyo|paani|panir|pani|bani|pane|pan)\b` |
| বাসা | **basha** | `bills` | `basa` (28), `basay` (10), `bashar` (8), `basha` (2), `vasa` (2), `bassa` (1) | `\b(?:bashar|bassai|bhasa|basai|bassa|basay|vasay|basae|basha|vasa|basa|bass|boss)\b` |
| বিদ্যুৎ | **biddut** | `bills` | `bidyut` (6), `bidut` (2), `biddut` (1) | `\b(?:bidyut|biddut|bidut)\b` |
| বিল | **bill** | `bills` | `bill` (7), `bil` (4), `boli` (3), `bola` (3), `bole` (2), `billy` (1) | `\b(?:billy|bill|bole|bola|boli|bil)\b` |
| রিচার্জ | **recharge** | `bills` | `recharge` (102), `recherge` (3), `reacharge` (2), `recharg` (2), `reacherge` (2), `rrecharge` (1) | `\b(?:reacharge|rrecharge|reacherge|recharge|rechatge|recherge|recharg|recarge|rechrge|rechrg|ricarg|rchrg|rcrg)\b` |
| লোড | **load** | `bills` | `load` (54), `dload` (8), `lode` (4), `flaxiload` (2), `flexi` (1), `lod` (1) | `\b(?:flaxiload|flexi|dload|lode|load|coad|lod)\b` |
| টাকা | **taka** | `currency` | `taka` (493), `tk` (432), `takay` (57), `takar` (47), `takai` (33), `thaka` (9) | `\b(?:takaye|takay|thaka|takhe|takai|taaky|takar|1taka|taaka|5taka|4taka|takla|takha|taka|teke|teka|tata|tska|jaka|laka|tky|tke|tka|tok|tik|tk)\b` |
| টাকায় | **takay** | `currency` | `takay` (20), `takai` (7), `taka` (2), `tk` (2), `tke` (1), `takaye` (0) | `\b(?:takaye|takay|takai|takat|taka|tke|tk)\b` |
| টাকার | **takar** | `currency` | `takar` (47), `taka` (8), `tkr` (5), `tkar` (2), `takai` (2), `rakar` (1) | `\b(?:takare|takai|takay|rakar|takar|taka|tker|tkar|tkr)\b` |
| পয়সা | **poisa** | `currency` | `poisao` (1), `poesha` (0), `poysha` (0), `poisha` (0), `poisa` (0) | `\b(?:poisao|poisha|poysha|poesha|poisa)\b` |
| খরচ | **khoroch** | `expense_keyword` | `khoroch` (17), `khoros` (9), `khoroc` (3), `khoroj` (2), `koros` (2), `khoroche` (1) | `\b(?:khorchay|khoroche|khorcha|khoroch|khorche|khorach|kharach|korechi|khroch|khorch|koroch|khoros|khoroj|khoroc|korsii|korsi|koroc|koros|korse|khrch)\b` |
| খরচের | **khorocher** | `expense_keyword` | `koroser` (0), `khorocer` (0), `korocher` (0), `khoroser` (0), `khorocher` (0) | `\b(?:khorocher|khoroser|khorocer|korocher|koroser)\b` |
| কফি | **coffee** | `food` | `coffee` (2), `coffe` (2) | `\b(?:coffee|coffe)\b` |
| খাবার | **khabar** | `food` | `khabar` (10), `khabaar` (0), `khabare` (0) | `\b(?:khabaar|khabare|khabar)\b` |
| চা | **cha** | `food` | `chai` (248), `chay` (51), `chan` (32), `char` (15), `chap` (15), `cha` (12) | `\b(?:chaoya|cheai|chaiy|chaay|cheye|chayy|chaii|chaoa|chaya|cayoa|icha|chat|chap|char|chai|ucha|acha|chaw|caia|chay|chea|chao|chal|chan|chsi|chas|sha|che|tea|chi|chk|chy|tha|cha|kha|cpa|ca)\b` |
| ডিনার | **dinner** | `food` | `dinner` (1), `dinar` (0) | `\b(?:dinner|dinar)\b` |
| নাস্তা | **nasta** | `food` | `nastah` (0), `nasta` (0), `nashta` (0) | `\b(?:nastah|nashta|nasta)\b` |
| পরোটা | **parata** | `food` | `parata` (0), `porata` (0), `porota` (0) | `\b(?:parata|porata|porota)\b` |
| ফুচকা | **fuchka** | `food` | `fuchka` (0), `fuska` (0) | `\b(?:fuchka|fuska)\b` |
| বিরিয়ানি | **biryani** | `food` | `biryani` (0), `biriyani` (0), `biriani` (0) | `\b(?:biriyani|biryani|biriani)\b` |
| ভাত | **bhat** | `food` | `bhat` (2), `vat` (2), `bhatu` (1), `vhat` (1), `bat` (1), `vaate` (1) | `\b(?:bhatu|vaate|bhat|vaat|vhat|bat|vat)\b` |
| রুটি | **ruti** | `food` | `truti` (6), `tuti` (1), `roote` (1), `roti` (0), `ruti` (0) | `\b(?:truti|roote|roti|ruti|tuti)\b` |
| লাঞ্চ | **lunch** | `food` | `luncher` (3), `lanch` (1), `lunch` (0) | `\b(?:luncher|lunch|lanch)\b` |
| সিঙ্গারা | **singara** | `food` | `singara` (0), `shingara` (0) | `\b(?:shingara|singara)\b` |
| আড়াই | **arai** | `fraction` | `adhai` (0), `arhai` (0), `arai` (0) | `\b(?:adhai|arhai|arai)\b` |
| দেড় | **der** | `fraction` | `der` (3), `dur` (1), `den` (1), `derh` (0), `dedh` (0) | `\b(?:derh|dedh|der|dur|den)\b` |
| সাড়ে | **sare** | `fraction` | `sare` (2), `saare` (1), `share` (0), `sade` (0) | `\b(?:share|saare|sare|sade)\b` |
| আলু | **aloo** | `grocery` | `alu` (1), `aloo` (0) | `\b(?:aloo|alu)\b` |
| চাল | **chal** | `grocery` | `chalu` (76), `chaliye` (45), `chalai` (44), `calu` (22), `saale` (18), `chalay` (15) | `\b(?:chailaii|chailiye|chaaaaal|chaliyea|chailay|chalaya|chaliye|chaliya|chailai|chailo|chaile|chaili|chalie|chalau|chalia|cholle|chalai|chalay|school|chalk|chole|chaal|chlay|calai|shilo|chalo|chala|chilo|saale|calao|chila|calay|colle|caluu|chali|chalu|calea|calia|calo|chat|chap|chai|saal|char|chaw|calu|call|clai|chal|chan|cha)\b` |
| ডাল | **dal** | `grocery` | `dal` (2), `dall` (1), `dile` (1), `daal` (0) | `\b(?:daal|dile|dall|dal)\b` |
| ডিম | **dim** | `grocery` | `dim` (9), `dims` (1), `odim` (1), `die` (1), `deem` (0) | `\b(?:dims|odim|deem|die|dim)\b` |
| তরকারি | **torkari** | `grocery` | `torkari` (3) | `\btorkari\b` |
| তেল | **tel** | `grocery` | `tel` (10), `tele` (3), `atel` (1), `ten` (1), `oil` (0), `toil` (0) | `\b(?:atel|toil|tele|ten|tel|oil)\b` |
| দুধ | **dudh** | `grocery` | `dudu` (1), `dod` (1), `dud` (0), `dudh` (0) | `\b(?:dudh|dudu|dod|dud)\b` |
| পেঁয়াজ | **peyaj** | `grocery` | `piyaj` (0), `peaj` (0), `peyaj` (0) | `\b(?:piyaj|peyaj|peaj)\b` |
| বাজার | **bazar** | `grocery` | `bazar` (10), `bajare` (5), `bajar` (4), `bazare` (2), `bazaar` (1), `baazar` (0) | `\b(?:bazaar|baazar|bajare|bazare|bazar|bajar)\b` |
| মরিচ | **morich** | `grocery` | `morich` (0), `moris` (0) | `\b(?:morich|moris)\b` |
| মাংস | **mangsho** | `grocery` | `mangsho` (1), `mangso` (0) | `\b(?:mangsho|mangso)\b` |
| মাছ | **mach** | `grocery` | `mach` (17), `macher` (6), `much` (2), `machi` (1), `maas` (1), `maach` (0) | `\b(?:macher|maach|machi|maas|mach|much)\b` |
| মুরগি | **murgi** | `grocery` | `murgi` (2), `murgir` (1), `morgi` (0), `moorgi` (0) | `\b(?:murgir|moorgi|morgi|murgi)\b` |
| রসুন | **roshun** | `grocery` | `rosun` (0), `roshun` (0) | `\b(?:roshun|rosun)\b` |
| সবজি | **sobji** | `grocery` | `sobji` (2), `shobjee` (0), `shobji` (0) | `\b(?:shobjee|shobji|sobji)\b` |
| ওষুধ | **osudh** | `health` | `osudh` (1), `oshudh` (1), `oiosudh` (1), `oushudh` (1), `oushodh` (1), `medicine` (0) | `\b(?:medicine|oiosudh|oushudh|oushodh|oshudh|ousodh|osudh)\b` |
| ডাক্তার | **doctor** | `health` | `doctor` (5), `daktar` (4), `doctorra` (1) | `\b(?:doctorra|doctor|daktar)\b` |
| হাসপাতাল | **hospital** | `health` | `haspatale` (4), `haspatal` (1), `hospital` (0) | `\b(?:haspatale|haspatal|hospital)\b` |
| ইনকাম | **income** | `income` | `income` (131), `incom` (3), `incomee` (1), `incomer` (1), `incoome` (1), `icome` (1) | `\b(?:incoome|incomee|incomer|invome|income|incam|icome|incom|incm)\b` |
| ক্যাশব্যাক | **cashback** | `income` | `cashback` (12) | `\bcashback\b` |
| বেতন | **beton** | `income` | `beton` (4), `betan` (1), `veton` (0) | `\b(?:veton|betan|beton)\b` |
| বোনাস | **bonus** | `income` | `bonus` (46), `bonas` (3), `bonous` (2), `bounas` (1) | `\b(?:bounas|bonous|bonus|bonas)\b` |
| কোটি | **koti** | `magnitude` | `koti` (11), `kotir` (1), `kuti` (1), `poti` (1), `kotee` (0), `crore` (0) | `\b(?:kotee|kotir|crore|poti|koti|kuti)\b` |
| লক্ষ | **lakh** | `magnitude` | `lokkho` (19), `lakh` (3), `lokkhyo` (3), `lokkhye` (3), `lokkhe` (2), `lokko` (2) | `\b(?:lokkhye|lokkhyo|lakkhyo|lokkhi|lokkho|lokkhe|lokho|lokkh|lokko|like|labh|lakh|lok)\b` |
| লাখ | **lakh** | `magnitude` | `lakh` (10), `lakho` (2), `4lakh` (1), `1lakh` (1), `lakhs` (1), `lakhe` (0) | `\b(?:4lakh|lakho|1lakh|lakhe|laakh|lakhs|lakh|lac)\b` |
| শ | **sho** | `magnitude` | `show` (133), `so` (71), `shot` (62), `soho` (47), `shei` (29), `tho` (15) | `\b(?:shuye|chaoa|chioo|shey|shaa|asho|chou|shei|shol|shod|shot|cheo|show|shop|shor|chao|soho|shoy|sha|tho|soo|oho|seo|sho|shi|she|so)\b` |
| শত | **shoto** | `magnitude` | `shoto` (3), `soto` (2), `shotok` (1), `shot` (1), `choto` (1), `sotoe` (1) | `\b(?:shotok|sotte|choto|shoto|sotoe|saat|jeto|soto|shot|sut|sat|sot)\b` |
| হাজার | **hajar** | `magnitude` | `hajar` (48), `hazar` (14), `hajaro` (2), `hajer` (2), `hajare` (1), `2hajar` (1) | `\b(?:hajare|2hajar|hajaro|hajar|hajer|hazar|hszar|halar|hajr|haja)\b` |
| উপায় | **upay** | `mfs` | `upay` (20), `upai` (7), `upaye` (4), `upaay` (1), `upoy` (1), `upaiye` (1) | `\b(?:upaiye|upaay|upaye|upye|upoy|upai|upay)\b` |
| নগদ | **nagad** | `mfs` | `nagad` (7), `nogod` (4), `nagat` (1), `nogode` (0), `nagade` (0) | `\b(?:nogode|nagade|nagad|nagat|nogod)\b` |
| বিকাশ | **bkash** | `mfs` | `bkash` (153), `bikash` (22), `bksh` (5), `bikas` (5), `bekash` (3), `bkas` (3) | `\b(?:bikashe|bkashe|bikash|bekash|bkash|bikas|bkas|bksh)\b` |
| রকেট | **rocket** | `mfs` | `rocket` (14), `roket` (5), `rockt` (1) | `\b(?:rocket|roket|rockt)\b` |
| সেলফিন | **cellfin** | `mfs` | `cellfin` (5) | `\bcellfin\b` |
| কত | **koto** | `question` | `koto` (491), `kto` (41), `moto` (19), `hoto` (11), `kosto` (8), `kot` (6) | `\b(?:kotok|kosto|kotoi|kotob|kotoo|koyta|kotai|khoto|khoti|kotoy|kotou|kutta|khota|kotto|korto|kooto|kotho|khete|joto|kita|ksto|hoto|khot|moto|kate|coto|kotw|kete|kote|koro|koto|kotu|kota|kato|kto|kot|oto)\b` |
| বাকি | **baki** | `question` | `baki` (79), `bai` (2) | `\b(?:baki|bai)\b` |
| ব্যালেন্স | **balance** | `question` | `balance` (27), `blance` (4), `balace` (1), `balence` (1), `blence` (1), `balens` (1) | `\b(?:balance|balence|blence|blance|balace|balens|blnc)\b` |
| মোট | **mot** | `question` | `mot` (17), `moto` (6), `mota` (5), `mote` (4), `moteo` (4), `mod` (3) | `\b(?:moteo|mota|mote|moti|muta|moth|moto|motu|muti|emot|dot|mod|mut|mot)\b` |
| সবচেয়ে | **shobcheye** | `question` | `sobcheye` (18), `shobcheye` (13), `sobceye` (4), `shbcheye` (1), `sabcheye` (1), `sbceye` (1) | `\b(?:shobcheye|sobcheye|sabcheye|shbcheye|shobche|sobceye|sbceye|sobche)\b` |
| হিসাব | **hisab** | `question` | `hisabe` (23), `hisab` (17), `hisebe` (13), `hishabe` (11), `hishebe` (5), `hisan` (1) | `\b(?:heshabe|hishebe|hishabe|hisebeo|hishab|hiseba|hisabe|hosebe|hisebe|hiseb|hisan|hisab)\b` |
| চালডাল | **chaldal** | `services` | `chaldal` (0) | `\bchaldal\b` |
| ফুডপান্ডা | **foodpanda** | `services` | `foodpanda` (1) | `\bfoodpanda\b` |
| স্বপ্ন | **shwapno** | `services` | `shopno` (2), `swapno` (0), `shwapno` (0) | `\b(?:shwapno|swapno|shopno)\b` |
| কাপড় | **kapor** | `shopping` | `kaporer` (2), `kapore` (1), `kapor` (0), `kapod` (0) | `\b(?:kaporer|kapore|kapor|kapod)\b` |
| জামা | **jama** | `shopping` | `jama` (2), `jaman` (2), `jamat` (1), `jamai` (1), `jamar` (1) | `\b(?:jamar|jaman|jamai|jamat|jama)\b` |
| দারাজ | **daraz** | `shopping` | `daraz` (11) | `\bdaraz\b` |
| শপিং | **shopping** | `shopping` | `shopping` (2), `shoping` (2) | `\b(?:shopping|shoping)\b` |
| আজ | **aj** | `temporal` | `aj` (166), `kaj` (29), `ajk` (26), `aaj` (18), `aje` (16), `az` (5) | `\b(?:aajo|ajai|aju|ajb|ajk|aje|aji|ajo|aaj|ajj|kaj|aja|as|az|aj)\b` |
| আজকে | **ajke** | `temporal` | `ajke` (87), `ajk` (26), `ajker` (12), `ajkei` (9), `ajka` (6), `ajkeo` (2) | `\b(?:aijka|aajke|ajkeo|ajkey|ajkai|ajkei|ajhke|ajkea|ajkao|ajker|aske|ajka|ajke|ajko|ajk)\b` |
| কালকে | **kalke** | `temporal` | `kalke` (41), `klk` (5), `kalkei` (3), `kalker` (2), `kalk` (2), `kalka` (2) | `\b(?:kalkeo|kalker|kalkei|kaalke|kalka|kalke|kalkk|kalk|klk)\b` |
| গতকাল | **gotokal** | `temporal` | `gotokal` (14), `gotokl` (1), `gottkal` (1), `gotokaler` (0), `gotokalke` (0) | `\b(?:gotokaler|gotokalke|gottkal|gotokal|gotokl)\b` |
| তারিখ | **tarikh** | `temporal` | `tarikhe` (17), `tarik` (17), `tarikh` (15), `tarike` (3), `tarkh` (1), `tarika` (1) | `\b(?:tarikheo|tarikhei|tarikhe|tarike|tariik|tarikh|tarika|tarkh|tarik|tarek|trik)\b` |
| পরশু | **porshu** | `temporal` | `porsu` (1), `prsu` (1), `porso` (1), `porshu` (0) | `\b(?:porshu|porso|porsu|prsu)\b` |
| অটো | **auto** | `transport` | `auto` (82), `ata` (7), `aita` (4), `outo` (1), `auro` (1), `akto` (1) | `\b(?:aita|otou|outo|auto|akto|auro|atuo|ata|ato)\b` |
| উবার | **uber** | `transport` | `uber` (0) | `\buber\b` |
| গাড়ি | **gari** | `transport` | `gari` (5), `garir` (3), `gadi` (0) | `\b(?:garir|gari|gadi)\b` |
| ট্রেন | **train** | `transport` | `train` (8), `traino` (1), `brain` (1), `trein` (0) | `\b(?:traino|train|brain|trein)\b` |
| পাঠাও | **pathao** | `transport` | `pathao` (0) | `\bpathao\b` |
| বাস | **bus** | `transport` | `basa` (28), `bas` (11), `but` (11), `basay` (10), `baas` (7), `bus` (5) | `\b(?:basai|buser|beshi|bashi|basha|bhasa|bassa|basay|basae|bash|basa|buje|bass|buse|besi|basi|buja|boss|baas|bas|bus|but)\b` |
| ভাড়া | **vara** | `transport` | `vara` (1), `bharay` (1), `varah` (0), `varar` (0), `bharar` (0), `bhara` (0) | `\b(?:bharar|bharay|varah|varar|bhara|vaada|bada|vara)\b` |
| মেট্রো | **metro** | `transport` | `metro` (1), `metroi` (1) | `\b(?:metroi|metro)\b` |
| রিকশা | **rickshaw** | `transport` | `rickshaw` (1), `rikshaw` (1), `ricksha` (1), `ricksa` (0), `riksha` (0), `riksa` (0) | `\b(?:rickshaw|rikshaw|ricksha|ricksa|riksha|riksa)\b` |
| সিএনজি | **cng** | `transport` | `cng` (3), `c.n.g` (0) | `\b(?:c\.n\.g|cng)\b` |
| কিনছি | **kinsi** | `verb_buy` | `kinchi` (6), `kinsi` (4), `kinci` (3), `kinchilam` (2), `kineci` (1), `kunci` (1) | `\b(?:kinchilam|kineci|kinchi|kunci|kinsi|kince|kinci)\b` |
| কিনব | **kinbo** | `verb_buy` | `kinbo` (40), `kinbe` (5), `kinba` (5), `kinboo` (1), `kenbo` (1), `kibo` (1) | `\b(?:kinboo|kinbe|kinba|kinbo|kenbo|kibo)\b` |
| কিনলাম | **kinlam** | `verb_buy` | `kinlam` (7), `kinlem` (0) | `\b(?:kinlem|kinlam)\b` |
| কিনেছি | **kinechi** | `verb_buy` | `kinesilam` (2), `kinechi` (1), `kineci` (1), `kineche` (0) | `\b(?:kinesilam|kinechi|kineche|kineci)\b` |
| গেছে | **gese** | `verb_cost` | `geche` (81), `gese` (57), `gece` (29), `gase` (9), `gace` (9), `gache` (6) | `\b(?:geseee|gesilo|gache|ghace|geche|gasce|gecce|gechi|gesen|gace|gche|gesa|geca|gese|gele|gece|gasa|gase|gesi|gse)\b` |
| গেল | **gelo** | `verb_cost` | `gele` (203), `gelo` (111), `galo` (16), `gelei` (12), `gula` (12), `gala` (9) | `\b(?:gelom|galay|golao|gelay|gulai|gelei|galai|geleo|galae|geele|gayla|gela|gale|gala|galy|gell|gula|gele|gulu|golo|gulo|gali|gely|gelo|galo|geli|gola|glo|gle)\b` |
| লাগবে | **lagbe** | `verb_cost` | `lagbe` (674), `lagba` (45), `lage` (20), `lgbe` (12), `lagve` (9), `lagbei` (7) | `\b(?:lagabay|laghbe|laggbe|lagbey|nlagbe|laagby|lagbea|lagbei|lagbeo|lagaba|laagbe|lagbay|lagve|lagba|lagbe|lakbe|lahbe|lajbe|lagbi|lagce|kagbe|lagte|lagle|lagbo|legbe|lagse|lagby|lgbe|lage|legb|labe)\b` |
| লাগলো | **laglo** | `verb_cost` | `laglo` (147), `laglu` (2), `lagle` (2), `lagloy` (1), `lglo` (1), `lagbo` (1) | `\b(?:lagsilo|lagloy|lagle|laglu|lagbo|laglo|lglo)\b` |
| হইছে | **hoise** | `verb_cost` | `hoiche` (73), `hoice` (63), `hoise` (57), `hoyse` (12), `hoisa` (8), `hyse` (7) | `\b(?:hoyeche|hoiche|hoicay|hoisay|haiche|hosche|hoicha|hocche|hoisen|hoicee|hoiasa|hoisee|hoyese|hoyche|hoyse|hoice|hoile|hoisy|hoisi|hyche|hoysa|hossa|hoyce|house|hoige|huice|hocce|hoica|hoisa|hoicy|haysa|hoise|hcce|hcse|hyse)\b` |
| হলো | **holo** | `verb_cost` | `holo` (433), `hoilo` (10), `hlo` (7), `holona` (6), `holei` (3), `hoto` (2) | `\b(?:holona|hyloi|hoile|hilei|holoo|kholo|hoolo|hoilo|holow|holei|hoto|hill|hola|halo|holo|hol|hlo)\b` |
| করছি | **korsi** | `verb_do` | `korchi` (189), `korci` (166), `korsi` (158), `korce` (20), `krsi` (19), `korse` (18) | `\b(?:korsilam|korechi|karchi|korcho|korssi|koresi|korche|khrchi|koroch|khoros|korchi|koreci|kirchi|korcii|korcci|korsii|korce|korsi|korso|kurci|korsa|korci|korei|korbi|korse|koris|korca|korco|krchi|korti|korc|krsi|kori|krci)\b` |
| করলাম | **korlam** | `verb_do` | `korlam` (369), `korlm` (11), `krlam` (10), `krlm` (7), `korlem` (2), `kortam` (2) | `\b(?:korlao|kortam|korlam|kirlam|korlem|korlm|krlam|korla|krlm)\b` |
| করেছি | **korechi** | `verb_do` | `korechi` (121), `koresi` (56), `koreci` (51), `korsi` (24), `korci` (16), `korchi` (14) | `\b(?:korechoi|khoroche|korechil|korichi|koreshi|khoroch|korashi|koreysi|koreche|korechi|korachi|karici|korecy|korece|krechi|korese|koresi|korche|koraci|korase|krishi|koreci|korchi|koraji|korace|korehi|korce|korsi|korci|koros|korse|koris|kreci|krchi|krsi|krci)\b` |
| খেয়েছি | **kheyechi** | `verb_eat` | `khaici` (1), `kheyechi` (0), `khaisi` (0) | `\b(?:kheyechi|khaici|khaisi)\b` |
| খেলাম | **khelam** | `verb_eat` | `khelam` (2), `khalam` (1), `khellem` (1), `khailam` (0) | `\b(?:khellem|khailam|khelam|khalam)\b` |
| দিছি | **disi** | `verb_give` | `disi` (75), `dici` (71), `dichi` (36), `dese` (4), `dissi` (3), `dechi` (3) | `\b(?:diyechi|dischi|diachi|ditesi|diyesi|disen|dissi|diche|deshi|dichi|dicci|disci|diasi|dechi|deci|dica|dese|dosh|dici|dise|dice|disi|dsi)\b` |
| দিতে | **dite** | `verb_give` | `dite` (541), `diten` (53), `dita` (27), `dile` (25), `site` (22), `deta` (20) | `\b(?:diteeee|deteyo|diteu|ditee|ditea|dithe|diteh|ditey|detay|ditei|diite|diten|diteo|ditay|detey|duita|druto|duate|duti|dete|site|dide|rate|dise|deti|dita|deta|dite|dice|nite|lite|date|dibe|data|dile|dute|dote|die|dte)\b` |
| দিবে | **dibe** | `verb_give` | `diben` (361), `dibe` (190), `dite` (20), `dile` (19), `debe` (12), `dibo` (8) | `\b(?:dibay|drive|dibee|dibae|dibey|diben|nibe|dike|dise|diva|dite|dice|dive|deba|debe|daba|dibe|dile|dibn|debo|dibo|diba|dbe|die)\b` |
| দিয়েছি | **diyechi** | `verb_give` | `disi` (11), `diyechi` (8), `dieci` (2), `diechi` (1), `diyeci` (1), `diachi` (1) | `\b(?:diyeche|diyechi|diyeci|deachi|diyesi|diachi|diechi|dieci|deyce|diaci|diyac|disi|dece)\b` |
| দিল | **dilo** | `verb_give` | `dile` (582), `dilei` (62), `dila` (50), `dilo` (37), `dileo` (33), `dibo` (16) | `\b(?:deley|deleo|daile|diley|daily|delei|dhile|dilei|dileo|dilai|dilay|delea|dilao|dilee|dail|dele|dolo|silo|dilm|dala|dely|dilo|cilo|dito|dily|dili|nilo|dile|dila|dela|dial|dibo|delo|dio|dle|dal|dol|dil)\b` |
| দিলাম | **dilam** | `verb_give` | `dilam` (162), `delam` (11), `dilm` (7), `dilem` (2), `diram` (1), `dlam` (1) | `\b(?:diasilam|disilam|dilam|delem|dilem|delam|diram|dilan|dlam|dilm|dlm)\b` |
| পাইছি | **paisi** | `verb_receive` | `paici` (40), `paichi` (36), `paisi` (26), `paice` (5), `pisi` (2), `payesi` (2) | `\b(?:paichiii|payesi|paiche|paisee|paichi|paysi|paisi|payci|pacci|passi|pachi|paici|paice|paisy|paise|pisi|pais)\b` |
| পেয়েছি | **peyechi** | `verb_receive` | `paichi` (5), `peyechi` (3), `payci` (3), `peyesi` (2), `peyeci` (1), `payachi` (1) | `\b(?:payachi|peyechi|peyesi|peyeci|paichi|payci|paici)\b` |
| পেলাম | **pelam** | `verb_receive` | `pelam` (97), `pailam` (8), `palam` (7), `pelan` (2), `paylam` (2), `felam` (1) | `\b(?:paylam|pailam|paialm|pelam|pelan|palam|felam|pelm|plm)\b` |
| পাঠাইছি | **pathaisi** | `verb_send` | `pathaisio` (1), `pathaichi` (1), `paithachi` (1), `pathayci` (0), `pathaisi` (0), `pathaysi` (0) | `\b(?:pathaichi|pathaisio|paithachi|pathaysi|pathayci|pathaisi)\b` |
| পাঠালাম | **pathalam** | `verb_send` | `pathalam` (2), `pathaylam` (0) | `\b(?:pathaylam|pathalam)\b` |
| পাঠিয়েছি | **pathiyechi** | `verb_send` | `pathiyechi` (1), `pathechi` (0), `pathayechi` (0) | `\b(?:pathiyechi|pathayechi|pathechi)\b` |

## 2. Top Unsupervised Financial Entities

Items frequently appearing with transaction verbs (`kinlam`, `khelam`, `dilam`, `lagse`, `khoroch`):

| Entity / Item | Frequency |
|---|---|
| `request` | 28 |
| `download` | 23 |
| `vlo` | 19 |
| `install` | 14 |
| `message` | 12 |
| `msg` | 11 |
| `bole` | 10 |
| `upload` | 8 |
| `req` | 8 |
| `withdraw` | 7 |
| `kharap` | 7 |
| `kaje` | 7 |
| `knock` | 6 |
| `bhalo` | 6 |
| `update` | 6 |
| `bolei` | 6 |
| `bad` | 4 |
| `valoi` | 4 |
| `flash` | 4 |
| `add` | 4 |
| `report` | 4 |
| `rom` | 4 |
| `dollar` | 3 |
| `sshot` | 3 |
| `requ` | 3 |
| `reply` | 3 |
| `search` | 3 |
| `pass` | 3 |
| `sms` | 3 |
| `reqest` | 2 |

## 3. High Frequency Tokens Around Currency (`tk`, `taka`)


| Token | Frequency Around `tk`/`taka` |
|---|---|
| `koto` | 39 |
| `10` | 27 |
| `50` | 22 |
| `20` | 20 |
| `but` | 16 |
| `100` | 15 |
| `30` | 11 |
| `trickrace` | 10 |
| `thakle` | 9 |
| `300` | 9 |
| `15` | 8 |
| `19` | 8 |
| `500` | 8 |
| `25` | 8 |
| `kivabe` | 8 |
| `katbe` | 6 |
| `kto` | 6 |
| `1000` | 6 |
| `200` | 6 |
| `12` | 6 |
| `60` | 6 |
| `45` | 6 |
| `oi` | 5 |
| `29` | 5 |
| `lagbe` | 5 |