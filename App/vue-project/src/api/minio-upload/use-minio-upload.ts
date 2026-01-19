import { useMutation } from "@tanstack/vue-query";
import { minioUpload } from "./minio-upload";

export function useMinioUpload() {
    return useMutation({
        mutationFn: (file: FormData) => {
            return minioUpload(file);
        }
    });
}