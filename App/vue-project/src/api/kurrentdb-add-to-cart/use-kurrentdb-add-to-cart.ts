import { kurrentdbAddToCart, type AddToCartEventRequest } from "./kurrentdb-add-to-cart";
import { useMutation } from "@tanstack/vue-query";

export function useKurrentdbAddToCart() {
    return useMutation({
        mutationFn: (req: AddToCartEventRequest) => {
            return kurrentdbAddToCart(req);
        }
    });
}