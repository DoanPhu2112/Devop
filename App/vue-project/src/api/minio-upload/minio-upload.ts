import { fetchApi } from "@/config/fetch-api";

export function minioUpload(file: FormData) {
    return fetchApi("/minio/upload", {
        method: "POST",
        headers: {
            "Content-Type": "multipart/form-data",
        },
        data: { file },
    })
}