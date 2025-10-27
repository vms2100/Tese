import psycopg2
from faker import Faker
import random

fake = Faker()

# Conexão com PostgreSQL
conn = psycopg2.connect(
    dbname="DB_Library_Heavy",
    user="x",
    password="x",
    host="localhost",
    port="5432"
)
cursor = conn.cursor()

def insert_libraries(n=500000):
    for _ in range(n):
        cursor.execute("""
            INSERT INTO "Library" ("Name", "Description", "Location")
            VALUES (%s, %s, %s)
        """, (fake.text(max_nb_chars=2000), fake.text(max_nb_chars=50000), fake.text(max_nb_chars=2000)))

def insert_authors(n=1000000):
    for _ in range(n):
        cursor.execute("""
            INSERT INTO "Author" ("Name", "Age", "Biography")
            VALUES (%s, %s, %s)
        """, (fake.text(max_nb_chars=2000), random.randint(25, 85), fake.text(max_nb_chars=20000)))

def insert_books(n=1500000):
    for _ in range(n):
        cursor.execute("""
            INSERT INTO "Book" ("ISBN", "Title", "Sinopse")
            VALUES (%s, %s, %s)
        """, (fake.isbn13(), fake.text(max_nb_chars=1000), fake.text(max_nb_chars=15000)))

def insert_clients(n=100000):
    for _ in range(n):
        cursor.execute("""
            INSERT INTO "Clients" ("Name", "Phone", "Email", "Adress")
            VALUES (%s, %s, %s, %s)
        """, (fake.text(max_nb_chars=2000), fake.phone_number(), fake.text(max_nb_chars=10000), fake.text(max_nb_chars=10000)))

def insert_book_authors(qtd=200000):
    cursor.execute('SELECT "ID_Book" FROM "Book"')
    book_ids = [row[0] for row in cursor.fetchall()]
    cursor.execute('SELECT "ID_Author" FROM "Author"')
    author_ids = [row[0] for row in cursor.fetchall()]
    
    for _ in range(qtd):
        cursor.execute("""
            INSERT INTO "Book_Author" ("ID_Book", "ID_Author")
            VALUES (%s, %s)
            ON CONFLICT DO NOTHING
        """, (random.choice(book_ids), random.choice(author_ids)))


def insert_stock(qtd=200000):
    cursor.execute('SELECT "ID_Library" FROM "Library"')
    library_ids = [row[0] for row in cursor.fetchall()]
    
    cursor.execute('SELECT "ID_Book" FROM "Book"')
    book_ids = [row[0] for row in cursor.fetchall()]
    
    for _ in range(qtd):
        qty_value = random.randint(1, 100) 
        cursor.execute("""
            INSERT INTO "Stock" ("ID_Library", "ID_Book", "Qty")
            VALUES (%s, %s, %s)
            ON CONFLICT DO NOTHING
        """, (
            random.choice(library_ids),
            random.choice(book_ids),
            qty_value
        ))

def insert_reservations(qtd=20000):
    cursor.execute('SELECT "ID_Library" FROM "Library"')
    lib_ids = [row[0] for row in cursor.fetchall()]
    cursor.execute('SELECT "ID_Book" FROM "Book"')
    book_ids = [row[0] for row in cursor.fetchall()]
    cursor.execute('SELECT "ID_Clients" FROM "Clients"')
    client_ids = [row[0] for row in cursor.fetchall()]
    
    for _ in range(qtd):
        cursor.execute("""
            INSERT INTO "Reservations" ("ID_Library", "ID_Book", "ID_Clients")
            VALUES (%s, %s, %s)
        """, (
            random.choice(lib_ids),
            random.choice(book_ids),
            random.choice(client_ids)
        ))
        
# Inserção em ordem adequada
insert_libraries()
insert_authors()
insert_books()
insert_clients()
insert_book_authors()
insert_stock()
insert_reservations()

# Confirma as alterações
conn.commit()

# Fecha conexão
cursor.close()
conn.close()
