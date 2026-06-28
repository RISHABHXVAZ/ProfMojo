import axios from "axios";

export const api = axios.create({
  // Hardcode the deployed render URL directly here to bypass Vercel env bugs
  baseURL: "https://profmojo-backend.onrender.com/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;