import { RemoveFromCartEventRequest, kurrentdbRemoveFromCart } from "./kurrentdb-remove-from-cart";
import { useMutation } from "@tanstack/vue-query";

export function useKurrentdbRemoveFromCart() {
    return useMutation({
        mutationFn: (req: RemoveFromCartEventRequest) => {
            return kurrentdbRemoveFromCart(req);
        }
    });
}
