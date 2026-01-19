import { BuyItemEventRequest, BuyAllItemsEventRequest, kurrentdbBuyItem, kurrentdbBuyAllItems } from "./kurrentdb-buy";
import { useMutation } from "@tanstack/vue-query";

export function useKurrentdbBuyItem() {
    return useMutation({
        mutationFn: (req: BuyItemEventRequest) => {
            return kurrentdbBuyItem(req);
        }
    });
}

export function useKurrentdbBuyAllItems() {
    return useMutation({
        mutationFn: (req: BuyAllItemsEventRequest) => {
            return kurrentdbBuyAllItems(req);
        }
    });
}
