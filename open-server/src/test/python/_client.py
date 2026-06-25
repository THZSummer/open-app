import os
import importlib.util

_spec = importlib.util.spec_from_file_location(
    "inspect_client",
    os.path.join(os.path.dirname(__file__), "inspect", "client.py")
)
_m = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_m)
api = _m.api
db = _m.db
db_val = _m.db_val
