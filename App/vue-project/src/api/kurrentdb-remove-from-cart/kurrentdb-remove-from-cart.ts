import { fetchApi } from "@/config/fetch-api";

export type RemoveFromCartEventRequest = {
    id: number;
    timestamp: number;
}

export function kurrentdbRemoveFromCart(request: RemoveFromCartEventRequest) {
    return fetchApi("/kurrentdb/events/remove-from-cart", {
        method: "POST",
        data: request,
    })
}
