import { fetchApi } from "@/config/fetch-api";

export function minioDownload(objectName: string) {
    return fetchApi("minio/download", {
        method: "GET",
        params: { objectName },
        responseType: "stream",
        
    })
}