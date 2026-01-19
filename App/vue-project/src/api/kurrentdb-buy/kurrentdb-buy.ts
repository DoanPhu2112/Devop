import { fetchApi } from "@/config/fetch-api";

export type BuyItemEventRequest = {
    id: number;
    name: string;
    price: number;
    timestamp: number;
}

export function kurrentdbBuyItem(request: BuyItemEventRequest) {
    return fetchApi("/kurrentdb/events/buy", {
        method: "POST",
        data: request,
    })
}

export type BuyAllItemsEventRequest = {
    items: Array<{
        id: number;
        name: string;
        price: number;
    }>;
    timestamp: number;
}

export function kurrentdbBuyAllItems(request: BuyAllItemsEventRequest) {
    return fetchApi("/kurrentdb/events/buy-all", {
        method: "POST",
        data: request,
    })
}
