# Gringotts Banking (local dev)

Quick notes to run the backend and frontend locally and troubleshoot the 'Network Error / Connection refused' you saw.

## Backend (Spring Boot)
1. Build and run (Windows PowerShell):

```powershell
cd C:\Users\2430171\Downloads\Projects\banking
# Build (uses bundled mvnw)
.\mvnw.cmd -DskipTests package
# Run the generated jar (replace the jar name if different)
java -jar target\gringotts-banking-0.0.1-SNAPSHOT.jar
```

2. Config & ports
- The backend reads `src/main/resources/application.properties`.
- By default it includes `server.port=8050` and `app.frontend.origin=http://localhost:5173`.
- To run on a different port or change allowed origin, update `application.properties` or pass `-Dserver.port=...` on the java command line.

3. Troubleshooting connection refused / network errors
- "Connection refused" means the backend process is not listening on port 8050 (or firewall blocks it).
- Verify a process is listening:

```powershell
# list listeners on Windows
netstat -ano | findstr 8050
```

- If nothing shows, the backend is not running. Start it (see step 1).
- Check backend logs (the java process stdout) for errors at startup.

4. CORS
- The backend is configured to accept requests from `http://localhost:5173` via `app.frontend.origin`.
- If you change the frontend port, update `app.frontend.origin` and restart the backend.

5. Uploads folder
- The backend will write uploaded profile images to the `uploads/` folder under the project working directory. The controller ensures the folder exists at runtime, but creating it manually avoids permission surprises.

## Frontend (Vite + React)
1. Run dev server:

```powershell
cd C:\Users\2430171\Downloads\Projects\banking\gringotts-frontend
npm install
npm run dev
```

2. Base API URL and token
- The frontend `src/api.js` uses `VITE_API_BASE_URL` (env) or default `http://localhost:8050`.
- Login stores the JWT in `localStorage.token`. The frontend attaches the token automatically to API requests.
- If you get 401/403: ensure you logged in, `localStorage.getItem('token')` exists, and token hasn't expired.

## Quick tests
- After starting backend and frontend, open http://localhost:5173 and login.
- Inspect DevTools > Network to verify requests to http://localhost:8050 succeed.

---
If you want, I can add scripts to automatically create the uploads folder or show a small health endpoint; tell me which and I'll add them.

