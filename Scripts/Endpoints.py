import random
from faker import Faker

FORMATS = ["json", "csv"]

class EndpointManager:
    def __init__(self, seed):
        self.seed = seed
        self.endpoints_list = [
            {"path": "/Books/", "id": "Simple", "fn": self._Books},
            {"path": "/Authors/", "id": "Simple", "fn": self._Authors},
            {"path": "/Library/", "id": "Simple", "fn": self._Library},
            {"path": "/Clients/", "id": "Simple", "fn": self._Clients},
            {"path": "/Reservations/", "id": "Simple", "fn": self._Reservations},
            {"path": "/Books/", "id": "Book", "fn": self._Books_AuthorName},
            {"path": "/Books/", "id": "Book", "fn": self._Books_AuthorID},
            {"path": "/Books/", "id": "Book", "fn": self._Books_BookName},
            {"path": "/Books/", "id": "Book", "fn": self._Books_BookID},
            {"path": "/Reservations/", "id": "Reservation", "fn": self._Reservations_ID_Reservation},
            {"path": "/Reservations/", "id": "Reservation", "fn": self._Reservations_ClientName},
            {"path": "/Reservations/", "id": "Reservation", "fn": self._Reservations_LibraryName},
            {"path": "/Reservations/", "id": "Reservation", "fn": self._Reservations_BookTitle},
            {"path": "/Stocks/", "id": "Stock", "fn": self._Stocks_BookTitle},
            {"path": "/Stocks/", "id": "Stock", "fn": self._Stocks_Library},
        ]

    def get_random_endpoint(self, index):
        local_random = random.Random(self.seed + index)
        return local_random.choice(self.endpoints_list)

    def generate_params(self, endpoint_data, index):
        local_random = random.Random(self.seed + index)
        local_faker = Faker()
        local_faker.seed_instance(self.seed + index)

        all_params = endpoint_data["fn"](local_faker, local_random)
        chosen_key = local_random.choice(list(all_params.keys()))
        chosen_value = all_params[chosen_key]
        chosen_format = local_random.choice(FORMATS)

        return {
            chosen_key: chosen_value,
            "format": chosen_format
        }, chosen_key, endpoint_data["id"], endpoint_data["path"]

    def generate_output_string(self, index):
        endpoint_data = self.get_random_endpoint(index)
        params, main_key, endpoint_id, path = self.generate_params(endpoint_data, index)
        format_type = params["format"]
        main_value = params[main_key]
    
        # Formatação por tipo de endpoint
        if endpoint_id == "Simple":
            return f"{path}{format_type}/?{main_key}={main_value}"
        elif endpoint_id == "Book":
            return f"{path}{format_type}/{main_key}/{main_value}/"
        elif endpoint_id == "Reservation":
            return f"{path}{format_type}/{main_key}/{main_value}/Info"
        elif endpoint_id == "Stock":
            return f"{path}{format_type}/{main_key}/{main_value}"

    # Métodos de cada endpoint (sem alteração)
    def _Books(self, f, r): return {"ID_Book": r.randint(1,1500000), "ISBN": f.isbn13(), "Title": f.sentence(nb_words=4).rstrip("."), "Sinopse": f.paragraph(nb_sentences=1).rstrip(".")}
    def _Authors(self, f, r): return {"ID_Authors": r.randint(1,100000), "Name": f.sentence(nb_words=4).rstrip("."), "age": r.randint(0, 100), "Bibliography": f.sentence(nb_words=4).rstrip(".")}
    def _Library(self, f, r): return {"ID_Library": r.randint(1, 500000), "Name": f.sentence(nb_words=4).rstrip("."), "Description": f.sentence(nb_words=4).rstrip("."), "Location": f.sentence(nb_words=4).rstrip(".")}
    def _Clients(self, f, r): return {"ID_Client": r.randint(1, 100000), "Name": f.sentence(nb_words=4).rstrip("."), "Phone": f.phone_number(), "Email": f.email(), "Adress": f.address().replace("\n", " ")}
    def _Reservations(self, f, r): return {"ID_Reservations": r.randint(1, 20000), "ID_Library": r.randint(1, 500000), "ID_Book": r.randint(1, 1500000), "ID_Clients": r.randint(1, 100000)}
    def _Books_AuthorName(self, f, r): return {"AuthorName": f.sentence(nb_words=4).rstrip(".")}
    def _Books_AuthorID(self, f, r): return {"AuthorID": r.randint(1, 100000)}
    def _Books_BookName(self, f, r): return {"BookName": f.sentence(nb_words=4).rstrip(".")}
    def _Books_BookID(self, f, r): return {"BookID": r.randint(1, 1500000)}
    def _Reservations_ID_Reservation(self, f, r): return {"ID_Reservation": r.randint(1, 20000)}
    def _Reservations_ClientName(self, f, r): return {"client": f.sentence(nb_words=4).rstrip(".")}
    def _Reservations_LibraryName(self, f, r): return {"Library": f.sentence(nb_words=4).rstrip(".")}
    def _Reservations_BookTitle(self, f, r): return {"Book": f.sentence(nb_words=4).rstrip(".")}
    def _Stocks_BookTitle(self, f, r): return {"Book": f.sentence(nb_words=4).rstrip(".")}
    def _Stocks_Library(self, f, r): return {"Library": f.sentence(nb_words=4).rstrip(".")}
