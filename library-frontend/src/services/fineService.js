import { FINES_API_BASE_URL } from "../config/api.js";

export const getAllFines = async () => {
    const response = await fetch(FINES_API_BASE_URL + "/fines");
    return await response.json();
}

export const payFine = async (fineId) => {
    const response = await fetch(FINES_API_BASE_URL + "/fines/" + fineId + "/pay", {
        method: "PATCH",
    });
    return await response.json();
}