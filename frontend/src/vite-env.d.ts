/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Базовый URL API без завершающего слэша, например https://api.onrender.com */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
