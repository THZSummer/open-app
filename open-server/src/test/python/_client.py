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
TEST_APP_ID = _m.TEST_APP_ID
INTERNAL_APP_ID = int(db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))
