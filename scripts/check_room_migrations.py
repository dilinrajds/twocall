"""Check upgrade SQL against SQLite and Room's generated target schema."""
import re
import sqlite3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "android-app/app/src/main/java/com/twocall/chat/data/local/AppDatabase.kt").read_text()
generated = (root / "android-app/app/build/generated/ksp/release/java/com/twocall/chat/data/local/AppDatabase_Impl.java").read_text()
schema = re.findall(r'db.execSQL\("(CREATE (?:TABLE|(?:UNIQUE )?INDEX)[^"\n]+)"\)', generated)
schema = [sql for sql in schema if "room_master_table" not in sql]
migrations = re.findall(r'db.execSQL\("([^"\n]+)"\)', source)

target = sqlite3.connect(":memory:")
for sql in schema:
    target.execute(sql)

for version in (1, 2):
    db = sqlite3.connect(":memory:")
    if version == 1:
        for sql in schema:
            if "messages" in sql:
                db.execute(sql)
        db.execute("CREATE TABLE conversations (pairId TEXT NOT NULL PRIMARY KEY, partnerDeviceId TEXT, partnerOnline INTEGER NOT NULL, partnerTyping INTEGER NOT NULL, lastActiveTimestamp INTEGER NOT NULL)")
    else:
        for sql in schema:
            db.execute(sql.replace("`partnerImageBase64` TEXT, ", ""))
    db.execute("INSERT INTO conversations (pairId, partnerDeviceId, partnerOnline, partnerTyping, lastActiveTimestamp" +
               (", lastMessageTimestamp, unreadCount, isActive" if version == 2 else "") + ") VALUES ('pair', 'partner', 1, 0, 42" +
               (", 0, 0, 1" if version == 2 else "") + ")")
    db.execute("INSERT INTO messages (id,pairId,senderDeviceId,clientMessageId,plaintext,ciphertext,iv,messageType,status,isOutgoing,timestamp,isDeleted) VALUES ('message','pair','partner','client','preserved','ciphertext','iv','TEXT','READ',0,42,0)")
    for sql in migrations if version == 1 else migrations[-1:]:
        db.execute(sql)
    assert db.execute("SELECT plaintext FROM messages").fetchone() == ("preserved",)
    assert db.execute("SELECT pairId, partnerDeviceId, partnerOnline FROM conversations").fetchone() == ("pair", "partner", 1)
    for table in ("messages", "conversations", "call_logs"):
        actual = sorted(row[1:] for row in db.execute(f"PRAGMA table_info({table})"))
        expected = sorted(row[1:] for row in target.execute(f"PRAGMA table_info({table})"))
        assert actual == expected, (version, table, actual, expected)
        actual_indices = sorted(row[1:3] for row in db.execute(f"PRAGMA index_list({table})"))
        expected_indices = sorted(row[1:3] for row in target.execute(f"PRAGMA index_list({table})"))
        assert actual_indices == expected_indices, (version, table)
    print(f"Room {version}->3: data preserved and target schema/indexes match")
