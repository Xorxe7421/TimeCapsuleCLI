# TimeCapsuleCLI

A command-line Java application for creating personal time capsules — messages, reflections, or files sealed until a future date. Capsules can be tagged, mood-scored, grouped into chains, filtered, and analyzed with rich statistics. Built with raw JDBC and PostgreSQL; no ORM, no frameworks.

---

## Features

- **Time-locked capsules** — write a message today, choose a future unlock date; the content is Base64-encoded and only revealed when you open it
- **File attachments** — attach any binary file to a capsule; it's stored in the database and written to disk on open
- **Mood tracking** — log your mood (1–10 scale) when creating a capsule; stats surface your average, highest, and lowest moods over time
- **Tags** — label capsules with 15 built-in tags (`PERSONAL`, `GOALS`, `CAREER`, `FITNESS`, `TRAVEL`, `REFLECTION`, and more)
- **Capsule chains** — group capsules into an ordered sequence where each new link can only be added after the previous capsule has been opened
- **Rich filtering** — list capsules by state (opened/closed), tags, creation date range, or unlock date range
- **Statistics dashboard** — capsule summary, time insights, mood overview, and tag frequency analysis, all selectable independently
- **Transactional writes** — capsule creation (content + tags + chain link) runs in a single JDBC transaction with rollback on failure

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Database | PostgreSQL (raw JDBC) |
| Data access | Vanilla JDBC with `PreparedStatement`, batch inserts, multi-result queries |
| Build | Maven (shaded JAR for single-file distribution) |
| Utilities | Lombok |

---

## Installation

### Prerequisites

- Java 25+
- PostgreSQL running on `localhost:5432`
- Maven

### Database Setup

Create the database and schema:

```sql
CREATE DATABASE time_capsule;
```

Then create the schema (adjust to match your setup):

```sql
CREATE SCHEMA main;

CREATE TABLE main.capsule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    attachment_name TEXT,
    attachment BYTEA,
    created_at DATE NOT NULL,
    unlock_at DATE NOT NULL,
    mood_score INT NOT NULL,
    is_opened BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE main.capsule_tag (
    capsule_id UUID REFERENCES main.capsule(id),
    tag TEXT NOT NULL
);

CREATE TABLE main.capsule_chain (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL
);

CREATE TABLE main.chain_link (
    chain_id UUID REFERENCES main.capsule_chain(id),
    capsule_id UUID REFERENCES main.capsule(id),
    sequence_order INT NOT NULL
);
```

### Configuration

Database host, port, and name are configured in `src/main/resources/datasource.properties`:

```properties
database.url=localhost
database.port=5432
database.name=time_capsule
```

Credentials are read from environment variables (never hardcoded):

```bash
export database_user=postgres
export database_password=yourpassword
```

### Build

```bash
mvn clean package
# Produces a shaded (fat) JAR with all dependencies included
java -jar target/TimeCapsuleCLI-1.0-SNAPSHOT.jar <command> [args] [--flags]
```

---

## Usage

### Commands

```
create capsule <title> <content> <unlockDate> <moodScore> [--flags]
create chain   <name>
list   capsule [--flags]
list   chain
open           <capsuleId>
stats          [--flags]
```

---

### `create capsule`

Create a new time capsule.

**Arguments:** `title`, `content`, `unlockDate` (ISO format: `YYYY-MM-DD`), `moodScore` (1–10)

**Optional flags:**
- `--attachment=<filepath>` — attach a file; stored in the DB and saved to the current directory on open
- `--tags=<tag1>,<tag2>` — one or more tags (e.g. `PERSONAL,GOALS`)
- `--chain-id=<uuid>` — add to an existing chain (only if the previous capsule in the chain has been opened)

```bash
java -jar app.jar create capsule "Letter to Future Me" "Work hard!" 2027-01-01 8
java -jar app.jar create capsule "2026 Goals" "Run a marathon" 2027-12-31 7 --tags=GOALS,FITNESS
java -jar app.jar create capsule "With a photo" "Remember this day" 2028-06-01 9 --attachment=/path/to/photo.jpg --tags=MEMORIES
```

---

### `create chain`

Create a named capsule chain.

```bash
java -jar app.jar create chain "Yearly Reflections"
```

---

### `list capsule`

List capsules, optionally filtered.

**Optional flags:**
- `--capsule-state=OPENED|CLOSED`
- `--tags=<tag1>,<tag2>` — only capsules that have **all** specified tags
- `--created-From=<date>` / `--created-to=<date>`
- `--unlock-from=<date>` / `--unlock-to=<date>`

```bash
java -jar app.jar list capsule
java -jar app.jar list capsule --capsule-state=CLOSED --tags=GOALS
java -jar app.jar list capsule --unlock-from=2027-01-01 --unlock-to=2027-12-31
```

---

### `list chain`

List all capsule chains with their linked capsules.

```bash
java -jar app.jar list chain
```

---

### `open`

Open a capsule by its UUID. The capsule must have reached its unlock date. Decodes the content, writes any attachment to the current directory, and marks the capsule as opened.

```bash
java -jar app.jar open f47ac10b-58cc-4372-a567-0e02b2c3d479
```

---

### `stats`

Display statistics. With no flags, shows all categories. Use flags to select specific sections.

**Optional flags:** `--CAPSULE`, `--TIME`, `--MOOD`, `--TAG`

```bash
java -jar app.jar stats
java -jar app.jar stats --MOOD --TAG
```

**Example output:**
```
CAPSULE SUMMARY
Total capsules: 12
Opened: 4
Unlocked: 6
Still locked: 6

TIME INSIGHTS
Oldest capsule: Jan 5, 2025
Newest capsule: Jun 20, 2026
Next unlock: Sep 1, 2026 (69 days)
Average wait time: 8.3 months

MOOD OVERVIEW
Average mood: 7.2/10
Highest mood: 9/10 (Excellent)
Lowest mood: 4/10 (Below Average)

TAG SUMMARY
Total unique tags: 7
Most used tag: GOALS (5 capsules)
Least used tag: TRAVEL (1 capsule)
```

---

## Moods

| Score | Label |
|---|---|
| 1 | Rock Bottom |
| 2 | Very Down |
| 3 | Low |
| 4 | Below Average |
| 5 | Neutral/Meh |
| 6 | Decent |
| 7 | Good |
| 8 | Great |
| 9 | Excellent |
| 10 | Euphoric |

---

## Available Tags

`PERSONAL` · `GOALS` · `CAREER` · `FITNESS` · `NEWYEAR` · `BIRTHDAY` · `FAMILY` · `HEALTH` · `REFLECTION` · `DREAMS` · `TRAVEL` · `WORK` · `ACHIEVEMENT` · `GRATITUDE` · `MEMORIES`

---

## Project Structure

```
src/main/java/org/pavl/
├── Main.java                    # Entry point
├── Utils.java                   # Base64 encode/decode, binary search upper bound, helpers
├── cli/
│   ├── CliHandler.java          # Command parsing and routing
│   └── domain/                  # CLI enums: Command, Entity, flags, CliInput record
└── db/
    ├── DbUtils.java             # PreparedStatement population utility
    ├── EntityUtils.java         # ResultSet → domain object mapping helpers
    ├── dao/
    │   ├── CapsuleDao.java      # Full CRUD + filtering + chain link logic
    │   ├── CapsuleChainDao.java # Chain listing with LEFT JOIN aggregation
    │   └── StatisticsDao.java   # Multi-result-set statistics queries
    └── domain/                  # Records and enums: CapsuleInput/Output, Mood, Tag, Stats, etc.
```

---

## Author

**Giorgi Pavliashvili** 
[LinkedIn](https://www.linkedin.com/in/giorgi-pavliashvili-6718861b6/)
