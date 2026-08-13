import { API_BASE_URL } from "../config/api.js";

export const getAllLoans = async () => {
    const response = await fetch(API_BASE_URL + "/loans");
    return await response.json();
}