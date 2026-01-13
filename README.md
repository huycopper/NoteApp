# 📝 NoteApp

An Android note-taking application with cloud synchronization via Supabase.

## 📥 Download APK



---

## ✨ Features

### 📋 Note Management
- ➕ **Create notes** - Add new notes with title and content
- ✏️ **Edit notes** - Update existing note content
- 🗑️ **Delete notes** - Remove unwanted notes
- 📜 **List notes** - View all notes in a RecyclerView

### ☁️ Cloud Sync (Supabase)
- 🔐 **Sign up / Sign in** - User authentication with Email/Password
- 🔄 **Two-way sync** - Upload and download notes from cloud
- 📴 **Offline mode** - Use the app without signing in
- 🔒 **Data security** - Row Level Security (RLS) ensures only note owners can access their data

### 🎨 User Interface
- 🌓 **Dark Mode support** - Automatically follows system settings
- 🟠 **Orange theme** - Modern and eye-catching design
- 📱 **Material Design 3** - Follows Android design guidelines

---

## 🛠️ Technologies Used

| Technology | Purpose |
|------------|---------|
| **Kotlin** | Primary programming language |
| **Android SDK 36** | Target SDK |
| **SQLite** | Local data storage |
| **Supabase** | Backend-as-a-Service (BaaS) |
| **Supabase Auth** | User authentication |
| **Supabase Postgrest** | Database API |
| **Ktor Client** | HTTP Client for Supabase SDK |
| **View Binding** | Type-safe view access |
| **Coroutines** | Asynchronous programming |
| **Material Components** | UI Components |

---

## 📁 Project Structure

```
app/src/main/java/com/example/noteapp/
├── MainActivity.kt          # Main screen - notes list
├── AddNoteActivity.kt        # Add new note screen
├── UpdateNoteActivity.kt     # Edit note screen
├── AuthActivity.kt           # Sign in/Sign up screen
├── Note.kt                   # Data class for local notes
├── NotesAdapter.kt           # RecyclerView Adapter
├── NotesDatabaseHelper.kt    # SQLite Database Helper
├── SupabaseClientManager.kt  # Supabase Client Manager
├── SyncManager.kt            # Data synchronization manager
└── NetworkUtils.kt           # Network connectivity checker
```

---

## 🚀 Installation & Setup

### Requirements
- Android Studio Arctic Fox or later
- JDK 11+
- Android SDK 24+ (min) / 36 (target)

### Step 1: Clone the project
```bash
git clone https://github.com/huycopper/NoteApp.git
cd NoteApp
```

### Step 2: Configure Supabase

1. Create a new project at [Supabase](https://supabase.com)
2. Run `supabase_setup.sql` in Supabase SQL Editor
3. Copy `.env.example` to `.env` and add your credentials:

```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
```

> ⚠️ **Note**: `.env` file is in `.gitignore` and will NOT be committed to version control.

### Step 3: Build & Run
```bash
./gradlew assembleDebug
```
Or open the project in Android Studio and click Run.

---

## 🗄️ Database Schema

### SQLite (Local)
```sql
CREATE TABLE allnotes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    content TEXT,
    supabase_id TEXT,
    user_id TEXT,
    updated_at INTEGER DEFAULT 0,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0
)
```

### Supabase (Cloud)
```sql
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id INTEGER,
    user_id UUID REFERENCES auth.users(id),
    title TEXT NOT NULL,
    content TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    is_deleted BOOLEAN DEFAULT FALSE
)
```