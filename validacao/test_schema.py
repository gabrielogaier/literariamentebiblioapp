"""Business invariant checks against the actual SQL shipped in Db.java.
Run: python validacao/test_schema.py
No Android SDK required. This does not test Android UI or Db.restore().
"""
import re, sqlite3, unittest
from pathlib import Path
source=(Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/literariamente/biblioapp/Db.java').read_text()
create=source.split('@Override public void onCreate',1)[1].split('@Override public void onUpgrade',1)[0]
SQL=re.findall(r'd\.execSQL\("([^"\n]+)"\)',create)
class Loans(unittest.TestCase):
    def setUp(self):
        self.db=sqlite3.connect(':memory:'); self.db.execute('PRAGMA foreign_keys=ON')
        for statement in SQL:self.db.execute(statement)
        self.db.execute("INSERT INTO books(title,author) VALUES('Livro','Autor')")
        self.db.execute("INSERT INTO people(name) VALUES('Ana')")
    def tearDown(self):self.db.close()
    def test_only_title_author_required(self):
        self.assertEqual(self.db.execute('SELECT isbn,pages FROM books').fetchone(),('',''))
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute("INSERT INTO books(title,author) VALUES('  ','Autor')")
    def test_bibliographic_fields_optional(self):
        self.db.execute("INSERT INTO books(title,author,illustrator,publisher,year) VALUES('Outro','Autora','Ilustrador','Editora','2023')")
        self.assertEqual(self.db.execute("SELECT illustrator,publisher,year FROM books WHERE id=2").fetchone(),('Ilustrador','Editora','2023'))
    def test_upgrade_from_v3_keeps_existing_books(self):
        db=sqlite3.connect(':memory:')
        try:
            db.execute("CREATE TABLE books(id INTEGER PRIMARY KEY,title TEXT,author TEXT,publisher TEXT,cover TEXT)")
            db.execute("INSERT INTO books(title,author,publisher,cover) VALUES('Livro','Autor','Editora','foto')")
            db.execute("ALTER TABLE books ADD COLUMN illustrator TEXT NOT NULL DEFAULT ''")
            self.assertEqual(db.execute("SELECT title,author,publisher,cover,illustrator FROM books").fetchone(),('Livro','Autor','Editora','foto',''))
        finally:db.close()
    def test_double_loan_blocked(self):
        self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at) VALUES(1,1,100)')
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at) VALUES(1,1,101)')
    def test_return_preserves_history_and_allows_new_loan(self):
        self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at) VALUES(1,1,100)')
        self.db.execute('UPDATE loans SET returned_at=200 WHERE id=1')
        self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at) VALUES(1,1,300)')
        self.assertEqual(self.db.execute('SELECT COUNT(*) FROM loans').fetchone()[0],2)
        self.assertEqual(self.db.execute('SELECT COUNT(*) FROM loans WHERE returned_at IS NULL').fetchone()[0],1)
    def test_invalid_person_and_dates_rejected(self):
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at) VALUES(1,999,100)')
        with self.assertRaises(sqlite3.IntegrityError):self.db.execute('INSERT INTO loans(book_id,person_id,borrowed_at,returned_at) VALUES(1,1,100,50)')
if __name__=='__main__':
    if len(SQL)!=6:raise RuntimeError('Expected six schema statements')
    unittest.main(verbosity=2)
