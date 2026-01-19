import { fetchApi } from "@/config/fetch-api";

export type AddToCartEventRequest = {
    id: number;
    name: string;
    price: number;
    timestamp: number;
}

export function kurrentdbAddToCart(request: AddToCartEventRequest) {
    return fetchApi("/kurrentdb/events/add-to-cart", {
        method: "POST",
        data: request,
    })
}