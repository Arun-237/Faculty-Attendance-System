# Backend configuration

The backend uses the MySQL configuration in `src/main/resources/application.properties` and the default `local` profile for local development. Keep `src/main/resources/application-local.properties` on your machine only; it is ignored by Git. Copy `src/main/resources/application-local.properties.example` to that filename and enter your local MySQL password and optional administrator/HOD seed passwords there.

The seeder creates administrator, HOD, or faculty accounts only when the corresponding password property is non-empty. Passwords are BCrypt-encoded before being stored, and existing accounts are not overwritten.

For a frontend hosted outside the local Vite setup, edit `app.cors.allowed-origins` in `application.properties` and the API URL in `frontend/src/App.jsx`.

The integration tests are intentionally gated to the dedicated `fas_verify_test` schema and use `create-drop`. Run them only against that schema by setting `fas.test.db.url` to its MySQL URL.
