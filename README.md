# Spendora

A modern Android personal finance app that tracks daily expenses, income, accounts, and shared (lent/borrowed) money with friends. Spendora keeps your data live in the cloud with a fast offline-first local cache, and lets you export your full statement to PDF or Excel.

> Internal module name: `DailyExpenseTracker` &nbsp;•&nbsp; Application ID: `com.sonarkini.spendora`

---

## Features

**Smart transaction tracking**
- Log expenses, salary/income, gifts, bill payments, self transfers between accounts, and custom transaction types.
- Each transaction is tied to a category, sub-category, account, payment mode, date, and optional note.
- Edit, soft-delete, and audit history (`ACTIVE` / `EDITED` / `DELETED` status with timestamps).

**Multi-account support**
- Track money across `BANK`, `INVESTMENT`, and `CASH` accounts.
- Account balances auto-update on every add / edit / delete via a Firestore transaction (atomic).
- Self-transfer between two accounts in a single entry.

**Categories & sub-categories**
- Ships with 15 default categories (Housing, Utilities, Groceries, Govt Services, Dining Out, Entertainment, Healthcare, Shopping, Education, Connectivity, Fitness, Subscriptions, Travel, Gifts, Miscellaneous).
- Add your own categories and sub-categories with custom icon and color.

**Friends, splits, and lent/borrowed**
- Add friends by email, phone, or username — registered Spendora users get linked automatically.
- Split any expense with a friend (equal / percentage / custom amount).
- Lend, borrow, repay, and receive — and the matching transaction is mirrored to your friend's account in real time, so both sides stay in sync.
- Net "you owe / you are owed" balance per friend.

**Insights**
- Home tab: today / week / month summaries, recent activity, balance overview.
- Insights tab: charts and breakdowns by category, time period, and account.
- Time filters across the app for any custom range.

**Authentication & profile**
- Firebase Auth: email/password and Google Sign-In (phone OTP scaffolding included).
- First-run registration (username, display name, date of birth, profile photo).
- Profile tab with theme toggle (dark mode is default), data export, clear-all-data, log out, and account deletion.

**Export**
- Generate a combined PDF statement of your transactions (iTextG).
- Excel export via Apache POI.
- Files are shared securely via `FileProvider`.

**Other niceties**
- Dark / light theme toggle ("Fintech" theme).
- Offline-first: Room mirrors every Firestore collection so the app remains usable without a network.
- Real-time updates: Firestore snapshot listeners push changes into the UI instantly.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose (BOM 2024.02.01), Material 3, Material Icons Extended |
| Architecture | MVVM with a single Repository, `StateFlow` everywhere |
| Local DB | Room 2.6.1 (KSP) — DB name `expense_tracker_db`, schema v24 |
| Cloud | Firebase Auth, Cloud Firestore, Firebase Analytics (BOM 32.7.2) |
| Auth | Google Play Services Auth 21.0.0 |
| Google APIs | Drive v3, Sheets v4 (client libs included) |
| Images | Coil 2.5.0 |
| PDF | iTextG 5.5.10 |
| Excel | Apache POI 4.1.2 (`poi`, `poi-ooxml`) |
| Build | Gradle (Kotlin DSL), AGP 8.2.2, JVM target 11 |

**SDK targets:** `minSdk 26`, `targetSdk 34`, `compileSdk 34`.

---

## Project structure

```
DailyExpenseTracker/
├── app/
│   ├── build.gradle.kts                  # App module (deps, compose, KSP, google-services)
│   ├── google-services.json              # *NOT in repo — see Setup*
│   └── src/main/
│       ├── AndroidManifest.xml           # Permissions: INTERNET, ACCESS_NETWORK_STATE; FileProvider
│       ├── res/                          # Themes (Theme.Spendora), strings, launcher icons, file_paths
│       └── java/com/example/dailyexpensetracker/
│           ├── MainActivity.kt           # Entry point — wires DB → Repo → ViewModel → Compose
│           ├── data/
│           │   ├── ExpenseRepository.kt  # Firestore listeners + Room cache + business rules
│           │   └── local/
│           │       ├── AppDatabase.kt    # Room DB (5 entities, version 24)
│           │       ├── TransactionEntity.kt / TransactionDao.kt
│           │       ├── AccountEntity.kt  / AccountDao.kt
│           │       ├── CategoryEntity.kt / SubCategoryEntity.kt / CategoryDao.kt
│           │       ├── FriendEntity.kt   / FriendDao.kt
│           │       └── UserEntity.kt     / UserDao.kt
│           ├── ui/
│           │   ├── screens/
│           │   │   ├── ExpenseTrackerScreen.kt   # Top-level scaffold + auth gating
│           │   │   ├── LoginScreen.kt            # Email / Google / Phone sign-in
│           │   │   ├── RegistrationScreen.kt    # First-run profile setup
│           │   │   ├── Charts.kt                 # Insights charts
│           │   │   ├── CommonComponents.kt
│           │   │   ├── TimeFilter.kt
│           │   │   └── tabs/
│           │   │       ├── HomeTab.kt            # Summary + recent activity
│           │   │       ├── InsightsTab.kt        # Trends & breakdowns
│           │   │       ├── LentBorrowedTab.kt    # Friends & splits
│           │   │       └── ProfileTab.kt         # Profile, settings, export
│           │   ├── theme/                        # FintechTheme, Color, Type
│           │   └── viewmodel/
│           │       └── ExpenseViewModel.kt       # Reactive flows + actions
│           └── utils/
│               ├── PdfGenerator.kt               # iTextG combined statement
│               ├── IconUtils.kt
│               └── Extensions.kt
├── build.gradle.kts                      # Root build script
├── settings.gradle.kts                   # rootProject.name = "Spendora"
├── gradle/libs.versions.toml             # Version catalog
└── gradlew / gradlew.bat
```

---

## Architecture

```
 Compose UI ──▶ ExpenseViewModel ──▶ ExpenseRepository ──▶ Firestore (source of truth)
                     ▲                       │
                     │                       └──▶ Room (offline cache, mirrored)
                     └────── StateFlow ◀─────┘
```

- **Source of truth:** Cloud Firestore, organized as `users/{uid}/{transactions|accounts|categories|subcategories|friends}`.
- **Offline cache:** Each Firestore snapshot listener writes through to Room, so the UI keeps working without network.
- **Real-time UI:** The repository exposes `Flow<List<...>>` from `callbackFlow { addSnapshotListener { ... } }`; the ViewModel turns those into `StateFlow`s with `WhileSubscribed(5000)`.
- **Atomic balance updates:** Account balance changes run inside `firestore.runTransaction { ... }` with a `signFactor` so add / edit / delete all reuse the same code path.
- **Friend sync:** When a transaction has a `friendUid`, the repo writes a mirrored, role-flipped copy (LENT ↔ BORROWED, REPAID ↔ RECEIVED) into the friend's user document — both sides see the entry instantly.

### Transaction model (key fields)

`TransactionEntity` covers: `amount`, `type` (EXPENSE / SALARY / LENT / BORROWED / RECEIVED / REPAID / SELF_TRANSFER / GIFT / BILL PAYMENT / LOAD GIFT CARD / OTHER), `categoryId`, `subCategoryId`, `accountId`, `toAccountId`, split fields (`isSplit`, `splitAmount`, `splitType`, `splitRatio`), friend fields (`friendUid`, `friendName`, `friendContact`), `note`, `status`, `spentAt`, `createdAt`, `updatedAt`, `originalTransactionId`.

---

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11
- An Android device or emulator running API 26+
- A Firebase project (free Spark plan works)

### 1. Clone and open
```bash
git clone <your-repo-url>
cd DailyExpenseTracker
```
Open the project in Android Studio.

### 2. Configure Firebase
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an Android app with package name **`com.sonarkini.spendora`**.
3. Download `google-services.json` and drop it into `app/`.
4. In **Authentication**, enable the providers you want:
   - Email / Password
   - Google (paste your support email)
   - (Optional) Phone
5. In **Firestore**, create a database in production mode. Suggested rules to start:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
         match /{coll}/{doc} {
           allow read, write: if request.auth != null && request.auth.uid == uid;
         }
       }
       // friend-mirror writes — tighten to your taste
       match /users/{uid}/transactions/{txId} {
         allow write: if request.auth != null;
       }
     }
   }
   ```
6. Replace `default_web_client_id` in `app/src/main/res/values/strings.xml` with your project's Web Client ID (from Firebase Console → Project Settings → OAuth 2.0 client IDs).

### 3. Build
```bash
./gradlew assembleDebug         # Linux / macOS
gradlew.bat assembleDebug       # Windows
```
Or hit **Run** in Android Studio.

---

## Running tests

```bash
./gradlew test                  # unit tests
./gradlew connectedAndroidTest  # instrumented tests (needs a device/emulator)
```

---

## Permissions

| Permission | Why |
|---|---|
| `INTERNET` | Firebase Auth + Firestore sync |
| `ACCESS_NETWORK_STATE` | Detect connectivity for offline-first behavior |

A `FileProvider` is registered under `${applicationId}.provider` so PDF / Excel exports can be shared securely.

---

## Roadmap / known TODOs

- Phone OTP sign-in is wired in the UI but the verify path is a stub.
- Schema currently uses `fallbackToDestructiveMigration()` — proper Room migrations should land before any production release.
- Excel export uses Apache POI which significantly increases method count; consider R8 / minify in release builds.
- Multi-currency support.

---

## License

No license file is included in this repository yet. Add one (e.g. MIT or Apache 2.0) before publishing.
