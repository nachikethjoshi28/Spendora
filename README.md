# Spendora – Daily Expense Tracker

An Android personal-finance app built with **Jetpack Compose**, **Firebase Firestore**, and **Room**.
Track spending, split bills with friends, view monthly PDF statements, and get insights into your financial health.

> Internal module name: `DailyExpenseTracker` · Application ID: `com.example.dailyexpensetracker`

---

## Table of Contents
1. [Features](#features)
2. [Architecture Overview](#architecture-overview)
3. [Project Structure](#project-structure)
4. [Data Model](#data-model)
5. [Transaction Type Reference](#transaction-type-reference)
6. [Math & Calculation Logic](#math--calculation-logic)
7. [Screen Guide](#screen-guide)
8. [Setup & Build](#setup--build)
9. [Known Quirks & Design Decisions](#known-quirks--design-decisions)

---

## Features

| Area | What you can do |
|---|---|
| **Transactions** | Add, edit, soft-delete income / expenses / loans / card payments |
| **Split expenses** | Split any expense equally, by percentage, or by fixed amount; supports "friend paid" mode |
| **Friends** | Track per-friend balances; add friends by email or phone; sync transactions bidirectionally |
| **Insights** | Interactive charts (line, bar, pie) filtered by time period |
| **Statements** | Download a full-colour PDF statement for any past or current month |
| **Accounts** | Multiple bank / credit-card / investment / cash accounts with live balances |
| **Categories** | Fully customisable categories and sub-categories (stored per-user in Firestore) |
| **Themes** | Light and dark mode via a toggle in the Profile tab |

---

## Architecture Overview

```
UI (Jetpack Compose)
    │
    ▼
ExpenseViewModel  ──────────────────────────────────┐
    │  (StateFlow / coroutines)                     │
    ▼                                               │
ExpenseRepository                                  │
    ├── Firestore (remote, real-time listeners)     │
    └── Room (local cache / offline fallback)  ─────┘
```

- **Single Activity** (`MainActivity`) hosts one `ExpenseTrackerScreen` composable.
- **No navigation library** – tab switching is handled by a `selectedTab` integer in `MainScaffold`.
- **ViewModel** is scoped to the Activity; it survives configuration changes.
- **Firestore listeners** stream data into `callbackFlow`s; each snapshot is also written to Room so the app works offline.

---

## Project Structure

```
app/src/main/java/com/example/dailyexpensetracker/
│
├── data/
│   ├── ExpenseRepository.kt          ← Single data-access layer (Firestore + Room)
│   └── local/
│       ├── AppDatabase.kt            ← Room database definition
│       ├── TransactionEntity.kt      ← Transaction data class (Room + Firestore)
│       ├── TransactionDao.kt         ← Room DAO for transactions
│       ├── CategoryEntity.kt         ← Category (parent) data class
│       ├── SubCategoryEntity.kt      ← Sub-category data class
│       ├── CategoryDao.kt            ← DAO for categories + sub-categories
│       ├── AccountEntity.kt          ← Account data class
│       ├── AccountDao.kt             ← Room DAO for accounts
│       ├── FriendEntity.kt           ← Friend contact data class
│       ├── FriendDao.kt              ← Room DAO for friends
│       ├── UserEntity.kt             ← Logged-in user profile
│       └── UserDao.kt
│
├── ui/
│   ├── components/
│   │   ├── CommonComponents.kt       ← Shared composables (TransactionItem, pickers, inputs…)
│   │   ├── Charts.kt                 ← Chart composables (line, bar, pie)
│   │   └── TimeFilter.kt             ← Time-filter enum + helper
│   ├── tabs/
│   │   ├── ExpenseTrackerScreen.kt   ← Root screen: auth gate → MainScaffold
│   │   ├── HomeTab.kt                ← Dashboard: balance card, history, statements
│   │   ├── AddTransactionTab.kt      ← Add / edit transaction form
│   │   ├── FriendsTab.kt             ← Friends list + per-friend detail
│   │   ├── InsightsTab.kt            ← Analytics charts and breakdowns
│   │   ├── ProfileTab.kt             ← User profile, accounts, settings
│   │   ├── LoginScreen.kt            ← Firebase Auth login / sign-up
│   │   └── RegistrationScreen.kt     ← Username + DOB registration step
│   ├── theme/
│   │   ├── Color.kt                  ← Colour tokens (light + dark palettes)
│   │   ├── Theme.kt                  ← MaterialTheme setup
│   │   ├── FintechTheme.kt           ← App-specific colour aliases
│   │   └── Type.kt                   ← Typography scale
│   └── viewmodel/
│       └── ExpenseViewModel.kt       ← All UI state + business logic
│
├── utils/
│   ├── PdfGenerator.kt               ← iText-based monthly statement PDF builder
│   ├── Extensions.kt                 ← String / formatting helpers (toSentenceCase…)
│   └── IconUtils.kt                  ← Maps category names to Material icons
│
└── MainActivity.kt                   ← Entry point; sets up ViewModel + Compose content
```

> **Dead file:** `ExpenseRepository.kt` at the root package level is an empty stub — safe to delete.
> The real repository is `data/ExpenseRepository.kt`.

---

## Data Model

### TransactionEntity (key fields)

| Field | Type | Description |
|---|---|---|
| `id` | String | UUID, primary key |
| `amount` | Double | **Full bill amount** (not split-adjusted) |
| `type` | String | See [Transaction Type Reference](#transaction-type-reference) |
| `categoryId` | String? | Links to `CategoryEntity.id` |
| `subCategoryId` | String? | Free-text sub-category name |
| `accountId` | String? | Debit account |
| `toAccountId` | String? | Credit account (card payments / transfers only) |
| `isSplit` | Boolean | Whether this expense is shared with a friend |
| `splitAmount` | Double | **Friend's share** (not yours) |
| `splitType` | String? | `EQUAL` / `PERCENTAGE` / `AMOUNT` |
| `friendPaid` | Boolean | `true` = friend paid full bill; I owe my share |
| `friendName` | String? | Display name of the involved friend |
| `friendUid` | String? | UID of the friend if they are a registered user |
| `status` | String | `ACTIVE` / `DELETED` / `EDITED` |
| `spentAt` | Long | Transaction date chosen by the user (epoch ms) |
| `createdAt` | Long | When the record was first created (epoch ms) |

**User's share** is always derived at read-time, never stored separately:
```kotlin
val userShare = if (isSplit) amount - splitAmount else amount
```

---

## Transaction Type Reference

| Type | Main Category | Meaning | Account balance impact |
|---|---|---|---|
| `EXPENSE` | Spent | Regular spending | −userShare |
| `OTHER` | Spent | Miscellaneous spending | −amount |
| `REPAID` | Spent | You paid back a debt to a friend | −amount |
| `SALARY` | Received | Salary / paycheck | +amount |
| `RECEIVED` | Received | Friend paid you back their debt | +amount |
| `BORROWED` | Received | Friend lent you money | +amount |
| `GIFT` | Received | Gift received | +amount |
| `INCOME` | Received | Other income | +amount |
| `LENT` | Debts & Loans | You lent money to a friend | −amount |
| `BILL PAYMENT` | Card Payment | Pay credit-card bill from a bank account | −fromAccount, +toAccount |
| `LOAD GIFT CARD` | Card Payment | Load a gift-card balance | −fromAccount, +toAccount |
| `SELF_TRANSFER` | (internal) | Transfer between own accounts | −fromAccount, +toAccount |

> **Important:** `BILL PAYMENT` is stored with a **space**, not an underscore.
> All code that checks this type (TransactionItem, account-balance updates, PdfGenerator)
> must use `"BILL PAYMENT"` — never `"BILL_PAYMENT"`.

---

## Math & Calculation Logic

### Income
```
income = SALARY + RECEIVED + BORROWED + GIFT + INCOME
```
- BORROWED is included because it is a real cash inflow (even though it creates a future liability).
- REPAID is **not** income — it is an outflow (debt payment).

### Expense
```
expense = EXPENSE(userShare) + OTHER + LENT + REPAID
```
For split `EXPENSE`: `userShare = amount – splitAmount`

### Net Dues (per-friend balance)
`balance > 0` → that friend owes **me**. `balance < 0` → **I** owe that friend.

| Transaction type | Contribution to balance |
|---|---|
| `LENT` | `+amount` — friend owes me |
| `BORROWED` | `−amount` — I owe friend |
| `RECEIVED` | `−amount` — friend paid me back (their debt clears) |
| `REPAID` | `+amount` — I paid friend back (my debt clears → balance rises) |
| `EXPENSE` split, I paid | `+splitAmount` — friend owes their share |
| `EXPENSE` split, friend paid | `−(amount − splitAmount)` — I owe friend my share |

> **Common mistake:** REPAID must contribute **+amount** to the balance.
> Using −amount (wrong) makes the balance go *more negative* after you repay a debt,
> as if you owe even more.

### Savings (hero card)
```
savings      = earnedIncome − expense
earnedIncome = SALARY + GIFT + INCOME   (BORROWED and RECEIVED excluded on purpose)
```
BORROWED/RECEIVED are excluded from `earnedIncome` so the hero card reflects true
savings discipline rather than being inflated by loans or repayments received.

---

## Screen Guide

### Home Tab (`HomeTab.kt`)
- **Hero card** — net savings for the selected time window; tapping "Statements" opens the PDF downloader.
- **Mini-cards** — Income / Expense / Dues totals at a glance.
- **History list** — paginated (15 per page); tap to expand details, long-press to edit/delete.
- **Time filter** — All Time / Last Month / Last 6 Months / Last Year / Custom date range.
- **Statements sheet** — groups transactions by calendar month; tapping a month generates a PDF and opens the system share-sheet.

### Add Transaction Tab (`AddTransactionTab.kt`)
- **Main type chips** — Spent / Received / Card Payment / Debts & Loans.
- **Sub-type chips** — refined type within the main group (e.g. EXPENSE / REPAID / OTHER under Spent).
- **Split toggle** — appears only for `EXPENSE` type; exposes split method and friend fields.
  - Split types: Equal (50/50), Percentage (you choose friend's %), Fixed Amount.
  - "Who paid?" toggle: "I paid" or "Friend paid".
- **Category / Sub-category** — required for `EXPENSE`; categories are loaded live from Firestore.
  New categories can be added inline via the "+" button (identical flow to sub-categories).
- **Account selector** — hidden when "Friend paid" is selected (no account debit in that case).

### Friends Tab (`FriendsTab.kt`)
- Lists friends with their net balance (green = they owe you, red = you owe them).
- Tap a friend card → drill-down (`FriendDetailView`) showing all shared transactions.
- FAB → "Add Friend" (search by email/phone) or "Create Group" (coming soon).
- **Add Friend dialog** — searches Firestore for a registered user; pre-fills nickname from username.

### Insights Tab (`InsightsTab.kt`)
- Time-filtered line chart (daily income vs expense), bar chart (monthly), and pie charts.
- Category breakdown, per-friend owed/owing amounts.
- Financial health score derived from savings rate.

### Profile Tab (`ProfileTab.kt`)
- Edit profile picture (local URI stored in Firestore).
- Manage accounts — add new, edit balance/name, view transaction history per account, delete.
- Toggle dark/light mode.
- Sign out / delete account (deletes all Firestore data).

---

## Setup & Build

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- A Firebase project with **Authentication** (Email/Password) and **Firestore** enabled

### Steps
1. Clone the repository.
2. Create a Firebase project at https://console.firebase.google.com.
3. Enable **Email/Password** authentication and **Cloud Firestore**.
4. Download `google-services.json` and place it in the `app/` directory.
5. Open the project in Android Studio.
6. **Build → Make Project** (or `./gradlew assembleDebug` from terminal if Java is configured).
7. Run on a device or emulator (API 26+).

### Firestore Collections (per-user path: `users/{uid}/`)
| Collection | Description |
|---|---|
| `transactions` | All transactions (active + soft-deleted) |
| `accounts` | User's financial accounts |
| `categories` | User's expense categories |
| `subcategories` | Sub-categories linked to a parent category |
| `friends` | Friend contacts |

---

## Known Quirks & Design Decisions

| Topic | Detail |
|---|---|
| **Date restriction in Add Transaction** | The date picker limits selection to the current calendar month (`minDate = 1st of current month`). This prevents cross-month backdating. Widening this requires removing the `minDate` restriction. |
| **BORROWED counted as income** | Borrowing counts as income in totals (it is a cash inflow), but is excluded from `earnedIncome` used for the hero savings figure. |
| **Soft deletes** | Transactions set `status = "DELETED"` rather than being removed from Firestore. This preserves the audit trail and allows friend-sync delete propagation. |
| **Category IDs** | Default (seed) categories use their name as `id` (e.g. `id = "Groceries"`). User-created categories use a UUID. |
| **Account balance on creation** | Adding an account with `balance > 0` auto-creates a `SALARY` transaction to represent that opening balance. |
| **Statement PDF coroutine scope** | `StatementDialog` uses `rememberCoroutineScope()`. `onDismiss()` must be called **inside** the PDF coroutine (after generation finishes). Calling it outside causes the composable to unmount, cancelling the scope and silently killing the PDF job — the share-sheet never appears. |
| **BILL PAYMENT type string** | Stored as `"BILL PAYMENT"` (space). If ever written as `"BILL_PAYMENT"` (underscore) the account-balance update and TransactionItem colour logic both miss it. All code in the app uses the space variant. |
