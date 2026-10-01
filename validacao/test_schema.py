"""Business invariant checks against the actual SQL shipped in Db.java.
Run: python validacao/test_schema.py
No Android SDK required. This does not test Android UI or Db.restore().
"""
import re, sqlite3, unittest
from pathlib import Path
SQL = re.findall(r'd\.execSQL\("([^"\n]+)"\)',
    (Path(__file__).resolve().parents[1]/'app/src/main/java/br/com/literariamente/biblioapp/Db.java').read_text())
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
    if len(SQL)!=5:raise RuntimeError('Expected five schema statements')
    unittest.main(verbosity=2)
