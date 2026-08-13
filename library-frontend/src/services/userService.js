import { API_BASE_URL } from "../config/api.js";

export const getAllUsers = async () => {
    const response = await fetch(API_BASE_URL + "/users");
    return await response.json();
}

export const getUserById = async (userId) => {
    const response = await fetch(API_BASE_URL + "/users/" + userId);
    return await response.json();
}