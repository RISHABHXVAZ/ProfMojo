import axios from "axios";

export const api = axios.create({
  // Checks for the cloud variable first, defaults to localhost if missing
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Adding this back to match your original export structure
export default api;