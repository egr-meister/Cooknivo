# Cooknivo

A native Android **offline personal recipe organizer**, built with Kotlin and Jetpack Compose.

> Keep your own recipes, ingredients, steps, and cooking notes in one private local collection.

Cooknivo lets you create and maintain a private collection of personal recipes. You manually save recipe names, categories, ingredients and quantities, ordered preparation steps, preparation/cooking/resting times, serving labels, personal notes, favorite status, and shopping-list ingredients. The home screen is a **recipe card box**: recipes appear as physical-style index cards inside a culinary card box, categories are divider tabs, and favorites carry a small bookmark marker.

---

## Main features

- Create, edit, duplicate, archive, restore, and delete personal recipes
- Ordered ingredient lists with optional groups (Dough, Filling, Sauce, …)
- Ordered, auto-numbered preparation steps with stable reordering
- Manual preparation, cooking, and resting times with a computed total
- Optional free-text serving labels and personal notes
- Default and custom categories (divider tabs) with reordering and hide-if-empty
- Favorites with a dedicated screen and bookmark markers
- Fully local search across names, categories, ingredients, steps, and notes
- Shopping list with preview-based generation from user-entered ingredients
- Neutral collection statistics (no health, diet, or nutrition scoring)
- Recently opened recipes, onboarding, and a settings screen
- Works fully offline with local-only storage

---

## Disclaimers and content rules

### Manual recipe disclaimer

> Cooknivo is a manual personal recipe organizer. Recipes, ingredients, quantities, preparation steps, times, and notes are entered by the user. The app does not provide online recipes, nutritional advice, dietary guidance, medical recommendations, or food safety guarantees.

### Ingredient and safety disclaimer

> Cooknivo does not verify ingredients, allergens, cooking temperatures, appliance settings, food condition, or dietary suitability. Review your recipe details and follow appropriate package, appliance, and food-safety guidance.

### User-created content rule

Cooknivo contains **only content entered by the user**. It does not include online recipe feeds, copied recipe articles, famous-chef recipes, restaurant recipes, branded recipes, scraped recipes, recipe-API content, copyrighted recipe photography, or external recommendations. Any example data would be generic, clearly fictional, and removable; the production build starts with an **empty collection** and guides you to create your first personal recipe.

### What Cooknivo does **not** do

- **No online recipes** — Cooknivo never downloads, scrapes, recommends, generates, or displays online recipes.
- **No third-party recipe content** — no food-brand, restaurant, supermarket, or copyrighted recipe content.
- **No recipe APIs** — no remote recipe services of any kind.
- **No calorie tracking** and **no nutritional advice**.
- **No diets**, **no weight-loss** features, **no fasting/medical meal plans**.
- **No medical advice** and **no allergy checking** or allergen identification.

Cooknivo does not claim to provide professional culinary instruction, guarantee cooking results, guarantee food safety, calculate nutrition, create balanced diets, support weight loss, identify allergens, check ingredient compatibility, recommend medical diets, download recipes, own third-party content, verify recipe originality, or replace package/appliance instructions.

---

## Architecture

Cooknivo uses a deliberately simple **MVVM** structure, prioritizing release stability over layered complexity:

- **One local repository** (`CooknivoRepository`) backed by DataStore Preferences with serialized JSON.
- **One app-wide `CooknivoViewModel`** exposing an observable `AppData` snapshot via `StateFlow`, plus thin, guarded action wrappers. A single ViewModel is intentional: it avoids fragile cross-screen state coordination and keeps every screen reading one consistent, immutable snapshot. Search is debounced and filtered off the main thread with `Flow` operators.
- **Focused utilities** for ordering, time calculation, search/filtering, shopping-list generation, and statistics.
- **No dependency-injection framework**, **no networking**, **no Room** — DataStore JSON is sufficient for this data model.
- Immutable UI state where useful; `StateFlow` for observable data.

### Offline-only architecture

The app has no `INTERNET` permission, no backend, no remote APIs, and no cloud sync. Everything runs on-device.

### Local storage

All application data is stored locally using **DataStore Preferences**, holding serialized JSON strings under these keys:

- `recipes_json`
- `recipe_ingredients_json`
- `preparation_steps_json`
- `recipe_categories_json`
- `shopping_items_json`
- `settings_json`

Deserialization is resilient: missing keys, empty JSON, unknown/added fields, and individual malformed records are all handled without crashing. Item-level recovery keeps valid records even if one object is corrupt. Default categories are seeded once on first launch (using stable IDs) and are **never duplicated** on relaunch. Fallback labels (`Deleted Recipe`, `Uncategorized`, `Ingredient unavailable`, `Step unavailable`) are used when a referenced record is missing. Complete stored JSON, recipe notes, and step instructions are **not** logged in release builds.

---

## Data models

Kotlin data classes with `kotlinx.serialization`, all with safe defaults for backward-compatible deserialization.

### Recipe

`id, name, categoryId, customCategoryName, description, preparationTimeMinutes?, cookingTimeMinutes?, restingTimeMinutes?, servingLabel, favorite, archived, notes, createdAt, updatedAt, lastOpenedAt`

- Name and category are required; description, times, serving, and notes are optional.
- Time values are non-negative integer minutes (max 10080 per field); missing times count as zero only for total calculation.
- Duplicate recipe names are allowed. IDs are generated locally.

### RecipeIngredient

`id, recipeId, name, quantityLabel, groupName, shoppingCategory, addToShoppingByDefault, sortOrder, note, createdAt, updatedAt`

Ingredient name is required; quantity is optional free text (e.g. "2 cups", "to taste"). Duplicate ingredients are allowed and order is preserved.

**Ingredient groups** are supported (e.g. Dough, Filling, Sauce, Topping, Main Ingredients) and are used only to visually group ingredients on the detail card. Ingredient text is never parsed for nutrition, allergies, health, or safety.

### PreparationStep

`id, recipeId, stepNumber, title, instruction, timerLabel, note, sortOrder, createdAt, updatedAt`

Instruction is required; title, timer label, and note are optional. Blank steps are never saved.

**Step ordering:** steps preserve order and are auto-renumbered (`stepNumber` 1..n, `sortOrder` 0..n-1) whenever they are added, deleted, duplicated, inserted, or reordered. Deleting a step safely renumbers the rest.

### RecipeCategory

`id, name, colorKey, sortOrder, isDefault, hiddenWhenEmpty, archived, createdAt, updatedAt`

### ShoppingItem

`id, title, quantityLabel, category, sourceRecipeId?, sourceIngredientId?, checked, note, createdAt, updatedAt`

### AppSettings

`onboardingCompleted, defaultCategoryId?, cardDensity, showRecentlyOpened, favoritesFirst, shoppingGenerationMode`

Enums: `ShoppingCategory` (Produce, Dairy, MeatAndFish, Bakery, Pantry, Frozen, Drinks, Spices, Household, Other), `RecipeCardDensity` (Comfortable, Compact), `ShoppingGenerationMode` (PreviewFirst, AddMissing, AddAll).

---

## Feature details

### Cooking-time calculation

Total time = preparation + cooking + resting (missing fields count as zero). Values are stored as positive integer minutes, use `Long` math internally to prevent overflow, and are capped at a reasonable per-field maximum (10080 minutes). If all fields are missing, the app shows **"Time not set"** and never presents zero minutes as meaningful data. Formatting examples: `25 min`, `1 hr 20 min`, `2 hr`, `Time not set`. Times are never estimated automatically.

### Serving labels

Optional free text such as "4 servings", "1 loaf", "2 jars", "Family portion". Cooknivo does not calculate serving sizes, calories per serving, or interpret labels.

### Categories

Neutral defaults: Breakfast, Main Dishes, Side Dishes, Soups, Salads, Baking, Desserts, Snacks, Drinks, Sauces, Other. No health-based categories (no Weight Loss, Diabetic, Detox, Low Calorie, Medical Diet, Healthy Only).

### Custom categories

You can add, rename, recolor, reorder, hide-when-empty, and delete **unused** custom categories. Default categories cannot be permanently deleted (they may be hidden when empty). Before deleting a category with recipes, reassign or move those recipes; the delete action is only offered for empty, non-default categories.

### Favorites

A manual user preference. Favorites show a bookmark marker, have a dedicated screen, and can be searched/sorted. Favorite status never implies health, quality, or recommendation, and favorites are never assigned automatically.

### Recipe search

Fully local and deterministic. Searches recipe names first, then categories, ingredients, description, steps, and notes. Case-insensitive and international-character friendly. Filters: category, favorite, archived, has time, has ingredients, has steps, recently opened. Sorts: name, recently updated/created/opened, total time, ingredient count, step count, favorites-first. Search input is debounced in the ViewModel and filtered off the main thread; queries are never sent anywhere. Empty result state: **"No matching recipes."**

### Recipe duplication

Creates a new recipe ID, copies ingredients and steps with new IDs, preserves ordering, appends "Copy" to the name, sets fresh timestamps, and never links future edits between copies. Example: "Tomato Soup" → "Tomato Soup Copy". Duplicate names are allowed.

### Recipe archive

Archived recipes disappear from the active card box but remain in the Archive screen and are still referenced by historical shopping items. They can be restored, duplicated, or permanently deleted (with confirmation). Archive empty state: **"No archived recipes."**

### Recipe notes

Optional personal notes (substitutions, preferred pan, family comments, reminders, variations). Line breaks are preserved, remaining characters are shown, and a 3000-character maximum applies. Cooknivo never generates recommendations from note content.

### Shopping list

Fully local. Unchecked items appear first, then checked; items are grouped by shopping category and show their source recipe. You can add custom items, add selected or all recipe ingredients, edit/check/uncheck/delete items, clear checked items, and filter by category. No store links, prices, affiliate links, delivery, product recommendations, or supermarket branding.

### Shopping preview and generation

Adding recipe ingredients opens a **preview** showing the recipe name, selected ingredients, quantity labels, shopping categories, and which items already exist in the list, with **Add Missing**, **Add All**, and **Cancel**. Generation rules: uses only user-entered ingredients, never invents ingredients, preserves quantity labels as text, groups only when trimmed names match case-insensitively, never combines incompatible quantities, never converts units, preserves manually created items, and prevents accidental duplicate generated items (while allowing explicit duplicates).

**Quantity-label limitation:** quantities are plain text only. Cooknivo never parses them into numbers, never converts units, and never combines quantities mathematically.

### Recently opened recipes

`lastOpenedAt` is updated locally when a recipe detail opens. A "Recently Opened" section appears at the front of the card box (toggleable in Settings). No analytics or usage data is created or sent, and recipe names are not logged in release builds.

### Collection statistics

Neutral figures only: total active recipes, favorites, archived, recipes by category, recipes with ingredients/steps, average ingredient/step counts, recently added count, and remaining shopping items. Visualizations are built with Compose (count columns, a favorite-ratio bar) — **no chart library**. No calorie totals, diet/health/quality/skill scores, or nutritional balance.

---

## Visual concept

**"Recipe Card Box" / "Kitchen Index Card Box".** The home screen resembles a physical culinary card box rendered entirely with Compose layouts, shapes, borders, shadows, and simple drawing — no external recipe-card images. It includes a warm wood-inspired box frame, category divider tabs, layered index cards with visible edges (the active card raised from the stack), favorite bookmark corners, an integrated Add Recipe card, a search action, a shopping-list slip near the bottom, and a recently-opened section. Each card shows the recipe name, category, total time, ingredient and step counts, favorite marker, and a short note preview.

### Layout uniqueness

Cooknivo deliberately avoids the generic "mascot → title → subtitle → stats card → stack of big buttons → settings" layout. Instead it uses a recipe-box layout: a compact header with a search action, horizontal category divider tabs, a card-box area with layered cards, favorite bookmark corners, a recently-used section, a shopping-list slip, and bottom navigation (Recipes, Favorites, Shopping, Categories, Settings). Sections are a mixture of index cards, divider tabs, recipe sheets, ingredient lines, numbered step strips, shopping slips, bookmark corners, and an archive drawer — not identical rounded cards.

### Recipe-card box rendering approach

The box frame and cards are pure Compose: `Box`/`Column`/`Row` with `RoundedCornerShape`, borders, background colors from the warm palette (terracotta, cream, wood), a colored left "ruled margin" per card, and a `Bookmark` icon for favorites. No bitmaps are required.

### App icon concept

A custom adaptive icon (foreground + background vector, plus PNG fallbacks for pre-API-26 launchers) showing a warm terracotta/wood background with a simplified open recipe-card box, two cream index cards, a category divider tab, and a small favorite bookmark. No food photograph, chef portrait, brand logo, calorie symbol, or text. It remains readable at small launcher sizes and is not the default Android icon.

### Splash screen concept

A custom static splash via the AndroidX Core SplashScreen API: a warm cream background, the centered recipe-card-box icon, and the app name, with a subtle terracotta divider tab. No photography, cloud symbols, or heavy animation.

---

## Technology stack

Kotlin · Jetpack Compose · Material 3 · Navigation Compose · Android ViewModel · Kotlin Coroutines · Kotlin Flow · DataStore Preferences · Kotlinx Serialization · Gradle Kotlin DSL. Portrait-only, English-only, fully offline.

Not used: Retrofit/OkHttp/Ktor, Firebase, SQLDelight/Realm/Room, networking or cloud SDKs, recipe/nutrition/health SDKs, barcode/OCR/image-loading libraries, chart or heavy-animation libraries, and DI frameworks.

### Absence statements

No push notifications · no background processing (no WorkManager/AlarmManager/services) · no camera · no image upload · no barcode scanner · no OCR · no account · no backend · no cloud sync · no Firebase · no ads · no analytics · no payments · no internet · no runtime permissions.

### Privacy note

> Cooknivo stores recipes, ingredients, preparation steps, cooking times, categories, favorites, notes, shopping items, and settings locally on this device. The app has no account, no cloud sync, no internet access, no online recipes, no ads, no analytics, no payments, no camera access, and no nutrition service.

---

## Screens

Onboarding · Recipe Card Box (home) · Add Recipe · Edit Recipe · Recipe Detail · Recipe Search · Favorites · Categories · Category Detail · Shopping List · Shopping Preview · Archive · Statistics · Settings. Navigation is handled with Navigation Compose and tolerates missing recipe/category/ingredient/step/shopping IDs, deleted or archived records, invalid filter arguments, empty DataStore, and restored process state — always showing a friendly fallback with a Back action instead of crashing.

---

## Building and running

### Open in Android Studio

1. Install **Android Studio** (Koala or newer recommended).
2. **File → Open** and select the project root (the folder containing `settings.gradle.kts`).
3. Let Gradle sync. Android Studio regenerates the Gradle wrapper on first sync if needed.

### Requirements

- **JDK 17**
- **Android API 35**: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 24`
- Android Gradle Plugin 8.5.x, Kotlin 1.9.24, Compose Compiler 1.5.14
- Gradle 8.9 (via the wrapper)

### 16 KB page-size compatibility

Because the app is pure Kotlin/Compose/DataStore with **no native third-party binaries**, it is compatible with Android 15+ 16 KB memory page sizes. Still verify the final release bundle on a 16 KB target/emulator.

### Debug build

```
./gradlew :app:assembleDebug
```

Install:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Non-minified release build (do this first)

The release build type ships with `isMinifyEnabled = false` and `isShrinkResources = false`. Build and validate this first:

```
./gradlew :app:assembleRelease
```

Install the signed non-minified release, launch it, and complete the functional checklist below while watching `adb logcat`.

### Enabling R8 (staged)

Only after the non-minified release is verified, enable shrinking in `app/build.gradle.kts`:

```kotlin
getByName("release") {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
    ...
}
```

Rebuild and reinstall the minified release, then re-test Kotlinx Serialization, DataStore, Navigation Compose, recipe editing, ordering, search, and shopping-list generation. The included `proguard-rules.pro` keeps `@Serializable` models and their generated serializers.

---

## Signing

Release APK and AAB are signed with a real **PKCS12** keystore. The build **never** falls back to the Android debug key for release artifacts — if release credentials are missing, the build fails with a clear message.

### Generate a keystore

```
keytool -genkeypair -v -storetype PKCS12 -keystore cooknivo-release-key.p12 -alias cooknivo_key -keyalg RSA -keysize 2048 -validity 10000
```

### Local signing setup

Create a git-ignored `keystore.properties` in the project root:

```
storeFile=/absolute/path/to/cooknivo-release-key.p12
storePassword=your-store-password
keyAlias=cooknivo_key
keyPassword=your-key-password
```

`app/build.gradle.kts` reads signing values from environment variables first (used by CI), then from `keystore.properties` (used locally). Both `signingConfigs.release` (APK and AAB) use these credentials.

**Never commit** the PKCS12 file, passwords, a decoded keystore, or secret signing properties. `.gitignore` already excludes `*.p12`, `*.jks`, `*.keystore`, `keystore.properties`, and build outputs.

### Required GitHub Secrets

- `ANDROID_KEYSTORE_BASE64` — base64 of your `.p12` file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Create the base64 value with `base64 -w0 cooknivo-release-key.p12` (Linux) or `base64 -i cooknivo-release-key.p12` (macOS).

---

## GitHub Actions

`.github/workflows/android-build.yml` runs on push to `main` and via manual dispatch. It checks out the repo, sets up JDK 17 and the Android SDK (Platform 35, Build Tools 35.0.0), provisions Gradle 8.9 with caching (generating the wrapper if absent), decodes `ANDROID_KEYSTORE_BASE64` into a temporary PKCS12 file, exposes signing secrets only as environment variables, runs unit tests, and builds the **signed release APK and AAB**. It then locates the APK, runs `apksigner verify --print-certs`, prints the certificate, **fails** if verification fails or if the certificate contains `CN=Android Debug`, and uploads the signed APK (test artifact) and signed AAB (Google Play artifact). Passwords and base64 values are never printed.

CI is responsible for compilation, signing, certificate verification, and artifact generation — it is **not** proof that the app launches. Always run a local install/launch test.

---

## Build outputs

- **Signed release APK** — for local installation and verification.
- **Signed release AAB** — for Google Play. **Only the `.aab`** is uploaded to Google Play.

---

## Local release verification

```
./gradlew :app:assembleRelease
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat
```

The signing certificate must **not** contain `CN=Android Debug`. Repeat all of the above after enabling R8.

Check `adb logcat` for: `ClassNotFoundException`, `NoSuchMethodError`, serialization crashes, DataStore parse crashes, navigation-argument crashes, missing recipe/category crashes, ingredient/step-order crashes, duplicate category initialization, invalid time parsing, shopping-generation crashes, card-box layout crashes, R8-related crashes, and signing misconfiguration.

---

## Local functional test checklist

Test, at minimum:

- First launch with empty storage; onboarding; skip onboarding; verify default categories
- Create/rename/reorder custom categories; hide empty category
- Create first recipe; recipes without ingredients / without steps / without time
- Add prep/cook/resting time; verify total time
- Add / edit / reorder / delete ingredients; grouped ingredients
- Add / edit / duplicate / reorder / delete steps; verify renumbering
- Add serving label, description, notes
- Mark / remove favorite; open Favorites
- Edit recipe; duplicate recipe; verify independent copied IDs
- Archive / restore; permanently delete an archived recipe
- Search by name, category, ingredient, step text, notes; filter favorites/archived; sort recently updated/opened
- Open Category Detail; add selected / all ingredients to shopping list; preview matching items; prevent accidental duplicates
- Add custom shopping item; check item; clear checked items
- Open Statistics
- Delete a category with recipes (verify it is blocked / reassignment path); restore default categories
- Delete shopping list; delete all recipes; reset all local data; relaunch
- Launch in airplane mode and confirm full functionality
- Confirm no INTERNET permission, no runtime permission dialog, no online search, no camera/image picker, no calorie/nutrition/diet fields
- Inspect `adb logcat`; verify release certificate; verify AAB generation; verify API 35; verify 16 KB page-size compatibility

---

## Settings and data reset behavior

Settings include default category, recipe-card density (Comfortable/Compact), show recently-opened toggle, favorites-first sorting toggle, default shopping generation mode (Preview First / Add Missing / Add All; default **Preview First**), show onboarding again, restore default categories, clear checked shopping items, delete archived recipes, delete shopping list, delete all recipes, reset all local data, app information, both disclaimers, and the privacy note. Destructive actions require explicit confirmation.

**Reset all local data** permanently removes every recipe, ingredient, preparation step, category, favorite, note, shopping item, and setting, then re-seeds the default categories.

---

## Manual-entry limitations

Every piece of content in Cooknivo is entered by you. The app does not verify, correct, complete, or interpret your recipes, ingredients, quantities, times, or notes, and it provides no nutrition, allergen, dietary, medical, or food-safety guidance. Always follow appropriate package, appliance, and food-safety instructions.
