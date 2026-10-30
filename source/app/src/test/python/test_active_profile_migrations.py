"""Run with Python unittest; uses the exported v1 Room schemas and actual migration SQL.

This verifies SQLite data preservation. Android Room open/identity validation is a
separate device installation check and is not represented as a host JVM test.
"""
import json
import re
import sqlite3
import unittest
from pathlib import Path

APP = Path(__file__).resolve().parents[3]
CASES = (("users", "User", "name"), ("devices", "Device", "serialNumber"))


class ActiveProfileMigrationTest(unittest.TestCase):
    def database(self, package, kind, primary_key):
        schema_path = APP / "schemas" / f"todoc.cochlear.remoteapp.database.{package}.Database{kind}" / "1.json"
        schema = json.loads(schema_path.read_text(encoding="utf-8-sig"))["database"]["entities"][0]
        table = schema["tableName"]
        columns = [field["columnName"] for field in schema["fields"]]
        source = APP / "src/main/java/todoc/cochlear/remoteapp/database" / package
        java = (source / f"Database{kind}.java").read_text(encoding="utf-8-sig")
        migration = re.search(r'\w+\.execSQL\("(ALTER TABLE [^"]+)"\)', java)
        self.assertIsNotNone(migration, "Expected explicit non-destructive v1 to v2 migration")
        connection = sqlite3.connect(":memory:")
        connection.execute(schema["createSql"].replace("${TABLE_NAME}", table))
        rows = []
        for index in range(2):
            row = tuple(None if column == "nickname" and index == 1 else f"sample-{column}-{index}" for column in columns)
            rows.append(row)
        names = ", ".join(f"`{column}`" for column in columns)
        connection.executemany(f"INSERT INTO `{table}` ({names}) VALUES ({','.join('?' for _ in columns)})", rows)
        connection.execute(migration.group(1))
        return connection, table, columns, rows, source, names

    def test_migration_keeps_every_legacy_field_and_activates_all_existing_rows(self):
        for package, kind, primary_key in CASES:
            with self.subTest(profile=kind):
                db, table, columns, rows, _, names = self.database(package, kind, primary_key)
                self.assertEqual(db.execute(f"SELECT {names} FROM `{table}` ORDER BY `{primary_key}`").fetchall(), rows)
                self.assertEqual(db.execute(f"SELECT isActive FROM `{table}` ORDER BY `{primary_key}`").fetchall(), [(1,), (1,)])
                column = next(item for item in db.execute(f"PRAGMA table_info(`{table}`)") if item[1] == "isActive")
                self.assertEqual((column[2], column[3], column[4]), ("INTEGER", 1, "1"))
                db.close()

    def test_activation_updates_only_one_flag_without_rewriting_other_fields(self):
        for package, kind, primary_key in CASES:
            with self.subTest(profile=kind):
                db, table, columns, rows, source, names = self.database(package, kind, primary_key)
                dao = (source / f"Dao{kind}.java").read_text(encoding="utf-8-sig")
                update = re.search(r'@Query\("(UPDATE [^"]+ SET isActive = :active WHERE [^"]+)"\)', dao)
                self.assertIsNotNone(update)
                key_parameter = re.search(r'WHERE \w+ = :(\w+)', update.group(1)).group(1)
                key = rows[0][columns.index(primary_key)]
                self.assertEqual(db.execute(update.group(1), {"active": False, key_parameter: key}).rowcount, 1)
                self.assertEqual(db.execute(f"SELECT isActive FROM `{table}` ORDER BY `{primary_key}`").fetchall(), [(0,), (1,)])
                self.assertEqual(db.execute(f"SELECT {names} FROM `{table}` ORDER BY `{primary_key}`").fetchall(), rows)
                self.assertEqual(db.execute(update.group(1), {"active": True, key_parameter: key}).rowcount, 1)
                self.assertEqual(db.execute(f"SELECT {names} FROM `{table}` ORDER BY `{primary_key}`").fetchall(), rows)
                self.assertEqual(db.execute(update.group(1), {"active": False, key_parameter: "missing"}).rowcount, 0)
                db.close()

    def test_new_rows_use_active_default_without_changing_primary_key_rules(self):
        for package, kind, primary_key in CASES:
            with self.subTest(profile=kind):
                db, table, columns, rows, _, names = self.database(package, kind, primary_key)
                new_row = tuple(f"new-{column}" for column in columns)
                insert = f"INSERT INTO `{table}` ({names}) VALUES ({','.join('?' for _ in columns)})"
                db.execute(insert, new_row)
                key = new_row[columns.index(primary_key)]
                self.assertEqual(db.execute(f"SELECT isActive FROM `{table}` WHERE `{primary_key}` = ?", (key,)).fetchone(), (1,))
                with self.assertRaises(sqlite3.IntegrityError):
                    db.execute(insert, new_row)
                db.close()


    def user_group_database(self):
        db, table, columns, _, source, names = self.database("users", "User", "name")
        dao = (source / "DaoUser.java").read_text(encoding="utf-8-sig")
        self.assertRegex(dao, r"@Insert\(onConflict = OnConflictStrategy\.ABORT\)\s*void insertGroupMember")
        delete = re.search(r'@Query\("(DELETE FROM EntityUser WHERE name = :name)"\)', dao).group(1)
        db.execute("DELETE FROM EntityUser")
        records = [
            ("PEK_L", "cipher-left", "left alias", "L", "Y", 1),
            ("PEK_R", "cipher-right", "right alias", "R", "N", 0),
        ]
        all_names = names + ", `isActive`"
        insert = f"INSERT INTO EntityUser ({all_names}) VALUES (?,?,?,?,?,?)"
        db.executemany(insert, records)
        db.commit()
        return db, insert, delete, all_names, records

    def test_group_rename_keeps_both_distinct_keys_aliases_and_active_flags(self):
        db, insert, delete, all_names, records = self.user_group_database()
        with db:
            for record in records:
                self.assertEqual(db.execute(delete, {"name": record[0]}).rowcount, 1)
            for record in records:
                db.execute(insert, (record[0].replace("PEK_", "NEW_"),) + record[1:])
        actual = db.execute(f"SELECT {all_names} FROM EntityUser ORDER BY name").fetchall()
        self.assertEqual(actual, [(record[0].replace("PEK_", "NEW_"),) + record[1:] for record in records])
        db.close()

    def test_second_ear_save_failure_rolls_back_first_ear_and_all_original_rows(self):
        db, insert, delete, all_names, records = self.user_group_database()
        db.execute("CREATE TRIGGER reject_right BEFORE INSERT ON EntityUser WHEN NEW.ear = 'R' BEGIN SELECT RAISE(ABORT, 'injected second-ear failure'); END")
        db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with db:
                for record in records:
                    db.execute(delete, {"name": record[0]})
                for record in records:
                    db.execute(insert, (record[0].replace("PEK_", "NEW_"),) + record[1:])
        self.assertEqual(db.execute(f"SELECT {all_names} FROM EntityUser ORDER BY name").fetchall(), records)
        db.close()

    def test_second_ear_delete_failure_restores_first_ear_too(self):
        db, _, delete, all_names, records = self.user_group_database()
        db.execute("CREATE TRIGGER reject_right_delete BEFORE DELETE ON EntityUser WHEN OLD.ear = 'R' BEGIN SELECT RAISE(ABORT, 'injected second-ear delete failure'); END")
        db.commit()
        with self.assertRaises(sqlite3.IntegrityError):
            with db:
                for record in records:
                    db.execute(delete, {"name": record[0]})
        self.assertEqual(db.execute(f"SELECT {all_names} FROM EntityUser ORDER BY name").fetchall(), records)
        db.close()

    def test_existing_target_primary_key_conflict_does_not_overwrite_either_group(self):
        db, insert, delete, all_names, records = self.user_group_database()
        target = ("NEW_R", "existing-cipher", "existing alias", "R", "N", 1)
        db.execute(insert, target)
        db.commit()
        before = db.execute(f"SELECT {all_names} FROM EntityUser ORDER BY name").fetchall()
        with self.assertRaises(sqlite3.IntegrityError):
            with db:
                for record in records:
                    db.execute(delete, {"name": record[0]})
                for record in records:
                    db.execute(insert, (record[0].replace("PEK_", "NEW_"),) + record[1:])
        self.assertEqual(db.execute(f"SELECT {all_names} FROM EntityUser ORDER BY name").fetchall(), before)
        db.close()

if __name__ == "__main__":
    unittest.main(verbosity=2)
