# 🟨 Yellows 🟨 Overdriven
An unmissable addition to everyone's favorite `yellows` for just 99.99₴
It contains such plugins
* base64
  * `overdriven.base64.decode_bytes` in: `in` (bytes), out: `out` (string)
  * `overdriven.base64.encode_bytes` in: `in` (string), out: `out` (bytes)
* files
  * `overdriven.files.list_files` in: `path` (string), out: `out` (string array)
  * `overdriven.files.read_file_bytes` in: `path` (string), out: `out` (bytes)
  * `overdriven.files.write_file_bytes` in: `path` (string), `data` (bytes)
* http
  * `overdriven.http.http_request` in: `method` (string), `url` (string), `headers` (object, optional), `body` (string, optional), out: `code` (int), `body` (string)
* json
  * `overdriven.json.ctx_to_json_string` in: `in` (object/array), out: `out` (string)
  * `overdriven.json.ctx_from_json_string` in: `in` (string), out: `out` (object/array)
* string
  * `overdriven.string.string_to_bytes` in: `in` (string), out: `out` (bytes)
  * `overdriven.string.string_from_bytes` in: `in` (bytes), out: `out` (string)
* utils
  * `overdriven.utils.for_loop` in: `in` (object/array), out: `value`, `key` (for object)
  * `overdriven.utils.inline_for_loop` in: `in` (object/array), out: `value`, `key` (for object)