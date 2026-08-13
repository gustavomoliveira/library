import { API_BASE_URL } from "../config/api.js";

export const getAllBooks = async () => {
    const response = await fetch(API_BASE_URL + "/books");
    return await response.json();
}

export const getBookById = async (bookId) => {
    const response = await fetch(API_BASE_URL + "/books/" + bookId);
    return await response.json();
}