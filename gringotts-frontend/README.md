# Gringotts Frontend (local dev)

Run the dev server and point to your backend.

1. Install & run
```powershell
cd gringotts-frontend
npm install
npm run dev
```

2. API base URL
- Set `VITE_API_BASE_URL` environment variable if your backend does not run on `http://localhost:8050`.
  Example (PowerShell):
  ```powershell
  $env:VITE_API_BASE_URL='http://localhost:8050'
  npm run dev
  ```

3. Notes
- The frontend attaches a JWT from `localStorage.token` to requests.
- If you see `Network Error` or `Connection refused` in the console, ensure the backend is running on the configured port.

