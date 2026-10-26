# Project Setup Instructions

This repository contains the frontend and backend of the project.

## Prerequisites

Install the following tools before running the project:

- **Node.js and npm** — for the frontend
- **Java JDK** — for the backend
- **Maven** — for building and running the backend, unless a Maven Wrapper is included


## Project Structure

```text
project/
├── frontend/
└── backend/
    ├── src/
    ├── pom.xml
    └── Dockerfile
```

## 1. Configure the Backend

1. Open a terminal in the `backend` directory.
2. Check `pom.xml` to confirm the required Java version and framework.
3. The API listens on port `8080` by default. Set `PORT` to change the port. CORS is enabled for all origins, methods, and headers.
4. Do not put passwords, API keys, or production credentials in this repository.

The polygon analysis endpoint is `POST /api/polygon/analyze`. It accepts a JSON array of points, each with numeric `x` and `y` values, and returns the polygon analysis as JSON.

### Run the backend


```bash
mvn spring-boot:run
```



## 2. Configure and Run the Frontend

1. Open another terminal in the `frontend` directory.
2. Install dependencies:

```bash
npm install
```

3. Copy `.env.example` to `.env` if you need to configure the API. The default `/api` URL uses the Vite proxy to reach `http://localhost:8080`; set `BACKEND_URL` if the local backend uses another address. For a separately deployed frontend, set `VITE_API_BASE_URL` to the backend origin plus `/api` (for example, `https://api.example.com/api`).
4. Start the development server if `package.json` defines the `dev` script:

```bash
npm run dev
```

5. Open the local URL printed in the terminal.

If the project uses a different start script, check the `scripts` section of `frontend/package.json`.





## Troubleshooting

- **Missing dependencies:** Confirm that Node.js/npm, Java, and Maven are installed.
- **Database connection errors:** Check that the database is running and the connection settings are correct.
- **Missing environment variables:** Create the required local environment configuration using the variable names expected by the application. Do not share secret values.
- **Frontend cannot reach backend:** Verify the backend URL, port, and any required CORS configuration.
