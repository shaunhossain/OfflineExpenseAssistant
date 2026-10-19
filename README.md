# 📱 Offline Expense Assistant (Android)

> **100% On-Device, Privacy-First Financial Assistant powered by Gemma 270M (LiteRT-LM), Model Context Protocol (MCP), and Natural Language Banglish Intelligence.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![LiteRT-LM](https://img.shields.io/badge/AI-LiteRT--LM%20Gemma%20270M-orange.svg)](https://ai.google.dev)
[![Architecture](https://img.shields.io/badge/Architecture-MCP%20%2B%20Clean%20Architecture-teal.svg)](https://modelcontextprotocol.io)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

---

## ✨ Overview

**Offline Expense Assistant** is a modern, privacy-focused Android application designed to track and query daily finances through conversational natural language — **without requiring an internet connection or external cloud API**.

It integrates **Google's LiteRT-LM (Gemma 270M Q8)** on-device LLM with the **Model Context Protocol (MCP)** tool calling interface, coupled with an ultra-fast deterministic parser specially tailored for **Banglish**, **Bengali script**, and **colloquial Bangladeshi financial expressions**.

---

## 🚀 Key Features

### 🔒 100% Offline & Private
- **Zero data leaves your device:** All transaction logging, classification, querying, and AI inference happen directly on the CPU/GPU using on-device models.
- No third-party servers, no analytics trackers, and no subscription fees.

### 🇧🇩 Natural Language Banglish & Bengali Comprehension
- Understands natural conversational Banglish and Bengali phrasing:
  - `"50 tk bus vara"`
  - `"cha ar bill 100 tk"`
  - `"dupure 250 tk lunch korsi"`
  - `"office theke bashay ashte rickshaw te 80 taka laglo"`
  - `"4 tarek 120 tk bajar korlam"`
  - `"amdr 1500 tk electricity bill disi"`
  - `"35000 tk salary payci"` *(automatically recognized as Income)*
- Supported in English, Banglish transliteration, and native Bengali script (`দেড়শ টাকা নাস্তা`, `বারোশ টাকা বাজার`, `বাসা ভাড়া`).

### 🧮 Traditional Bengali Fractions & Spoken Number Words
Deep native support for traditional spoken fractions and hundred-based number systems:
- **`der` / `dersho` / `derso` / `der so` / `der soto`:** 1.5 ৳, 150 ৳, 1,500 ৳ (`der hajar`).
- **`arai` / `araisho` / `araiso` / `arai soto`:** 2.5 ৳, 250 ৳, 2,500 ৳ (`arai hajar`).
- **`sare <number>` (সাড়ে):** `sare sotero` (17.5 ৳), `sare barosho` (1,250 ৳), `sare tin hajar` (3,500 ৳).
- **Hundred-words:** `barosho` (1,200 ৳), `choddosho` (1,400 ৳), `ponerosho` (1,500 ৳), `satsho` (700 ৳).
- **`sowa` (+0.25) & `poune` (-0.25):** `sowa tin` (3.25 ৳), `poune char` (3.75 ৳), `sowa hajar` (1,250 ৳).

### 🔍 Natural Language Financial Analytics & Queries
Ask conversational questions to get instant financial summaries and item breakdowns:
- **Balance:** `"amar balance koto"`, `"koto taka baki ache"`, `"koto taka ace"`
- **Category / Item Spends:** `"bazar e koto khoroch hoise"`, `"total bus vara koto"`, `"rickshaw te koto gese"`
- **Highest Spends:** `"highest expense konta"`, `"sobcheye beshi khoros konta"`
- **Daily Summaries:** `"ajke koto khoroch"`, `"gotokal koto khoroch holo"`, `"ajk saradin e koto koroch holo"`
- **Monthly Summaries:** `"ei mashe koto khoroch hoise"`, `"month data dekhao"`

### ⚡ Hybrid Inference Architecture: Fast-Deterministic + Gemma LLM
- **Sub-10ms Instant Matching:** Common spending inputs and queries are resolved deterministically using pre-compiled phonetic regular expressions.
- **On-Device LLM Fallback:** Complex and ambiguous conversational prompts fall back to **Gemma 270M Q8** running through LiteRT-LM.

### 🛠️ Model Context Protocol (MCP) Integration
- Strict tool-use pattern separating intent extraction from database operations.
- Supported local MCP tools:
  - `add_transaction`: Record single or multiple structured expenses / incomes.
  - `get_financial_summary`: Fetch balance, total incomes, and category aggregations.
  - `get_item_expenses`: Query spending on specific tags, merchants, or keywords.
  - `get_highest_expense`: Identify single largest expense transaction.
  - `set_expense_reminder`: Schedule daily notifications.
  - `seed_mock_data`: 1-click batch generation of 1,000 realistic transactions for testing.

### 📊 Modern Material 3 Analytics UI
- Interactive Monthly & Daily Analytics Dashboard.
- Visual category distribution (Food, Transport, Grocery, Bills, Health, Shopping, Entertainment, etc.).
- Complete paginated transaction ledger with instant search and filtering.

---

## 🏗️ Architecture & Tech Stack

```
OfflineExpenseAssistant/
├── app/src/main/java/com/example/expenseassistant/
│   ├── ai/                      # On-device AI runtime
│   │   ├── BanglishNormalizer.kt# Phonetic & Bengali fraction pre-normalizer
│   │   ├── GemmaRunner.kt       # LiteRT-LM Gemma runtime engine
│   │   └── PromptTemplates.kt   # System prompts and few-shot templates
│   ├── mcp/                     # Model Context Protocol
│   │   ├── McpClient.kt         # Tool invocation coordinator
│   │   └── McpToolRegistry.kt   # Structured schemas & tool execution handlers
│   ├── domain/                  # Business Logic & Validation
│   │   └── TransactionValidator.kt # Category mapping, bounds check, income detection
│   ├── data/                    # Storage Layer
│   │   ├── AppDatabase.kt       # Room Database
│   │   └── TransactionDao.kt    # SQL queries, sums, item search, date filters
│   └── ui/                      # Presentation Layer (Jetpack Compose)
│       ├── MainActivity.kt      # Main Entry & Navigation
│       ├── MainViewModel.kt     # State machine & conversational pipeline
│       └── components/          # Theme, Dialogs, Charts, and Chat UI
```

* **Language:** Kotlin 2.0+
* **Framework:** Android Jetpack (Compose, Navigation, ViewModel, Lifecycle)
* **Database:** Room (SQLite)
* **AI Runtime:** Google LiteRT-LM (formerly TFLite-LM)
* **LLM:** Gemma 270M IT (Int8 / Q8 quantization)
* **Dependency Injection:** Modern Android Architecture Components

---

## 📥 Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2+ or Android Studio Meerkat
- JDK 17 or higher
- Android SDK 35 (Minimum SDK 26)
- Physical Android device or Emulator with at least 4GB RAM (e.g. Google Pixel 8)

### Clone & Build
```bash
git clone https://github.com/shaunhossain/OfflineExpenseAssistant.git
cd OfflineExpenseAssistant

# Run unit tests (34+ automated tests for Banglish & fractions)
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug

# Install directly to connected device
./gradlew installDebug
```

### On-Device Model Setup
The app expects the quantized Gemma model (`gemma-270m-it-cpu-int8.bin` or similar) in the application's external files directory:
```bash
adb push gemma-270m-it-cpu-int8.bin /sdcard/Android/data/com.example.expenseassistant.debug/files/
```
*(Note: If the model is not present, the app continues to operate seamlessly using its fast deterministic NLP and MCP tool registry!)*

---

## 🧪 Testing

Comprehensive unit tests cover Banglish transliterations, phonetic variations, edge cases, and Bengali fractional expressions:

```bash
./gradlew testDebugUnitTest
```

Key test suites:
- `testBengaliSpokenFractionsAndHundreds`: Verifies `der`, `arai`, `sare sotero`, `barosho`, `derso`, `der soto`.
- `testMinedBanglishExpenseVariations`: Verifies real-world spelling variants mined from the BanglaTLit dataset.
- `testMinedBanglishQueryVariations`: Verifies item totals, balances, and period summaries.
- `testDateExtractionWithTarikh`: Verifies relative and explicit date parsing (e.g., `4 tarikh 100 tk sawpno bazar`).

---

## 📄 License

This project is open-source and licensed under the **[MIT License](LICENSE)**.

```
Copyright (c) 2026 Shaun Hossain
```
