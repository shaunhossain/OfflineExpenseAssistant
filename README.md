# 📱 Offline Expense Assistant (Android)

> **100% On-Device, Privacy-First Personal Financial Assistant powered by Gemma 3 270M (LiteRT-LM), Model Context Protocol (MCP), and Natural Language Banglish Intelligence.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![LiteRT-LM](https://img.shields.io/badge/AI-LiteRT--LM%20Gemma%203%20270M-orange.svg)](https://ai.google.dev)
[![Architecture](https://img.shields.io/badge/Architecture-MCP%20%2B%20Clean%20Architecture-teal.svg)](https://modelcontextprotocol.io)
[![Tests](https://img.shields.io/badge/Unit%20Tests-93%20Passed%20(100%25)-brightgreen.svg)](./app/src/test/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## ✨ Overview

**Offline Expense Assistant** is a modern, privacy-focused Android application designed to log, manage, and query daily personal finances through natural conversational language — **without requiring an internet connection or external cloud API**.

The application combines **Google's LiteRT-LM (Gemma 3 270M Q8)** on-device LLM with the **Model Context Protocol (MCP)** tool calling interface, coupled with an ultra-fast deterministic parser specially tailored for **Banglish**, **Bengali script**, and **colloquial Bangladeshi financial expressions**.

---

## 🚀 Key Features

### 🔒 100% Offline & Private
- **Zero data leaves your device:** All transaction logging, classification, querying, and AI inference happen entirely on-device using local CPU/GPU acceleration.
- No third-party servers, no analytics trackers, and no subscription fees.

### 🇧🇩 Natural Language Banglish & Bengali Comprehension
- Understands colloquial conversational Banglish and Bengali phrasing:
  - `"50 tk bus vara"`
  - `"cha ar bill 100 tk"`
  - `"dupure 250 tk lunch korsi"`
  - `"office theke bashay ashte rickshaw te 80 taka laglo"`
  - `"4 tarek 120 tk bajar korlam"`
  - `"bills babod 1500 tk electricity bill disi"`
  - `"food category te 350 tk khoroch"`
  - `"35000 tk salary payci"` *(automatically recognized as Income)*
- Supported across English, Banglish phonetic transliteration, and native Bengali script (`দেড়শ টাকা নাস্তা`, `বারোশ টাকা বাজার`, `বাসা ভাড়া`).

### 🛡️ Anti-Hallucination Guardrails & Negative Statement Interception
- Small language models (SLMs) can hallucinate expense creation when users say negative sentences. Deterministic pre-compiled regex guards intercept conversational dismissals and negatives before reaching the model:
  - `"kono khoroch hoy nai"` / `"ajke khoroch kori nai"`
  - `"bad dao"` / `"thak dorkar nai"` / `"cancel"`
  - `"কোনো খরচ হয়নি"` / `"দরকার নেই"` / `"থাক কিছু না"`
- Prevents false transaction proposals and saves on-device battery/RAM.

### 🔄 Sequential Expense Isolation
- Keyed Jetpack Compose state architecture (`remember(confirmation)` & `key(conf)`) and synchronous state dismissal in `MainViewModel`.
- Automatic post-action KV-cache resets in `GemmaEngine` and user-input presence checks in `AssistantRouter` guarantee that entering one expense (e.g. `50 tk bus vara`) and then another (e.g. `100 tk cha`) **never carries over stale descriptions**.

### 🧮 Traditional Bengali Fractions & Spoken Number Words
Deep native support for traditional spoken fractions and hundred-based number systems:
- **`der` / `dersho` / `derso` / `der so` / `der soto`:** 1.5 ৳, 150 ৳, 1,500 ৳ (`der hajar`).
- **`arai` / `araisho` / `araiso` / `arai soto`:** 2.5 ৳, 250 ৳, 2,500 ৳ (`arai hajar`).
- **`sare <number>` (সাড়ে):** `sare sotero` (17.5 ৳), `sare barosho` (1,250 ৳), `sare tin hajar` (3,500 ৳).
- **Hundred-words:** `barosho` (1,200 ৳), `choddosho` (1,400 ৳), `ponerosho` (1,500 ৳), `satsho` (700 ৳).
- **`sowa` (+0.25) & `poune` (-0.25):** `sowa tin` (3.25 ৳), `poune char` (3.75 ৳), `sowa hajar` (1,250 ৳).

### 🔍 Conversational Financial Analytics & Queries
Ask natural questions to get instant financial summaries and item breakdowns:
- **Balance & Cash:** `"amar balance koto"`, `"pocket e koto taka ache"`, `"haate koto ache"`, `"টাকা পয়সার হিসাব"`, `"পকেটে কত টাকা আছে"`
- **Category / Sector Spends:** `"bazar e koto khoroch hoise"`, `"total bus vara koto"`, `"medical khate koto laglo"`
- **Spending Patterns & Trends:** `"spending patterns dekhao"`, `"khorocher dhoron"`, `"ব্যয়ের বিশ্লেষণ"`
- **Highest Spends:** `"highest expense konta"`, `"sobcheye beshi khoros konta"`
- **Daily & Periodic Summaries:** `"ajke koto khoroch"`, `"gotokal koto khoroch holo"`, `"shesh 7 diner hisab dekhao"`
- **Search Transactions:** `"search biryani"`, `"napa search koro"`, `"খুঁজে বের করো নাপা"`
- **Transaction Updates & Deletes:** `"update transaction 5 to 500 tk"`, `"last expense delete koro"`

### ⚡ Hybrid Inference Architecture
- **Sub-10ms Instant Matching:** Common spending inputs and queries are resolved deterministically using pre-compiled regular expressions in `BanglishNormalizer`.
- **On-Device LLM:** Complex, compound, and advisory prompts run through **Gemma 3 270M Q8** via Google LiteRT-LM.

### 🛠️ Model Context Protocol (MCP) Integration
Strict tool-use design pattern separating natural language intent extraction from local SQLite database operations:
- `prepare_expense` / `prepare_income`: Record structured single expenses or incomes.
- `prepare_multiple_expenses`: Batch extraction for compound statements (`100 tk cha ar 50 tk rickshaw`).
- `update_transaction`: Modify amount, note, or category of existing entries by ID.
- `delete_transaction` / `delete_last_transaction`: Delete specific or most recent transaction.
- `search_transactions`: Full-text search across notes, tags, and merchants.
- `get_financial_balance`: Calculate net balance, cash, and total savings.
- `get_spending_summary` / `get_period_summary`: Aggregate month/week/day spending.
- `get_spending_patterns`: Extract recurring spending trends and habit insights.
- `propose_reminder` / `list_reminders` / `toggle_reminder` / `delete_reminder`: Schedule and control daily expense notifications.
- `get_database_stats`: View total records, telemetry, and database footprint.
- `clear_all_transactions`: 1-click ledger reset with confirmation safety.

---

## 🏗️ Architecture & Tech Stack

```
OfflineExpenseAssistant/
├── app/src/main/java/com/example/expenseassistant/
│   ├── ai/                      # On-device AI runtime
│   │   ├── AssistantRouter.kt   # Dual-engine router (Gemma LLM + Deterministic)
│   │   ├── BanglishNormalizer.kt# Phonetic, dialect, fraction & regex normalizer
│   │   ├── BanglishTransliterator.kt # 245k-corpus Banglish semantic bridge
│   │   ├── ConversationalAiHelper.kt # Offline advisory & coaching fallbacks
│   │   ├── GemmaEngine.kt       # Google LiteRT-LM runtime manager
│   │   ├── ModelOutputParser.kt # Strict JSON parser for model responses
│   │   └── PromptBuilder.kt     # System prompts, few-shot schemas & context injection
│   ├── mcp/                     # Model Context Protocol
│   │   ├── ExpenseTools.kt      # Core MCP financial tools implementation
│   │   └── McpToolRegistry.kt   # Tool dispatch, schema registry & execution
│   ├── domain/                  # Business Logic & Validation
│   │   ├── Money.kt             # Minor-unit currency representations
│   │   └── TransactionValidator.kt # Category classification & sanity bounds
│   ├── data/                    # Storage Layer
│   │   ├── AppDatabase.kt       # Room Database
│   │   ├── ExpenseDao.kt        # SQLite operations, aggregations & searches
│   │   └── ExpenseRepository.kt # Reactive data repository & habit analytics
│   └── ui/                      # Presentation Layer (Jetpack Compose)
│       ├── MainActivity.kt      # Entry Activity & Bottom Navigation
│       ├── MainViewModel.kt     # State machine & chat orchestration
│       ├── components/          # ConfirmationDialog, MultipleDialog, ReminderDialog
│       └── screens/             # ChatAssistantScreen, AnalyticsScreen, RemindersScreen
└── scripts/                     # Fine-tuning, dataset generation & corpus mining
    ├── Banglish_Normalizer_Mining.ipynb
    ├── Banglish_Gemma3_LoRA_Finetune_LiteRT.ipynb
    └── generate_banglish_finetune_dataset.py
```

* **Language:** Kotlin 2.0+
* **Framework:** Android Jetpack (Compose, Navigation, ViewModel, Coroutines, Flow)
* **Database:** Room (SQLite) with reactive Flow streams
* **AI Runtime:** Google LiteRT-LM (`com.google.ai.edge.litert:litert-lm`)
* **LLM:** Gemma 3 270M IT (Int8 / Q8 quantization) tracked via Git LFS
* **Design System:** Material 3 with Dynamic Color & Dark Mode

---

## 📥 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2+) or Meerkat
- JDK 17 or higher
- Android SDK 35 (Minimum SDK 26)
- Git & Git LFS (`git lfs install`)
- Physical device or Emulator with 4GB+ RAM

### Clone & Build
```bash
# Clone with Git LFS to pull the quantized Gemma model in assets
git clone https://github.com/shaunhossain/OfflineExpenseAssistant.git
cd OfflineExpenseAssistant
git lfs pull

# Run unit tests (93 automated unit tests)
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Install directly to connected device
./gradlew installDebug
```

---

## 🧪 Testing

Comprehensive automated unit tests cover Banglish transliterations, anti-hallucination patterns, sequential description isolation, and MCP tool executions:

```bash
./gradlew testDebugUnitTest
```

Key test suites in [`app/src/test/`](file:///Users/shaunhossain/AndroidStudioProjects/personal/OfflineExpenseAssistant/app/src/test/):
- **[`BanglishNormalizerTest.kt`](file:///Users/shaunhossain/AndroidStudioProjects/personal/OfflineExpenseAssistant/app/src/test/java/com/example/expenseassistant/BanglishNormalizerTest.kt)**:
  - `testAntiHallucinationAndAdvancedQueryPatterns`: Intercepts cancellations, negative statements, spending trends, and pocket balances.
  - `testRegexPatternBasedQueryExtraction`: Validates pre-compiled regex extraction for updates, searches, limits, and deletions.
  - `testBengaliSpokenFractionsAndHundreds`: Verifies traditional fractions (`der`, `arai`, `sare`, `barosho`, `satsho`).
  - `testMinedBanglishExpenseVariations`: Tests real-world spelling variants mined from parallel corpora.
- **[`AssistantRouterTest.kt`](file:///Users/shaunhossain/AndroidStudioProjects/personal/OfflineExpenseAssistant/app/src/test/java/com/example/expenseassistant/AssistantRouterTest.kt)**:
  - `testSequentialExpensesDoNotLeakPreviousDescription`: Verifies that entering sequential expenses isolates descriptions across turns without multi-turn carryover.
  - `testRouterPrioritizesGemmaStructuredOutput`: Tests LLM structured tool calling and fallback pathways.
- **[`PromptBuilderTest.kt`](file:///Users/shaunhossain/AndroidStudioProjects/personal/OfflineExpenseAssistant/app/src/test/java/com/example/expenseassistant/PromptBuilderTest.kt)**:
  - Verifies dynamic financial context injection and Banglish semantic bridge construction.

---

## 📄 License

This project is open-source and licensed under the **[MIT License](LICENSE)**.

```
Copyright (c) 2026 Shaun Hossain
```
